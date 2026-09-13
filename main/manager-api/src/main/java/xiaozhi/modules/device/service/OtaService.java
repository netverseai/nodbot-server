package xiaozhi.modules.device.service;

import java.util.Map;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.device.entity.OtaEntity;

/**
 * OTA固件管理
 */
public interface OtaService extends BaseService<OtaEntity> {
    PageData<OtaEntity> page(Map<String, Object> params);

    boolean save(OtaEntity entity);

    void update(OtaEntity entity);

    void delete(String[] ids);

    OtaEntity getLatestOta(String type);

    /**
     * 将指定固件重新同步到 R2（读本地文件上传，成功则回写 r2_object_key）。
     *
     * @param id 固件ID
     * @return 同步成功返回 true
     */
    boolean resyncR2(String id);
}