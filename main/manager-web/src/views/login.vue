<template>
  <div class="disguised-page">
    <!-- 极其隐蔽的右上角管理员入口 -->
    <div class="console-trigger" @click="toggleLogin">
      {{ showLoginForm ? '[ EXIT_CONSOLE ]' : 'SYSTEM_CONSOLE' }}
    </div>

    <transition name="fade-slide" mode="out-in">
      <!-- 第一层：伪装 404 页面 (复古终端风格) -->
      <div v-if="!showLoginForm" key="404" class="error-wrapper">
        <pre class="ascii-logo">
 ███╗   ██╗ ██████╗ ██████╗ ██████╗  ██████╗ ████████╗
 ████╗  ██║██╔═══██╗██╔══██╗██╔══██╗██╔═══██╗╚══██╔══╝
 ██╔██╗ ██║██║   ██║██║  ██║██████╔╝██║   ██║   ██║
 ██║╚██╗██║██║   ██║██║  ██║██╔══██╗██║   ██║   ██║
 ██║ ╚████║╚██████╔╝██████╔╝██████╔╝╚██████╔╝   ██║
 ╚═╝  ╚═══╝ ╚═════╝ ╚═════╝ ╚═════╝  ╚═════╝    ╚═╝</pre>
        <h1 class="error-code">404</h1>
        <h2 class="error-title">HTTP_ERROR: RESOURCE_NOT_FOUND</h2>
        <p class="error-message">
          The requested system resource is unavailable.<br />
          Your session ID and IP have been logged by the security subsystem.
        </p>
        <div class="action-bar">
          <div class="terminal-btn" @click="goOuterSpace">> DISCONNECT_SESSION</div>
        </div>
      </div>

      <!-- 第二层：隐藏的终端登录（无注册、无协议、无隐私链接） -->
      <div v-else key="login" class="login-wrapper">
        <div class="terminal-card" @keyup.enter="login">
          <div class="terminal-header">
            <span class="status-dot"></span>
            <span class="terminal-title">ADMINISTRATOR AUTHENTICATION</span>
          </div>

          <el-form :model="form" ref="loginForm" class="terminal-form">
            <!-- 标识符 -->
            <div class="input-row">
              <span class="prompt">ID_</span>
              <el-input
                v-model="form.username"
                placeholder="Enter Identifier"
                spellcheck="false"
                autocomplete="off"
              />
            </div>

            <!-- 凭证 -->
            <div class="input-row">
              <span class="prompt">PW_</span>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="Enter Credential"
                show-password
              />
            </div>

            <!-- 安全令牌 (验证码) -->
            <el-row :gutter="10" type="flex" align="middle" class="captcha-row">
              <el-col :span="14">
                <div class="input-row">
                  <span class="prompt">TK_</span>
                  <el-input v-model="form.captcha" placeholder="Token" autocomplete="off" />
                </div>
              </el-col>
              <el-col :span="10">
                <img v-if="captchaUrl" :src="captchaUrl" class="terminal-captcha" @click="fetchCaptcha" />
              </el-col>
            </el-row>

            <div class="execute-btn" @click="login">
              > EXECUTE_LOGIN_SEQUENCE
            </div>
          </el-form>
        </div>
      </div>
    </transition>

    <div class="footer-note">
      ESTABLISHING SECURE CONNECTION... [{{ currentIp || 'LOGGED' }}]
    </div>
  </div>
</template>

<script>
import Api from '@/apis/api';
import { getUUID, goToPage, showDanger, showSuccess } from '@/utils';

export default {
  name: 'login',
  data() {
    return {
      showLoginForm: false,
      currentIp: '',
      form: {
        username: '',
        password: '',
        captcha: '',
        captchaId: ''
      },
      captchaUuid: '',
      captchaUrl: ''
    };
  },
  mounted() {
    this.fetchCaptcha();
    // 模拟随机IP增加伪装真实感
    this.currentIp = Array.from({length: 4}, () => Math.floor(Math.random() * 256)).join('.');
  },
  methods: {
    toggleLogin() {
      this.showLoginForm = !this.showLoginForm;
      if (this.showLoginForm) {
        this.fetchCaptcha();
      }
    },
    fetchCaptcha() {
      if (this.$store.getters.getToken) {
        goToPage('/home');
      } else {
        this.captchaUuid = getUUID();
        Api.user.getCaptcha(this.captchaUuid, (res) => {
          if (res.status === 200) {
            const blob = new Blob([res.data], { type: res.data.type });
            this.captchaUrl = URL.createObjectURL(blob);
          }
        });
      }
    },
    async login() {
      if (!this.form.username || !this.form.password || !this.form.captcha) {
        showDanger('REQUIRED_FIELDS_MISSING');
        return;
      }
      this.form.captchaId = this.captchaUuid;
      Api.user.login(this.form, ({ data }) => {
        showSuccess('AUTHENTICATION_GRANTED');
        this.$store.commit('setToken', JSON.stringify(data.data));
        goToPage('/home');
      }, (err) => {
        showDanger(err.data?.msg || 'ACCESS_DENIED');
        this.fetchCaptcha();
      });
    },
    goOuterSpace() {
      window.location.href = "https://www.google.com";
    }
  }
};
</script>

