<template>
  <div class="page-container">
    <HeaderBar />

    <div class="operation-bar">
      <h2 class="page-title">设备管理</h2>
      <div class="right-operations">
        <el-input placeholder="请输入设备型号或Mac地址查询" v-model="searchKeyword" class="search-input"
          @keyup.enter.native="handleSearch" clearable />
        <el-button class="btn-search" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="device-card" shadow="never">
            <div class="table-responsive">
              <el-table ref="deviceTable" :data="paginatedDeviceList" class="transparent-table"
                :header-cell-class-name="headerCellClassName" v-loading="loading" element-loading-text="拼命加载中"
                element-loading-spinner="el-icon-loading" element-loading-background="rgba(255, 255, 255, 0.7)">
                <el-table-column label="选择" align="center" width="120">
                  <template slot-scope="scope">
                    <el-checkbox v-model="scope.row.selected"></el-checkbox>
                  </template>
                </el-table-column>
                <el-table-column label="设备型号" prop="model" align="center">
                  <template slot-scope="scope">
                    {{ getFirmwareTypeName(scope.row.model) }}
                  </template>
                </el-table-column>
                <el-table-column label="固件版本" prop="firmwareVersion" align="center"></el-table-column>
                <el-table-column label="Mac地址" prop="macAddress" align="center"></el-table-column>
                <el-table-column label="绑定时间" prop="bindTime" align="center"></el-table-column>
                <el-table-column label="最近对话" prop="lastConversation" align="center"></el-table-column>
                <el-table-column label="备注" align="center" min-width="150">
                  <template #default="{ row }">
                    <el-input v-show="row.isEdit" v-model="row.remark" size="mini" maxlength="64" show-word-limit
                      @blur="onRemarkBlur(row)" @keyup.enter.native="onRemarkEnter(row)" />
                    <span v-show="!row.isEdit" class="remark-view">
                      <i class="el-icon-edit" @click="row.isEdit = true" style="cursor: pointer;"></i>
                      <span @click="row.isEdit = true">
                        {{ row.remark || '—' }}
                      </span>
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="OTA升级" align="center">
                  <template slot-scope="scope">
                    <el-switch v-model="scope.row.otaSwitch" size="mini" active-color="#13ce66"
                      inactive-color="#ff4949" @change="handleOtaSwitchChange(scope.row)"></el-switch>
                  </template>
                </el-table-column>
                <el-table-column label="操作" align="center" fixed="right">
                  <template slot-scope="scope">
                    <el-button size="mini" type="text" @click="handleUnbind(scope.row.device_id)">
                      解绑
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="table_bottom">
              <div class="ctrl_btn">
                <el-button size="mini" type="primary" class="select-all-btn" @click="handleSelectAll">
                  {{ isAllSelected ? '取消全选' : '全选' }}
                </el-button>
                <el-button type="success" size="mini" class="add-device-btn" @click="handleAddDevice">
                  新增
                </el-button>
                <el-button size="mini" type="danger" icon="el-icon-delete" @click="deleteSelected">解绑</el-button>
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
                <span class="total-text">共{{ deviceList.length }}条记录</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <AddDeviceDialog :visible.sync="addDeviceDialogVisible" :agent-id="currentAgentId"
      @refresh="fetchBindDevices(currentAgentId)" />

  </div>
</template>

<script>
import Api from '@/apis/api';
import AddDeviceDialog from "@/components/AddDeviceDialog.vue";
import HeaderBar from "@/components/HeaderBar.vue";

