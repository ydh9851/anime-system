<template>
  <div class="app-container">
    <el-alert type="primary" :closable="false" show-icon class="mode-tip" :title="modeTip"/>

    <el-form :inline="true">
      <el-form-item label="推荐算法">
        <el-select v-model="query.algo" style="width:340px">
          <el-option-group v-for="g in algoGroups" :key="g.group" :label="g.group">
            <el-option v-for="a in g.items" :key="a.value" :label="a.label" :value="a.value"/>
          </el-option-group>
        </el-select>
      </el-form-item>
      <el-form-item v-if="mode === 'user'" label="用户ID">
        <el-input v-model="query.userId" placeholder="如 15（= 动漫评分用户）" style="width:150px"/>
      </el-form-item>
      <el-form-item v-if="needMovie" label="动漫名">
        <el-input v-model="query.movieTitle" placeholder="如 Fullmetal Alchemist: Brotherhood" style="width:220px"/>
      </el-form-item>
      <el-form-item v-if="query.algo === 'usr_keywords'" label="关键词">
        <el-input v-model="query.keywords" placeholder="空格分隔，如 spy hero war army" style="width:220px"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="search">生成推荐</el-button>
      </el-form-item>
    </el-form>

    <el-alert type="info" :closable="false" show-icon class="algo-tip" :title="tipText"/>

    <div v-if="list.length" class="result-head">
      <h3>推荐结果 <span class="count">{{ list.length }}</span></h3>
      <el-button round size="small" @click="exportCsv">导出 CSV</el-button>
    </div>

    <div v-if="list.length" class="rec-grid stagger">
      <div v-for="(item, idx) in list" :key="item.movieId" class="rec-card">
        <div class="rec-poster">
          <img v-if="poster(item)" :src="poster(item)" alt="">
          <span v-else>番</span>
        </div>
        <div class="rec-rank" :class="'r-' + (idx + 1)">{{ idx + 1 }}</div>
        <div class="rec-body">
          <div class="rec-title" :title="item.title">{{ item.title }}</div>
          <div class="rec-meta">
            <span>ID {{ item.movieId }}</span>
            <span>热度 {{ item.popularity == null ? '-' : item.popularity }}</span>
            <span>均分 {{ item.voteAverage == null ? '-' : item.voteAverage }}</span>
          </div>
          <el-progress :percentage="scorePercent(item.score)" :stroke-width="8" :show-text="false" class="rec-progress" />
          <div class="rec-score">算法得分：{{ item.score == null ? '-' : Number(item.score).toFixed(4) }}</div>
        </div>
      </div>
    </div>

    <el-empty v-else-if="!loading" description="暂无推荐结果，点击上方「生成推荐」" />
  </div>
</template>

<script>
import recommendApi from '@/api/recommend'

// 统一算法清单（与《03-算法设计说明书》A8~A12 对应，按类别分组）
// mode: user=用户属性推荐入口 / video=动漫属性推荐入口
const ALGO_GROUPS = [
  {
    group: '统计与热门',
    items: [
      { label: '人口统计热门（IMDB 加权）', value: 'demographic', mode: 'user', type: '统计推荐', needUser: false, needMovie: false }
    ]
  },
  {
    group: '内容推荐（基于动漫属性）',
    items: [
      { label: '动漫简介内容相似（TF-IDF）', value: 'content', mode: 'video', type: '内容推荐', needUser: false, needMovie: true },
      { label: '动漫标签关键词（TF-IDF）', value: 'keyword', mode: 'video', type: '内容推荐', needUser: false, needMovie: true },
      { label: '动漫相似度协同 KNN（Item-KNN）', value: 'movie_knn', mode: 'video', type: '协同过滤', needUser: false, needMovie: true }
    ]
  },
  {
    group: '协同过滤与矩阵分解（基于用户属性）',
    items: [
      { label: '用户协同过滤 User-KNN', value: 'user_knn', mode: 'user', type: '协同过滤', needUser: true, needMovie: false },
      { label: 'SVD 评分矩阵分解', value: 'svd', mode: 'user', type: '矩阵分解', needUser: true, needMovie: false }
    ]
  },
  {
    group: '深度学习',
    items: [
      { label: 'NCF 神经协同过滤（NeuMF · PyTorch）', value: 'ncf', mode: 'user', type: '深度学习', needUser: true, needMovie: false }
    ]
  },
  {
    group: '集成推荐',
    items: [
      { label: '集成：User-KNN + SVD', value: 'knn_svd', mode: 'user', type: '集成', needUser: true, needMovie: false },
      { label: '集成：User-KNN + 关键词', value: 'usr_keywords', mode: 'user', type: '集成', needUser: true, needMovie: false, needKeywords: true },
      { label: '集成：User-KNN + Item-KNN', value: 'usr_movie_knn', mode: 'user', type: '集成', needUser: true, needMovie: true }
    ]
  }
]

