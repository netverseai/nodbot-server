<template>
  <div class="page-container">
    <HeaderBar />

    <div class="operation-bar">
      <h2 class="page-title">字段管理</h2>
      <div class="right-operations">
        <el-dropdown trigger="click" @command="handleSelectModelType" @visible-change="handleDropdownVisibleChange">
          <el-button class="category-btn">
            类别筛选 {{ selectedModelTypeLabel }}<i class="el-icon-arrow-down el-icon--right"
              :class="{ 'rotate-down': DropdownVisible }"></i>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="">全部</el-dropdown-item>
            <el-dropdown-item v-for="item in modelTypes" :key="item.value" :command="item.value">
              {{ item.label }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
        <el-input placeholder="请输入供应器名称查询" v-model="searchName" class="search-input" @keyup.enter.native="handleSearch"
          clearable />
        <el-button class="btn-search" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="provider-card" shadow="never">
            <div class="table-responsive">
              <el-table ref="providersTable" :data="filteredProvidersList" class="transparent-table" v-loading="loading"
                element-loading-text="拼命加载中" element-loading-spinner="el-icon-loading"
                element-loading-background="rgba(255, 255, 255, 0.7)" :header-cell-class-name="headerCellClassName">
                <el-table-column label="选择" align="center" width="120">
                  <template slot-scope="scope">
                    <el-checkbox v-model="scope.row.selected"></el-checkbox>
                  </template>
                </el-table-column>

                <el-table-column label="类别" prop="modelType" align="center" width="200">
                  <template slot="header" slot-scope="scope">
                    <el-dropdown trigger="click" @command="handleSelectModelType"
                      @visible-change="isDropdownOpen = $event">
                      <span class="dropdown-trigger" :class="{ 'active': isDropdownOpen }">
                        类别{{ selectedModelTypeLabel }} <i class="dropdown-arrow"
                          :class="{ 'is-active': isDropdownOpen }"></i>
                      </span>
                      <el-dropdown-menu slot="dropdown">
                        <el-dropdown-item command="">全部</el-dropdown-item>
                        <el-dropdown-item v-for="item in modelTypes" :key="item.value" :command="item.value">
                          {{ item.label }}
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </el-dropdown>
                  </template>
                  <template slot-scope="scope">
                    <el-tag :type="getModelTypeTag(scope.row.modelType)">
                      {{ getModelTypeLabel(scope.row.modelType) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="供应器编码" prop="providerCode" align="center" width="150"></el-table-column>
                <el-table-column label="名称" prop="name" align="center"></el-table-column>
                <el-table-column label="字段配置" align="center">
                  <template slot-scope="scope">
                    <el-popover placement="top-start" width="400" trigger="hover">
                      <div class="field-list">
                        <div v-for="field in scope.row.fields" :key="field.key" class="field-item">
                          <span class="field-label">{{ field.label }}:</span>
                          <span class="field-type">{{ field.type }}</span>
                          <span v-if="isSensitiveField(field.key)" class="sensitive-tag">敏感</span>
                        </div>
                      </div>
                      <el-button slot="reference" size="mini" type="text">查看字段</el-button>
                    </el-popover>
                  </template>
                </el-table-column>
                <el-table-column label="排序" prop="sort" align="center" width="80"></el-table-column>
                <el-table-column label="操作" align="center" width="180" fixed="right">
                  <template slot-scope="scope">
                    <el-button size="mini" type="text" @click="editProvider(scope.row)">编辑</el-button>
                    <el-button size="mini" type="text" @click="deleteProvider(scope.row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="table_bottom">
              <div class="ctrl_btn">
                <el-button size="mini" type="primary" class="select-all-btn" @click="handleSelectAll">
                  {{ isAllSelected ? '取消全选' : '全选' }}
                </el-button>
                <el-button size="mini" type="success" @click="showAddDialog">新增</el-button>
                <el-button size="mini" type="danger" icon="el-icon-delete" @click="deleteSelectedProviders">删除
                </el-button>
              </div>
              <div class="custom-pagination">
                <el-select v-model="pageSize" @change="handlePageSizeChange" class="page-size-select">
                  <el-option v-for="item in pageSizeOptions" :key="item" :label="`${item}条/页`" :value="item">
                  </el-option>
                </el-select>
                <div class="pagination-nav">
                  <button class="pagination-btn" :disabled="currentPage === 1" @click="goFirst">
                    首页
                  </button>
                  <button class="pagination-btn" :disabled="currentPage === 1" @click="goPrev">
                    上一页
                  </button>
                  <button v-for="page in visiblePages" :key="page" class="pagination-btn"
                    :class="{ active: page === currentPage }" @click="goToPage(page)">
                    {{ page }}
                  </button>
                  <button class="pagination-btn" :disabled="currentPage === pageCount" @click="goNext">
                    下一页
                  </button>
                </div>
                <span class="total-text">共{{ total }}条记录</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 新增/编辑供应器对话框 -->
    <provider-dialog :title="dialogTitle" :visible.sync="dialogVisible" :form="providerForm" :model-types="modelTypes"
      @submit="handleSubmit" @cancel="dialogVisible = false" />

    <el-footer class="page-footer">
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import ProviderDialog from "@/components/ProviderDialog.vue";
import VersionFooter from "@/components/VersionFooter.vue";

export default {
  components: { HeaderBar, ProviderDialog, VersionFooter },
  data() {
    return {
      searchName: "",
      searchModelType: "",
      providersList: [],
      modelTypes: [
        { value: "ASR", label: "语音识别" },
        { value: "TTS", label: "语音合成" },
        { value: "LLM", label: "大语言模型" },
        { value: "VLLM", label: "视觉大语言模型" },
        { value: "Intent", label: "意图识别" },
        { value: "Memory", label: "记忆模块" },
        { value: "VAD", label: "语音活动检测" },
        { value: "Plugin", label: "插件工具" }
      ],
      currentPage: 1,
      loading: false,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      total: 0,
      dialogVisible: false,
      dialogTitle: "新增供应器",
      isAllSelected: false,
      isDropdownOpen: false,
      sensitive_keys: ["api_key", "personal_access_token", "access_token", "token", "secret", "access_key_secret", "secret_key"],
      providerForm: {
        id: null,
        modelType: "",
        providerCode: "",
        name: "",
        fields: [],
        sort: 0
      },
      DropdownVisible: false,
    };
  },
  created() {
    this.fetchProviders();
  },
  computed: {
    selectedModelTypeLabel() {
      if (!this.searchModelType) return "（全部）";
      const selectedType = this.modelTypes.find(item => item.value === this.searchModelType);
      return selectedType ? `（${selectedType.label}）` : "";
    },
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
    filteredProvidersList() {
      return this.providersList;

      // let list = this.providersList.filter(item => {
      //   const nameMatch = item.name.toLowerCase().includes(this.searchName.toLowerCase());
      //   const typeMatch = !this.searchModelType || item.model_type === this.searchModelType;
      //   return nameMatch && typeMatch;
      // });

      // list.sort((a, b) => a.sort - b.sort);

      // // 分页处理
      // const start = (this.currentPage - 1) * this.pageSize;
      // return list.slice(start, start + this.pageSize);
    }
  },
  methods: {
    fetchProviders() {
      this.loading = true;

      Api.model.getModelProvidersPage(
        {
          page: this.currentPage,
          limit: this.pageSize,
          name: this.searchName,
          modelType: this.searchModelType
        },
        ({ data }) => {
          this.loading = false;
          if (data.code === 0) {
            this.providersList = data.data.list.map(item => {
              return {
                ...item,
                selected: false,
                fields: JSON.parse(item.fields)
              };
            });
            this.total = data.data.total;
          } else {
            this.$message.error({
              message: data.msg || '获取参数列表失败'
            });
          }
        }
      );
    },
    handleSearch() {
      this.currentPage = 1;
      this.fetchProviders();
    },
    handleSelectModelType(value) {
      this.isDropdownOpen = false;
      this.searchModelType = value;
      this.handleSearch();
    },
    handleSelectAll() {
      this.isAllSelected = !this.isAllSelected;
      this.providersList.forEach(row => {
        row.selected = this.isAllSelected;
      });
    },
    showAddDialog() {
      this.dialogTitle = "新增供应器";
      this.providerForm = {
        id: null,
        modelType: "",
        providerCode: "",
        name: "",
        fields: [],
        sort: 0
      };
      this.dialogVisible = true;
    },
    editProvider(row) {
      this.dialogTitle = "编辑供应器";
      this.providerForm = {
        ...row,
        fields: JSON.parse(JSON.stringify(row.fields))
      };
      this.dialogVisible = true;
    },
    handleSubmit({ form, done }) {
      this.loading = true;
      if (form.id) {
        // 编辑
        Api.model.updateModelProvider(form, ({ data }) => {

          if (data.code === 0) {
            this.fetchProviders(); // 刷新表格
            this.$message.success({
              message: "修改成功",
              showClose: true
            });
          }
        });
      } else {
        // 新增
        Api.model.addModelProvider(form, ({ data }) => {
          if (data.code === 0) {
            this.fetchProviders(); // 刷新表格
            this.$message.success({
              message: "新增成功",
              showClose: true
            });
            this.total += 1;
          }
        });
      }
      this.loading = false;
      this.dialogVisible = false;
      done && done();
    },
    deleteSelectedProviders() {
      const selectedRows = this.providersList.filter(row => row.selected);
      if (selectedRows.length === 0) {
        this.$message.warning({
          message: "请先选择需要删除的供应器",
          showClose: true
        });
        return;
      }
      this.deleteProvider(selectedRows);
    },
    deleteProvider(row) {
      const providers = Array.isArray(row) ? row : [row];
      const providerCount = providers.length;

      this.$confirm(`确定要删除选中的${providerCount}个供应器吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        const ids = providers.map(provider => provider.id);
        Api.model.deleteModelProviderByIds(ids, ({ data }) => {
          if (data.code === 0) {

            this.isAllSelected = false;
            this.fetchProviders(); // 刷新表格

            this.$message.success({
              message: `成功删除${providerCount}个参数`,
              showClose: true
            });
          } else {
            this.$message.error({
              message: data.msg || '删除失败，请重试',
              showClose: true
            });
          }
        });
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消删除',
          showClose: true,
          duration: 1000
        });
      });
    },
    getModelTypeTag(type) {
      const typeMap = {
        'ASR': 'success',
        'TTS': 'warning',
        'LLM': 'danger',
        'Intent': 'info',
        'Memory': '',
        'VAD': 'primary'
      };
      return typeMap[type] || '';
    },
    getModelTypeLabel(type) {
      const typeItem = this.modelTypes.find(item => item.value === type);
      return typeItem ? typeItem.label : type;
    },
    isSensitiveField(fieldKey) {
      if (typeof fieldKey !== 'string') return false;
      return this.sensitive_keys.some(key =>
        fieldKey.toLowerCase().includes(key.toLowerCase())
      );
    },
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.fetchProviders();
    },
    headerCellClassName({ columnIndex }) {
      if (columnIndex === 0) {
        return "custom-selection-header";
      }
      return "";
    },
    goFirst() {
      this.currentPage = 1;
      this.fetchProviders();
    },
    goPrev() {
      if (this.currentPage > 1) {
        this.currentPage--;
        this.fetchProviders();
      }
    },
    goNext() {
      if (this.currentPage < this.pageCount) {
        console.log("this.currentPage", this.currentPage);
        this.currentPage++;
        this.fetchProviders();
      }
    },
    goToPage(page) {
      this.currentPage = page;
      this.fetchProviders();
    },
    handleDropdownVisibleChange(visible) {
      this.DropdownVisible = visible;
    },
  },
};
</script>

<style lang="scss" scoped>
@import './management.scss';

.category-btn {
  .el-icon--right {
    transition: transform 0.3s;
  }

  .rotate-down {
    transform: rotate(180deg);
  }
}

.dropdown-trigger {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
}

.dropdown-arrow {
  display: inline-block;
  width: 0;
  height: 0;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 5px solid;
  transition: transform 0.3s;
  margin-left: 5px;

  &.is-active {
    transform: rotate(180deg);
  }
}

.field-list {
  max-height: 300px;
  overflow-y: auto;
}

.field-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 0;
  border-bottom: 1px solid #f0f2f5;

  &:last-child {
    border-bottom: none;
  }
}

.field-label {
  font-weight: 500;
  color: #303133;
}

.field-type {
  color: #909399;
}

.sensitive-tag {
  background-color: #f56c6c;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.page-footer {
  padding: 0;
}
</style>