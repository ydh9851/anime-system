import { post, get, put, del } from '@/utils/request'

export default {
  getVideoPageList: query => post('/api/admin/video/page/list', query),
  /** 筛选面板元数据：各维度选项与命中数量 */
  getFilterOptions: () => get('/api/admin/video/filter-options'),
  createVideo: query => post('/api/admin/video', query),
  editVideo: (id, query) => put('/api/admin/video/' + id, query),
  selectVideo: id => get('/api/admin/video/' + id),
  deleteVideo: id => del('/api/admin/video/' + id),
  selectByVideoName: query => get('/api/admin/video/name', query),
  getVideoDetailByVideoId: id => get('/api/admin/video/' + id + '/basic'),
  /** 相关番剧推荐（题材重合度 + 年代接近 + 热度排序） */
  relatedVideos: (id, limit = 6) => get('/api/admin/video/' + id + '/related?limit=' + limit),
  userAnalysis: id => get('/api/admin/video/' + id + '/user-analysis')
}
