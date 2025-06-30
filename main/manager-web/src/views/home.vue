<template>
  <div class="home-container">
    <!-- 公共头部 -->
    <HeaderBar :devices="devices" @search="handleSearch" @search-reset="handleSearchReset" />
    <el-main class="home-main">
      <div>
        <!-- 首页内容 -->
        <div class="add-device">
          <div class="add-device-bg">
            <div class="hello-text-container">
              <div class="hello-text">
                你好，Nodbot
              </div>
              <div class="hello-text">
                让我们度过
                <span style="color: #ed1c24;">美好的一天！</span>
              </div>
              <div class="hi-hint">
                Hello, Let's have a wonderful day!
              </div>
            </div>
            <div class="add-device-btn" @click="showAddDialog">
              <div class="left-add">
                添加智能体
              </div>
              <div class="arrow-deco" />
              <div class="right-add">
                <i class="el-icon-right" />
              </div>
            </div>
          </div>
        </div>
        <div class="device-list-container">
          <template v-if="isLoading">
            <div v-for="i in skeletonCount" :key="'skeleton-' + i" class="skeleton-item">
              <div class="skeleton-image"></div>
              <div class="skeleton-content">
                <div class="skeleton-line"></div>
                <div class="skeleton-line-short"></div>
              </div>
            </div>
          </template>

          <template v-else>
            <DeviceItem v-for="(item, index) in devices" :key="index" :device="item" @configure="goToRoleConfig"
              @deviceManage="handleDeviceManage" @delete="handleDeleteAgent" @chat-history="handleShowChatHistory" />
          </template>
        </div>
      </div>
      <AddWisdomBodyDialog :visible.sync="addDeviceDialogVisible" @confirm="handleWisdomBodyAdded" />
    </el-main>
    <el-footer class="home-footer">
      <version-footer />
    </el-footer>
    <chat-history-dialog :visible.sync="showChatHistory" :agent-id="currentAgentId" :agent-name="currentAgentName" />
  </div>

</template>

<script>
import Api from '@/apis/api';
import AddWisdomBodyDialog from '@/components/AddWisdomBodyDialog.vue';
import ChatHistoryDialog from '@/components/ChatHistoryDialog.vue';
import DeviceItem from '@/components/DeviceItem.vue';
import HeaderBar from '@/components/HeaderBar.vue';
import VersionFooter from '@/components/VersionFooter.vue';

export default {
  name: 'HomePage',
  components: { DeviceItem, AddWisdomBodyDialog, HeaderBar, VersionFooter, ChatHistoryDialog },
  data() {
    return {
      addDeviceDialogVisible: false,
      devices: [],
      originalDevices: [],
      isSearching: false,
      searchRegex: null,
      isLoading: true,
      skeletonCount: localStorage.getItem('skeletonCount') || 8,
      showChatHistory: false,
      currentAgentId: '',
      currentAgentName: ''
    }
  },

  mounted() {
    this.fetchAgentList();
  },

  methods: {
    showAddDialog() {
      this.addDeviceDialogVisible = true
    },
    goToRoleConfig() {
      // 点击配置角色后跳转到角色配置页
      this.$router.push('/role-config')
    },
    handleWisdomBodyAdded(res) {
      this.fetchAgentList();
      this.addDeviceDialogVisible = false;
    },
    handleDeviceManage() {
      this.$router.push('/device-management');
    },
    handleSearch(regex) {
      this.isSearching = true;
      this.searchRegex = regex;
      this.applySearchFilter();
    },
    handleSearchReset() {
      this.isSearching = false;
      this.searchRegex = null;
      this.devices = [...this.originalDevices];
    },
    applySearchFilter() {
      if (!this.isSearching || !this.searchRegex) {
        this.devices = [...this.originalDevices];
        return;
      }

      this.devices = this.originalDevices.filter(device => {
        return this.searchRegex.test(device.agentName);
      });
    },
    // 搜索更新智能体列表
    handleSearchResult(filteredList) {
      this.devices = filteredList; // 更新设备列表
    },
    // 获取智能体列表
    fetchAgentList() {
      this.isLoading = true;
      Api.agent.getAgentList(({ data }) => {
        if (data?.data) {
          this.originalDevices = data.data.map(item => ({
            ...item,
            agentId: item.id
          }));

          // 动态设置骨架屏数量（可选）
          this.skeletonCount = Math.min(
            Math.max(this.originalDevices.length, 3), // 最少3个
            10 // 最多10个
          );

          this.handleSearchReset();
        }
        this.isLoading = false;
      }, (error) => {
        console.error('Failed to fetch agent list:', error);
        this.isLoading = false;
      });
    },
    // 删除智能体
    handleDeleteAgent(agentId) {
      this.$confirm('确定要删除该智能体吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        Api.agent.deleteAgent(agentId, (res) => {
          if (res.data.code === 0) {
            this.$message.success({
              message: '删除成功',
              showClose: true
            });
            this.fetchAgentList(); // 刷新列表
          } else {
            this.$message.error({
              message: res.data.msg || '删除失败',
              showClose: true
            });
          }
        });
      }).catch(() => { });
    },
    handleShowChatHistory({ agentId, agentName }) {
      this.currentAgentId = agentId;
      this.currentAgentName = agentName;
      this.showChatHistory = true;
    }
  }
}
</script>

