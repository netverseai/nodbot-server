package xiaozhi.modules.model.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.hutool.core.collection.CollectionUtil;
import lombok.AllArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.modules.agent.dao.AgentDao;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.model.dao.ModelConfigDao;
import xiaozhi.modules.model.dto.ModelBasicInfoDTO;
import xiaozhi.modules.model.dto.ModelConfigBodyDTO;
import xiaozhi.modules.model.dto.ModelConfigDTO;
import xiaozhi.modules.model.dto.ModelProviderDTO;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.model.service.ModelProviderService;

@Service
@AllArgsConstructor
public class ModelConfigServiceImpl extends BaseServiceImpl<ModelConfigDao, ModelConfigEntity>
        implements ModelConfigService {

    private final ModelConfigDao modelConfigDao;
    private final ModelProviderService modelProviderService;
    private final RedisUtils redisUtils;
    private final AgentDao agentDao;

    @Override
    public List<ModelBasicInfoDTO> getModelCodeList(String modelType, String modelName) {
        List<ModelConfigEntity> entities = modelConfigDao.selectList(
                new QueryWrapper<ModelConfigEntity>()
                        .eq("model_type", modelType)
                        .eq("is_enabled", 1)
                        .like(StringUtils.isNotBlank(modelName), "model_name", "%" + modelName + "%")
                        .select("id", "model_name"));
        return ConvertUtils.sourceToTarget(entities, ModelBasicInfoDTO.class);
    }

    @Override
    public PageData<ModelConfigDTO> getPageList(String modelType, String modelName, String page, String limit) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constant.PAGE, page);
        params.put(Constant.LIMIT, limit);
        IPage<ModelConfigEntity> modelConfigEntityIPage = modelConfigDao.selectPage(
                getPage(params, "sort", true),
                new QueryWrapper<ModelConfigEntity>()
                        .eq("model_type", modelType)
                        .like(StringUtils.isNotBlank(modelName), "model_name", "%" + modelName + "%"));
        return getPageData(modelConfigEntityIPage, ModelConfigDTO.class);
    }

    @Override
    public ModelConfigDTO add(String modelType, String provideCode, ModelConfigBodyDTO modelConfigBodyDTO) {
        // 先验证有没有供应器
        if (StringUtils.isBlank(modelType) || StringUtils.isBlank(provideCode)) {
            throw new RenException(ErrorCode.MODEL_TYPE_PROVIDER_EMPTY);
        }
        List<ModelProviderDTO> providerList = modelProviderService.getList(modelType, provideCode);
        if (CollectionUtil.isEmpty(providerList)) {
            throw new RenException(ErrorCode.MODEL_PROVIDER_NOT_EXIST);
        }

        // 再保存供应器提供的模型
        ModelConfigEntity modelConfigEntity = ConvertUtils.sourceToTarget(modelConfigBodyDTO, ModelConfigEntity.class);
        modelConfigEntity.setModelType(modelType);
        modelConfigEntity.setIsDefault(0);
        modelConfigDao.insert(modelConfigEntity);
        return ConvertUtils.sourceToTarget(modelConfigEntity, ModelConfigDTO.class);
    }

    @Override
    public ModelConfigDTO edit(String modelType, String provideCode, String id, ModelConfigBodyDTO modelConfigBodyDTO) {
        // 先验证有没有供应器
        if (StringUtils.isBlank(modelType) || StringUtils.isBlank(provideCode)) {
            throw new RenException(ErrorCode.MODEL_TYPE_PROVIDER_EMPTY);
        }
        List<ModelProviderDTO> providerList = modelProviderService.getList(modelType, provideCode);
        if (CollectionUtil.isEmpty(providerList)) {
            throw new RenException(ErrorCode.MODEL_PROVIDER_NOT_EXIST);
        }

        // 再更新供应器提供的模型
        ModelConfigEntity modelConfigEntity = ConvertUtils.sourceToTarget(modelConfigBodyDTO, ModelConfigEntity.class);
        modelConfigEntity.setId(id);
        modelConfigEntity.setModelType(modelType);
        modelConfigDao.updateById(modelConfigEntity);
        // 清除缓存
        redisUtils.delete(RedisKeys.getModelConfigById(modelConfigEntity.getId()));
        return ConvertUtils.sourceToTarget(modelConfigEntity, ModelConfigDTO.class);
    }

    @Override
    public void delete(String id) {
        // 查看是否是默认
        ModelConfigEntity modelConfig = modelConfigDao.selectById(id);
        if (modelConfig != null && modelConfig.getIsDefault() == 1) {
            throw new RenException(ErrorCode.MODEL_IS_DEFAULT_CANNOT_DELETE);
        }
        // 验证是否有引用
        checkAgentReference(id);
        checkIntentConfigReference(id);

        modelConfigDao.deleteById(id);
    }

    /**
     * 检查智能体配置是否有引用
     * 
     * @param modelId 模型ID
     */
    private void checkAgentReference(String modelId) {
        List<AgentEntity> agents = agentDao.selectList(
                new QueryWrapper<AgentEntity>()
                        .eq("vad_model_id", modelId)
                        .or()
                        .eq("asr_model_id", modelId)
                        .or()
                        .eq("llm_model_id", modelId)
                        .or()
                        .eq("tts_model_id", modelId)
                        .or()
                        .eq("mem_model_id", modelId)
                        .or()
                        .eq("intent_model_id", modelId));
        if (!agents.isEmpty()) {
            String agentNames = agents.stream()
                    .map(AgentEntity::getAgentName)
                    .collect(Collectors.joining("、"));
            throw new RenException(ErrorCode.MODEL_REFERENCED_BY_AGENT, agentNames);
        }
    }

    /**
     * 检查意图识别配置是否有引用
     * 
     * @param modelId 模型ID
     */
    private void checkIntentConfigReference(String modelId) {
        ModelConfigEntity modelConfig = modelConfigDao.selectById(modelId);
        if (modelConfig != null
                && "LLM".equals(modelConfig.getModelType() == null ? null : modelConfig.getModelType().toUpperCase())) {
            List<ModelConfigEntity> intentConfigs = modelConfigDao.selectList(
                    new QueryWrapper<ModelConfigEntity>()
                            .eq("model_type", "Intent")
                            .like("config_json", "%" + modelId + "%"));
            if (!intentConfigs.isEmpty()) {
                throw new RenException(ErrorCode.MODEL_REFERENCED_BY_INTENT);
            }
        }
    }

    @Override
    public String getModelNameById(String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }

        String cachedName = (String) redisUtils.get(RedisKeys.getModelNameById(id));

        if (StringUtils.isNotBlank(cachedName)) {
            return cachedName;
        }

        ModelConfigEntity entity = modelConfigDao.selectById(id);
        if (entity != null) {
            String modelName = entity.getModelName();
            if (StringUtils.isNotBlank(modelName)) {
                redisUtils.set(RedisKeys.getModelNameById(id), modelName);
            }
            return modelName;
        }

        return null;
    }

    @Override
    public ModelConfigEntity getModelById(String id, boolean isCache) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        if (isCache) {
            ModelConfigEntity cachedConfig = (ModelConfigEntity) redisUtils.get(RedisKeys.getModelConfigById(id));
            if (cachedConfig != null) {
                return ConvertUtils.sourceToTarget(cachedConfig, ModelConfigEntity.class);
            }
        }
        ModelConfigEntity entity = modelConfigDao.selectById(id);
        if (entity != null) {
            redisUtils.set(RedisKeys.getModelConfigById(id), entity);
        }
        return entity;
    }

    @Override
    public void setDefaultModel(String modelType, int isDefault) {
        ModelConfigEntity entity = new ModelConfigEntity();
        entity.setIsDefault(isDefault);
        modelConfigDao.update(entity, new QueryWrapper<ModelConfigEntity>()
                .eq("model_type", modelType));
    }

    @Override
    public void applyDefaultToAllAgents(String modelType) {
        // 找到指定类型的默认模型（MySQL collation 大小写不敏感，可用前端小写匹配 'TTS' 等）
        ModelConfigEntity defaultModel = modelConfigDao.selectOne(
                new QueryWrapper<ModelConfigEntity>()
                        .eq("model_type", modelType)
                        .eq("is_default", 1)
                        .last("LIMIT 1"));
        if (defaultModel == null) {
            throw new RenException(ErrorCode.MODEL_CONFIG_NOT_EXIST);
        }

        String modelId = defaultModel.getId();
        String column = null;
        boolean isTts = false;
        switch (modelType.toUpperCase()) {
            case "ASR":
                column = "asr_model_id";
                break;
            case "VAD":
                column = "vad_model_id";
                break;
            case "LLM":
                column = "llm_model_id";
                break;
            case "VLLM":
                column = "vllm_model_id";
                break;
            case "TTS":
                column = "tts_model_id";
                isTts = true;
                break;
            case "MEMORY":
                column = "mem_model_id";
                break;
            case "INTENT":
                column = "intent_model_id";
                break;
            default:
                return;
        }

        // 更新所有智能体对应的模型字段
        // 注意：需带恒真 WHERE 条件（id IS NOT NULL），否则会被 MyBatis-Plus 的
        // BlockAttackInnerInterceptor（防全表更新）拦截报 Prohibition of table update operation
        UpdateWrapper<AgentEntity> wrapper = new UpdateWrapper<AgentEntity>()
                .set(column, modelId)
                .isNotNull("id");
        // TTS 换默认模型时同步清空音色，避免保留旧模型音色导致失效（与模板换模型行为保持一致）
        if (isTts) {
            wrapper.set("tts_voice_id", null);
        }
        agentDao.update(null, wrapper);
    }
}
