import { post, get } from '@/utils/request'

export default {
  /** 首页指标 +趋势数据 */
  index: () => post('/api/admin/dashboard/index'),
  /** 首页可视化分布：年份分布(柱状图) / 类型分布(饼图) / 热度TOP10 */
  distribution: () => get('/api/admin/dashboard/distribution'),
  /** 热度榜（自定义条数） */
  heatTop: (limit = 10) => post('/api/admin/dashboard/heat-top', { limit }),
  /** 重算全库热度 */
  recalculateHeat: () => post('/api/admin/dashboard/recalculate-heat')
}
