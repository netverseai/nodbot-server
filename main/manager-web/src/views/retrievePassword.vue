<template>
  <div class="welcome" @keyup.enter="retrievePassword">
    <el-container class="auth-page-container">
      <!-- 保持相同的头部 -->
      <el-header class="auth-header">
        <div class="logo-container">
          <img loading="lazy" alt="logo" src="@/assets/xiaozhi-logo.png" class="logo-img" />
          <img loading="lazy" alt="logo-text" src="@/assets/xiaozhi-ai.png" class="logo-text-img" />
        </div>
      </el-header>
      <el-main class="auth-main">
        <div class="auth-container">
          <div class="login-person">
            <img loading="lazy" alt="" src="@/assets/login/register-person.png" style="width: 100%;" />
          </div>
          <div class="login-box">
            <!-- 修改标题部分 -->
            <div class="login-header">
              <img loading="lazy" alt="" src="@/assets/login/hi.png" class="hi-img" />
              <div class="login-text">重置密码</div>
              <div class="login-welcome">
                PASSWORD RETRIEVE
              </div>
            </div>

            <div class="login-form-container">
              <el-form :model="form" ref="retrieveForm" class="login-form" @submit.prevent="retrievePassword">
                <!-- 手机号输入 -->
                <el-form-item prop="mobile">
                  <div class="input-box">
                    <el-row :gutter="10" style="width: 100%">
                      <el-col :xs="12" :sm="10">
                        <el-select v-model="form.areaCode">
                          <el-option v-for="item in mobileAreaList" :key="item.key"
                            :label="`${item.name} (${item.key})`" :value="item.key" />
                        </el-select>
                      </el-col>
                      <el-col :xs="12" :sm="14">
                        <el-input v-model="form.mobile" placeholder="请输入手机号码" />
                      </el-col>
                    </el-row>
                  </div>
                </el-form-item>

                <el-form-item prop="captcha">
                  <el-row :gutter="10" type="flex" align="middle">
                    <el-col :span="14">
                      <div class="input-box" style="margin-top: 0;">
                        <img loading="lazy" alt="" class="input-icon" src="@/assets/login/shield.png" />
                        <el-input v-model="form.captcha" placeholder="请输入验证码" />
                      </div>
                    </el-col>
                    <el-col :span="10">
                      <img v-if="captchaUrl" :src="captchaUrl" alt="验证码" class="captcha-img" @click="fetchCaptcha" />
                    </el-col>
                  </el-row>
                </el-form-item>

                <!-- 手机验证码 -->
                <el-form-item prop="mobileCaptcha">
                  <el-row :gutter="10" type="flex" align="middle">
                    <el-col :span="14">
                      <div class="input-box" style="margin-top: 0;">
                        <img loading="lazy" alt="" class="input-icon" src="@/assets/login/phone.png" />
                        <el-input v-model="form.mobileCaptcha" placeholder="请输入手机验证码" maxlength="6" />
                      </div>
                    </el-col>
                    <el-col :span="10">
                      <el-button type="primary" class="send-captcha-btn" :disabled="!canSendMobileCaptcha"
                        @click="sendMobileCaptcha">
                        <span>
                          {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
                        </span>
                      </el-button>
                    </el-col>
                  </el-row>
                </el-form-item>

                <!-- 新密码 -->
                <el-form-item prop="newPassword">
                  <div class="input-box">
                    <img loading="lazy" alt="" class="input-icon" src="@/assets/login/password.png" />
                    <el-input v-model="form.newPassword" placeholder="请输入新密码" type="password" />
                  </div>
                </el-form-item>

                <!-- 确认新密码 -->
                <el-form-item prop="confirmPassword">
                  <div class="input-box">
                    <img loading="lazy" alt="" class="input-icon" src="@/assets/login/password.png" />
                    <el-input v-model="form.confirmPassword" placeholder="请确认新密码" type="password" />
                  </div>
                </el-form-item>

                <!-- 修改底部链接 -->
                <el-form-item>
                  <div class="link-btn" @click="goToLogin">返回登录</div>
                </el-form-item>

                <el-form-item>
                  <!-- 修改按钮文本 -->
                  <div class="login-btn" @click="retrievePassword">立即修改</div>
                </el-form-item>
              </el-form>
            </div>

            <!-- 保持相同的协议声明 -->
            <div class="terms-text">
              同意
              <span class="link-btn">《用户协议》</span>
              和
              <span class="link-btn">《隐私政策》</span>
            </div>
          </div>
        </div>
      </el-main>

      <!-- 保持相同的页脚 -->
      <el-footer class="auth-footer">
        <version-footer />
      </el-footer>
    </el-container>
  </div>
</template>

<script>
import Api from '@/apis/api';
import VersionFooter from '@/components/VersionFooter.vue';
import { getUUID, goToPage, showDanger, showSuccess, validateMobile } from '@/utils';
import { mapState } from 'vuex';

export default {
  name: 'retrieve',
  components: {
    VersionFooter
  },
  computed: {
    ...mapState({
      allowUserRegister: state => state.pubConfig.allowUserRegister,
      mobileAreaList: state => state.pubConfig.mobileAreaList
    }),
    canSendMobileCaptcha() {
      return this.countdown === 0 && validateMobile(this.form.mobile, this.form.areaCode);
    }
  },
  data() {
    return {
      form: {
        areaCode: '+86',
        mobile: '',
        captcha: '',
        captchaId: '',
        smsCode: '',
        newPassword: '',
        confirmPassword: ''
      },
      captchaUrl: '',
      countdown: 0,
      timer: null
    }
  },
  mounted() {
    this.fetchCaptcha();
  },
  methods: {
    // 复用验证码获取方法
    fetchCaptcha() {
      this.form.captchaId = getUUID();
      Api.user.getCaptcha(this.form.captchaId, (res) => {
        if (res.status === 200) {
          const blob = new Blob([res.data], { type: res.data.type });
          this.captchaUrl = URL.createObjectURL(blob);

        } else {
          console.error('验证码加载异常:', error);
          showDanger('验证码加载失败，点击刷新');
        }
      });
    },

    // 封装输入验证逻辑
    validateInput(input, message) {
      if (!input.trim()) {
        showDanger(message);
        return false;
      }
      return true;
    },

    // 发送手机验证码
    sendMobileCaptcha() {
      if (!validateMobile(this.form.mobile, this.form.areaCode)) {
        showDanger('请输入正确的手机号码');
        return;
      }

      // 验证图形验证码
      if (!this.validateInput(this.form.captcha, '请输入图形验证码')) {
        this.fetchCaptcha();
        return;
      }

      // 清除可能存在的旧定时器
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }

      // 开始倒计时
      this.countdown = 60;
      this.timer = setInterval(() => {
        if (this.countdown > 0) {
          this.countdown--;
        } else {
          clearInterval(this.timer);
          this.timer = null;
        }
      }, 1000);

      // 调用发送验证码接口
      Api.user.sendSmsVerification({
        phone: this.form.areaCode + this.form.mobile,
        captcha: this.form.captcha,
        captchaId: this.form.captchaId
      }, (res) => {
        showSuccess('验证码发送成功');
      }, (err) => {
        showDanger(err.data.msg || '验证码发送失败');
        this.countdown = 0;
        this.fetchCaptcha();
      });
    },

    // 修改逻辑
    retrievePassword() {
      // 验证逻辑
      if (!validateMobile(this.form.mobile, this.form.areaCode)) {
        showDanger('请输入正确的手机号码');
        return;
      }
      if (!this.form.captcha) {
        showDanger('请输入图形验证码');
        return;
      }
      if (!this.form.mobileCaptcha) {
        showDanger('请输入短信验证码');
        return;
      }
      if (this.form.newPassword !== this.form.confirmPassword) {
        showDanger('两次输入的密码不一致');
        return;
      }

      Api.user.retrievePassword({
        phone: this.form.areaCode + this.form.mobile,
        password: this.form.newPassword,
        code: this.form.mobileCaptcha
      }, (res) => {
        showSuccess('密码重置成功');
        goToPage('/login');
      }, (err) => {
        showDanger(err.data.msg || '重置失败');
        if (err.data != null && err.data.msg != null && err.data.msg.indexOf('图形验证码') > -1) {
          this.fetchCaptcha()
        }
      });
    },

    goToLogin() {
      goToPage('/login')
    }
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }
}
</script>

