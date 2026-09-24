<template>
  <div class="login-container">
    <div class="aurora">
      <span class="orb orb-1" />
      <span class="orb orb-2" />
      <span class="orb orb-3" />
    </div>
    <div class="grid-overlay" />

    <div class="login-card pop-in">
      <div class="login-brand">
        <img src="@/assets/logo.png" class="brand-logo" alt="logo">
        <div>
          <h3 class="title">番析 AniScope</h3>
          <p class="subtitle">动漫数据分析与推荐系统</p>
        </div>
      </div>

      <div class="lang-chips stagger">
        <span v-for="l in langs" :key="l" class="chip">{{ l }}</span>
      </div>

      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form" auto-complete="on" label-position="left">
        <el-form-item prop="userName">
          <span class="svg-container">
            <svg-icon icon-class="user" />
          </span>
          <el-input
            ref="userName"
            v-model="loginForm.userName"
            placeholder="用户名"
            name="userName"
            type="text"
            tabindex="1"
            auto-complete="on"
          />
        </el-form-item>

        <el-tooltip v-model="capsTooltip" content="大写锁定已开启" placement="top" manual>
          <el-form-item prop="password">
            <span class="svg-container">
              <svg-icon icon-class="password" />
            </span>
            <el-input
              :key="passwordType"
              ref="password"
              v-model="loginForm.password"
              :type="passwordType"
              placeholder="密码"
              name="password"
              tabindex="2"
              auto-complete="on"
              @keyup="checkCapslock"
              @blur="capsTooltip = false"
              @keyup.enter="handleLogin"
            />
            <span class="show-pwd" @click="showPwd">
              <svg-icon :icon-class="passwordType === 'password' ? 'eye' : 'eye-open'" />
            </span>
          </el-form-item>
        </el-tooltip>

        <div class="login-options">
          <el-checkbox v-model="loginForm.remember">记住密码</el-checkbox>
        </div>

        <el-button :loading="loading" type="primary" class="login-btn" @click.prevent="handleLogin">
          登 录
        </el-button>
      </el-form>

      <div class="login-footer">
        <p>默认账号：admin &nbsp;|&nbsp; 密码：123456</p>
      </div>
    </div>
  </div>
</template>

<script>
import { mapMutations } from 'vuex'
import loginApi from '@/api/login'

export default {
  name: 'Login',
  data () {
    const validateUsername = (rule, value, callback) => {
      if (value.length < 5) {
        callback(new Error('用户名不能少于5个字符'))
      } else {
        callback()
      }
    }
    const validatePassword = (rule, value, callback) => {
      if (value.length < 5) {
        callback(new Error('密码不能少于5个字符'))
      } else {
        callback()
      }
    }
    return {
      langs: ['日本語', 'English', '中文', '한국어'],
      loginForm: {
        userName: '',
        password: '',
        remember: false
      },
      loginRules: {
        userName: [{ required: true, trigger: 'blur', validator: validateUsername }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }]
      },
      passwordType: 'password',
      capsTooltip: false,
      loading: false,
      showDialog: false
    }
  },
  mounted () {
    if (this.loginForm.userName === '') {
      this.$refs.userName.focus()
    } else if (this.loginForm.password === '') {
      this.$refs.password.focus()
    }
  },
  methods: {
    checkCapslock ({ shiftKey, key } = {}) {
      if (key && key.length === 1) {
        // eslint-disable-next-line no-mixed-operators
        if (shiftKey && (key >= 'a' && key <= 'z') || !shiftKey && (key >= 'A' && key <= 'Z')) {
          this.capsTooltip = true
        } else {
          this.capsTooltip = false
        }
      }
      if (key === 'CapsLock' && this.capsTooltip === true) {
        this.capsTooltip = false
      }
    },
    showPwd () {
      if (this.passwordType === 'password') {
        this.passwordType = ''
      } else {
        this.passwordType = 'password'
      }
      this.$nextTick(() => {
        this.$refs.password.focus()
      })
    },
    handleLogin () {
      let _this = this
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          loginApi.login(this.loginForm).then(function (result) {
            if (result && result.code === 1) {
              _this.setUserName(_this.loginForm.userName)
              _this.$router.push({ path: '/' })
            } else {
              _this.loading = false
              _this.$message({
                message: result.message,
                type: 'error'
              })
            }
          }).catch(function (reason) {
            _this.loading = false
          })
        } else {
          return false
        }
      })
    },
    ...mapMutations('user', ['setUserName'])
  }
}
</script>

<style lang="scss" scoped>
$bg: #0b0f1a;
$card-bg: rgba(15, 20, 32, 0.72);
$light_gray: #fff;
$accent: #f2a7c3;

.login-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background-color: $bg;
}

