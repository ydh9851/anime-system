import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import store from './store'
import 'normalize.css/normalize.css'
import ElementPlus, { ElMessage, ElLoading } from 'element-plus'
import 'element-plus/dist/index.css'
import '@/styles/index.scss'
import 'virtual:svg-icons-register'
import SvgIcon from '@/components/SvgIcon'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import Cookies from 'js-cookie'
import { applyTheme, currentTheme } from '@/utils/theme'

const app = createApp(App)

app.use(ElementPlus, {
  size: 'default'
})
app.use(store)
app.use(router)

// 全局注册 svg-icon 组件（旧代码里通过 Vue.component 注册）
app.component('svg-icon', SvgIcon)

// 兼容旧代码里通过 this.$message / this.$loading 调用的写法
app.config.globalProperties.$message = ElMessage
app.config.globalProperties.$loading = ElLoading.service

NProgress.configure({ showSpinner: false })

// 无需登录即可访问的路径
const WHITE_LIST = ['/login', '/404']

const isLogged = () => {
  const hasName = !!Cookies.get('adminUserName')
  const hasSession = document.cookie.includes('SESSION') || document.cookie.includes('JSESSIONID')
  return hasName || hasSession
}

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  if (to.meta.title !== undefined) {
    document.title = to.meta.title
  } else {
    document.title = '\u200E'
  }
  store.commit('router/initRoutes')

  // 登录校验：已登录访问登录页 -> 回首页；未登录访问受保护页面 -> 跳登录页并带上 redirect
  if (WHITE_LIST.includes(to.path)) {
    if (to.path === '/login' && isLogged()) {
      next({ path: '/' })
      return
    }
    next()
    return
  }
  if (!isLogged()) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.path && typeof window._hmt !== 'undefined') {
    window._hmt.push(['_trackPageview', '/#' + to.fullPath])
  }
  next()
})

router.afterEach(() => {
  NProgress.done()
})

// 兼容旧代码里通过 this.$$router 跳转的写法
app.config.globalProperties.$$router = router

applyTheme(currentTheme())
app.mount('#app')
