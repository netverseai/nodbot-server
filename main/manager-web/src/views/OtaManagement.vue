<template>
    <div class="management-container">
        <HeaderBar />

        <div class="operation-bar">
            <h2 class="page-title">固件管理</h2>
            <div class="right-operations">
                <el-input placeholder="请输入固件名称查询" v-model="searchName" class="search-input"
                    @keyup.enter.native="handleSearch" clearable />
                <el-button class="btn-search" @click="handleSearch">搜索</el-button>
            </div>
        </div>

        <div class="content-panel">
            <el-table ref="paramsTable" :data="paramsList" class="management-table" v-loading="loading"
                element-loading-text="拼命加载中" element-loading-spinner="el-icon-loading"
                element-loading-background="rgba(255, 255, 255, 0.7)"
                :header-cell-class-name="headerCellClassName">
                <el-table-column label="选择" align="center" width="120">
                    <template slot-scope="scope">
                        <el-checkbox v-model="scope.row.selected"></el-checkbox>
                    </template>
                </el-table-column>
                <el-table-column label="固件名称" prop="firmwareName" align="center"></el-table-column>
                <el-table-column label="固件类型" prop="type" align="center">
                    <template slot-scope="scope">
                        {{ getFirmwareTypeName(scope.row.type) }}
                    </template>
                </el-table-column>
                <el-table-column label="版本号" prop="version" align="center"></el-table-column>
                <el-table-column label="文件大小" prop="size" align="center">
                    <template slot-scope="scope">
                        {{ formatFileSize(scope.row.size) }}
                    </template>
                </el-table-column>
                <el-table-column label="备注" prop="remark" align="center"
                    show-overflow-tooltip></el-table-column>
                <el-table-column label="创建时间" prop="createDate" align="center">
                    <template slot-scope="scope">
                        {{ formatDate(scope.row.createDate) }}
                    </template>
                </el-table-column>
                <el-table-column label="更新时间" prop="updateDate" align="center">
                    <template slot-scope="scope">
                        {{ formatDate(scope.row.updateDate) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" align="center">
                    <template slot-scope="scope">
                        <el-button size="mini" type="text"
                            @click="downloadFirmware(scope.row)">下载</el-button>
                        <el-button size="mini" type="text" @click="editParam(scope.row)">编辑</el-button>
                        <el-button size="mini" type="text" @click="deleteParam(scope.row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pagination-container">
                <div class="left-actions">
                    <el-button size="mini" type="primary" class="select-all-btn" @click="handleSelectAll">
                        {{ isAllSelected ? '取消全选' : '全选' }}
                    </el-button>
                    <el-button size="mini" type="success" @click="showAddDialog"
                        style="background: #5bc98c;border: None;">新增</el-button>
                    <el-button size="mini" type="danger" icon="el-icon-delete"
                        @click="deleteSelectedParams">删除</el-button>
                </div>
                <div class="custom-pagination">
                    <el-select v-model="pageSize" @change="handlePageSizeChange" class="page-size-select">
                        <el-option v-for="item in pageSizeOptions" :key="item" :label="`${item}条/页`"
                            :value="item">
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
                    </button>
                    <button class="pagination-btn" :disabled="currentPage === pageCount" @click="goNext">
                        下一页
                    </button>
                    <span class="total-text">共{{ total }}条记录</span>
                </div>
            </div>
        </div>

        <!-- 新增/编辑固件对话框 -->
        <firmware-dialog :title="dialogTitle" :visible.sync="dialogVisible" :form="firmwareForm"
            :firmware-types="firmwareTypes" @submit="handleSubmit" @cancel="dialogVisible = false" />
        <el-footer>
            <version-footer />
        </el-footer>
    </div>
</template>

<script>
import Api from "@/apis/api";
import FirmwareDialog from "@/components/FirmwareDialog.vue";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import { formatDate, formatFileSize } from "@/utils/format";

export default {
    components: { HeaderBar, FirmwareDialog, VersionFooter },
    data() {
        return {
            searchName: "",
            loading: false,
            paramsList: [],
            firmwareList: [],
            currentPage: 1,
            pageSize: 10,
            pageSizeOptions: [10, 20, 50, 100],
            total: 0,
            dialogVisible: false,
            dialogTitle: "新增固件",
            isAllSelected: false,
            firmwareForm: {
                id: null,
                firmwareName: "",
                type: "",
                version: "",
                size: 0,
                remark: "",
                firmwarePath: ""
            },
            firmwareTypes: [],
        };
    },
    created() {
        this.fetchFirmwareList();
        this.getFirmwareTypes();
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
            this.fetchFirmwareList();
        },
        fetchFirmwareList() {
            this.loading = true;
            const params = {
                pageNum: this.currentPage,
                pageSize: this.pageSize,
                firmwareName: this.searchName || "",
                orderField: "create_date",
                order: "desc"
            };
            Api.ota.getOtaList(params, (res) => {
                this.loading = false;
                res = res.data
                if (res.code === 0) {
                    this.firmwareList = res.data.list.map(item => ({
                        ...item,
                        selected: false
                    }));
                    this.paramsList = this.firmwareList;
                    this.total = res.data.total || 0;
                } else {
                    this.firmwareList = [];
                    this.paramsList = [];
                    this.total = 0;
                    this.$message.error({
                        message: res?.data?.msg || '获取固件列表失败',
                        showClose: true
                    });
                }
            });
        },
        handleSearch() {
            this.currentPage = 1;
            this.fetchFirmwareList();
        },
        handleSelectAll() {
            this.isAllSelected = !this.isAllSelected;
            this.firmwareList.forEach(row => {
                row.selected = this.isAllSelected;
            });
        },
        showAddDialog() {
            this.dialogTitle = "新增固件";
            // 完全重置表单数据
            this.firmwareForm = {
                id: null,
                firmwareName: "",
                type: "",
                version: "",
                size: 0,
                remark: "",
                firmwarePath: ""
            };
            this.$nextTick(() => {
                // 重置表单的校验状态
                if (this.$refs.firmwareDialog && this.$refs.firmwareDialog.$refs.form) {
                    this.$refs.firmwareDialog.$refs.form.clearValidate();
                }
            });
            this.dialogVisible = true;
        },
        editParam(row) {
            this.dialogTitle = "编辑固件";
            this.firmwareForm = { ...row };
            this.dialogVisible = true;
        },
        handleSubmit(form) {
            if (form.id) {
                // 编辑
                Api.ota.updateOta(form.id, form, (res) => {
                    res = res.data;
                    if (res.code === 0) {
                        this.$message.success({
                            message: "修改成功",
                            showClose: true
                        });
                        this.dialogVisible = false;
                        this.fetchFirmwareList();
                    } else {
                        this.$message.error({
                            message: res.msg || "修改失败",
                            showClose: true
                        });
                    }
                });
            } else {
                // 新增
                Api.ota.saveOta(form, (res) => {
                    res = res.data;
                    if (res.code === 0) {
                        this.$message.success({
                            message: "新增成功",
                            showClose: true
                        });
                        this.dialogVisible = false;
                        this.fetchFirmwareList();
                    } else {
                        this.$message.error({
                            message: res.msg || "新增失败",
                            showClose: true
                        });
                    }
                });
            }
        },

        deleteSelectedParams() {
            const selectedRows = this.firmwareList.filter(row => row.selected);
            if (selectedRows.length === 0) {
                this.$message.warning({
                    message: "请先选择需要删除的固件",
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
            this.$confirm(`确定要删除选中的${paramCount}个固件吗？`, '警告', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning',
                distinguishCancelAndClose: true
            }).then(() => {
                const ids = params.map(param => param.id);
                if (ids.some(id => !id)) {
                    this.$message.error({
                        message: '存在无效的参数ID',
                        showClose: true
                    });
                    return;
                }

                Api.ota.deleteOta(ids, (res) => {
                    res = res.data;
                    if (res.code === 0) {
                        this.$message.success({
                            message: `成功删除${paramCount}个固件`,
                            showClose: true
                        });
                        this.fetchFirmwareList();
                    } else {
                        this.$message.error({
                            message: res.msg || '删除失败，请重试',
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
            this.fetchFirmwareList();
        },
        goPrev() {
            if (this.currentPage > 1) {
                this.currentPage--;
                this.fetchFirmwareList();
            }
        },
        goNext() {
            if (this.currentPage < this.pageCount) {
                this.currentPage++;
                this.fetchFirmwareList();
            }
        },
        goToPage(page) {
            this.currentPage = page;
            this.fetchFirmwareList();
        },
        downloadFirmware(firmware) {
            if (!firmware || !firmware.id) {
                this.$message.error('固件信息不完整');
                return;
            }
            // 先获取下载链接
            Api.ota.getDownloadUrl(firmware.id, (res) => {
                if (res.data.code === 0) {
                    const uuid = res.data.data;
                    const baseUrl = process.env.VUE_APP_API_BASE_URL || '';
                    window.open(`${window.location.origin}${baseUrl}/otaMag/download/${uuid}`);
                } else {
                    this.$message.error('获取下载链接失败');
                }
            });
        },
        formatDate,
        formatFileSize,
        async getFirmwareTypes() {
            try {
                const res = await Api.dict.getDictDataByType('FIRMWARE_TYPE')
                this.firmwareTypes = res.data
            } catch (error) {
                console.error('获取固件类型失败:', error)
                this.$message.error(error.message || '获取固件类型失败')
            }
        },
        getFirmwareTypeName(type) {
            const firmwareType = this.firmwareTypes.find(item => item.key === type)
            return firmwareType ? firmwareType.name : type
        },
    },
};
</script>

<style lang="scss" scoped>
@import "./management.scss";
</style>