<style lang="scss" scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f6f8fb;
}

.home-main {
  padding: 24px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.add-device {
  border-radius: 12px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(145deg, #fdecec, #ff6b6b, #ed1c24);
  background-size: 400% 400%;
  animation: gradientBG 15s ease infinite;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(237, 28, 36, 0.1);
}

@keyframes gradientBG {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.add-device-bg {
  width: 100%;
  height: 100%;
  min-height: 200px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  text-align: left;
  background-image: url("@/assets/home/main-top-bg.png");
  background-size: cover;
  background-position: center;
  box-sizing: border-box;
  padding: 32px 40px;
  position: relative;
  z-index: 1;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(90deg, rgba(255,255,255,0.9) 0%, rgba(255,255,255,0.8) 50%, rgba(255,255,255,0) 100%);
    z-index: -1;
  }

  .hello-text-container {
    color: #333;
  }

  .hello-text {
    font-size: 36px;
    font-weight: 700;
    letter-spacing: -0.5px;
    line-height: 1.2;
    margin-bottom: 4px;

    span {
      color: #ed1c24;
      position: relative;
      
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        width: 100%;
        height: 4px;
        background: rgba(237, 28, 36, 0.2);
        border-radius: 2px;
      }
    }
  }

  .hi-hint {
    font-weight: 400;
    font-size: 14px;
    color: #666;
    margin-top: 8px;
    opacity: 0.8;
  }
}

.add-device-btn {
  display: flex;
  align-items: center;
  cursor: pointer;
  background: #ed1c24;
  border-radius: 8px;
  flex-shrink: 0;
  transition: all 0.3s;
  overflow: hidden;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(237, 28, 36, 0.2);
    background: #c6121b;

    .right-add {
      background: rgba(0, 0, 0, 0.2);
    }
  }

  .left-add {
    padding: 12px 20px;
    color: white;
    font-weight: 500;
    font-size: 15px;
  }

  .right-add {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.1);
    transition: all 0.3s;

    i {
      color: white;
      font-size: 16px;
    }
  }
}

.device-list-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  padding: 4px;
}

.skeleton-item {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  gap: 16px;
  animation: pulse 1.5s infinite;
}

.skeleton-image {
  width: 80px;
  height: 80px;
  background: #f0f0f0;
  border-radius: 8px;
  flex-shrink: 0;
}

.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0;
}

.skeleton-line {
  height: 16px;
  background: #f0f0f0;
  border-radius: 4px;
  width: 100%;
}

.skeleton-line-short {
  height: 16px;
  background: #f0f0f0;
  border-radius: 4px;
  width: 60%;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
  100% {
    opacity: 1;
  }
}

@media screen and (max-width: 768px) {
  .home-main {
    padding: 16px;
  }

  .add-device-bg {
    flex-direction: column;
    gap: 24px;
    text-align: center;
    padding: 24px;
    min-height: 160px;

    .hello-text {
      font-size: 28px;
    }
  }

  .device-list-container {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}
</style>