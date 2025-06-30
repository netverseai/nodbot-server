<template>
    <div class="page-container">
        <HeaderBar />

        <div class="operation-bar">
            <h2 class="page-title">字典管理</h2>
            <div class="right-operations">
                <el-input placeholder="请输入字典值标签查询" v-model="search" class="search-input" clearable
                    @keyup.enter.native="handleSearch" />
                <el-button class="btn-search" @click="handleSearch">
                    搜索
                </el-button>
            </div>
        </div>

        <!-- 主体内容 -->
        <div class="main-wrapper">
            <div class="content-panel dict-layout">
                <!-- 左侧字典类型列表 -->
                <div class="dict-type-panel">
                    <div class="dict-type-header">
                        <el-button type="success" size="mini" @click="showAddDictTypeDialog">新增字典类型</el-button>
                        <el-button type="danger" size="mini" @click="batchDeleteDictType"
                            :disabled="selectedDictTypes.length === 0">
                            批量删除字典类型
                        </el-button>
                    </div>
                    <div class="table-responsive">
                        <el-table ref="dictTypeTable" :data="dictTypeList" style="width: 100%" v-loading="dictTypeLoading"
                            element-loading-text="拼命加载中" element-loading-spinner="el-icon-loading"
                            element-loading-background="rgba(255, 255, 255, 0.7)" @row-click="handleDictTypeRowClick"
                            @selection-change="handleDictTypeSelectionChange" :row-class-name="tableRowClassName"
                            class="dict-type-table">
                            <el-table-column type="selection" width="55" align="center"></el-table-column>
                            <el-table-column label="字典类型名称" prop="dictName" align="center"></el-table-column>
                            <el-table-column label="操作" width="100" align="center">
                                <template slot-scope="scope">
                                    <el-button type="text" size="mini" @click.stop="editDictType(scope.row)">编辑</el-button>
                                </template>
                            </el-table-column>
                        </el-table>
                    </div>
                </div>

                <!-- 右侧字典数据列表 -->
                <div class="content-area">
                    <el-card class="dict-data-card" shadow="never">
                        <div class="table-responsive">
                            <el-table ref="dictDataTable" :data="dictDataList" style="width: 100%"
                                v-loading="dictDataLoading" element-loading-text="拼命加载中"
                                element-loading-spinner="el-icon-loading"
                                element-loading-background="rgba(255, 255, 255, 0.7)" class="data-table"
                                header-row-class-name="table-header">
                                <el-table-column label="选择" align="center" width="55">
                                    <template slot-scope="scope">
                                        <el-checkbox v-model="scope.row.selected"></el-checkbox>
                                    </template>
                                </el-table-column>
                                <el-table-column label="字典标签" prop="dictLabel" align="center"></el-table-column>
                                <el-table-column label="字典值" prop="dictValue" align="center"></el-table-column>
                                <el-table-column label="排序" prop="sort" align="center"></el-table-column>
                                <el-table-column label="操作" align="center" width="180px" fixed="right">
                                    <template slot-scope="scope">
                                        <el-button type="text" size="mini" @click="editDictData(scope.row)"
                                            class="edit-btn">
                                            修改
                                        </el-button>
                                        <el-button type="text" size="mini" @click="deleteDictData(scope.row)"
                                            class="delete-btn">
                                            删除
                                        </el-button>
                                    </template>
                                </el-table-column>
                            </el-table>
                        </div>
                        <div class="table_bottom">
                            <div class="ctrl_btn">
                                <el-button size="mini" type="primary" @click="selectAllDictData">
                                    {{ isAllDictDataSelected ? '取消全选' : '全选' }}
                                </el-button>
                                <el-button type="success" size="mini" @click="showAddDictDataDialog" class="add-btn">
                                    新增字典数据
                                </el-button>
                                <el-button size="mini" type="danger" icon="el-icon-delete" @click="batchDeleteDictData">
                                    批量删除字典数据
                                </el-button>
                            </div>
                            <div class="custom-pagination">
                                <el-select v-model="pageSize" @change="handlePageSizeChange" class="page-size-select">
                                    <el-option v-for="item in pageSizeOptions" :key="item" :label="`${item}条/页`"
                                        :value="item">
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

        <!-- 使用字典类型编辑弹框组件 -->
        <DictTypeDialog :visible.sync="dictTypeDialogVisible" :title="dictTypeDialogTitle" :dictTypeData="dictTypeForm"
            @save="saveDictType" />

        <!-- 使用字典数据编辑弹框组件 -->
        <DictDataDialog :visible.sync="dictDataDialogVisible" :title="dictDataDialogTitle" :dictData="dictDataForm"
            :dictTypeId="selectedDictType?.id" @save="saveDictData" />
        <el-footer class="page-footer">
            <version-footer />
        </el-footer>
    </div>
