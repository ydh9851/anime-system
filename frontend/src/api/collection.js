import { post } from '@/utils/request'
export default {
  toggle: (videoId) => post('/api/collection/toggle?videoId=' + videoId, {}),
  list: () => post('/api/collection/list', {})
}
