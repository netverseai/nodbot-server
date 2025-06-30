<template>
  <div class="management-container">
    <HeaderBar />

    <div class="operation-bar">
      <h2 class="page-title">服务端管理</h2>
    </div>

    <div class="content-panel">
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
      currentPage: 1,
      loading: false,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      total: 0,
      dialogVisible: false,
      dialogTitle: "新增参数",
      isAllSelected: false,
      sensitive_keys: ["api_key", "personal_access_token", "access_token", "token", "secret", "access_key_secret", "secret_key"],
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
  },

  computed: {
    pageCount() {
      return Math.ceil(this.total / this.pageSize);
    },
    visiblePages() {
      const pages = [];
      const maxVisible = 3;
      let start = Math.max(1, this.currentPage - 1);
      let end = Math.min(this.pageCount, start + maxVisible - 1);

      if (end - start + 1 < maxVisible) {
        start = Math.max(1, end - maxVisible + 1);
      }

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }
      return pages;
    },
  },
  methods: {
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.fetchParams();
    },
    fetchParams() {
      this.loading = true;
      Api.admin.getWsServerList(
        {},
        ({ data }) => {
          this.loading = false;
          if (data.code === 0) {
            this.paramsList = data.data.map(item => ({ address: item }));
            this.total = data.data.length;
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
      // 弹开询问框
      this.$confirm(actionItem.message, actionItem.title, {
        confirmButtonText: actionItem.confirmText, // 确认按钮文本
      }).then(() => {
        // 用户点击了确认按钮
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

::v-deep .custom-selection-header {
  .el-checkbox {
    display: none;
  }
}
</style>
