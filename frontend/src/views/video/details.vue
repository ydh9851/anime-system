<template>
  <div class="app-container detail-page" v-loading="loading">
    <template v-if="form.videoId">
      <!-- Hero -->
      <el-card class="glass-card hero-card fade-up">
        <div class="hero">
          <div class="poster">
            <img v-if="poster" :src="poster" :alt="form.videoName">
            <div v-else class="poster-empty">暂无海报</div>
            <span v-if="form.releaseYear" class="poster-year">{{ form.releaseYear }}</span>
          </div>
          <div class="info">
            <div class="title-row">
              <h2>{{ form.videoName }}</h2>
              <el-tag v-if="categoryName" size="small" effect="plain" class="cat-tag">{{ categoryName }}</el-tag>
            </div>
            <p v-if="form.originalTitle" class="sub">{{ form.originalTitle }}</p>
            <div class="chips">
              <span v-if="langName" class="chip">🗣 {{ langName }}</span>
              <span v-if="form.releaseYear" class="chip">📅 {{ form.releaseYear }} 年</span>
              <span v-if="form.runtime" class="chip">⏱ {{ form.runtime }} 分钟</span>
              <span v-if="form.budget" class="chip">📺 {{ form.budget }} 集</span>
            </div>

            <div class="score-row">
              <div class="score-main">
                <b>{{ form.voteAverage == null ? '-' : form.voteAverage }}</b>
                <small>评分</small>
              </div>
              <el-rate v-model="starRate" disabled :colors="['#f2a7c3','#f2a7c3','#f2a7c3']" />
              <div class="score-side">
                <div><b>{{ fmtCount(form.voteCount) }}</b><small>评分人数</small></div>
                <div><b>{{ fmtMoney(form.revenue) }}</b><small>热度(成员数)</small></div>
                <div><b>{{ fmtCount(form.popularity) }}</b><small>受欢迎度</small></div>
              </div>
            </div>

            <p v-if="form.overview" class="overview">{{ form.overview }}</p>

            <div class="actions">
              <router-link :to="{ path: '/video/play', query: { id: form.videoId } }">
                <el-button type="primary" round icon="el-icon-video-play">立即播放</el-button>
              </router-link>
              <el-button round icon="el-icon-star-off" @click="toggleCollect">
                {{ collected ? '已追番' : '加入追番' }}
              </el-button>
              <router-link :to="{ path: '/recommend/subject/edit', query: { title: form.videoName } }">
                <el-button round>相似番剧</el-button>
              </router-link>
            </div>
          </div>
        </div>
      </el-card>

      <!-- Related -->
      <el-card class="glass-card fade-up" v-if="related.length">
        <template #header>
          <div class="rel-head">
            <b>相关番剧推荐</b>
            <span class="rel-sub">热门与相似番剧</span>
          </div>
        </template>
        <div class="rel-grid stagger">
          <router-link
            v-for="r in related"
            :key="r.videoId"
            :to="{ path: '/video/details', query: { id: r.videoId } }"
            class="rel-card"
          >
            <div class="rel-cover">
              <img v-if="r.posterPath && String(r.posterPath).startsWith('http')" :src="r.posterPath" :alt="r.videoName">
              <span v-else class="ph">番</span>
            </div>
            <div class="rel-name" :title="r.videoName">{{ r.videoName }}</div>
            <div class="rel-meta">评分 {{ r.voteAverage == null ? '-' : r.voteAverage }}</div>
          </router-link>
        </div>
      </el-card>
    </template>

    <el-empty v-else-if="!loading" description="未找到该动漫信息" />
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'
import videoApi from '@/api/video'
import collectionApi from '@/api/collection'

const LANG_MAP = {
  ja: '日语', zh: '中文', en: '英语', ko: '韩语', fr: '法语', de: '德语',
  es: '西班牙语', ru: '俄语', it: '意大利语', pt: '葡萄牙语', hi: '印地语',
  sv: '瑞典语', da: '丹麦语', ar: '阿拉伯语', nl: '荷兰语', no: '挪威语', pl: '波兰语'
}