<style lang="scss" scoped>
.disguised-page {
  height: 100vh;
  width: 100vw;
  background-color: #050505;
  color: #444;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.console-trigger {
  position: absolute;
  top: 15px;
  right: 20px;
  font-size: 10px;
  color: #111;
  cursor: pointer;
  z-index: 1000;
  transition: color 0.5s;
  &:hover {
    color: #333;
  }
}

.error-wrapper {
  text-align: center;
  animation: flicker 3s infinite;

  .ascii-logo {
    display: inline-block;
    text-align: left;
    color: #004400; /* 复古暗绿色 */
    font-size: 12px;
    line-height: 1.2;
    margin-bottom: 20px;
    font-family: 'Consolas', 'Courier New', monospace;
    text-shadow: 0 0 5px rgba(0, 80, 0, 0.4);
  }

  .error-code {
    font-size: 80px;
    margin: 10px 0;
    color: #0a0a0a; /* 极深色，隐约可见 */
    font-weight: 100;
    letter-spacing: 20px;
  }

  .error-title {
    color: #1a1a1a;
    letter-spacing: 4px;
    font-size: 14px;
    margin-bottom: 30px;
  }

  .error-message {
    font-size: 12px;
    color: #222;
    line-height: 2;
    margin-bottom: 40px;
  }

  .terminal-btn {
    display: inline-block;
    border: 1px solid #1a1a1a;
    padding: 10px 30px;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.3s;
    &:hover {
      background: #111;
      color: #00ff00;
      border-color: #004400;
    }
  }
}

.login-wrapper {
  width: 100%;
  max-width: 400px;
  padding: 20px;
}

.terminal-card {
  background: #0d0d0d;
  border: 1px solid #1a1a1a;
  box-shadow: 0 0 40px rgba(0, 255, 0, 0.03);

  .terminal-header {
    background: #151515;
    padding: 10px 15px;
    display: flex;
    align-items: center;
    gap: 10px;

    .status-dot {
      width: 8px;
      height: 8px;
      background: #00ff00;
      border-radius: 50%;
      box-shadow: 0 0 5px #00ff00;
    }

    .terminal-title {
      font-size: 10px;
      color: #666;
      letter-spacing: 1px;
    }
  }
}

.terminal-form {
  padding: 30px;

  .input-row {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #1a1a1a;
    margin-bottom: 25px;

    .prompt {
      color: #00ff00;
      margin-right: 10px;
      font-weight: bold;
    }

    :deep(.el-input__inner) {
      background: transparent !important;
      border: none !important;
      color: #00ff00 !important;
      font-family: inherit;
      padding-left: 0;
      &::placeholder { color: #222; }
    }

    /* 移除自动填充时的背景色 */
    :deep(.el-input__inner:-webkit-autofill) {
      -webkit-box-shadow: 0 0 0px 1000px #0d0d0d inset !important;
      -webkit-text-fill-color: #00ff00 !important;
    }
  }
}

.terminal-captcha {
  width: 100%;
  height: 32px;
  filter: grayscale(1) contrast(150%) invert(1);
  cursor: pointer;
  opacity: 0.5;
  &:hover { opacity: 0.8; }
}

.execute-btn {
  margin-top: 10px;
  border: 1px dashed #00ff00;
  color: #00ff00;
  text-align: center;
  padding: 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
  &:hover {
    background: rgba(0, 255, 0, 0.05);
    box-shadow: 0 0 15px rgba(0, 255, 0, 0.1);
  }
}

.footer-note {
  position: absolute;
  bottom: 20px;
  font-size: 9px;
  color: #1a1a1a;
  letter-spacing: 2px;
}

@keyframes flicker {
  0% { opacity: 0.98; }
  5% { opacity: 0.95; }
  10% { opacity: 0.9; }
  50% { opacity: 1; }
  90% { opacity: 0.95; }
  100% { opacity: 1; }
}

.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.4s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: scale(0.98);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: scale(1.02);
}
</style>
