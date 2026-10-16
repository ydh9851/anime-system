import { get, post } from '@/utils/request'

export default {
  /** 支持的算法列表 */
  algos: () => get('/api/admin/model/algos'),
  /** 提交训练任务（异步，立即返回 taskId） */
  train: query => post('/api/admin/model/train', query),
  /** 全部训练任务 */
  list: () => get('/api/admin/model/list'),
  /** 任务状态 + 评估指标（轮询用） */
  task: taskId => get(`/api/admin/model/task/${taskId}`),
  /** 各状态任务数 */
  stats: () => get('/api/admin/model/stats')
}
