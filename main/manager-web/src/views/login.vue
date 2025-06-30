<template>
  <div class="welcome">
    <el-container class="auth-page-container">
      <el-header class="auth-header">
        <div class="logo-container">
          <img loading="lazy" alt="logo" src="@/assets/xiaozhi-logo.png" class="logo-img" />
          <img loading="lazy" alt="logo-text" src="@/assets/xiaozhi-ai.png" class="logo-text-img" />
        </div>
      </el-header>
      <el-main class="auth-main">
        <div class="auth-container">
          <div class="login-person">
            <img loading="lazy" alt="" src="@/assets/login/login-person.png" style="width: 100%;" />
          </div>
          <div class="login-box" @keyup.enter="login">
            <div class="login-header">
              <img loading="lazy" alt="" src="@/assets/login/hi.png" class="hi-img" />
              <div class="login-text">登录</div>
              <div class="login-welcome">
                WELCOME TO LOGIN
              </div>
            </div>
            <div class="login-form-container">
              <el-form :model="form" ref="loginForm" class="login-form">
                <!-- 用户名登录 -->
                <template v-if="!isMobileLogin">
                  <el-form-item prop="username">
                    <div class="input-box">
                      <img loading="lazy" alt="" class="input-icon" src="@/assets/login/username.png" />
                      <el-input v-model="form.username" placeholder="请输入用户名" />
                    </div>
                  </el-form-item>
                </template>

                <!-- 手机号登录 -->
                <template v-else>
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
                </template>

                <el-form-item prop="password">
                  <div class="input-box">
                    <img loading="lazy" alt="" class="input-icon" src="@/assets/login/password.png" />
                    <el-input v-model="form.password" placeholder="请输入密码" type="password" />
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

                <el-form-item>
                  <div class="form-actions">
                    <div v-if="allowUserRegister" class="link-btn" @click="goToRegister">新用户注册</div>
                    <div class="link-btn" @click="goToForgetPassword" v-if="enableMobileRegister">忘记密码?</div>
                  </div>
                </el-form-item>

                <el-form-item>
                  <div class="login-btn" @click="login">登录</div>
                </el-form-item>
              </el-form>

              <!-- 登录方式切换按钮 -->
              <div class="login-type-container" v-if="enableMobileRegister">
                <el-tooltip content="手机号码登录" placement="bottom">
                  <el-button :type="isMobileLogin ? 'primary' : 'default'" icon="el-icon-mobile" circle
                    @click="switchLoginType('mobile')"></el-button>
                </el-tooltip>
                <el-tooltip content="用户名登录" placement="bottom">
                  <el-button :type="!isMobileLogin ? 'primary' : 'default'" icon="el-icon-user" circle
                    @click="switchLoginType('username')"></el-button>
                </el-tooltip>
              </div>

              <div class="terms-text">
                登录即同意
                <span class="link-btn">《用户协议》</span>
                和
                <span class="link-btn">《隐私政策》</span>
              </div>
            </div>
          </div>
        </div>
      </el-main>
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
  name: 'login',
  components: {
    VersionFooter
  },
  computed: {
    ...mapState({
      allowUserRegister: state => state.pubConfig.allowUserRegister,
      enableMobileRegister: state => state.pubConfig.enableMobileRegister,
      mobileAreaList: state => state.pubConfig.mobileAreaList
    })
  },
  data() {
    return {
      activeName: "username",
      form: {
        username: '',
        password: '',
        captcha: '',
        captchaId: '',
        areaCode: '+86',
        mobile: ''
      },
      captchaUuid: '',
      captchaUrl: '',
      isMobileLogin: false
    }
  },
  mounted() {
    this.fetchCaptcha();
    this.$store.dispatch('fetchPubConfig').then(() => {
      // 根据配置决定默认登录方式
      this.isMobileLogin = this.enableMobileRegister;
    });
  },
  methods: {
    fetchCaptcha() {
      if (this.$store.getters.getToken) {
        if (this.$route.path !== '/home') {
          this.$router.push('/home')
        }
      } else {
        this.captchaUuid = getUUID();

        Api.user.getCaptcha(this.captchaUuid, (res) => {
          if (res.status === 200) {
            const blob = new Blob([res.data], { type: res.data.type });
            this.captchaUrl = URL.createObjectURL(blob);
          } else {
            showDanger('验证码加载失败，点击刷新');
          }
        });
      }
    },

    // 切换登录方式
    switchLoginType(type) {
      this.isMobileLogin = type === 'mobile';
      // 清空表单
      this.form.username = '';
      this.form.mobile = '';
      this.form.password = '';
      this.form.captcha = '';
      this.fetchCaptcha();
    },

    // 封装输入验证逻辑
    validateInput(input, message) {
      if (!input.trim()) {
        showDanger(message);
        return false;
      }
      return true;
    },

    async login() {
      if (this.isMobileLogin) {
        // 手机号登录验证
        if (!validateMobile(this.form.mobile, this.form.areaCode)) {
          showDanger('请输入正确的手机号码');
          return;
        }
        // 拼接手机号作为用户名
        this.form.username = this.form.areaCode + this.form.mobile;
      } else {
        // 用户名登录验证
        if (!this.validateInput(this.form.username, '用户名不能为空')) {
          return;
        }
      }

      // 验证密码
      if (!this.validateInput(this.form.password, '密码不能为空')) {
        return;
      }
      // 验证验证码
      if (!this.validateInput(this.form.captcha, '验证码不能为空')) {
        return;
      }

      this.form.captchaId = this.captchaUuid
      Api.user.login(this.form, ({ data }) => {
        showSuccess('登录成功！');
        this.$store.commit('setToken', JSON.stringify(data.data));
        goToPage('/home');
      }, (err) => {
        showDanger(err.data.msg || '登录失败')
        if (err.data != null && err.data.msg != null && err.data.msg.indexOf('图形验证码') > -1) {
          this.fetchCaptcha()
        }
      })

      // 重新获取验证码
      setTimeout(() => {
        this.fetchCaptcha();
      }, 1000);
    },

    goToRegister() {
      goToPage('/register')
    },
    goToForgetPassword() {
      goToPage('/retrieve-password')
    },
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

.form-actions {
  display: flex;
  justify-content: space-between;
  width: 100%;
  font-size: 14px;
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

.login-type-container {
  text-align: center;
  margin: 20px 0;
}

.terms-text {
  font-size: 14px;
  color: #979db1;
  text-align: center;
}

.auth-footer {
  height: auto;
}

:deep(.el-button--primary) {
  background-color: #5778ff;
  border-color: #5778ff;

  &:hover,
  &:focus {
    background-color: #4a6ae8;
    border-color: #4a6ae8;
  }

  &:active {
    background-color: #3d5cd6;
    border-color: #3d5cd6;
  }
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
}
</style>
