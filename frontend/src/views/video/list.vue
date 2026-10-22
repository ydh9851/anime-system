<template>
  <div class="app-container">
    <div class="toolbar fade-up">
      <el-input v-model="queryParam.videoName" placeholder="搜索动漫名称" prefix-icon="el-icon-search"
                clearable @keyup.enter.native="submitForm" class="search-input" />
      <el-button type="primary" round icon="el-icon-search" @click="submitForm">查询</el-button>
    </div>

    <div v-loading="listLoading" class="card-grid" v-if="tableData.length">
      <div class="anime-card fade-up" v-for="(row, idx) in tableData" :key="row.videoId" :style="{ animationDelay: (idx % 10) * 40 + 'ms' }">
        <div class="poster-wrap">
          <img v-if="validPoster(row.posterPath)" :src="row.posterPath" :alt="row.videoName" loading="lazy" class="poster">
          <div v-else class="poster-placeholder">
            <span class="ph-char">番</span>
          </div>
          <span v-if="row.premiered" class="premiered-badge">{{ row.premiered }}</span>
          <span v-if="countryName(row.country)" class="country-badge" :class="'c-' + (row.country || '')">{{ countryName(row.country) }}</span>
          <div class="poster-overlay">
            <router-link :to="{ path: '/video/details', query: { id: row.videoId } }">
              <el-button size="mini" type="primary" round plain>详情</el-button>
            </router-link>
            <el-button size="mini" round plain @click="collect(row)" :icon="row.collected ? 'el-icon-star-off' : 'el-icon-star-off'">
              {{ row.collected ? '已追番' : '追番' }}
            </el-button>
          </div>
        </div>
        <div class="card-body">
          <div class="name-row">
            <span class="name" :title="row.videoName">{{ row.videoName }}</span>
            <span v-if="row.videoType" class="type-tag">{{ typeName(row.videoType) }}</span>
          </div>
          <div class="meta-row">
            <span class="score">
              <b>{{ row.voteAverage == null ? '-' : Number(row.voteAverage).toFixed(1) }}</b>
              <i class="score-star">★</i>
            </span>
            <span class="votes">{{ fmtCount(row.voteCount) }} 人评分</span>
          </div>
          <div class="meta-row sub">
            <span v-if="row.budget" class="chips">📺 {{ row.budget }} 集</span>
            <span v-if="row.revenue" class="chips">🔥 {{ fmtHeat(row.revenue) }}</span>
            <span v-if="countryName(row.country)" class="chips flag">🌏 {{ countryName(row.country) }}</span>
          </div>
          <div class="card-actions">
            <el-button size="mini" icon="el-icon-edit" @click="editVideo(row)">编辑</el-button>
            <el-button size="mini" icon="el-icon-video-play" @click="playVideo(row)">播放</el-button>
            <el-button size="mini" type="danger" icon="el-icon-delete" @click="deleteVideo(row)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else-if="!listLoading" description="暂无动漫数据" />

    <div class="pager-wrap">
      <pagination v-show="total>0" :total="total" v-model:page="queryParam.pageIndex" v-model:limit="queryParam.pageSize"
                  @pagination="search"/>
    </div>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import Pagination from '@/components/Pagination'
import videoApi from '@/api/video'
import collectionApi from '@/api/collection'

const COUNTRY_MAP = { JP: '日本', CN: '中国', KR: '韩国' }
const TYPE_MAP = {
  TV: 'TV动画', Movie: '剧场版', OVA: 'OVA', ONA: '网络动画', Special: '特别篇', Music: '音乐'
}

