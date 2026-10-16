import { get } from '@/utils/request'

export default {
  overview: () => get('/api/admin/analysis/overview'),
  videoOverview: () => get('/api/admin/analysis/video-overview'),
  userOverview: () => get('/api/admin/analysis/user-overview')
}
