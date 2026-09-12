-- 升级豆包(火山引擎)语音合成大模型2.0：双向流式接口 + 新控制台 X-Api-Key 鉴权 + seed-tts-2.0 资源

-- 1. 升级供应器字段定义（前端后台可配置项：ws_url/api_key/resource_id/speaker/model/format/speech_rate/loudness_rate）
delete from `ai_model_provider` where id = 'SYSTEM_TTS_HSDSTTS';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_TTS_HSDSTTS', 'TTS', 'huoshan_double_stream', '豆包语音合成2.0(双向流式)', '[{"key":"ws_url","label":"WebSocket地址","type":"string"},{"key":"api_key","label":"API-Key(X-Api-Key)","type":"string"},{"key":"resource_id","label":"模型资源ID","type":"string"},{"key":"speaker","label":"默认音色","type":"string"},{"key":"model","label":"模型版本(复刻音色时填,如seed-tts-2.0-standard)","type":"string"},{"key":"format","label":"音频格式(pcm/mp3/ogg_opus)","type":"string"},{"key":"speech_rate","label":"语速(-50~100,越大越快)","type":"number"},{"key":"loudness_rate","label":"音量(-50~100,越大越响)","type":"number"}]', 13, 1, NOW(), 1, NOW());

-- 2. 升级运行时配置（经 /config/agent-models 下发到 xiaozhi-server）
delete from `ai_model_config` where id = 'TTS_HuoshanDoubleStreamTTS';
INSERT INTO `ai_model_config` VALUES ('TTS_HuoshanDoubleStreamTTS', 'TTS', 'HuoshanDoubleStreamTTS', '豆包语音合成2.0(双向流式)', 0, 1, '{"type": "huoshan_double_stream", "ws_url": "wss://openspeech.bytedance.com/api/v3/tts/bidirection", "api_key": "你的火山引擎新控制台API-Key", "resource_id": "seed-tts-2.0", "speaker": "zh_female_gaolengyujie_uranus_bigtts", "format": "pcm", "speech_rate": 0, "loudness_rate": 0, "output_dir": "tmp/"}', NULL, NULL, 16, NULL, NULL, NULL, NULL);

-- 3. 升级配置说明
UPDATE `ai_model_config` SET
`doc_link` = 'https://www.volcengine.com/docs/6561/1257544',
`remark` = '豆包语音合成大模型2.0 双向流式配置说明：
1. 新控制台API-Key：https://console.volcengine.com/speech/new/setting/apikeys?projectName=default
2. 2.0音色库：https://console.volcengine.com/speech/new/voices?projectName=default
3. resource_id 固定为 seed-tts-2.0(大模型2.0)；复刻音色为 seed-icl-2.0
4. 流式推荐 format=pcm；下行采样率由服务端握手决定(默认24000)，无需配置' WHERE `id` = 'TTS_HuoshanDoubleStreamTTS';

