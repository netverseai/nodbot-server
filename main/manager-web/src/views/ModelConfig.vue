<template>
  <div class="page-container">
    <HeaderBar />

    <div class="operation-bar">
      <h2 class="page-title">{{ modelTypeText }}</h2>
      <div class="right-operations">
        <el-input placeholder="请输入模型名称查询" v-model="search" class="search-input" clearable
          @keyup.enter.native="handleSearch" />
        <el-button class="btn-search" @click="handleSearch">
          搜索
        </el-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="main-wrapper">
      <div class="content-panel model-config-layout">
        <!-- 左侧导航 -->
        <el-menu :default-active="activeTab" class="nav-panel" @select="handleMenuSelect">
          <el-menu-item index="vad">
            <span class="menu-text">语言活动检测</span>
          </el-menu-item>
          <el-menu-item index="asr">
            <span class="menu-text">语音识别</span>
          </el-menu-item>
          <el-menu-item index="llm">
            <span class="menu-text">大语言模型</span>
          </el-menu-item>
          <el-menu-item index="vllm">
            <span class="menu-text">视觉大模型</span>
          </el-menu-item>
          <el-menu-item index="intent">
            <span class="menu-text">意图识别</span>
          </el-menu-item>
          <el-menu-item index="tts">
            <span class="menu-text">语音合成</span>
          </el-menu-item>
          <el-menu-item index="memory">
            <span class="menu-text">记忆</span>
          </el-menu-item>
        </el-menu>

        <!-- 右侧内容 -->
        <div class="content-area">
          <el-card class="model-card" shadow="never">
            <div class="table-responsive">
              <el-table ref="modelTable" style="width: 100%" v-loading="loading" element-loading-text="拼命加载中"
                element-loading-spinner="el-icon-loading" element-loading-background="rgba(255, 255, 255, 0.7)"
                :data="modelList" class="data-table" header-row-class-name="table-header"
                :header-cell-class-name="headerCellClassName" @selection-change="handleSelectionChange">
                <el-table-column type="selection" width="55" align="center"></el-table-column>
                <el-table-column label="模型ID" prop="id" align="center"></el-table-column>
                <el-table-column label="模型名称" prop="modelName" align="center"></el-table-column>
                <el-table-column label="提供商" align="center">
                  <template slot-scope="scope">
                    {{ scope.row.configJson.type || '未知' }}
                  </template>
                </el-table-column>
                <el-table-column label="是否启用" align="center">
                  <template slot-scope="scope">
                    <el-switch v-model="scope.row.isEnabled" class="custom-switch" :active-value="1" :inactive-value="0"
                      @change="handleStatusChange(scope.row)" />
                  </template>
                </el-table-column>
                <el-table-column label="是否默认" align="center">
                  <template slot-scope="scope">
                    <el-switch v-model="scope.row.isDefault" class="custom-switch" :active-value="1" :inactive-value="0"
                      @change="handleDefaultChange(scope.row)" />
                  </template>
                </el-table-column>
                <el-table-column v-if="activeTab === 'tts'" label="音色管理" align="center">
                  <template slot-scope="scope">
                    <el-button type="text" size="mini" @click="openTtsDialog(scope.row)" class="voice-management-btn">
                      音色管理
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="操作" align="center" width="150px" fixed="right">
                  <template slot-scope="scope">
                    <el-button type="text" size="mini" @click="editModel(scope.row)" class="edit-btn">
                      修改
                    </el-button>
                    <el-button type="text" size="mini" @click="deleteModel(scope.row)" class="delete-btn">
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="table_bottom">
              <div class="ctrl_btn">
                <el-button size="mini" type="primary" @click="selectAll">
                  {{ isAllSelected ?
                    '取消全选' : '全选' }}
                </el-button>
                <el-button type="success" size="mini" @click="addModel" class="add-btn">
                  新增
                </el-button>
                <el-button size="mini" type="danger" icon="el-icon-delete" @click="batchDelete">
                  删除
                </el-button>
              </div>
              <div class="custom-pagination">

                <el-select v-model="pageSize" @change="handlePageSizeChange" class="page-size-select">
                  <el-option v-for="item in pageSizeOptions" :key="item" :label="`${item}条/页`" :value="item">
                  </el-option>
                </el-select>

                <div class="pagination-nav">
                  <button class="pagination-btn" :disabled="currentPage === 1" @click="goFirst">首页</button>
                  <button class="pagination-btn" :disabled="currentPage === 1" @click="goPrev">上一页</button>

                  <button v-for="page in visiblePages" :key="page" class="pagination-btn"
                    :class="{ active: page === currentPage }" @click="goToPage(page)">
                    {{ page }}
                  </button>

                  <button class="pagination-btn" :disabled="currentPage === pageCount" @click="goNext">下一页</button>
                </div>
                <span class="total-text">共{{ total }}条记录</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <ModelEditDialog :modelType="activeTab" :visible.sync="editDialogVisible" :modelData="editModelData"
        @save="handleModelSave" />
      <TtsModel :visible.sync="ttsDialogVisible" :ttsModelId="selectedTtsModelId" />
      <AddModelDialog :modelType="activeTab" :visible.sync="addDialogVisible" @confirm="handleAddConfirm" />
    </div>
    <el-footer class="page-footer">
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import AddModelDialog from "@/components/AddModelDialog.vue";
import HeaderBar from "@/components/HeaderBar.vue";
import ModelEditDialog from "@/components/ModelEditDialog.vue";
import TtsModel from "@/components/TtsModel.vue";
import VersionFooter from "@/components/VersionFooter.vue";
export default {
  components: { HeaderBar, ModelEditDialog, TtsModel, AddModelDialog, VersionFooter },
  data() {
    return {
      addDialogVisible: false,
      activeTab: 'llm',
      search: '',
      editDialogVisible: false,
      editModelData: {},
      ttsDialogVisible: false,
      selectedTtsModelId: '',
      modelList: [],
      pageSizeOptions: [10, 20, 50, 100],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      selectedModels: [],
      isAllSelected: false,
      loading: false
    };
  },

  created() {
    this.loadData();
  },

  computed: {
    modelTypeText() {
      const map = {
        vad: '语言活动检测模型(VAD)',
        asr: '语音识别模型(ASR)',
        llm: '大语言模型（LLM）',
        vllm: '视觉大模型（VLLM）',
        intent: '意图识别模型(Intent)',
        tts: '语音合成模型(TTS)',
        memory: '记忆模型(Memory)'
      }
      return map[this.activeTab] || '模型配置'
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
    }
  },

  methods: {
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.loadData();
    },
    openTtsDialog(row) {
      this.selectedTtsModelId = row.id;
      this.ttsDialogVisible = true;
    },
    headerCellClassName({ column, columnIndex }) {
      if (columnIndex === 0) {
        return 'custom-selection-header';
      }
      return '';
    },
    handleMenuSelect(index) {
      this.activeTab = index;
      this.currentPage = 1;  // 重置到第一页
      this.pageSize = 10;     // 可选：重置每页条数
      this.loadData();
    },
    handleSearch() {
      this.currentPage = 1;
      this.loadData();
    },
    // 批量删除
    batchDelete() {
      if (this.selectedModels.length === 0) {
        this.$message.warning('请先选择要删除的模型')
        return
      }

      this.$confirm('确定要删除选中的模型吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const deletePromises = this.selectedModels.map(model =>
          new Promise(resolve => {
            Api.model.deleteModel(
              model.id,
              ({ data }) => resolve(data.code === 0)
            )
          })
        )

        Promise.all(deletePromises).then(results => {
          if (results.every(Boolean)) {
            this.$message.success({
              message: '批量删除成功',
              showClose: true
            })
            this.loadData()
          } else {
            this.$message.error({
              message: '部分删除失败',
              showClose: true
            })
          }
        })
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    addModel() {
      this.addDialogVisible = true;
    },
    editModel(model) {
      this.editModelData = JSON.parse(JSON.stringify(model));
      this.editDialogVisible = true;
    },
    // 删除单个模型
    deleteModel(model) {
      this.$confirm('确定要删除该模型吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        Api.model.deleteModel(
          model.id,
          ({ data }) => {
            if (data.code === 0) {
              this.$message.success({
                message: '删除成功',
                showClose: true
              })
              this.loadData()
            } else {
              this.$message.error({
                message: data.msg || '删除失败',
                showClose: true
              })
            }
          }
        )
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    handleCurrentChange(page) {
      this.currentPage = page;
      this.$refs.modelTable.clearSelection();
    },
    handleModelSave({ provideCode, formData, done }) {
      const modelType = this.activeTab;
      const id = formData.id;

      Api.model.updateModel(
        { modelType, provideCode, id, formData },
        ({ data }) => {
          if (data.code === 0) {
            this.$message.success('保存成功');
            this.loadData();
            this.editDialogVisible = false;
          } else {
            this.$message.error(data.msg || '保存失败');
          }
          done && done(); // 调用done回调关闭加载状态
        }
      );
    },
    selectAll() {
      if (this.isAllSelected) {
        this.$refs.modelTable.clearSelection();
      } else {
        this.$refs.modelTable.toggleAllSelection();
      }
    },
    handleSelectionChange(val) {
      this.selectedModels = val;
      this.isAllSelected = val.length === this.modelList.length;
      if (val.length === 0) {
        this.isAllSelected = false;
      }
    },

    // 新增模型配置
    handleAddConfirm(newModel) {
      const params = {
        modelType: this.activeTab,
        provideCode: newModel.provideCode,
        formData: {
          ...newModel,
          isDefault: newModel.isDefault ? 1 : 0,
          isEnabled: newModel.isEnabled ? 1 : 0,
          configJson: newModel.configJson
        }
      };

      Api.model.addModel(params, ({ data }) => {
        if (data.code === 0) {
          this.$message.success({
            message: '新增成功',
            showClose: true
          });
          this.loadData();
        } else {
          this.$message.error({
            message: data.msg || '新增失败',
            showClose: true
          });
        }
      });
    },

    // 分页器
    goFirst() {
      this.currentPage = 1;
      this.loadData();
    },
    goPrev() {
      if (this.currentPage > 1) {
        this.currentPage--;
        this.loadData();
      }
    },
    goNext() {
      if (this.currentPage < this.pageCount) {
        this.currentPage++;
        this.loadData();
      }
    },
    goToPage(page) {
      this.currentPage = page;
      this.loadData();
    },

    // 获取模型配置列表
    loadData() {
      this.loading = true; // 开始加载
      const params = {
        modelType: this.activeTab,
        modelName: this.search,
        page: this.currentPage,
        limit: this.pageSize
      };

      Api.model.getModelList(params, ({ data }) => {
        this.loading = false; // 结束加载
        if (data.code === 0) {
          this.modelList = data.data.list;
          this.total = data.data.total;
        } else {
          this.$message.error(data.msg || '获取模型列表失败');
        }
      });
    },
    // 处理启用/禁用状态变更
    handleStatusChange(model) {
      const newStatus = model.isEnabled ? 1 : 0
      const originalStatus = model.isEnabled

      model.isEnabled = !model.isEnabled

      Api.model.updateModelStatus(
        model.id,
        newStatus,
        ({ data }) => {
          if (data.code === 0) {
            this.$message.success(newStatus === 1 ? '启用成功' : '禁用成功')
            // 保持新状态
            model.isEnabled = newStatus
          } else {
            // 操作失败时恢复原状态
            model.isEnabled = originalStatus
            this.$message.error(data.msg || '操作失败')
          }
        }
      )
    },
    handleDefaultChange(model) {
      Api.model.setDefaultModel(model.id, ({ data }) => {
        if (data.code === 0) {
          this.$message.success('设置默认模型成功')
          this.loadData()
        }
      })
    }
  },
};
</script>

<style lang="scss" scoped>
@import './management.scss';

.model-config-layout {
  display: flex;
  gap: 20px;
}

.nav-panel {
  flex: 0 0 200px;
  border-right: 1px solid #ebeef5;
}

.content-area {
  flex: 1;
  background-color: transparent;
  padding: 0;
}

.page-footer {
  padding: 0;
}

@media (max-width: 992px) {
  .model-config-layout {
    flex-direction: column;
  }

  .nav-panel {
    flex: 0 0 auto;
    border-right: none;
    border-bottom: 1px solid #ebeef5;
  }
}
</style>