.aurora {
  position: absolute;
  inset: -20%;
  background: linear-gradient(120deg, #fdeef4 0%, #f7e4f2 30%, #eaf4f8 60%, #fdf0f6 100%);
  background-size: 300% 300%;
  animation: anime-aurora 20s ease infinite;
  filter: blur(50px) saturate(1.3);
  opacity: 0.9;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.55;
}

.orb-1 {
  width: 420px;
  height: 420px;
  top: -80px;
  left: -60px;
  background: radial-gradient(circle, rgba(242, 167, 195, 0.9), transparent 70%);
  animation: anime-float 9s ease-in-out infinite;
}

.orb-2 {
  width: 380px;
  height: 380px;
  bottom: -100px;
  right: -40px;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.85), transparent 70%);
  animation: anime-float 11s ease-in-out infinite reverse;
}

.orb-3 {
  width: 300px;
  height: 300px;
  top: 40%;
  left: 55%;
  background: radial-gradient(circle, rgba(217, 167, 224, 0.85), transparent 70%);
  animation: anime-float 13s ease-in-out infinite;
}

.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.045) 1px, transparent 1px);
  background-size: 46px 46px;
  mask-image: radial-gradient(circle at 50% 50%, #000 0%, transparent 78%);
  -webkit-mask-image: radial-gradient(circle at 50% 50%, #000 0%, transparent 78%);
}

.login-card {
  position: relative;
  z-index: 2;
  width: 460px;
  max-width: 92%;
  padding: 40px 44px 32px;
  border-radius: 24px;
  background: $card-bg;
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 30px 90px rgba(0, 0, 0, 0.55), inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.login-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 22px;

  .brand-logo {
    width: 54px;
    height: 54px;
    border-radius: 16px;
    margin-right: 16px;
    box-shadow: 0 0 24px rgba(242, 167, 195, 0.5);
  }

  .title {
    font-size: 22px;
    color: #fff;
    margin: 0;
    font-weight: 700;
    letter-spacing: 0.5px;
  }

  .subtitle {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.5);
    margin: 6px 0 0;
    letter-spacing: 1.6px;
    text-transform: uppercase;
  }
}

.lang-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-bottom: 24px;

  .chip {
    font-size: 11px;
    letter-spacing: 0.5px;
    color: rgba(255, 255, 255, 0.72);
    padding: 4px 12px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }
}

.login-form {
  .el-form-item {
    position: relative;
    height: 52px;
    padding: 0 14px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    background: rgba(0, 0, 0, 0.22);
    border-radius: 14px;
    margin-bottom: 26px;
    transition: border-color 0.3s, box-shadow 0.3s, background 0.3s;

    &:hover {
      border-color: rgba(242, 167, 195, 0.45);
    }

    &:focus-within {
      border-color: $accent;
      background: rgba(0, 0, 0, 0.32);
      box-shadow: 0 0 0 4px rgba(242, 167, 195, 0.14);
    }

    :deep(.el-form-item__content) {
      height: 100%;
      display: flex;
      align-items: center;
      flex-wrap: nowrap;
      line-height: normal;
    }

    :deep(.el-form-item__error) {
      top: 100%;
      padding-top: 4px;
    }
  }

  .svg-container {
    flex: 0 0 auto;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 10px;
    color: rgba(255, 255, 255, 0.45);

    .svg-icon {
      width: 18px;
      height: 18px;
    }
  }

  .el-input {
    flex: 1 1 0%;
    width: auto;
    height: 100%;
    min-width: 0;

    :deep(.el-input__wrapper) {
      height: 100%;
      padding: 0;
      background: transparent !important;
      box-shadow: none !important;
    }

    :deep(.el-input__inner) {
      height: 100%;
      padding: 0;
      border: 0;
      border-radius: 0;
      background: transparent;
      color: $light_gray;
      font-size: 15px;
      caret-color: $accent;
      line-height: 1;

      &:-webkit-autofill {
        box-shadow: 0 0 0px 1000px #141821 inset !important;
        -webkit-text-fill-color: $light_gray !important;
      }

      &::placeholder {
        color: rgba(255, 255, 255, 0.35);
      }
    }
  }

  .show-pwd {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    margin-left: 8px;
    font-size: 16px;
    color: rgba(255, 255, 255, 0.45);
    cursor: pointer;
    user-select: none;
    transition: color 0.2s;

    &:hover {
      color: $accent;
    }
  }
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  color: rgba(255, 255, 255, 0.6);

  :deep(.el-checkbox__label) {
    color: rgba(255, 255, 255, 0.65);
  }

  :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
    color: $accent;
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: $accent;
    border-color: $accent;
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 4px;
  box-shadow: 0 12px 30px rgba(242, 167, 195, 0.35);
}

.login-footer {
  margin-top: 26px;
  text-align: center;
  color: rgba(255, 255, 255, 0.35);
  font-size: 12px;
}
</style>