-- 4. 升级为豆包语音合成大模型2.0音色（*_uranus_bigtts，seed-tts-2.0 对应音色）
delete from `ai_tts_voice` where tts_model_id = 'TTS_HuoshanDoubleStreamTTS';
INSERT INTO `ai_tts_voice` VALUES ('TTS_HuoshanDoubleStreamTTS_0001', 'TTS_HuoshanDoubleStreamTTS', '爽快思思/Skye2.0', 'zh_female_shuangkuaisisi_uranus_bigtts', '中文、英文', NULL, NULL, 1, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0002', 'TTS_HuoshanDoubleStreamTTS', '温暖阿虎/Alvin2.0', 'zh_male_wennuanahu_uranus_bigtts', '中文、英文', NULL, NULL, 2, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0003', 'TTS_HuoshanDoubleStreamTTS', '少年梓辛/Brayan2.0', 'zh_male_shaonianzixin_uranus_bigtts', '中文、英文', NULL, NULL, 3, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0004', 'TTS_HuoshanDoubleStreamTTS', '邻家女孩2.0', 'zh_female_linjianvhai_uranus_bigtts', '中文', NULL, NULL, 4, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0005', 'TTS_HuoshanDoubleStreamTTS', '渊博小叔2.0', 'zh_male_yuanboxiaoshu_uranus_bigtts', '中文', NULL, NULL, 5, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0006', 'TTS_HuoshanDoubleStreamTTS', '阳光青年2.0', 'zh_male_yangguangqingnian_uranus_bigtts', '中文', NULL, NULL, 6, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0007', 'TTS_HuoshanDoubleStreamTTS', '京腔侃爷/Harmony2.0', 'zh_male_jingqiangkanye_uranus_bigtts', '中文、英文', NULL, NULL, 7, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0008', 'TTS_HuoshanDoubleStreamTTS', '湾湾小何2.0', 'zh_female_wanwanxiaohe_uranus_bigtts', '中文', NULL, NULL, 8, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0009', 'TTS_HuoshanDoubleStreamTTS', '湾区大叔2.0', 'zh_female_wanqudashu_uranus_bigtts', '中文', NULL, NULL, 9, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0010', 'TTS_HuoshanDoubleStreamTTS', '呆萌川妹2.0', 'zh_female_daimengchuanmei_uranus_bigtts', '中文', NULL, NULL, 10, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0011', 'TTS_HuoshanDoubleStreamTTS', '广州德哥2.0', 'zh_male_guozhoudege_uranus_bigtts', '中文', NULL, NULL, 11, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0012', 'TTS_HuoshanDoubleStreamTTS', '北京小爷2.0', 'zh_male_beijingxiaoye_uranus_bigtts', '中文', NULL, NULL, 12, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0013', 'TTS_HuoshanDoubleStreamTTS', '浩宇小哥2.0', 'zh_male_haoyuxiaoge_uranus_bigtts', '中文', NULL, NULL, 13, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0014', 'TTS_HuoshanDoubleStreamTTS', '广西远舟2.0', 'zh_male_guangxiyuanzhou_uranus_bigtts', '中文', NULL, NULL, 14, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0015', 'TTS_HuoshanDoubleStreamTTS', '妹坨洁儿2.0', 'zh_female_meituojieer_uranus_bigtts', '中文', NULL, NULL, 15, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0016', 'TTS_HuoshanDoubleStreamTTS', '豫州子轩2.0', 'zh_male_yuzhouzixuan_uranus_bigtts', '中文', NULL, NULL, 16, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0017', 'TTS_HuoshanDoubleStreamTTS', '高冷御姐2.0', 'zh_female_gaolengyujie_uranus_bigtts', '中文', NULL, NULL, 17, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0018', 'TTS_HuoshanDoubleStreamTTS', '傲娇霸总2.0', 'zh_male_aojiaobazong_uranus_bigtts', '中文', NULL, NULL, 18, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0019', 'TTS_HuoshanDoubleStreamTTS', '魅力女友2.0', 'zh_female_meilinvyou_uranus_bigtts', '中文', NULL, NULL, 19, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0020', 'TTS_HuoshanDoubleStreamTTS', '深夜播客2.0', 'zh_male_shenyeboke_uranus_bigtts', '中文', NULL, NULL, 20, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0021', 'TTS_HuoshanDoubleStreamTTS', '柔美女友2.0', 'zh_female_sajiaonvyou_uranus_bigtts', '中文', NULL, NULL, 21, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0022', 'TTS_HuoshanDoubleStreamTTS', '撒娇学妹2.0', 'zh_female_yuanqinvyou_uranus_bigtts', '中文', NULL, NULL, 22, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0023', 'TTS_HuoshanDoubleStreamTTS', 'かずね（和音）2.0', 'multi_male_jingqiangkanye_uranus_bigtts', '日语、西语', NULL, NULL, 23, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0024', 'TTS_HuoshanDoubleStreamTTS', 'はるこ（晴子）2.0', 'multi_female_shuangkuaisisi_uranus_bigtts', '日语、西语', NULL, NULL, 24, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0025', 'TTS_HuoshanDoubleStreamTTS', 'あけみ（朱美）2.0', 'multi_female_gaolengyujie_uranus_bigtts', '日语', NULL, NULL, 25, NULL, NULL, NULL, NULL),
('TTS_HuoshanDoubleStreamTTS_0026', 'TTS_HuoshanDoubleStreamTTS', 'ひろし（広志）2.0', 'multi_male_wanqudashu_uranus_bigtts', '日语、西语', NULL, NULL, 26, NULL, NULL, NULL, NULL);