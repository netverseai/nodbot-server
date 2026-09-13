import os
import uuid
import json
import gzip
import struct
import time
import websockets
from typing import Optional, Tuple, List
from config.logger import setup_logging
from core.providers.asr.base import ASRProviderBase
from core.providers.asr.dto.dto import InterfaceType

TAG = __name__
logger = setup_logging()

# 豆包流式语音识别模型2.0 (SA-UC / seedasr) 协议常量
# 与官方 docs/sauc_python/protocol.py 一致
PROTOCOL_VERSION = 0b0001

CLIENT_FULL_REQUEST = 0b0001
CLIENT_AUDIO_ONLY_REQUEST = 0b0010

NO_SEQUENCE = 0b0000
POS_SEQUENCE = 0b0001
NEG_SEQUENCE = 0b0010
NEG_WITH_SEQUENCE = 0b0011

SERVER_FULL_RESPONSE = 0b1001
SERVER_ERROR_RESPONSE = 0b1111

JSON = 0b0001
GZIP = 0b0001

# 2.0 默认资源（小时版），也支持 volc.seedasr.sauc.concurrent（并发版）
DEFAULT_RESOURCE_ID = "volc.seedasr.sauc.duration"


def parse_response(res: bytes) -> dict:
    """
    解析 SA-UC 2.0 WebSocket 响应（与官方 ResponseParser 相同）。
    header_size: 4bits, message_type/flags: 4bits, serialization/compression: 4bits
    """
    header_size = res[0] & 0x0F
    message_type = res[1] >> 4
    message_type_specific_flags = res[1] & 0x0F
    message_compression = res[2] & 0x0F

    payload = res[header_size * 4:]

    result = {}
    if message_type_specific_flags & 0x01:  # POS_SEQUENCE
        result["payload_sequence"] = struct.unpack(">i", payload[:4])[0]
        payload = payload[4:]
    if message_type_specific_flags & 0x02:
        result["is_last_package"] = True
    if message_type_specific_flags & 0x04:
        result["event"] = struct.unpack(">i", payload[:4])[0]
        payload = payload[4:]

    payload_msg = None
    if message_type == SERVER_FULL_RESPONSE:
        result["payload_size"] = struct.unpack(">I", payload[:4])[0]
        payload = payload[4:]
    elif message_type == SERVER_ERROR_RESPONSE:
        result["code"] = struct.unpack(">i", payload[:4])[0]
        result["payload_size"] = struct.unpack(">I", payload[4:8])[0]
        payload = payload[8:]

    if not payload:
        return result

    if message_compression == GZIP:
        payload = gzip.decompress(payload)
    try:
        payload_msg = json.loads(payload.decode("utf-8"))
    except Exception as e:
        logger.bind(tag=TAG).error(f"解析SA-UC响应payload失败: {e}")
        return result
    result["payload_msg"] = payload_msg
    return result


