-- 豆包流式语音识别2.0 供应器字段追加「热词词表ID」配置项
-- 通过 UPDATE 追加字段，不修改历史 changeset；幂等：已含该字段时不重复追加

UPDATE `ai_model_provider` SET `fields` = JSON_ARRAY_APPEND(
  `fields`,
  '$',
  JSON_OBJECT('key', 'boosting_table_id', 'label', '热词词表ID(控制台配置后获取)', 'type', 'string')
) WHERE `id` = 'SYSTEM_ASR_DoubaoSeedASR'
  AND NOT JSON_CONTAINS(`fields`, JSON_OBJECT('key', 'boosting_table_id'));
