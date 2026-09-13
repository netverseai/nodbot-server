-- OTA 固件：新增 R2 对象存储 key。
-- 仅在固件成功同步到 R2 后才写入该字段，下发时据此判断是否可走 R2 直链，
-- 避免「配置了 R2 但对象上传失败」时仍下发直链导致设备 404。
ALTER TABLE `ai_ota` ADD COLUMN `r2_object_key` VARCHAR(255) NULL COMMENT 'R2对象存储key(内容寻址：firmware/{md5}{ext})' AFTER `firmware_path`;