export default {
  components: { Pagination },
  data () {
    return {
      queryParam: {
        videoName: '',
        role: 1,
        pageIndex: 1,
        pageSize: 12
      },
      listLoading: true,
      tableData: [],
      total: 0
    }
  },
  created () {
    this.search()
  },
  methods: {
    search () {
      this.listLoading = true
      videoApi.getVideoPageList(this.queryParam).then(data => {
        const re = data.response
        this.tableData = re.list
        this.total = re.total
        this.queryParam.pageIndex = re.pageNum
        this.listLoading = false
      }).catch(() => {
        listLoading = false
      })
    },
    editVideo (row) {
      this.$router.push({ path: '/video/edit', query: { id: row.videoId } })
    },
    playVideo (row) {
      this.$router.push({ path: '/video/play', query: { id: row.videoId } })
    },
    deleteVideo (row) {
      this.$confirm('确定删除「' + row.videoName + '」吗？', '提示', {
        confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
      }).then(() => {
        videoApi.deleteVideo(row.videoId).then(re => {
          if (re.code === 1) {
            this.$message.success(re.message)
            this.search()
          } else {
            this.$message.error(re.message)
          }
        }).catch(() => {
        listLoading = false
      })
      }).catch(() => {})
    },
    collect (row) {
      collectionApi.toggle(row.videoId).then(re => {
        row.collected = re.response === 1
        this.$message.success(row.collected ? '已加入我的追番' : '已取消追番')
      }).catch(() => {})
    },
    submitForm () {
      this.queryParam.pageIndex = 1
      this.search()
    },
    validPoster (p) {
      return p && String(p).startsWith('http')
    },
    countryName (c) {
      return c ? (COUNTRY_MAP[c] || c) : ''
    },
    typeName (t) {
      return TYPE_MAP[t] || t || ''
    },
    fmtCount (v) {
      if (v == null) return '-'
      return Number(v).toLocaleString('en-US')
    },
    fmtHeat (v) {
      if (v == null) return '-'
      const n = Number(v)
      if (n >= 1e8) return (n / 1e8).toFixed(1) + ' 亿'
      if (n >= 1e4) return (n / 1e4).toFixed(1) + ' 万'
      return String(n)
    }
  },
  computed: {
    ...mapGetters('enumItem', ['enumFormat']),
    ...mapState('enumItem', { categoryEnum: state => state.user.categoryEnum })
  }
}
</script>

<style lang="scss" scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  .search-input { width: 320px; }
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}
@media (max-width: 1400px) { .card-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 1000px) { .card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) { .card-grid { grid-template-columns: repeat(1, minmax(0, 1fr)); } }

.anime-card {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s;
  animation: anime-fade-up 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 24px 48px rgba(168, 85, 247, 0.16);
    .poster-overlay { opacity: 1; }
  }
}

.poster-wrap {
  position: relative;
  aspect-ratio: 3 / 4;
  background: linear-gradient(135deg, #f8eef4, #eef2ff);
  overflow: hidden;
}
.poster {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.45s cubic-bezier(0.22, 1, 0.36, 1);
}
.anime-card:hover .poster { transform: scale(1.06); }
.poster-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ph-char { font-size: 64px; font-weight: 800; color: #f2a7c3; }

.premiered-badge {
  position: absolute;
  left: 10px;
  top: 10px;
  background: rgba(15, 23, 42, 0.72);
  color: #fff;
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 999px;
  backdrop-filter: blur(4px);
}
.country-badge {
  position: absolute;
  right: 10px;
  top: 10px;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 999px;
  backdrop-filter: blur(4px);
  &.c-JP { background: rgba(255, 87, 87, 0.9); color: #fff; }
  &.c-CN { background: rgba(249, 115, 22, 0.9); color: #fff; }
  &.c-KR { background: rgba(99, 102, 241, 0.9); color: #fff; }
}

.poster-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(15, 23, 42, 0.32);
  opacity: 0;
  transition: opacity 0.25s;
  :deep(.el-button) { background: rgba(255, 255, 255, 0.92) !important; color: #0f172a; border: none; font-weight: 600; }
}

.card-body { padding: 12px 14px 14px; }
.name-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.name {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.type-tag {
  flex: 0 0 auto;
  font-size: 10px;
  color: #a855f7;
  background: rgba(168, 85, 247, 0.1);
  border: 1px solid rgba(168, 85, 247, 0.25);
  padding: 2px 7px;
  border-radius: 999px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  &.sub { gap: 6px; flex-wrap: wrap; }
}
.score {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
  b { font-size: 16px; font-weight: 800; color: #a855f7; }
  .score-star { font-style: normal; color: #f2a7c3; font-size: 12px; }
}
.votes { font-size: 11px; color: #94a3b8; }
.chips { font-size: 11px; color: #64748b; background: #f1f5f9; padding: 2px 8px; border-radius: 999px; }

.card-actions {
  display: flex;
  gap: 6px;
  margin-top: 12px;
  :deep(.el-button) { margin-left: 0; padding: 5px 9px; }
}

.pager-wrap { margin-top: 18px; display: flex; justify-content: center; }
</style>