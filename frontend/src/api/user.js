import { post, get, put, del } from '@/utils/request'

export default {
  /** 分页查询（组合条件放请求体，用 POST） */
  getUserPageList: query => post('/api/admin/user/page/list', query),
  /** 用户行为日志分页 */
  getUserEventPageList: query => post('/api/admin/user/event/page/list', query),
  /** 新增用户 */
  createUser: query => post('/api/admin/user', query),
  /** 修改指定用户 */
  updateUserById: (id, query) => put(`/api/admin/user/${id}`, query),
  /** 修改本人信息 */
  updateProfile: query => put('/api/admin/user/profile', query),
  /** 启用 / 禁用 */
  changeStatus: id => put(`/api/admin/user/${id}/status`),
  /** 删除（逻辑删除） */
  deleteUser: id => del(`/api/admin/user/${id}`),
  /** 用户详情 */
  getUserDetail: id => get(`/api/admin/user/${id}`),
  /** 兼容旧调用名 */
  selectUser: id => get(`/api/admin/user/${id}`),
  /** 兼容旧调用名 */
  getUserDetailByUserId: id => get(`/api/admin/user/${id}`),
  /** 修改指定用户（兼容旧调用：默认取表单里的 id） */
  updateUser: query => put(`/api/admin/user/${query.id}`, query),
  /** 当前登录用户 */
  getCurrentUser: () => get('/api/admin/user/current'),
  /** 用户名下拉（模糊查询） */
  selectByUserName: userName => get('/api/admin/user/names', { userName })
}
