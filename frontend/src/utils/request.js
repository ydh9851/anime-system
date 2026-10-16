import axios from 'axios'
import { ElLoading, ElMessage } from 'element-plus'
import { router } from '@/router'
import Cookies from 'js-cookie'

/**
 * 统一请求封装。
 *
 * 设计要点：
 *  1. 用 axios.create 建实例，baseURL / 超时 / withCredentials 只配一次；
 *  2. 请求拦截器：统一补请求头，避免每个接口重复写；
 *  3. 响应拦截器：按后端统一返回结构 { code, message, response } 统一处理，
 *     code=1 放行；401/502 视为未登录（清 Cookie 并跳登录页）；
 *     其余业务错误统一弹ElMessage提示并 reject，页面只需处理成功分支；
 *  4. HTTP 层异常（4xx/5xx/超时/断网）也在这里兜底转成可读提示。
 */

// 后端 SystemCode：1 成功 / 401 未登录 / 402 账密错误 / 500 内部错误 / 501 参数校验 / 502 无权限
const SUCCESS_CODE = 1
const AUTH_CODES = [401, 402, 502]

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_URL,
  timeout: 30000,
  // 后端登录态是 Session + Spring Session Redis，必须携带 Cookie
  withCredentials: true
})

// ---------------- 请求拦截器 ----------------
service.interceptors.request.use(
  (config) => {
    config.headers['X-Requested-With'] = 'XMLHttpRequest'
    // GET / DELETE 的参数走 query，不能设成 json body
    if (config.method !== 'get' && config.method !== 'delete') {
      if (!(config.data instanceof FormData)) {
        config.headers['Content-Type'] = 'application/json'
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------------- Loading 计数，避免并发请求互相提前关闭 ----------------
let loadingCount = 0
let loadingInstance = null

const openLoading = () => {
  loadingCount += 1
  if (!loadingInstance) {
    loadingInstance = ElLoading.service({
      lock: false,
      text: '正在加载中…',
      background: 'rgba(0, 0, 0, 0.4)'
    })
  }
}

const closeLoading = () => {
  if (loadingCount <= 0) return
  loadingCount -= 1
  if (loadingCount === 0 && loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
}

const clearLogin = () => {
  Cookies.remove('adminUserName')
  Cookies.remove('adminUserInfo')
}

// ---------------- 响应拦截器 ----------------
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非统一结构（如文件流、第三方回调）直接透传
    if (!res || typeof res.code === 'undefined') {
      return res
    }
    if (res.code === SUCCESS_CODE) {
      return res
    }
    if (AUTH_CODES.includes(res.code)) {
      clearLogin()
      if (router.currentRoute.value.path !== '/login') {
        router.push({ path: '/login' })
      }
      return Promise.reject(res)
    }
    // 其余业务错误：统一提示，页面只需处理成功分支
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(res)
  },
  (error) => {
    const status = error.response ? error.response.status : 0
    let message = '网络异常，请稍后重试'
    if (status === 401 || status === 403) {
      message = '登录状态已失效，请重新登录'
      clearLogin()
      if (router.currentRoute.value.path !== '/login') {
        router.push({ path: '/login' })
      }
    } else if (status === 404) {
      message = '接口不存在(404)'
    } else if (status === 413) {
      message = '上传文件超过大小限制'
    } else if (status >= 500) {
      message = '服务器内部错误，请稍后重试'
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请检查网络'
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/**
 * 带全局 Loading 的请求（登录等关键操作用）
 */
const postWithLoadTip = function (url, params) {
  openLoading()
  return service.post(url, params).finally(closeLoading)
}

const post = function (url, params) {
  return service.post(url, params)
}

const postWithOutLoadTip = function (url, params) {
  return service.post(url, params)
}

const get = function (url, params) {
  return service.get(url, { params })
}

/**
 * PUT 请求。params 走请求体；需要把参数放URL 上的场景（如更新名称/备注）传第三个参数 config
 * 例：put(`/api/admin/dataset/${id}`, undefined, { params: { name, remark } })
 */
const put = function (url, params, config) {
  return service.put(url, params, config)
}

const del = function (url, params) {
  return service.delete(url, { params })
}

// 文件上传：multipart/form-data，Content-Type 交给浏览器自动带 boundary
const form = function (url, params) {
  return service.post(url, params, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export {
  service,
  post,
  postWithLoadTip,
  postWithOutLoadTip,
  get,
  put,
  del,
  form
}
