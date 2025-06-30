<template>
  <el-header class="header">
    <div class="header-container">
      <!-- Left side with Logo and Mobile Menu -->
      <div class="header-left">
        <img loading="lazy" alt="Logo" src="@/assets/xiaozhi-logo.png" class="logo-img" @click="goHome" />
        <img loading="lazy" alt="Brand" src="@/assets/xiaozhi-ai.png" class="brand-img" @click="goHome" />
        <!-- Mobile Menu Button -->
        <div class="hamburger-menu" @click="toggleMenu">
          <i :class="isMenuOpen ? 'el-icon-close' : 'el-icon-menu'"></i>
        </div>
      </div>

      <!-- Center Navigation -->
      <div class="header-center" :class="{ 'is-open': isMenuOpen }">
        <nav class="navigation-menu">
          <div class="menu-item" :class="{ 'active-tab': isRouteActive(['/home', '/role-config', '/device-management']) }" @click="goHome">
            <img loading="lazy" alt="" src="@/assets/header/robot.png" :style="{ filter: isRouteActive(['/home', '/role-config', '/device-management']) ? 'brightness(0) invert(1)' : 'none' }" />
            <span class="menu-text">智能体管理</span>
          </div>
          
          <div v-if="isSuperAdmin" class="menu-item" :class="{ 'active-tab': isRouteActive(['/model-config']) }" @click="goModelConfig">
            <img loading="lazy" alt="" src="@/assets/header/model_config.png" :style="{ filter: isRouteActive(['/model-config']) ? 'brightness(0) invert(1)' : 'none' }" />
            <span class="menu-text">模型配置</span>
          </div>
          
          <div v-if="isSuperAdmin" class="menu-item" :class="{ 'active-tab': isRouteActive(['/user-management']) }" @click="goUserManagement">
            <img loading="lazy" alt="" src="@/assets/header/user_management.png" :style="{ filter: isRouteActive(['/user-management']) ? 'brightness(0) invert(1)' : 'none' }" />
            <span class="menu-text">用户管理</span>
          </div>
          
          <div v-if="isSuperAdmin" class="menu-item" :class="{ 'active-tab': isRouteActive(['/ota-management']) }" @click="goOtaManagement">
            <img loading="lazy" alt="" src="@/assets/header/firmware_update.png" :style="{ filter: isRouteActive(['/ota-management']) ? 'brightness(0) invert(1)' : 'none' }" />
            <span class="menu-text">OTA管理</span>
          </div>
          
          <el-dropdown v-if="isSuperAdmin" trigger="click" class="menu-item more-dropdown" :class="{ 'active-tab': isRouteActive(['/dict-management', '/params-management', '/provider-management', '/server-side-management']) }" @visible-change="handleParamDropdownVisibleChange">
            <span class="el-dropdown-link">
              <img loading="lazy" alt="" src="@/assets/header/param_management.png" :style="{ filter: isRouteActive(['/dict-management', '/params-management', '/provider-management', '/server-side-management']) ? 'brightness(0) invert(1)' : 'none' }" />
              <span class="menu-text">参数字典</span>
              <i class="el-icon-arrow-down el-icon--right" :class="{ 'rotate-down': paramDropdownVisible }"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item @click.native="goParamManagement">参数管理</el-dropdown-item>
              <el-dropdown-item @click.native="goDictManagement">字典管理</el-dropdown-item>
              <el-dropdown-item @click.native="goProviderManagement">字段管理</el-dropdown-item>
              <el-dropdown-item @click.native="goServerSideManagement">服务端管理</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </nav>
      </div>

      <!-- Right side: Search and User Info -->
      <div class="header-right">
        <div class="search-container" v-if="$route.path === '/home'">
          <el-input v-model="search" placeholder="输入名称搜索..." class="custom-search-input" @keyup.enter.native="handleSearch">
            <i slot="suffix" class="el-icon-search search-icon" @click="handleSearch"></i>
          </el-input>
        </div>
        <div class="user-info">
          <img loading="lazy" alt="Avatar" src="@/assets/home/avatar.png" class="avatar-img" />
          <el-dropdown trigger="click" class="user-dropdown" @visible-change="handleUserDropdownVisibleChange">
            <span class="el-dropdown-link">
              <span class="username">{{ userInfo.username || '加载中...' }}</span>
              <i class="el-icon-arrow-down el-icon--right" :class="{ 'rotate-down': userDropdownVisible }"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item @click.native="showChangePasswordDialog">修改密码</el-dropdown-item>
              <el-dropdown-item @click.native="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>
    </div>
    <!-- Mobile Menu Overlay -->
    <div v-if="isMenuOpen" class="menu-overlay" @click="closeMenu"></div>
    <!-- Change Password Dialog -->
    <ChangePasswordDialog 
      :visible.sync="isChangePasswordDialogVisible" 
      @close="isChangePasswordDialogVisible = false"
    />
  </el-header>
