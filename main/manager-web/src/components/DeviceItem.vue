<template>
  <div class="device-item">
    <div class="device-header">
      <div class="device-title">{{ device.agentName }}</div>
      <div class="device-actions">
        <el-tooltip content="删除" placement="top">
          <img src="@/assets/home/delete.png" alt="删除" class="action-icon" @click.stop="handleDelete" />
        </el-tooltip>
        <el-tooltip class="item" effect="dark" :content="device.systemPrompt" placement="top" popper-class="custom-tooltip">
          <img src="@/assets/home/info.png" alt="信息" class="action-icon" />
        </el-tooltip>
      </div>
    </div>

    <div class="device-info">
      <div class="info-item">
        <span class="info-label">语言模型：</span>
        <span class="info-value">{{ device.llmModelName }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">音色模型：</span>
        <span class="info-value">{{ device.ttsModelName }} ({{ device.ttsVoiceName }})</span>
      </div>
    </div>

    <div class="device-actions-group">
      <el-button class="action-btn" type="primary" @click="handleConfigure">配置角色</el-button>
      <el-button class="action-btn" type="primary" plain @click="handleDeviceManage">设备管理({{ device.deviceCount }})</el-button>
      <el-tooltip 
        v-if="device.memModelId === 'Memory_nomem'" 
        content="请先在配置角色界面开启记忆" 
        placement="top"
        popper-class="memory-tooltip"
      >
        <div style="display: inline-block;">
          <el-button 
            class="action-btn"
            type="primary"
            plain
            disabled
          >
            聊天记录
          </el-button>
        </div>
      </el-tooltip>
      <el-button 
        v-else
        class="action-btn"
        type="primary"
        plain
        @click="handleChatHistory"
      >
        聊天记录
      </el-button>
    </div>

    <div class="device-footer">
      <div class="last-chat">最近对话：{{ formattedLastConnectedTime }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DeviceItem',
  props: {
    device: { type: Object, required: true }
  },
  data() {
    return { switchValue: false }
  },
  computed: {
    formattedLastConnectedTime() {
      if (!this.device.lastConnectedAt) return '暂未对话';

      const lastTime = new Date(this.device.lastConnectedAt);
      const now = new Date();
      const diffMinutes = Math.floor((now - lastTime) / (1000 * 60));

      if (diffMinutes <= 1) {
        return '刚刚';
      } else if (diffMinutes < 60) {
        return `${diffMinutes}分钟前`;
      } else if (diffMinutes < 24 * 60) {
        const hours = Math.floor(diffMinutes / 60);
        const minutes = diffMinutes % 60;
        return `${hours}小时${minutes > 0 ? minutes + '分钟' : ''}前`;
      } else {
        return this.device.lastConnectedAt;
      }
    }
  },
  methods: {
    handleDelete() {
      this.$emit('delete', this.device.agentId)
    },
    handleConfigure() {
      this.$router.push({ path: '/role-config', query: { agentId: this.device.agentId } });
    },
    handleDeviceManage() {
      this.$router.push({ path: '/device-management', query: { agentId: this.device.agentId } });
    },
    handleChatHistory() {
      if (this.device.memModelId === 'Memory_nomem') {
        return
      }
      this.$emit('chat-history', { agentId: this.device.agentId, agentName: this.device.agentName })
    }
  }
}
</script>

<style lang="scss" scoped>
.device-item {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  width: 100%;
  box-sizing: border-box;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }
}

.device-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.device-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.device-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.action-icon {
  width: 20px;
  height: 20px;
  cursor: pointer;
  opacity: 0.6;
  transition: all 0.3s;

  &:hover {
    opacity: 1;
  }
}

.device-info {
  margin-bottom: 20px;
}

.info-item {
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;

  &:last-child {
    margin-bottom: 0;
  }
}

.info-label {
  color: #999;
}

.info-value {
  color: #333;
}

.device-actions-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  height: 32px;
  padding: 0 16px;
  font-size: 13px;
  border-radius: 6px;

  &.el-button--primary {
    background-color: #ed1c24;
    border-color: #ed1c24;

    &:hover, &:focus {
      background-color: #c6121b;
      border-color: #c6121b;
    }

    &.is-plain {
      background-color: transparent;
      color: #ed1c24;

      &:hover, &:focus {
        background-color: rgba(237, 28, 36, 0.1);
        border-color: #c6121b;
        color: #c6121b;
      }

      &[disabled] {
        background-color: #f5f5f5;
        border-color: #ddd;
        color: #999;

        &:hover, &:focus {
          background-color: #f5f5f5;
          border-color: #ddd;
          color: #999;
        }
      }
    }
  }

  &.el-button--default {
    color: #ed1c24;
    border-color: #ed1c24;
    background: transparent;

    &:hover, &:focus {
      color: #c6121b;
      border-color: #c6121b;
      background-color: rgba(237, 28, 36, 0.1);
    }
  }

  &.disabled {
    background-color: #f5f5f5;
    border-color: #ddd;
    color: #999;
    cursor: not-allowed;

    &:hover, &:focus {
      background-color: #f5f5f5;
      border-color: #ddd;
      color: #999;
    }
  }
}

.device-footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.last-chat {
  font-size: 13px;
  color: #999;
}

.custom-tooltip {
  max-width: 400px;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.5;
  padding: 8px 12px;
}

.memory-tooltip {
  font-size: 13px;
  padding: 8px 12px;
  line-height: 1.4;
  max-width: 200px;
  word-break: keep-all;
  white-space: nowrap;
}

@media screen and (max-width: 768px) {
  .device-item {
    padding: 20px;
  }

  .device-title {
    font-size: 16px;
  }

  .info-item {
    font-size: 13px;
  }

  .action-btn {
    height: 30px;
    padding: 0 12px;
    font-size: 12px;
  }
}
</style>

<style>
.memory-tooltip {
  font-size: 13px !important;
  padding: 8px 12px !important;
  line-height: 1.4 !important;
}

.custom-tooltip {
  max-width: 400px;
  word-break: break-word;
}
</style>