export default {
  name: 'VideoDetail',
  data () {
    return {
      form: {},
      related: [],
      loading: false,
      collected: false
    }
  },
  computed: {
    poster () {
      const p = this.form.posterPath
      return p && String(p).startsWith('http') ? p : ''
    },
    starRate () {
      return Math.min(5, Math.max(0, (this.form.voteAverage || 0) / 2))
    },
    langName () {
      return this.form.originalLanguage ? (LANG_MAP[this.form.originalLanguage] || this.form.originalLanguage) : ''
    },
    categoryName () {
      if (this.form.videoCategory == null) return ''
      return this.enumFormat(this.categoryEnum, this.form.videoCategory)
    },
    ...mapGetters('enumItem', ['enumFormat']),
    ...mapState('enumItem', { categoryEnum: state => state.user.categoryEnum })
  },
  created () {
    const id = this.$route.query.id
    if (id && parseInt(id) !== 0) {
      this.load(id)
    }
  },
  methods: {
    load (id) {
      this.loading = true
      videoApi.selectVideo(id).then(re => {
        this.form = re.response || {}
        return videoApi.userAnalysis(id).then(r2 => {
          this.related = (r2.response && r2.response.videoRecommendList) || []
        }).catch(() => { this.related = [] })
      }).catch(() => { this.form = {} })
        .finally(() => { this.loading = false })
    },
    toggleCollect () {
      if (!this.form.videoId) return
      collectionApi.toggle(this.form.videoId).then(re => {
        this.collected = re.response === 1
        this.$message.success(this.collected ? '已加入我的追番' : '已取消追番')
      })
    },
    fmtMoney (v) {
      if (v == null) return '-'
      const n = Number(v)
      if (n >= 1e8) return (n / 1e8).toFixed(2) + ' 亿'
      if (n >= 1e4) return (n / 1e4).toFixed(1) + ' 万'
      return n.toLocaleString('en-US')
    },
    fmtCount (v) {
      if (v == null) return '-'
      return Number(v).toLocaleString('en-US')
    }
  }
}
</script>

<style lang="scss" scoped>
.hero-card { margin-bottom: 18px; }
.hero { display: flex; gap: 28px; }
.poster { position: relative; flex: 0 0 auto; width: 230px; }
.poster img { width: 230px; height: 320px; object-fit: cover; border-radius: 16px; box-shadow: 0 16px 40px rgba(15,23,42,.22); }
.poster-empty { width: 230px; height: 320px; border-radius: 16px; background: linear-gradient(135deg,#f8eef4,#eef2ff); display:flex; align-items:center; justify-content:center; color:#f2a7c3; font-size: 40px; font-weight: 800; }
.poster-year { position: absolute; left: 10px; bottom: 10px; background: rgba(15,23,42,.7); color:#fff; font-size: 12px; padding: 3px 10px; border-radius: 999px; }

.info { flex: 1 1 auto; min-width: 0; }
.title-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.info h2 { margin: 0; font-size: 24px; color: #0f172a; font-weight: 800; }
.cat-tag { color: #d9a7e0; border-color: rgba(168,85,247,.35); background: rgba(168,85,247,.08); }
.sub { color: #94a3b8; margin: 8px 0 14px; }

.chips { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.chip { font-size: 12px; padding: 5px 12px; border-radius: 999px; color: #475569; background: #f1f5f9; }

.score-row { display: flex; align-items: center; gap: 18px; flex-wrap: wrap; padding: 14px 18px; border-radius: 16px; background: linear-gradient(135deg, rgba(255,95,158,.08), rgba(168,85,247,.08)); margin-bottom: 16px; }
.score-main { display: flex; flex-direction: column; align-items: center; }
.score-main b { font-size: 28px; color: #f2a7c3; font-weight: 800; line-height: 1; }
.score-main small { font-size: 11px; color: #94a3b8; margin-top: 4px; }
.score-side { display: flex; gap: 24px; }
.score-side div { display: flex; flex-direction: column; }
.score-side b { font-size: 16px; color: #0f172a; font-weight: 700; }
.score-side small { font-size: 11px; color: #94a3b8; margin-top: 2px; }

.overview { color: #475569; line-height: 1.9; font-size: 13px; margin: 0 0 18px; max-height: 140px; overflow: auto; }

.actions { display: flex; gap: 12px; flex-wrap: wrap; }

.rel-head { display: flex; align-items: baseline; gap: 12px; }
.rel-head b { font-size: 16px; color: #0f172a; }
.rel-sub { font-size: 12px; color: #94a3b8; }

.rel-grid { display: grid; grid-template-columns: repeat(6, minmax(0,1fr)); gap: 14px; }
@media (max-width: 1200px) { .rel-grid { grid-template-columns: repeat(4, minmax(0,1fr)); } }
@media (max-width: 760px) { .rel-grid { grid-template-columns: repeat(3, minmax(0,1fr)); } }

.rel-card { text-decoration: none; background: #fff; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 26px rgba(15,23,42,.07); transition: transform .25s, box-shadow .25s; }
.rel-card:hover { transform: translateY(-5px); box-shadow: 0 18px 40px rgba(15,23,42,.14); }
.rel-cover { height: 150px; background: #f8eef4; display: flex; align-items: center; justify-content: center; }
.rel-cover img { width: 100%; height: 100%; object-fit: cover; }
.ph { font-size: 30px; color: #f2a7c3; font-weight: 800; }
.rel-name { padding: 8px 10px 2px; font-size: 13px; font-weight: 700; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rel-meta { padding: 0 10px 10px; font-size: 11px; color: #94a3b8; }
</style>