</template>

<script>
import userApi from '@/apis/module/user';
import { mapActions, mapGetters } from 'vuex';
import ChangePasswordDialog from './ChangePasswordDialog.vue';

export default {
  name: 'HeaderBar',
  components: {
    ChangePasswordDialog
  },
  props: ['devices'],
  data() {
    return {
      search: '',
      userInfo: {
        username: '',
        mobile: ''
      },
      isChangePasswordDialogVisible: false,
      userDropdownVisible: false,
      paramDropdownVisible: false,
      isMenuOpen: true,
      mobileBreakpoint: 768,
      isMobileView: false
    }
  },
  computed: {
    ...mapGetters(['getIsSuperAdmin']),
    isSuperAdmin() {
      return this.getIsSuperAdmin;
    }
  },
  watch: {
    '$route'() {
      if (this.isMobileView) {
        this.closeMenu();
      }
    },
    isMenuOpen(newVal) {
      if (this.isMobileView) {
        localStorage.setItem('headerMenuState', newVal.toString());
      }
    }
  },
  created() {
    const savedState = localStorage.getItem('headerMenuState');
    if (savedState !== null) {
      this.isMenuOpen = savedState === 'true';
    }
  },
  mounted() {
    this.fetchUserInfo();
    window.addEventListener('resize', this.handleResize);
    this.handleResize();
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
  },
  methods: {
    handleResize() {
      const isMobile = window.innerWidth <= this.mobileBreakpoint;
      if (this.isMobileView !== isMobile) {
        this.isMobileView = isMobile;
        if (!isMobile) {
          this.isMenuOpen = true;
        } else {
          const savedState = localStorage.getItem('headerMenuState');
          this.isMenuOpen = savedState === null ? true : savedState === 'true';
        }
      }
    },
    toggleMenu() {
      if (this.isMobileView) {
        this.isMenuOpen = !this.isMenuOpen;
        document.body.style.overflow = this.isMenuOpen ? 'hidden' : '';
      }
    },
    closeMenu() {
      if (this.isMobileView) {
        this.isMenuOpen = false;
        document.body.style.overflow = '';
      }
    },
    isRouteActive(routes) {
      return routes.some(route => this.$route.path === route);
    },
    handleParamDropdownVisibleChange(visible) {
      this.paramDropdownVisible = visible;
    },
    handleUserDropdownVisibleChange(visible) {
      this.userDropdownVisible = visible;
    },
    showChangePasswordDialog() {
      this.isChangePasswordDialogVisible = true;
    },
    handleSearch() {
      const searchValue = this.search.trim();
      if (!searchValue) {
        this.$emit('search-reset');
        return;
      }
      this.$emit('search', searchValue);
    },
    handleLogout() {
      this.$confirm('确定退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('token');
        this.$router.push('/login');
      }).catch(() => {});
    },
    goHome() {
      this.$router.push('/home');
      this.closeMenu();
    },
    goModelConfig() {
      this.$router.push('/model-config');
      this.closeMenu();
    },
    goUserManagement() {
      this.$router.push('/user-management');
      this.closeMenu();
    },
    goOtaManagement() {
      this.$router.push('/ota-management');
      this.closeMenu();
    },
    goParamManagement() {
      this.$router.push('/params-management');
      this.closeMenu();
    },
    goDictManagement() {
      this.$router.push('/dict-management');
      this.closeMenu();
    },
    goProviderManagement() {
      this.$router.push('/provider-management');
      this.closeMenu();
    },
    goServerSideManagement() {
      this.$router.push('/server-side-management');
      this.closeMenu();
    },
    fetchUserInfo() {
      userApi.getUserInfo(({ data }) => {
        this.userInfo = data.data;
        if (data.data.superAdmin !== undefined) {
          this.$store.commit('setUserInfo', data.data);
        }
      });
    }
  }
}
</script>

