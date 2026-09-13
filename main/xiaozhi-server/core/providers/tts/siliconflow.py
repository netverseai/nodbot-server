import requests
from core.providers.tts.base import TTSProviderBase


class TTSProvider(TTSProviderBase):
    def __init__(self, config, delete_audio_file):
        super().__init__(config, delete_audio_file)
        self.model = config.get("model")
        self.access_token = config.get("access_token")
        if config.get("private_voice"):
            self.voice = config.get("private_voice")
        else:
            self.voice = config.get("voice")
        self.response_format = config.get("response_format", "wav")
        self.audio_file_type = config.get("response_format", "wav")
        self.speed = config.get("speed", 1.0)
        self.gain = config.get("gain")

        self.host = "api.siliconflow.cn"
        self.api_url = f"https://{self.host}/v1/audio/speech"

    async def text_to_speak(self, text, output_file):
        request_json = {
            "model": self.model,
            "input": text,
            "voice": self.voice,
            "response_format": self.response_format,
        }
        # 依据 API 文档补齐语速/音量（此前读取了配置却未发送）
        # speed: 0.25~4.0，1.0 默认；gain: -10~10
        # 采样率无需指定：依赖后端relay统一重采样为协商下行采样率
        # 空字符串/None 视为未配置，避免 float() 抛 ValueError
        if self.speed not in (None, ""):
            request_json["speed"] = float(self.speed)
        if self.gain is not None and self.gain != "":
            request_json["gain"] = float(self.gain)
        headers = {
            "Authorization": f"Bearer {self.access_token}",
            "Content-Type": "application/json",
        }
        try:
            response = requests.request(
                "POST", self.api_url, json=request_json, headers=headers
            )
            response.raise_for_status()
            data = response.content
            if output_file:
                with open(output_file, "wb") as file_to_save:
                    file_to_save.write(data)
            else:
                return data
        except Exception as e:
            raise Exception(f"{__name__} error: {e}")