export default {
  name: 'RecommendIndex',
  data () {
    return {
      query: { algo: '', userId: '15', movieTitle: 'Fullmetal Alchemist: Brotherhood', keywords: '', top: 10 },
      list: [],
      loading: false
    }
  },
  computed: {
    // 由路由 meta.mode 区分：user=用户属性推荐 / video=动漫属性推荐
    mode () {
      return this.$route.meta && this.$route.meta.mode === 'video' ? 'video' : 'user'
    },
    // 按当前模式过滤出的分组（用于下拉分组展示）
    algoGroups () {
      return ALGO_GROUPS
        .map(g => ({ group: g.group, items: g.items.filter(i => i.mode === this.mode) }))
        .filter(g => g.items.length)
    },
    // 扁平算法清单（用于查找与校验）
    algos () {
      return ALGO_GROUPS.reduce((acc, g) => acc.concat(g.items), [])
    },
    currentAlgo () {
      return this.algos.find(a => a.value === this.query.algo)
    },
    needUser () {
      return !!(this.currentAlgo && this.currentAlgo.needUser)
    },
    needMovie () {
      return !!(this.currentAlgo && this.currentAlgo.needMovie)
    },
    modeTip () {
      if (this.mode === 'video') {
        return '本页为「动漫属性推荐」：从某部动漫的内容属性出发（简介、标签、协同相似度），找出与之相似的番剧。只需提供动漫名，无需用户ID。'
      }
      return '本页为「用户属性推荐」：从看番用户的属性与行为出发（热门兜底、相似用户、评分矩阵等），为该用户生成个性化推荐。请填写动漫评分用户ID（页面中 11 = 动漫评分用户1）。'
    },
    tipText () {
      const algo = this.query.algo
      if (algo === 'ncf') {
        return '当前为深度学习算法：NCF 神经协同过滤（NeuMF 结构，PyTorch 实现）。通过用户/动漫嵌入向量与多层感知机学习非线性交互，首次调用需训练并缓存模型（约 15~30 秒），之后秒级返回。'
      }
      if (algo.startsWith('usr_') || algo === 'knn_svd') {
        return '当前为集成/混合算法：先基于用户KNN生成候选动漫，再结合第二种算法精排。服务端需训练多个协同过滤模型，耗时较长，请耐心等待。'
      }
      if (algo === 'user_knn' || algo === 'svd' || algo === 'movie_knn') {
        return '提示：该算法需在服务端训练协同过滤模型，耗时较长，请耐心等待。'
      }
      return 'demographic 为全局热门榜（无需用户ID），content/keyword 为基于动漫内容的相似推荐。'
    }
  },
  watch: {
    '$route.meta.mode' () {
      // 切换「用户属性 / 动漫属性」推荐模式时，重置为该模式的默认算法
      this.resetAlgo()
    }
  },
  mounted () {
    this.resetAlgo()
    // 从详情页“相似番剧”跳转时带入动漫名，自动预填
    const qTitle = this.$route.query && this.$route.query.title
    if (qTitle) {
      this.query.movieTitle = String(qTitle)
    }
  },
  methods: {
    resetAlgo () {
      // 默认算法：用户模式=热门兜底；动漫模式=内容相似
      this.query.algo = this.mode === 'video' ? 'content' : 'demographic'
      this.list = []
    },
    search () {
      const algo = this.query.algo
      if (!algo) {
        this.$message.warning('请先选择推荐算法')
        return
      }
      if (this.needUser && (this.query.userId === '' || this.query.userId == null)) {
        this.$message.error('算法「' + this.algoLabel(algo) + '」需要填写用户ID（如 11 = 动漫评分用户1）')
        return
      }
      if (this.needMovie && (this.query.movieTitle === '' || this.query.movieTitle == null)) {
        this.$message.error('算法「' + this.algoLabel(algo) + '」需要填写动漫名（如 Fullmetal Alchemist: Brotherhood）')
        return
      }
      if (algo === 'usr_keywords' && (this.query.keywords === '' || this.query.keywords == null)) {
        this.$message.error('算法「' + this.algoLabel(algo) + '」需要填写关键词（空格分隔，如 spy hero war army）')
        return
      }
      this.loading = true
      const q = { algo: algo, top: this.query.top }
      if (this.needUser) q.userId = Number(this.query.userId)
      if (this.needMovie) q.movieTitle = this.query.movieTitle.trim()
      if (algo === 'usr_keywords') q.keywords = this.query.keywords.trim()
      recommendApi.recommend(q).then(re => {
        this.list = re.response || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    poster (item) {
      const p = item.posterPath
      return p && String(p).startsWith('http') ? p : ''
    },
    algoLabel (value) {
      const hit = this.algos.find(a => a.value === value)
      return hit ? hit.label : value
    },
    scorePercent (score) {
      if (score == null) return 0
      const max = Math.max.apply(null, this.list.map(i => Number(i.score) || 0).concat([0.0001]))
      return Math.min(100, Math.round(Number(score) / max * 100))
    },
    exportCsv () {
      if (!this.list.length) return
      const headers = ['动漫ID', '动漫名称', '热度', '均分', '算法得分']
      const rows = this.list.map(i => [i.movieId, i.title, i.popularity == null ? '' : i.popularity, i.voteAverage == null ? '' : i.voteAverage, i.score == null ? '' : i.score])
      const csv = [headers].concat(rows)
        .map(r => r.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(','))
        .join('\n')
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = '番析-推荐结果-' + Date.now() + '.csv'
      a.click()
      URL.revokeObjectURL(a.href)
      this.$message.success('推荐结果已导出')
    }
  }
}
</script>

<style scoped>
.mode-tip {
  margin-bottom: 14px;
}
.algo-tip {
  margin-bottom: 12px;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 18px 0 14px;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;

    .count {
      display: inline-block;
      margin-left: 8px;
      font-size: 12px;
      color: #fff;
      background: linear-gradient(135deg, #f2a7c3, #d9a7e0);
      border-radius: 999px;
      padding: 2px 10px;
      vertical-align: middle;
    }
  }
}

.rec-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1100px) {
  .rec-grid { grid-template-columns: 1fr; }
}

.rec-card {
  position: relative;
  display: flex;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.05);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.25s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 44px rgba(15, 23, 42, 0.13);
  }
}