</template>

<script>
import dictApi from '@/apis/module/dict'
import DictDataDialog from '@/components/DictDataDialog.vue'
import DictTypeDialog from '@/components/DictTypeDialog.vue'
import HeaderBar from '@/components/HeaderBar.vue'
import VersionFooter from '@/components/VersionFooter.vue'
export default {
    name: 'DictManagement',
    components: {
        HeaderBar,
        DictTypeDialog,
        DictDataDialog,
        VersionFooter
    },
    data() {
        return {
            // 字典类型相关
            dictTypeList: [],
            dictTypeLoading: false,
            selectedDictType: null,
            selectedDictTypes: [],  // 恢复多选数组
            dictTypeDialogVisible: false,
            dictTypeDialogTitle: '新增字典类型',
            dictTypeForm: {
                id: null,
                dictName: '',
                dictType: ''
            },

            // 字典数据相关
            dictDataList: [],
            dictDataLoading: false,
            isAllDictDataSelected: false,
            dictDataDialogVisible: false,
            dictDataDialogTitle: '新增字典数据',
            dictDataForm: {
                id: null,
                dictTypeId: null,
                dictLabel: '',
                dictValue: '',
                sort: 0
            },
            search: '',
            // 添加分页相关数据
            pageSizeOptions: [10, 20, 50, 100],
            currentPage: 1,
            pageSize: 10,
            total: 0
        }
    },
    created() {
        this.loadDictTypeList()
    },
    methods: {
        // 字典类型相关方法
        loadDictTypeList() {
            this.dictTypeLoading = true
            dictApi.getDictTypeList({
                page: 1,
                limit: 100,
                dictName: this.search
            }, ({ data }) => {
                if (data.code === 0) {
                    this.dictTypeList = data.data.list
                    if (this.dictTypeList.length > 0) {
                        this.selectedDictType = this.dictTypeList[0]
                        this.loadDictDataList(this.dictTypeList[0].id)
                        this.$nextTick(() => {
                            this.$refs.dictTypeTable.setCurrentRow(this.dictTypeList[0])
                        })
                    }
                }
                this.dictTypeLoading = false
            })
        },
        handleDictTypeRowClick(row) {
            this.selectedDictType = row
            this.loadDictDataList(row.id)
            this.$refs.dictTypeTable.setCurrentRow(row)
        },
        handleDictTypeSelectionChange(val) {
            this.selectedDictTypes = val
        },
        tableRowClassName({ row }) {
            return row === this.selectedDictType ? 'current-row' : ''
        },
        showAddDictTypeDialog() {
            this.dictTypeDialogTitle = '新增字典类型'
            this.dictTypeForm = {
                id: null,
                dictName: '',
                dictType: ''
            }
            this.dictTypeDialogVisible = true
        },
        editDictType(row) {
            this.dictTypeDialogTitle = '编辑字典类型'
            this.dictTypeForm = { ...row }
            this.dictTypeDialogVisible = true
        },
        saveDictType(formData) {
            const api = formData.id ? dictApi.updateDictType : dictApi.addDictType
            api(formData, ({ data }) => {
                if (data.code === 0) {
                    this.$message.success('保存成功')
                    this.dictTypeDialogVisible = false
                    this.loadDictTypeList()
                }
            })
        },
        batchDeleteDictType() {
            if (this.selectedDictTypes.length === 0) {
                this.$message.warning('请选择要删除的字典类型')
                return
            }

            this.$confirm('确定要删除选中的字典类型吗?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                const ids = this.selectedDictTypes.map(item => item.id)
                dictApi.deleteDictType(ids, ({ data }) => {
                    if (data.code === 0) {
                        this.$message.success('删除成功')
                        this.loadDictTypeList()
                    }
                })
            })
        },

        // 字典数据相关方法
        loadDictDataList(dictTypeId) {
            if (!dictTypeId) return
            this.dictDataLoading = true
            dictApi.getDictDataList({
                dictTypeId,
                page: this.currentPage,
                limit: this.pageSize,
                dictLabel: this.search,
                dictValue: ''
            }, ({ data }) => {
                if (data.code === 0) {
                    this.dictDataList = data.data.list.map(item => ({
                        ...item,
                        selected: false
                    }))
                    this.total = data.data.total
                } else {
                    this.$message.error(data.msg || '获取字典数据失败')
                }
                this.dictDataLoading = false
            })
        },
        selectAllDictData() {
            this.isAllDictDataSelected = !this.isAllDictDataSelected
            this.dictDataList.forEach(row => {
                row.selected = this.isAllDictDataSelected
            })
        },
        showAddDictDataDialog() {
            if (!this.selectedDictType) {
                this.$message.warning('请先选择字典类型')
                return
            }
            this.dictDataDialogTitle = '新增字典数据'
            this.dictDataForm = {
                id: null,
                dictTypeId: this.selectedDictType.id,
                dictLabel: '',
                dictValue: '',
                sort: 0
            }
            this.dictDataDialogVisible = true
        },
        editDictData(row) {
            this.dictDataDialogTitle = '编辑字典数据'
            this.dictDataForm = { ...row }
            this.dictDataDialogVisible = true
        },
        saveDictData(formData) {
            const api = formData.id ? dictApi.updateDictData : dictApi.addDictData
            api(formData, ({ data }) => {
                if (data.code === 0) {
                    this.$message.success('保存成功')
                    this.dictDataDialogVisible = false
                    this.loadDictDataList(formData.dictTypeId)
                }
            })
        },
        deleteDictData(row) {
            this.$confirm('确定要删除该字典数据吗?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                dictApi.deleteDictData([row.id], ({ data }) => {
                    if (data.code === 0) {
                        this.$message.success('删除成功')
                        this.loadDictDataList(row.dictTypeId)
                    }
                })
            })
        },
        batchDeleteDictData() {
            const selectedRows = this.dictDataList.filter(row => row.selected)
            if (selectedRows.length === 0) {
                this.$message.warning('请选择要删除的字典数据')
                return
            }

            this.$confirm(`确定要删除选中的${selectedRows.length}个字典数据吗?`, '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                const ids = selectedRows.map(item => item.id)
                dictApi.deleteDictData(ids, ({ data }) => {
                    if (data.code === 0) {
                        this.$message.success('删除成功')
                        this.loadDictDataList(this.selectedDictType.id)
                    }
                })
            })
        },
        handleSearch() {
            if (!this.selectedDictType) {
                this.$message.warning('请先选择字典类型')
                return
            }
            this.currentPage = 1
            this.loadDictDataList(this.selectedDictType.id)
        },
        // 添加分页相关方法
        handlePageSizeChange(val) {
            this.pageSize = val;
            this.currentPage = 1;
            this.loadDictDataList(this.selectedDictType?.id);
        },
        goFirst() {
            this.currentPage = 1;
            this.loadDictDataList(this.selectedDictType?.id);
        },
        goPrev() {
            if (this.currentPage > 1) {
                this.currentPage--;
                this.loadDictDataList(this.selectedDictType?.id);
            }
        },
        goNext() {
            if (this.currentPage < this.pageCount) {
                this.currentPage++;
                this.loadDictDataList(this.selectedDictType?.id);
            }
        },
        goToPage(page) {
            this.currentPage = page;
            this.loadDictDataList(this.selectedDictType?.id);
        }
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
        }
    }
}
</script>

<style lang="scss" scoped>
@import './management.scss';

.dict-layout {
    display: flex;
    gap: 20px;
}

.dict-type-panel {
    flex: 0 0 300px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    background: white;
    padding: 15px;
    border-radius: 4px;
}

.dict-type-header {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
}

.content-area {
    flex: 1;
    background-color: transparent;
    padding: 0;
}

.page-footer {
    padding: 0;
}

:deep(.current-row) {
    background-color: #ecf5ff !important;
}

@media (max-width: 992px) {
    .dict-layout {
        flex-direction: column;
    }

    .dict-type-panel {
        flex: 0 0 auto;
    }
}
</style>