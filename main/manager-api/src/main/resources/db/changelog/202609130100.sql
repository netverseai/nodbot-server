-- 新增豆包流式语音识别模型2.0（SA-UC / seedasr）：bigmodel_nostream 接口 + 新控制台 X-Api-Key 鉴权 + volc.seedasr.sauc.* 资源

-- 1. 供应器字段定义（前端后台可配置项：api_key/resource_id/language/seg_duration/output_dir）
delete from `ai_model_provider` where id = 'SYSTEM_ASR_DoubaoSeedASR';
INSERT INTO `ai_model_provider` (`id`, `model_type`, `provider_code`, `name`, `fields`, `sort`, `creator`, `create_date`, `updater`, `update_date`) VALUES
('SYSTEM_ASR_DoubaoSeedASR', 'ASR', 'doubao_seedasr', '豆包流式语音识别2.0', '[{"key":"api_key","label":"API-Key(X-Api-Key)","type":"string"},{"key":"resource_id","label":"资源ID(hours:volc.seedasr.sauc.duration / concurrency:volc.seedasr.sauc.concurrent)","type":"string"},{"key":"language","label":"语种(留空自动识别)","type":"string"},{"key":"seg_duration","label":"分片时长(ms)","type":"number"},{"key":"output_dir","label":"输出目录","type":"string"}]', 8, 1, NOW(), 1, NOW());

-- 2. 运行时配置（经 /config/agent-models 下发到 xiaozhi-server）
delete from `ai_model_config` where id = 'ASR_DoubaoSeedASR';
INSERT INTO `ai_model_config` VALUES ('ASR_DoubaoSeedASR', 'ASR', 'DoubaoSeedASR', '豆包流式语音识别2.0', 0, 1, '{"type": "doubao_seedasr", "api_key": "你的火山引擎新控制台API-Key", "resource_id": "volc.seedasr.sauc.duration", "language": "", "format": "pcm", "codec": "raw", "seg_duration": 200, "output_dir": "tmp/"}', NULL, NULL, 8, NULL, NULL, NULL, NULL);

-- 3. 配置说明
UPDATE `ai_model_config` SET
`doc_link` = 'https://www.volcengine.com/docs/6561/2534861',
`remark` = '豆包流式语音识别模型2.0 配置说明：
1. 新控制台API-Key：https://console.volcengine.com/speech/new/setting/apikeys?projectName=default
2. resource_id：小时版 volc.seedasr.sauc.duration / 并发版 volc.seedasr.sauc.concurrent
3. language 留空时自动识别：中文、英文、上海话、闽南话、四川话、陕西话、粤语
4. 接口：wss://openspeech.bytedance.com/api/v3/sauc/bigmodel_nostream（流式输入返回整句，非实时）' WHERE `id` = 'ASR_DoubaoSeedASR';