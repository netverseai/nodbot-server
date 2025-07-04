<template>
  <div class="management-container">
    <HeaderBar />

    <div class="operation-bar">
      <h2 class="page-title">用户管理</h2>
      <div class="right-operations">
        <el-input placeholder="请输入手机号码查询" v-model="searchPhone" class="search-input" clearable
          @keyup.enter.native="handleSearch" />
        <el-button class="btn-search" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <div class="content-panel">
      <el-table ref="userTable" :data="userList" class="management-table" v-loading="loading"
        element-loading-text="拼命加载中" element-loading-spinner="el-icon-loading"
        element-loading-background="rgba(255, 255, 255, 0.7)">
        <el-table-column label="选择" align="center" width="120">
          <template slot-scope="scope">
            <el-checkbox v-model="scope.row.selected"></el-checkbox>
          </template>
        </el-table-column>
        <el-table-column label="用户Id" prop="userid" align="center"></el-table-column>
        <el-table-column label="手机号码" prop="mobile" align="center"></el-table-column>
        <el-table-column label="设备数量" prop="deviceCount" align="center"></el-table-column>
        <el-table-column label="注册时间" prop="createDate" align="center"></el-table-column>
        <el-table-column label="状态" prop="status" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 1" type="success">正常</el-tag>
            <el-tag v-else type="danger">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="text" @click="resetPassword(scope.row)">重置密码</el-button>
            <el-button size="mini" type="text" v-if="scope.row.status === 1"
              @click="handleChangeStatus(scope.row, 0)">禁用账户</el-button>
            <el-button size="mini" type="text" v-if="scope.row.status === 0"
              @click="handleChangeStatus(scope.row, 1)">恢复账号</el-button>
            <el-button size="mini" type="text" @click="deleteUser(scope.row)">删除用户</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <div class="left-actions">
          <el-button size="mini" type="primary" class="select-all-btn" @click="handleSelectAll">
            {{ isAllSelected ? '取消全选' : '全选' }}
          </el-button>
          <el-button size="mini" type="success" icon="el-icon-circle-check" @click="batchEnable">启用</el-button>
          <el-button size="mini" type="warning" @click="batchDisable"><i
              class="el-icon-remove-outline rotated-icon"></i>禁用</el-button>
          <el-button size="mini" type="danger" icon="el-icon-delete" @click="batchDelete">删除</el-button>
        </div>
        <div class="custom-pagination">
          <el-select v-model="pageSize" @change="handlePageSizeChange" class="page-size-select">
            <el-option v-for="item in pageSizeOptions" :key="item" :label="`${item}条/页`" :value="item">
            </el-option>
          </el-select>

          <button class="pagination-btn" :disabled="currentPage === 1" @click="goFirst">
            首页
          </button>
          <button class="pagination-btn" :disabled="currentPage === 1" @click="goPrev">
            上一页
          </button>
          <button v-for="page in visiblePages" :key="page" class="pagination-btn"
            :class="{ active: page === currentPage }" @click="goToPage(page)">
            {{ page }}
          </button> <button class="pagination-btn" :disabled="currentPage === pageCount" @click="goNext">
            下一页
          </button>
          <span class="total-text">共{{ total }}条记录</span>
        </div>
      </div>
    </div>

    <view-password-dialog :visible.sync="showViewPassword" :password="currentPassword" />
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import ViewPasswordDialog from "@/components/ViewPasswordDialog.vue";
export default {
  components: { HeaderBar, ViewPasswordDialog, VersionFooter },
  data() {
    return {
      showViewPassword: false,
      currentPassword: "",
      searchPhone: "",
      userList: [],
      pageSizeOptions: [10, 20, 50, 100],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      isAllSelected: false,
      loading: false,
    };
  },
  created() {
    this.fetchUsers();
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
      this.fetchUsers();
    },

    fetchUsers() {
      this.loading = true;
      Api.admin.getUserList(
        {
          page: this.currentPage,
          limit: this.pageSize,
          mobile: this.searchPhone,
        },
        ({ data }) => {
          this.loading = false; // 结束加载
          if (data.code === 0) {
            this.userList = data.data.list.map(item => ({
              ...item,
              selected: false
            }));
            this.total = data.data.total;
          }
        }
      );
    },
    handleSearch() {
      this.currentPage = 1;
      this.fetchUsers();
    },
    handleSelectAll() {
      this.isAllSelected = !this.isAllSelected;
      this.userList.forEach(row => {
        row.selected = this.isAllSelected;
      });
    },
    batchDelete() {
      const selectedUsers = this.userList.filter(user => user.selected);
      if (selectedUsers.length === 0) {
        this.$message.warning("请先选择需要删除的用户");
        return;
      }

      this.$confirm(`确定要删除选中的${selectedUsers.length}个用户吗？`, "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(async () => {
          const loading = this.$loading({
            lock: true,
            text: "正在删除中...",
            spinner: "el-icon-loading",
            background: "rgba(0, 0, 0, 0.7)",
          });

          try {
            const results = await Promise.all(
              selectedUsers.map((user) => {
                return new Promise((resolve) => {
                  Api.admin.deleteUser(user.userid, ({ data }) => {
                    if (data.code === 0) {
                      resolve({ success: true, userid: user.userid });
                    } else {
                      resolve({ success: false, userid: user.userid, msg: data.msg });
                    }
                  });
                });
              })
            );

            const successCount = results.filter((r) => r.success).length;
            const failCount = results.length - successCount;

            if (failCount === 0) {
              this.$message.success({
                message: `成功删除${successCount}个用户`,
                showClose: true
              });
            } else if (successCount === 0) {
              this.$message.error({
                message: '删除失败，请重试',
                showClose: true
              });
            } else {
              this.$message.warning(
                `成功删除${successCount}个用户，${failCount}个删除失败`
              );
            }

            this.fetchUsers();
          } catch (error) {
            this.$message.error("删除过程中发生错误");
          } finally {
            loading.close();
          }
        })
        .catch(() => {
          this.$message.info("已取消删除");
        });
    },
    batchEnable() {
      const selectedUsers = this.userList.filter(user => user.selected);
      this.handleChangeStatus(selectedUsers, 1);
    },
    batchDisable() {
      const selectedUsers = this.userList.filter(user => user.selected);
      this.handleChangeStatus(selectedUsers, 0);
    },
    resetPassword(row) {
      this.$confirm("重置后将会生成新密码，是否继续？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
      }).then(() => {
        Api.admin.resetUserPassword(row.userid, ({ data }) => {
          if (data.code === 0) {
            this.currentPassword = data.data;
            this.showViewPassword = true;
            this.$message.success({
              message: "密码已重置，请通知用户使用新密码登录",
              showClose: true
            });
          }
        });
      });
    },
    deleteUser(row) {
      this.$confirm("确定要删除该用户吗？", "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          Api.admin.deleteUser(row.userid, ({ data }) => {
            if (data.code === 0) {
              this.$message.success({
                message: "删除成功",
                showClose: true
              });
              this.fetchUsers();
            } else {
              this.$message.error({
                message: data.msg || "删除失败",
                showClose: true
              });
            }
          });
        })
        .catch(() => { });
    },
    goFirst() {
      this.currentPage = 1;
      this.fetchUsers();
    },
    goPrev() {
      if (this.currentPage > 1) {
        this.currentPage--;
        this.fetchUsers();
      }
    },
    goNext() {
      if (this.currentPage < this.pageCount) {
        this.currentPage++;
        this.fetchUsers();
      }
    },
    goToPage(page) {
      this.currentPage = page;
      this.fetchUsers();
    },
    handleChangeStatus(row, status) {
      // 处理单个用户或用户数组
      const users = Array.isArray(row) ? row : [row];
      const confirmText = status === 0 ? '禁用' : '启用';
      const userCount = users.length;

      this.$confirm(`确定要${confirmText}选中的${userCount}个用户吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const userIds = users.map(user => user.userid);
        if (userIds.some(id => isNaN(id))) {
          this.$message.error('存在无效的用户ID');
          return;
        }

        Api.user.changeUserStatus(status, userIds, ({ data }) => {
          if (data.code === 0) {
            this.$message.success({
              message: `成功${confirmText}${userCount}个用户`,
              showClose: true
            });
            this.fetchUsers(); // 刷新用户列表
          } else {
            this.$message.error({
              message: '操作失败，请重试',
              showClose: true
            });
          }
        });
      }).catch(() => {
        // 用户取消操作
      });
    },
  },
};
</script>

<style lang="scss" scoped>
@import "./management.scss";

.rotated-icon {
  transform: rotate(90deg);
}
</style>
