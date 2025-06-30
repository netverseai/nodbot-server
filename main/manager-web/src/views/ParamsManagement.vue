<template>
    <div class="page-container">
        <HeaderBar />

        <div class="operation-bar">
            <h2 class="page-title">参数管理</h2>
            <div class="right-operations">
                <el-input placeholder="请输入参数编码或备注查询" v-model="searchCode" class="search-input"
                    @keyup.enter.native="handleSearch" clearable />
                <el-button class="btn-search" @click="handleSearch">搜索</el-button>
            </div>
        </div>

        <div class="main-wrapper">
            <div class="content-panel">
                <div class="content-area">
                    <el-card class="params-card" shadow="never">
                        <div class="table-responsive">
                            <el-table ref="paramsTable" :data="paramsList" class="transparent-table" v-loading="loading"
                                element-loading-text="拼命加载中" element-loading-spinner="el-icon-loading"
                                element-loading-background="rgba(255, 255, 255, 0.7)"
                                :header-cell-class-name="headerCellClassName">
                                <el-table-column label="选择" align="center" width="120">
                                    <template slot-scope="scope">
                                        <el-checkbox v-model="scope.row.selected"></el-checkbox>
                                    </template>
                                </el-table-column>
                                <el-table-column label="参数编码" prop="paramCode" align="center"></el-table-column>
                                <el-table-column label="参数值" prop="paramValue" align="center" show-overflow-tooltip>
                                    <template slot-scope="scope">
                                        <div v-if="isSensitiveParam(scope.row.paramCode)" class="sensitive-value-cell">
                                            <span v-if="!scope.row.showValue">{{ maskSensitiveValue(scope.row.paramValue)
                                            }}</span>
                                            <span v-else>{{ scope.row.paramValue }}</span>
                                            <el-button size="mini" type="text" @click="toggleSensitiveValue(scope.row)">
                                                {{ scope.row.showValue ? '隐藏' : '查看' }}
                                            </el-button>
                                        </div>
                                        <span v-else>{{ scope.row.paramValue }}</span>
                                    </template>
                                </el-table-column>
                                <el-table-column label="备注" prop="remark" align="center"></el-table-column>
                                <el-table-column label="操作" align="center" fixed="right">
                                    <template slot-scope="scope">
                                        <el-button size="mini" type="text" @click="editParam(scope.row)">编辑</el-button>
                                        <el-button size="mini" type="text" @click="deleteParam(scope.row)">删除</el-button>
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
                                <el-button size="mini" type="danger" icon="el-icon-delete"
                                    @click="deleteSelectedParams">删除</el-button>
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

        <!-- 新增/编辑参数对话框 -->
        <param-dialog :title="dialogTitle" :visible.sync="dialogVisible" :form="paramForm" @submit="handleSubmit"
            @cancel="dialogVisible = false" />
        <el-footer class="page-footer">
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
            searchCode: "",
            paramsList: [],
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
            Api.admin.getParamsList(
                {
                    page: this.currentPage,
                    limit: this.pageSize,
                    paramCode: this.searchCode,
                },
                ({ data }) => {
                    this.loading = false;
                    if (data.code === 0) {
                        this.paramsList = data.data.list.map(item => ({
                            ...item,
                            selected: false,
                            showValue: false
                        }));
                        this.total = data.data.total;
                    } else {
                        this.$message.error({
                            message: data.msg || '获取参数列表失败',
                            showClose: true
                        });
                    }
                }
            );
        },
        handleSearch() {
            this.currentPage = 1;
            this.fetchParams();
        },
        handleSelectAll() {
            this.isAllSelected = !this.isAllSelected;
            this.paramsList.forEach(row => {
                row.selected = this.isAllSelected;
            });
        },
        showAddDialog() {
            this.dialogTitle = "新增参数";
            this.paramForm = {
                id: null,
                paramCode: "",
                paramValue: "",
                remark: ""
            };
            this.dialogVisible = true;
        },
        editParam(row) {
            this.dialogTitle = "编辑参数";
            this.paramForm = { ...row };
            this.dialogVisible = true;
        },

        handleSubmit({ form, done }) {
            if (form.id) {
                // 编辑
                Api.admin.updateParam(form, ({ data }) => {
                    if (data.code === 0) {
                        this.$message.success({
                            message: "修改成功",
                            showClose: true
                        });
                        this.dialogVisible = false;
                        this.fetchParams();
                    }
                    done && done();
                });
            } else {
                // 新增
                Api.admin.addParam(form, ({ data }) => {
                    if (data.code === 0) {
                        this.$message.success({
                            message: "新增成功",
                            showClose: true
                        });
                        this.dialogVisible = false;
                        this.fetchParams();
                    }
                    done && done();
                });
            }
        },

        deleteSelectedParams() {
            const selectedRows = this.paramsList.filter(row => row.selected);
            if (selectedRows.length === 0) {
                this.$message.warning({
                    message: "请先选择需要删除的参数",
                    showClose: true
                });
                return;
            }
            this.deleteParam(selectedRows);
        },
        deleteParam(row) {
            // 处理单个参数或参数数组
            const params = Array.isArray(row) ? row : [row];

            if (Array.isArray(row) && row.length === 0) {
                this.$message.warning({
                    message: "请先选择需要删除的参数",
                    showClose: true
                });
                return;
            }

            const paramCount = params.length;
            this.$confirm(`确定要删除选中的${paramCount}个参数吗？`, '警告', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
                distinguishCancelAndClose: true
            }).then(() => {
                const ids = params.map(param => param.id);
                if (ids.some(id => isNaN(id))) {
                    this.$message.error({
                        message: '存在无效的参数ID',
                        showClose: true
                    });
                    return;
                }

                Api.admin.deleteParam(ids, ({ data }) => {
                    if (data.code === 0) {
                        this.$message.success({
                            message: `成功删除${paramCount}个参数`,
                            showClose: true
                        });
                        this.fetchParams();
                    } else {
                        this.$message.error({
                            message: data.msg || '删除失败，请重试',
                            showClose: true
                        });
                    }
                });
            }).catch(action => {
                if (action === 'cancel') {
                    this.$message({
                        type: 'info',
                        message: '已取消删除操作',
                        duration: 1000
                    });
                } else {
                    this.$message({
                        type: 'info',
                        message: '操作已关闭',
                        duration: 1000
                    });
                }
            });
        },
        headerCellClassName({ columnIndex }) {
            if (columnIndex === 0) {
                return "custom-selection-header";
            }
            return "";
        },
        goFirst() {
            this.currentPage = 1;
            this.fetchParams();
        },
        goPrev() {
            if (this.currentPage > 1) {
                this.currentPage--;
                this.fetchParams();
            }
        },
        goNext() {
            if (this.currentPage < this.pageCount) {
                this.currentPage++;
                this.fetchParams();
            }
        },
        goToPage(page) {
            this.currentPage = page;
            this.fetchParams();
        },
        isSensitiveParam(paramCode) {
            return this.sensitive_keys.some(key => paramCode.toLowerCase().includes(key.toLowerCase()));
        },
        maskSensitiveValue(value) {
            if (!value) return '';
            if (value.length <= 8) return '****';
            return value.substring(0, 4) + '****' + value.substring(value.length - 4);
        },
        toggleSensitiveValue(row) {
            this.$set(row, 'showValue', !row.showValue);
        },
    },
};
</script>

<style lang="scss" scoped>
@import './management.scss';

.sensitive-value-cell {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
}

.page-footer {
    padding: 0;
}
</style>