<style lang="scss" scoped>
.header {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  height: 64px !important;
  padding: 0 20px;
  position: sticky;
  top: 0;
  z-index: 1000;
  width: 100%;
}

.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 180px;
}

.logo-img {
  height: 32px;
  width: auto;
  cursor: pointer;
}

.brand-img {
  height: 24px;
  width: auto;
  cursor: pointer;
  @media screen and (max-width: 768px) {
    display: none;
  }
}

.hamburger-menu {
  display: none;
  @media screen and (max-width: 768px) {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    cursor: pointer;
    margin-left: auto;
    i {
      font-size: 24px;
      color: #333;
    }
  }
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  
  @media screen and (max-width: 768px) {
    position: fixed;
    top: 64px;
    left: 0;
    right: 0;
    background: white;
    padding: 16px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    transform: translateY(-100%);
    transition: transform 0.3s ease;
    z-index: 1000;
    height: calc(100vh - 64px);
    overflow-y: auto;

    &.is-open {
      transform: translateY(0);
    }

    .navigation-menu {
      flex-direction: column;
      width: 100%;
      
      .menu-item {
        width: 100%;
        justify-content: flex-start;
        padding: 16px;
        margin: 4px 0;
        border-radius: 8px;
        
        &:hover {
          background: rgba(237, 28, 36, 0.1);
        }
        
        &.active-tab {
          background: #ed1c24;
        }
      }
    }
  }
}

.navigation-menu {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  color: #333;
  font-size: 14px;

  img {
    width: 20px;
    height: 20px;
    transition: filter 0.3s;
  }

  &:hover {
    background: rgba(237, 28, 36, 0.1);
    color: #ed1c24;
    img {
      filter: brightness(0) saturate(100%) invert(21%) sepia(75%) saturate(7414%) hue-rotate(353deg) brightness(89%) contrast(122%);
    }
  }

  &.active-tab {
    background: #ed1c24;
    color: white;
    img {
      filter: brightness(0) invert(1);
    }
  }

  &.more-dropdown {
    .el-dropdown-link {
      display: flex;
      align-items: center;
      gap: 8px;
      color: inherit;
      cursor: pointer;
    }

    .el-icon-arrow-down {
      transition: transform 0.3s;
      &.rotate-down {
        transform: rotate(180deg);
      }
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
  min-width: 180px;
}

.search-container {
  position: relative;
  width: 240px;
  @media screen and (max-width: 1200px) {
    width: 180px;
  }
  @media screen and (max-width: 768px) {
    display: none;
  }
}

.custom-search-input {
  ::v-deep .el-input__inner {
    border-radius: 20px;
    padding-right: 35px;
    &:focus {
      border-color: #ed1c24;
    }
  }
  .search-icon {
    cursor: pointer;
    color: #666;
    transition: color 0.3s;
    &:hover {
      color: #ed1c24;
    }
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
}

.user-dropdown {
  cursor: pointer;
  .el-dropdown-link {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #333;
    .el-icon-arrow-down {
      transition: transform 0.3s;
      &.rotate-down {
        transform: rotate(180deg);
      }
    }
  }
}

.menu-overlay {
  position: fixed;
  top: 64px;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px);
  z-index: 999;
  display: none;
  @media screen and (max-width: 768px) {
    display: block;
  }
}

@media screen and (max-width: 768px) {
  .header {
    padding: 0 16px;
  }

  .username {
    display: none;
  }
}
</style>