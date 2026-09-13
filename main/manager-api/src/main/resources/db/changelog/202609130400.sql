-- OTA R2（Cloudflare R2 / S3 兼容对象存储）配置参数
-- server.ota_storage = local(本地磁盘，默认) | s3(本地 + R2 双写，下载走 R2 公开直链)
INSERT INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark) VALUES (600, 'server.ota_storage', 'local', 'string', 1, 'OTA存储方式local/s3');
INSERT INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark) VALUES (601, 'server.ota_s3_endpoint', 'null', 'string', 1, 'OTA对象存储endpoint');
INSERT INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark) VALUES (602, 'server.ota_s3_access_key_id', 'null', 'string', 1, 'OTA对象存储AccessKeyID');
INSERT INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark) VALUES (603, 'server.ota_s3_access_key_secret', 'null', 'string', 1, 'OTA对象存储AccessKeySecret');
INSERT INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark) VALUES (604, 'server.ota_s3_bucket', 'null', 'string', 1, 'OTA对象存储Bucket');
INSERT INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark) VALUES (605, 'server.ota_s3_public_domain', 'null', 'string', 1, 'OTA对象存储公开访问域名');
INSERT INTO `sys_params` (id, param_code, param_value, value_type, param_type, remark) VALUES (606, 'server.ota_s3_region', 'auto', 'string', 1, 'OTA对象存储Region');