<style lang="scss" scoped>
@import './auth.scss';

.auth-page-container {
  height: 100vh;
}

.auth-header {
  height: auto;
  padding: 15px 20px;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-img {
  width: 45px;
  height: 45px;
}

.logo-text-img {
  height: 18px;
}

.auth-main {
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
  padding: 0 30px;
}

.hi-img {
  width: 34px;
  height: 34px;
}

.login-form-container {
  padding: 0 30px;
}

.captcha-img {
  width: 100%;
  height: 40px;
  cursor: pointer;
  border-radius: 8px;
}

.link-btn {
  color: #5778ff;
  cursor: pointer;
  transition: color 0.3s;
}

.link-btn:hover {
  color: #4a6ae8;
}

.login-btn {
  width: 100%;
  margin: 0;
}

.send-captcha-btn {
  width: 100%;
  height: 40px;
}

.terms-text {
  font-size: 14px;
  color: #979db1;
  text-align: center;
  padding: 0 30px 20px;
}

.auth-footer {
  height: auto;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input-group__append),
:deep(.el-input-group__prepend) {
  background-color: #f6f8fb;
  border: 1px solid #e4e6ef;
}

:deep(.el-select .el-input__inner) {
  padding: 0 10px;
}

@media (max-width: 480px) {
  .login-header {
    padding: 0 15px;
    gap: 10px;
    margin-bottom: 20px;
  }

  .login-form-container {
    padding: 0 15px;
  }

  .terms-text {
    padding: 0 15px 15px;
  }

  .send-captcha-btn {
    padding: 10px;
    height: auto;
    font-size: 12px;
  }
}
</style>
