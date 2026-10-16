import { get, put, del, service } from '@/utils/request'

export default {
  /** 列表查询 */
  list: () => get('/api/admin/dataset/list'),
  /** 详情 */
  detail: id => get(`/api/admin/dataset/${id}`),
  /** 上传数据集（multipart，字段名固定为 file） */
  upload: (file, name, remark) => {
    const form = new FormData()
    form.append('file', file)
    if (name) form.append('name', name)
    if (remark) form.append('remark', remark)
    return service.post('/api/admin/dataset/upload', form)
  },
  /** 修改名称 / 备注 */
  update: (id, name, remark) => put(`/api/admin/dataset/${id}`, undefined, { params: { name, remark } }),
  /** 删除（同时清理磁盘文件） */
  delete: id => del(`/api/admin/dataset/${id}`)
}