.rec-poster { flex: 0 0 auto; width: 54px; height: 76px; border-radius: 10px; overflow: hidden; background: #f8eef4; display: flex; align-items: center; justify-content: center; color: #f2a7c3; font-weight: 800; }
.rec-poster img { width: 100%; height: 100%; object-fit: cover; }
.rec-rank {
  flex: 0 0 auto;
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 14px;
  color: #64748b;
  background: #f1f5f9;
}

.rec-rank.r-1 { background: linear-gradient(135deg, #ffd700, #ffaa00); color: #fff; }
.rec-rank.r-2 { background: linear-gradient(135deg, #c0c0c0, #a0a0a0); color: #fff; }
.rec-rank.r-3 { background: linear-gradient(135deg, #cd7f32, #b87333); color: #fff; }

.rec-body {
  flex: 1 1 auto;
  min-width: 0;
}

.rec-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rec-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 8px 0 10px;
  font-size: 12px;
  color: #64748b;
}

.rec-progress {
  :deep(.el-progress-bar__outer) {
    border-radius: 999px;
    background: #eef2f7;
  }

  :deep(.el-progress-bar__inner) {
    border-radius: 999px;
    background: linear-gradient(90deg, #f2a7c3, #d9a7e0);
  }
}

.rec-score {
  margin-top: 6px;
  font-size: 12px;
  color: #d9a7e0;
  font-weight: 600;
}
</style>