export default {
  components: {HeaderBar, AddDeviceDialog},
  data() {
    return {
      addDeviceDialogVisible: false,
      selectedDevices: [],
      isAllSelected: false,
      searchKeyword: "",
      activeSearchKeyword: "",
      currentAgentId: this.$route.query.agentId || '',
      currentPage: 1,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      deviceList: [],
      loading: false,
      userApi: null,
      firmwareTypes: [],
    };
  },
  computed: {
    filteredDeviceList() {
      const keyword = this.activeSearchKeyword.toLowerCase();
      if (!keyword) return this.deviceList;
      return this.deviceList.filter(device =>
          (device.model && device.model.toLowerCase().includes(keyword)) ||
          (device.macAddress && device.macAddress.toLowerCase().includes(keyword))
      );
    },

    paginatedDeviceList() {
      const start = (this.currentPage - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.filteredDeviceList.slice(start, end);
    },
    pageCount() {
      return Math.ceil(this.filteredDeviceList.length / this.pageSize);
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
  mounted() {
    const agentId = this.$route.query.agentId;
    if (agentId) {
      this.fetchBindDevices(agentId);
    }
  },
  created() {
    this.getFirmwareTypes()
  },
  methods: {
    async getFirmwareTypes() {
      try {
        const res = await Api.dict.getDictDataByType('FIRMWARE_TYPE')
        this.firmwareTypes = res.data
      } catch (error) {
        console.error('获取固件类型失败:', error)
        this.$message.error(error.message || '获取固件类型失败')
      }
    },
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
    },
    handleSearch() {
      this.activeSearchKeyword = this.searchKeyword;
      this.currentPage = 1;
    },

    handleSelectAll() {
      this.isAllSelected = !this.isAllSelected;
      this.paginatedDeviceList.forEach(row => {
        row.selected = this.isAllSelected;
      });
      this.selectedDevices = this.paginatedDeviceList.filter(device => device.selected);
    },

    deleteSelected() {
      this.selectedDevices = this.paginatedDeviceList.filter(device => device.selected);
      if (this.selectedDevices.length === 0) {
        this.$message.warning({
          message: '请至少选择一条记录',
          showClose: true
        });
        return;
      }

      this.$confirm(`确认要解绑选中的 ${this.selectedDevices.length} 台设备吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        const deviceIds = this.selectedDevices.map(device => device.device_id);
        this.batchUnbindDevices(deviceIds);
      });
    },
    batchUnbindDevices(deviceIds) {
      const promises = deviceIds.map(id => {
        return new Promise((resolve, reject) => {
          Api.device.unbindDevice(id, ({data}) => {
            if (data.code === 0) {
              resolve();
            } else {
              reject(data.msg || '解绑失败');
            }
          });
        });
      });
      Promise.all(promises)
          .then(() => {
            this.$message.success({
              message: `成功解绑 ${deviceIds.length} 台设备`,
              showClose: true
            });
            this.fetchBindDevices(this.currentAgentId);
            this.selectedDevices = [];
            this.isAllSelected = false;
          })
          .catch(error => {
            this.$message.error({
              message: error || '批量解绑过程中出现错误',
              showClose: true
            });
          });
    },
    handleAddDevice() {
      this.addDeviceDialogVisible = true;
    },
    submitRemark(row) {
      if (row._submitting) return;

      const text = (row.remark || '').trim();
      if (text.length > 64) {
        this.$message.warning('备注不能超过 64 字符');
        return;
      }
      if (text === row._originalRemark) {
        return;
      }

      row._submitting = true;
      this.updateDeviceInfo(row.device_id, { alias: text }, (ok, resp) => {
        if (ok) {
          row._originalRemark = text;
          this.$message.success('备注已保存');
        } else {
          row.remark = row._originalRemark;
          this.$message.error(resp.msg || '备注保存失败');
        }
        row._submitting = false;
      });
    },
    // 备注输入框：失焦时提交
    onRemarkBlur(row) {
      row.isEdit = false;
      setTimeout(() => {
        this.submitRemark(row);
      }, 100); // 延迟 100ms，避开 enter+blur 同时触发的窗口
    },
    // 备注输入框：按回车时提交
    onRemarkEnter(row) {
      row.isEdit = false;
      this.submitRemark(row);
    },
    handleUnbind(device_id) {
      this.$confirm('确认要解绑该设备吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        Api.device.unbindDevice(device_id, ({data}) => {
          if (data.code === 0) {
            this.$message.success({
              message: '设备解绑成功',
              showClose: true
            });
            this.fetchBindDevices(this.$route.query.agentId);
          } else {
            this.$message.error({
              message: data.msg || '设备解绑失败',
              showClose: true
            });
          }
        });
      });
    },
    goFirst() {
      this.currentPage = 1;
    },
    goPrev() {
      if (this.currentPage > 1) this.currentPage--;
    },
    goNext() {
      if (this.currentPage < this.pageCount) this.currentPage++;
    },
    goToPage(page) {
      this.currentPage = page;
    },

    fetchBindDevices(agentId) {
      this.loading = true;
      Api.device.getAgentBindDevices(agentId, ({data}) => {
        this.loading = false;
        if (data.code === 0) {
          this.deviceList = data.data.map(device => {
            return {
              device_id: device.id,
              model: device.board,
              firmwareVersion: device.appVersion,
              macAddress: device.macAddress,
              bindTime: device.createDate,
              lastConversation: device.lastConnectedAt,
              remark: device.alias,
              _originalRemark: device.alias,
              isEdit: false,
              _submitting: false,
              otaSwitch: device.autoUpdate === 1,
              rawBindTime: new Date(device.createDate).getTime()
            };
          })
              .sort((a, b) => a.rawBindTime - b.rawBindTime);
          this.activeSearchKeyword = "";
          this.searchKeyword = "";
        } else {
          this.$message.error(data.msg || '获取设备列表失败');
        }
      });
    },
    headerCellClassName({columnIndex}) {
      if (columnIndex === 0) {
        return "custom-selection-header";
      }
      return "";
    },
    getFirmwareTypeName(type) {
      const firmwareType = this.firmwareTypes.find(item => item.key === type)
      return firmwareType ? firmwareType.name : type
    },
    updateDeviceInfo(device_id, payload, callback) {
      return Api.device.updateDeviceInfo(device_id, payload, ({data}) => {
        callback(data.code === 0, data);
      })
    },
    handleOtaSwitchChange(row) {
      this.updateDeviceInfo(row.device_id, {autoUpdate: row.otaSwitch ? 1 : 0}, (result, {msg}) => {
        if (result) {
          this.$message.success(row.otaSwitch ? '已设置成自动升级' : '已关闭自动升级');
          return;
        }
        row.otaSwitch = !row.otaSwitch
        this.$message.error(msg || '操作失败')
      })
    },
  }
};
</script>

<style lang="scss" scoped>
@import './management.scss';

.remark-view {
  display: flex;
  align-items: center;
  gap: 5px;
}
</style>