class ASRProvider(ASRProviderBase):
    def __init__(self, config: dict, delete_audio_file: bool):
        super().__init__()
        self.interface_type = InterfaceType.NON_STREAM
        self.api_key = config.get("api_key", "")
        self.app_key = config.get("app_key", "")
        self.access_key = config.get("access_key", "")
        self.resource_id = config.get("resource_id", DEFAULT_RESOURCE_ID)
        self.output_dir = config.get("output_dir", "tmp/")
        self.delete_audio_file = delete_audio_file

        self.ws_url = "wss://openspeech.bytedance.com/api/v3/sauc/bigmodel_nostream"
        self.model_name = config.get("model_name", "bigmodel")
        self.language = config.get("language", "")
        # 输入音频为解码后的裸 PCM 流，采样率 16k（与设备 opus 一致），故 format=pcm / codec=raw
        # 注意：24000 是 TTS 输出采样率，与本处 ASR 输入无关
        self.format = config.get("format", "pcm")
        self.codec = config.get("codec", "raw")
        # 数值字段统一 int 强转：config_json 可能以字符串形式下发，避免后续 /、//、* 运算抛 TypeError
        self.rate = int(config.get("sample_rate", 16000))
        self.bits = int(config.get("bits", 16))
        self.channel = int(config.get("channel", 1))
        self.seg_duration = int(config.get("seg_duration", 200))

        # request 参数
        self.enable_itn = config.get("enable_itn", True)
        self.enable_punc = config.get("enable_punc", True)
        self.enable_ddc = config.get("enable_ddc", True)
        self.show_utterances = config.get("show_utterances", True)
        self.enable_auto_lang = config.get("enable_auto_lang", False)

        os.makedirs(self.output_dir, exist_ok=True)

    def _auth_headers(self) -> dict:
        headers = {
            "X-Api-Resource-Id": self.resource_id,
            "X-Api-Request-Id": str(uuid.uuid4()),
        }
        # 新版控制台鉴权用 X-Api-Key；否则用旧版 X-Api-App-Key + X-Api-Access-Key
        if self.api_key:
            headers["X-Api-Key"] = self.api_key
        else:
            headers["X-Api-App-Key"] = self.app_key
            headers["X-Api-Access-Key"] = self.access_key
        return headers

    def _generate_header(
        self,
        message_type=CLIENT_FULL_REQUEST,
        message_type_specific_flags=POS_SEQUENCE,
    ) -> bytearray:
        header = bytearray()
        header_size = 1
        header.append((PROTOCOL_VERSION << 4) | header_size)
        header.append((message_type << 4) | message_type_specific_flags)
        header.append((JSON << 4) | GZIP)
        header.append(0x00)
        return header

    def _construct_request(self, reqid=None) -> dict:
        request = {
            "user": {"uid": f"streaming_asr_{uuid.uuid4()}"},
            "audio": {
                "language": self.language,
                "format": self.format,
                "codec": self.codec,
                "rate": self.rate,
                "bits": self.bits,
                "channel": self.channel,
            },
            "request": {
                "model_name": self.model_name,
                "enable_itn": self.enable_itn,
                "enable_punc": self.enable_punc,
                "enable_ddc": self.enable_ddc,
                "show_utterances": self.show_utterances,
                "enable_auto_lang": self.enable_auto_lang,
            },
        }
        # 指定语种时禁止同时开启自动语种
        if self.enable_auto_lang and self.language:
            request["request"]["enable_auto_lang"] = False
        return request

    async def _send_request(self, pcm_data: bytes, segment_size: int) -> Optional[str]:
        try:
            seq = 1
            headers = self._auth_headers()
            async with websockets.connect(
                self.ws_url, additional_headers=headers,
                max_size=1000000000, ping_interval=None, ping_timeout=None,
                close_timeout=10,
            ) as websocket:

                # 1. 发送完整客户端请求（gzip）
                payload_bytes = gzip.compress(
                    json.dumps(self._construct_request()).encode("utf-8")
                )
                full_client_request = self._generate_header()
                full_client_request.extend(struct.pack(">i", seq))
                full_client_request.extend(struct.pack(">I", len(payload_bytes)))
                full_client_request.extend(payload_bytes)
                await websocket.send(full_client_request)

                # 2. 流式发送音频（最后一段使用 NEG_WITH_SEQUENCE 负序号）
                for chunk, last in self.slice_data(pcm_data, segment_size):
                    if last:
                        audio_request = self._generate_header(
                            message_type=CLIENT_AUDIO_ONLY_REQUEST,
                            message_type_specific_flags=NEG_WITH_SEQUENCE,
                        )
                        seq = -seq
                    else:
                        audio_request = self._generate_header(
                            message_type=CLIENT_AUDIO_ONLY_REQUEST,
                            message_type_specific_flags=POS_SEQUENCE,
                        )
                    payload = gzip.compress(chunk)
                    audio_request.extend(struct.pack(">i", seq))
                    audio_request.extend(struct.pack(">I", len(payload)))
                    audio_request.extend(payload)
                    await websocket.send(audio_request)
                    if not last:
                        seq += 1

                # 3. 接收结果，整句识别
                texts = []
                while True:
                    res = await websocket.recv()
                    result = parse_response(res)
                    if "code" in result and result["code"] != 0:
                        logger.bind(tag=TAG).error(f"SA-UC 2.0 识别错误: {result}")
                        return None
                    pm = result.get("payload_msg") or {}
                    if isinstance(pm, dict):
                        if pm.get("code") not in (0, 1013):  # 1013 无有效语音
                            logger.bind(tag=TAG).error(f"SA-UC 2.0 识别错误: {pm}")
                            return None
                        for res_item in (pm.get("result") or []):
                            t = res_item.get("text")
                            if t:
                                texts.append(t)
                    if result.get("is_last_package"):
                        break

                text = "".join(texts).strip()
                return text if text else ""

        except Exception as e:
            logger.bind(tag=TAG).error(f"SA-UC 2.0 识别请求失败: {e}", exc_info=True)
            return None

    @staticmethod
    def slice_data(data: bytes, chunk_size: int):
        """将PCM数据按 chunk_size 切片，最后一段标记 last=True"""
        data_len = len(data)
        offset = 0
        while offset + chunk_size < data_len:
            yield data[offset: offset + chunk_size], False
            offset += chunk_size
        yield data[offset:data_len], True

    async def speech_to_text(
        self, opus_data: List[bytes], session_id: str, audio_format="opus"
    ) -> Tuple[Optional[str], Optional[str]]:
        file_path = None
        try:
            if audio_format == "pcm":
                pcm_data = opus_data
            else:
                pcm_data = self.decode_opus(opus_data)
            combined_pcm_data = b"".join(pcm_data)

            if not self.delete_audio_file:
                file_path = self.save_audio_to_file(pcm_data, session_id)

            # 单声道 16bit 采样率 size_per_sec = 1 * 2 * rate
            size_per_sec = self.channel * (self.bits // 8) * self.rate
            segment_size = int(size_per_sec * self.seg_duration / 1000)

            start_time = time.time()
            text = await self._send_request(combined_pcm_data, segment_size)
            if text:
                logger.bind(tag=TAG).debug(
                    f"SA-UC 2.0 语音识别耗时: {time.time() - start_time:.3f}s | 结果: {text}"
                )
                return text, file_path
            return "", file_path
        except Exception as e:
            logger.bind(tag=TAG).error(f"SA-UC 2.0 语音识别失败: {e}", exc_info=True)
            return "", file_path