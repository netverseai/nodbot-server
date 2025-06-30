<template>
  <el-dialog
    :visible.sync="dialogVisible"
    width="400px"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
    center
    custom-class="change-password-dialog"
    @close="handleClose"
  >
    <div class="dialog-header">
      <div class="header-icon">
        <img loading="lazy" src="@/assets/login/shield.png" alt="" />
      </div>
      <span>修改密码</span>
    </div>

    <div class="dialog-divider"></div>

    <div class="dialog-content">
      <div class="form-group">
        <div class="form-label">
          <span class="required">*</span>
          <span>旧密码：</span>
        </div>
        <el-input
          v-model="oldPassword"
          type="password"
          placeholder="请输入旧密码"
          show-password
          @keyup.enter.native="confirm"
        />
      </div>

      <div class="form-group">
        <div class="form-label">
          <span class="required">*</span>
          <span>新密码：</span>
        </div>
        <el-input
          v-model="newPassword"
          type="password"
          placeholder="请输入新密码"
          show-password
          @keyup.enter.native="confirm"
        />
      </div>

      <div class="form-group">
        <div class="form-label">
          <span class="required">*</span>
          <span>确认新密码：</span>
        </div>
        <el-input
          v-model="confirmNewPassword"
          type="password"
          placeholder="请再次输入新密码"
          show-password
          @keyup.enter.native="confirm"
        />
      </div>
    </div>

    <div class="dialog-footer">
      <el-button type="primary" class="confirm-btn" @click="confirm" :loading="loading">确定</el-button>
      <el-button class="cancel-btn" @click="handleClose">取消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import userApi from '@/apis/module/user';
import { mapActions } from 'vuex';

export default {
  name: 'ChangePasswordDialog',
  props: {
    visible: {
      type: Boolean,
      required: true
    }
  },
  data() {
    return {
      dialogVisible: false,
      oldPassword: "",
      newPassword: "",
      confirmNewPassword: "",
      loading: false
    }
  },
  watch: {
    visible: {
      immediate: true,
      handler(val) {
        this.dialogVisible = val;
      }
    },
    dialogVisible(val) {
      this.$emit('update:visible', val);
      if (!val) {
        this.resetForm();
      }
    }
  },
  methods: {
    ...mapActions(['logout']),
    async confirm() {
      if (this.loading) return;
      
      if (!this.oldPassword.trim() || !this.newPassword.trim() || !this.confirmNewPassword.trim()) {
        this.$message.error('请填写所有字段');
        return;
      }
      if (this.newPassword !== this.confirmNewPassword) {
        this.$message.error('两次输入的新密码不一致');
        return;
      }
      if (this.newPassword === this.oldPassword) {
        this.$message.error('新密码不能与旧密码相同');
        return;
      }

      this.loading = true;
      try {
        const res = await new Promise((resolve, reject) => {
          userApi.changePassword(this.oldPassword, this.newPassword, resolve, reject);
        });

        if (res.data.code === 0) {
          this.$message.success({
            message: '密码修改成功，请重新登录',
            showClose: true
          });
          this.handleClose();
          await this.logout();
          this.$router.push('/login');
        } else {
          this.$message.error(res.data.msg || '密码修改失败');
        }
      } catch (err) {
        this.$message.error(err.msg || '密码修改失败');
      } finally {
        this.loading = false;
      }
    },
    handleClose() {
      this.dialogVisible = false;
      this.$emit('close');
    },
    resetForm() {
      this.oldPassword = "";
      this.newPassword = "";
      this.confirmNewPassword = "";
      this.loading = false;
    }
  }
}
</script>

<style lang="scss" scoped>
.change-password-dialog {
  border-radius: 12px;
  overflow: hidden;

  ::v-deep .el-dialog__header {
    display: none;
  }

  ::v-deep .el-dialog__body {
    padding: 0;
  }
}

.dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 24px;
  font-size: 18px;
  font-weight: 600;
  color: #333;

  .header-icon {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: #ed1c24;
    display: flex;
    align-items: center;
    justify-content: center;

    img {
      width: 20px;
      height: 20px;
      filter: brightness(0) invert(1);
    }
  }
}

.dialog-divider {
  height: 1px;
  background: #f0f0f0;
}

.dialog-content {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;

  .required {
    color: #ed1c24;
    margin-right: 4px;
  }
}

.el-input {
  ::v-deep .el-input__inner {
    height: 40px;
    line-height: 40px;
    border-radius: 4px;
    border-color: #ddd;
    transition: all 0.3s;

    &:focus {
      border-color: #ed1c24;
      box-shadow: 0 0 0 2px rgba(237, 28, 36, 0.1);
    }
  }
}

.dialog-footer {
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.confirm-btn {
  min-width: 80px;
  background-color: #ed1c24;
  border-color: #ed1c24;
  
  &:hover, &:focus {
    background-color: #c6121b;
    border-color: #c6121b;
  }

  &.is-loading {
    background-color: #ed1c24;
    border-color: #ed1c24;
    opacity: 0.8;
  }
}

.cancel-btn {
  min-width: 80px;
  color: #666;
  border-color: #ddd;
  
  &:hover, &:focus {
    color: #ed1c24;
    border-color: #ed1c24;
    background-color: rgba(237, 28, 36, 0.1);
  }
}

@media screen and (max-width: 768px) {
  .change-password-dialog {
    width: 90% !important;
    margin: 15vh auto !important;
  }

  .dialog-header {
    padding: 16px 20px;
    font-size: 16px;
  }

  .dialog-content {
    padding: 20px;
  }

  .dialog-footer {
    padding: 12px 20px;
  }
}
</style>