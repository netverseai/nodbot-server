package xiaozhi.modules.device.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import xiaozhi.common.page.PageData;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.modules.device.dao.OtaDao;
import xiaozhi.modules.device.entity.OtaEntity;
import xiaozhi.modules.device.service.OtaService;
import xiaozhi.modules.device.storage.OtaStorageService;

@Service
@RequiredArgsConstructor
public class OtaServiceImpl extends BaseServiceImpl<OtaDao, OtaEntity> implements OtaService {

    private final OtaStorageService otaStorageService;

    @Override
    public PageData<OtaEntity> page(Map<String, Object> params) {
        IPage<OtaEntity> page = baseDao.selectPage(
                getPage(params, "update_date", true),
                getWrapper(params));

        return new PageData<>(page.getRecords(), page.getTotal());
    }

    private QueryWrapper<OtaEntity> getWrapper(Map<String, Object> params) {
        String firmwareName = (String) params.get("firmwareName");

        QueryWrapper<OtaEntity> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(firmwareName), "firmware_name", firmwareName);

        return wrapper;
    }

    @Override
    public void update(OtaEntity entity) {
        // 检查是否存在相同类型和版本的固件（排除当前记录）
        QueryWrapper<OtaEntity> queryWrapper = new QueryWrapper<OtaEntity>()
                .eq("type", entity.getType())
                .eq("version", entity.getVersion())
                .ne("id", entity.getId()); // 排除当前记录

        if (baseDao.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("已存在相同类型和版本的固件，请修改后重试");
        }

        entity.setUpdateDate(new Date());
        baseDao.updateById(entity);
        // 同步到 R2；成功才写入 r2ObjectKey，避免静默失败仍下发直链导致设备 404
        syncR2ObjectKey(entity);
        baseDao.updateById(trimForUpdate(entity));
    }

    @Override
    public void delete(String[] ids) {
        // 先取要删除固件的对象 key，用于同步清理 R2
        List<OtaEntity> list = baseDao.selectBatchIds(Arrays.asList(ids));
        baseDao.deleteBatchIds(Arrays.asList(ids));
        if (list != null) {
            for (OtaEntity entity : list) {
                if (StringUtils.isNotBlank(entity.getR2ObjectKey())) {
                    otaStorageService.removeObject(entity.getR2ObjectKey());
                }
            }
        }
    }

    @Override
    public boolean save(OtaEntity entity) {
        boolean inserted = true;
        QueryWrapper<OtaEntity> queryWrapper = new QueryWrapper<OtaEntity>()
                .eq("type", entity.getType());
        // 同类固件只保留最新的一条
        List<OtaEntity> otaList = baseDao.selectList(queryWrapper);
        if (otaList != null && otaList.size() > 0) {
            OtaEntity otaBefore = otaList.getFirst();
            entity.setId(otaBefore.getId());
            baseDao.updateById(entity);
            inserted = false;
        } else {
            baseDao.insert(entity);
        }
        // 同步到 R2；成功才写入 r2ObjectKey
        syncR2ObjectKey(entity);
        baseDao.updateById(trimForUpdate(entity));
        return inserted;
    }

    /**
     * 将本地固件同步上传到 R2，仅在成功后把对象 key 写回实体。
     */
    private void syncR2ObjectKey(OtaEntity entity) {
        if (StringUtils.isBlank(entity.getFirmwarePath())) {
            return;
        }
        if (!otaStorageService.isConfigured()) {
            return;
        }
        String objectKey = otaStorageService.uploadLocalFile(entity.getFirmwarePath());
        if (objectKey != null) {
            entity.setR2ObjectKey(objectKey);
        }
    }

    /** 仅回写需要持久化的字段，避免覆盖请求类其它脏数据。 */
    private OtaEntity trimForUpdate(OtaEntity entity) {
        OtaEntity patch = new OtaEntity();
        patch.setId(entity.getId());
        patch.setR2ObjectKey(entity.getR2ObjectKey());
        patch.setUpdateDate(new Date());
        return patch;
    }

    @Override
    public boolean resyncR2(String id) {
        if (!otaStorageService.isConfigured()) {
            return false;
        }
        OtaEntity entity = baseDao.selectById(id);
        if (entity == null || StringUtils.isBlank(entity.getFirmwarePath())) {
            return false;
        }
        String r2ObjectKeyBefore = entity.getR2ObjectKey();
        syncR2ObjectKey(entity);
        if (StringUtils.isBlank(entity.getR2ObjectKey())) {
            // 重同步失败；若之前已有 key，回滚避免误清
            entity.setR2ObjectKey(r2ObjectKeyBefore);
            return false;
        }
        baseDao.updateById(trimForUpdate(entity));
        return true;
    }

    @Override
    public OtaEntity getLatestOta(String type) {
        QueryWrapper<OtaEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("type", type)
                .orderByDesc("update_date")
                .last("LIMIT 1");
        return baseDao.selectOne(wrapper);
    }
}