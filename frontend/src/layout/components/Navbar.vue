<template>
  <div class="navbar">
    <hamburger id="hamburger-container" :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb id="breadcrumb-container" class="breadcrumb-container" />

    <div class="right-menu">
      <div class="right-menu-item hover-effect theme-toggle" @click="toggle">
        <span class="theme-ico">{{ theme === 'dark' ? '🌙' : '☀️' }}</span>
      </div>
      <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="click">
        <div class="avatar-wrapper">
          <span class="avatar-dot">{{ avatarText }}</span>
          <span class="user-name">{{ userName }}</span>
          <i class="el-icon-caret-bottom" />
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/profile/index">
              <el-dropdown-item>个人信息</el-dropdown-item>
            </router-link>
            <router-link to="/">
              <el-dropdown-item>主页</el-dropdown-item>
            </router-link>
            <el-dropdown-item  @click="logout"  divided>退出</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapMutations } from 'vuex'
import loginApi from '@/api/login'
import Breadcrumb from '@/components/Breadcrumb'
import Hamburger from '@/components/Hamburger'
import { toggleTheme, currentTheme } from '@/utils/theme'

export default {
  components: {
    Breadcrumb,
    Hamburger
  },
  data () {
    return { theme: currentTheme() }
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'device',
      'userName'
    ]),
    avatarText () {
      const name = (this.userName || 'A').trim()
      return name.charAt(0).toUpperCase()
    }
  },
  methods: {
    toggle () {
      this.theme = toggleTheme()
    },
    toggleSideBar () {
      this.$store.dispatch('app/toggleSideBar')
    },
    logout () {
      let _this = this
      loginApi.logout().then(function (result) {
        if (result && result.code === 1) {
          _this.clearLogin()
          _this.$router.push({ path: '/login' })
        }
      })
    },
    ...mapMutations('user', ['clearLogin'])
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 56px;
  overflow: hidden;
  position: relative;
  background: rgba(15, 20, 32, 0.9);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.18);

  .hamburger-container {
    line-height: 56px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color:transparent;
    color: #bfcbd9;

    &:hover {
      background: rgba(255, 107, 157, 0.08);
      color: #ff6b9d;
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 56px;

    &:focus {
      outline: none;
    }

    .theme-ico { font-size: 17px; line-height: 1; }
    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #bfcbd9;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background .3s, color .3s;

        &:hover {
          background: rgba(255, 107, 157, 0.08);
          color: #ff6b9d;
        }
      }
    }

    .avatar-container {
      margin-right: 24px;

      .avatar-wrapper {
        margin-top: 7px;
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 4px 12px 4px 4px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.06);
        border: 1px solid rgba(255, 255, 255, 0.08);
        cursor: pointer;
        transition: background 0.25s, transform 0.25s, box-shadow 0.25s;

        &:hover {
          background: rgba(242, 167, 195, 0.16);
          transform: translateY(-1px);
          box-shadow: 0 8px 18px rgba(242, 167, 195, 0.18);
        }

        .avatar-dot {
          width: 30px;
          height: 30px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 13px;
          font-weight: 700;
          color: #fff;
          background: linear-gradient(135deg, #f2a7c3, #d9a7e0);
          box-shadow: 0 4px 12px rgba(242, 167, 195, 0.35);
        }

        .user-name {
          font-size: 14px;
          color: #fff;
          letter-spacing: 0.5px;
        }

        .el-icon-caret-bottom {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.6);
        }
      }
    }
  }
}
</style>
