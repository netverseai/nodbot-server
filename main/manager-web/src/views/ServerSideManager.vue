<template>
  <div class="management-container">
    <HeaderBar />

    <div class="operation-bar">
      <h2 class="page-title">服务端管理</h2>
    </div>

    <div class="content-panel">
      <!-- 系统全局配置区域 -->
      <div class="config-section">
        <h3 class="section-title"><i class="el-icon-setting"></i> 系统全局配置</h3>
        <el-card class="config-card" shadow="never">
          <el-form :model="systemConfigs" label-width="180px" label-position="left">
            <el-form-item label="Token 有效期 (天)">
              <div class="config-item-wrapper">
                <el-input-number v-model="systemConfigs.tokenExpire" :min="1" :max="365" size="medium"></el-input-number>
                <el-button type="primary" size="medium" @click="saveConfig('server.token_expire', systemConfigs.tokenExpire)" :loading="saving.tokenExpire">保存</el-button>
                <span class="config-tip">设置用户登录后 Token 的有效时长。</span>
              </div>
            </el-form-item>

            <el-form-item label="允许新用户注册">
              <div class="config-item-wrapper">
                <el-switch v-model="systemConfigs.allowRegister" active-color="#13ce66"></el-switch>
                <el-button type="primary" size="medium" @click="saveConfig('server.allow_user_register', systemConfigs.allowRegister)" :loading="saving.allowRegister">保存</el-button>
                <span class="config-tip">是否允许非管理员用户在登录页进行注册。</span>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <div class="instances-section">
        <h3 class="section-title"><i class="el-icon-monitor"></i> 服务端实例</h3>
        <el-table
          ref="paramsTable"
          :data="paramsList"
          class="management-table"
          v-loading="loading"
          element-loading-text="拼命加载中"
          element-loading-spinner="el-icon-loading"
          element-loading-background="rgba(255, 255, 255, 0.7)"
          :header-cell-class-name="headerCellClassName"
        >
          <el-table-column label="选择" align="center" width="120">
            <template slot-scope="scope">
              <el-checkbox v-model="scope.row.selected"></el-checkbox>
            </template>
          </el-table-column>
          <el-table-column
            label="ws地址"
            prop="address"
            align="center"
          ></el-table-column>
          <el-table-column
            label="操作"
            prop="operator"
            align="center"
            show-overflow-tooltip
          >
            <template slot-scope="scope">
              <el-button
                size="medium"
                type="text"
                @click="emitAction(scope.row, actionMap.restart)"
                >重启</el-button
              >
              <el-button
                size="medium"
                type="text"
                @click="emitAction(scope.row, actionMap.update_config)"
                >更新配置</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import ParamDialog from "@/components/ParamDialog.vue";
import VersionFooter from "@/components/VersionFooter.vue";

export default {
  components: { HeaderBar, ParamDialog, VersionFooter },
  data() {
    return {
      paramsList: [],
      actionMap: {
        restart: {
          value: 'restart',
          title: "重启服务端",
          message: "确定要重启服务端吗？",
          confirmText: "重启",
        },
        update_config: {
          value: 'update_config',
          title: "更新配置",
          message: "确定要更新配置吗？",
          confirmText: "更新",
        }
      },
      systemConfigs: {
        tokenExpire: 30,
        allowRegister: false
      },
      saving: {
        tokenExpire: false,
        allowRegister: false
      },
      allParams: [], // 存储所有参数用于查找 ID
      loading: false,
      dialogVisible: false,
      dialogTitle: "新增参数",
      paramForm: {
        id: null,
        paramCode: "",
        paramValue: "",
        remark: ""
      },
    };
  },
  created() {
    this.fetchParams();
    this.fetchSystemConfigs();
  },

  methods: {
    fetchSystemConfigs() {
      Api.admin.getParamsList({ page: 1, limit: 1000 }, ({ data }) => {
        if (data.code === 0) {
          this.allParams = data.data.list;
          const tokenItem = this.allParams.find(i => i.paramCode === 'server.token_expire');
          const registerItem = this.allParams.find(i => i.paramCode === 'server.allow_user_register');

          if (tokenItem) this.systemConfigs.tokenExpire = parseInt(tokenItem.paramValue);
          if (registerItem) this.systemConfigs.allowRegister = registerItem.paramValue === 'true';
        }
      });
    },
    saveConfig(code, value) {
      const field = code === 'server.token_expire' ? 'tokenExpire' : 'allowRegister';
      this.saving[field] = true;

      const config = this.allParams.find(i => i.paramCode === code);
      if (!config) {
        this.$message.error('未找到配置项：' + code);
        this.saving[field] = false;
        return;
      }

      Api.admin.updateParam({
        ...config,
        paramValue: value.toString()
      }, ({ data }) => {
        this.saving[field] = false;
        if (data.code === 0) {
          this.$message.success('配置更新成功');
          this.fetchSystemConfigs(); // 重新加载以保持同步
        } else {
          this.$message.error(data.msg || '更新失败');
        }
      });
    },
    fetchParams() {
      this.loading = true;
      Api.admin.getWsServerList(
        {},
        ({ data }) => {
          this.loading = false;
          if (data.code === 0) {
            this.paramsList = data.data.map(item => ({ address: item }));
          } else {
            this.$message.error({
              message: data.msg || '获取参数列表失败',
              showClose: true
            });
          }
        }
      );
    },
    emitAction(rowItem, actionItem) {
      if (actionItem === undefined || rowItem.address === undefined) {
        return;
      }
      this.$confirm(actionItem.message, actionItem.title, {
        confirmButtonText: actionItem.confirmText,
      }).then(() => {
        Api.admin.sendWsServerAction({
          targetWs: rowItem.address,
          action: actionItem.value
        }, ({ data }) => {
          if (data.code !== 0) {
            this.$message.error({
              message: data.msg || '操作失败',
              showClose: true
            });
            return;
          }
          this.$message.success({
            message: `${actionItem.title}成功`,
            showClose: true
          })
        })
      })
    },
    headerCellClassName({ columnIndex }) {
      if (columnIndex === 0) {
        return "custom-selection-header";
      }
      return "";
    }
  },
};
</script>

<style lang="scss" scoped>
@import "./management.scss";

.config-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 18px;
  color: #303133;
  margin-bottom: 20px;
  padding-left: 10px;
  border-left: 4px solid #ed1c24;
  display: flex;
  align-items: center;
  i {
    margin-right: 8px;
  }
}

.config-card {
  border-radius: 8px;
  background-color: #fff;
}

.config-item-wrapper {
  display: flex;
  align-items: center;
  gap: 15px;

  .el-button {
    margin-left: 10px;
  }
}

.config-tip {
  font-size: 13px;
  color: #909399;
  margin-left: 10px;
}

.instances-section {
  margin-top: 20px;
}

::v-deep .custom-selection-header {
  .el-checkbox {
    display: none;
  }
}
</style>
