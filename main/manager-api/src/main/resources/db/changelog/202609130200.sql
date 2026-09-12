-- 为硅基流动(CosyVoice)TTS 补后台可配置项：语速(speed)、音量增益(gain)
-- 适用：已部署库（HistorySS等既有数据库），在项目启动时由 Liquibase 自动执行
-- provider.fields：追加 speed/gain 字段定义，后台「模型管理→TTS」即可编辑
UPDATE `ai_model_provider`
SET `fields` = '[{"key":"model","label":"模型","type":"string"},{"key":"voice","label":"音色","type":"string"},{"key":"output_dir","label":"输出目录","type":"string"},{"key":"access_token","label":"访问令牌","type":"string"},{"key":"response_format","label":"响应格式","type":"string"},{"key":"speed","label":"语速(0.25~4.0)","type":"number"},{"key":"gain","label":"音量增益(-10~10)","type":"number"}]',
    `update_date` = NOW()
WHERE `id` = 'SYSTEM_TTS_siliconflow';

-- config.config_json：增量并入 speed/gain 默认值(JSON_SET)，不覆盖用户已存在的自定义配置
UPDATE `ai_model_config`
SET `config_json` = JSON_SET(
        IFNULL(`config_json`, '{}'),
        '$.speed', 1.0,
        '$.gain', 0
    ),
    `update_date` = NOW()
WHERE `id` = 'TTS_CosyVoiceSiliconflow';