<template>
  <div class="dashboard-container">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-bg" />
      <div class="hero-orbs">
        <span class="orb o1" />
        <span class="orb o2" />
      </div>
      <div class="hero-content fade-up">
        <span class="hero-kicker">ANIME ANALYTICS PLATFORM</span>
        <h1>番析 AniScope</h1>
        <p>动漫数据分析与推荐系统 · 洞察评分、热度、题材与趋势</p>
        <div class="hero-tags">
          <span>数据探索</span>
          <span>热度预测</span>
          <span>智能推荐</span>
        </div>
      </div>
    </section>

    <!-- Quick actions -->
    <section class="quick-actions fade-up">
      <router-link v-for="q in quickActions" :key="q.path" :to="q.path" class="qa-card">
        <span class="qa-icon">{{ q.icon }}</span>
        <span class="qa-text">
          <b>{{ q.label }}</b>
          <small>{{ q.desc }}</small>
        </span>
      </router-link>
    </section>

    <!-- Stats cards -->
    <section class="stat-grid stagger">
      <div class="stat-card">
        <div class="stat-icon icon-pink"><svg-icon icon-class="user" /></div>
        <div class="stat-meta">
          <span class="stat-label">本月新增用户</span>
          <count-to :start-val="0" :end-val="newUserCount" :duration="2600" class="stat-value" />
        </div>
        <div class="stat-glow glow-pink" />
      </div>

      <div class="stat-card">
        <div class="stat-icon icon-blue"><svg-icon icon-class="exam" /></div>
        <div class="stat-meta">
          <span class="stat-label">新增动漫</span>
          <count-to :start-val="0" :end-val="newVideoCount" :duration="3000" class="stat-value" />
        </div>
        <div class="stat-glow glow-blue" />
      </div>

      <div class="stat-card">
        <div class="stat-icon icon-green"><svg-icon icon-class="doexampaper" /></div>
        <div class="stat-meta">
          <span class="stat-label">动漫播放次数</span>
          <count-to :start-val="0" :end-val="doPlayVideoCount" :duration="3600" class="stat-value" />
        </div>
        <div class="stat-glow glow-green" />
      </div>

      <div class="stat-card">
        <div class="stat-icon icon-purple"><svg-icon icon-class="star" /></div>
        <div class="stat-meta">
          <span class="stat-label">最佳番剧</span>
          <div class="stat-value hot-video">{{ hotVideoCount || '-' }}</div>
        </div>
        <div class="stat-glow glow-purple" />
      </div>
    </section>

    <!-- Charts -->
    <section class="chart-grid">
      <div class="glass-card chart-card fade-up" v-loading="loading">
        <header class="card-head">
          <h3>用户活跃度趋势</h3>
          <span class="badge">近 30 天</span>
        </header>
        <div id="echarts-moth-user" class="chart" />
      </div>
      <div class="glass-card chart-card fade-up" v-loading="loading">
        <header class="card-head">
          <h3>动漫月播放次数</h3>
          <span class="badge">MONTHLY</span>
        </header>
        <div id="echarts-moth-question" class="chart" />
      </div>
    </section>

    <!-- Heat top list -->
    <section class="glass-card heat-card fade-up" v-loading="loading">
      <header class="card-head">
        <h3>动漫热度榜单 TOP10</h3>
        <span class="badge badge-hot">实时</span>
      </header>
      <el-table :data="heatTopList" fit style="width:100%" class="anime-table">
        <el-table-column type="index" label="排名" width="80" align="center">
          <template #default="{ $index }">
            <span class="rank-badge" :class="'rank-' + ($index + 1)">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="videoName" label="动漫名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="voteAverage" label="评分" width="120" align="center">
          <template #default="{ row }">
            <el-rate v-model="row.starRate" disabled :colors="['#f2a7c3', '#f2a7c3', '#f2a7c3']" />
            <span class="rate-num">{{ row.voteAverage }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="popularity" label="受欢迎度" width="120" align="center" />
        <el-table-column prop="heatScore" label="热度" width="120" align="center">
          <template #default="{ row }">
            <span class="heat-score">{{ formatHeat(row.heatScore) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script>
import resize from './components/mixins/resize'
import CountTo from '@/components/CountTo'
import dashboardApi from '@/api/dashboard'
import * as echarts from 'echarts'
import 'echarts/theme/macarons'
export default {
  mixins: [resize],
  components: {
    CountTo
  },
  data () {
    return {
      newUserCount: 0,
      doPlayVideoCount: 0,
      newVideoCount: 0,
      hotVideoCount: '',
      heatTopList: [],
      quickActions: [
        { path: '/analysis/eda', icon: '📊', label: '数据探索', desc: '19 张 EDA 图表' },
        { path: '/predict/index', icon: '🔥', label: '热度预测', desc: '6 种模型在线预测' },
        { path: '/recommend/UserList', icon: '🎯', label: '智能推荐', desc: '9 种推荐算法' },
        { path: '/video/list', icon: '🎌', label: '动漫库', desc: '5000+ 动漫数据' }
      ],
      echartsUserAction: null,
      echartsQuestion: null,
      loading: false
    }
  },
  mounted () {
    this.echartsUserAction = echarts.init(document.getElementById('echarts-moth-user'), 'macarons')
    this.echartsQuestion = echarts.init(document.getElementById('echarts-moth-question'), 'macarons')
    let _this = this
    this.loading = true
    dashboardApi.index().then(re => {
      let response = re.response
      _this.newUserCount = response.newUserCount
      _this.doPlayVideoCount = response.doPlayVideoCount
      _this.newVideoCount = response.newVideoCount
      _this.hotVideoCount = response.hotVideoCount
      const list = (response.heatTopList || []).map(item => {
        return { ...item, starRate: Math.min(5, Math.max(0, (item.voteAverage || 0) / 2)) }
      })
      _this.heatTopList = list
      _this.echartsUserAction.setOption(this.lineOption('用户活跃度', '活跃度', response.mothDayText, response.mothDayUserActionValue, '#f2a7c3'))
      _this.echartsQuestion.setOption(this.lineOption('动漫月播放次数', '播放次数', response.mothDayText, response.mothDayVideoPlayValue, '#d9a7e0'))
      this.loading = false
    })
  },
  methods: {
    formatHeat (val) {
      if (val === undefined || val === null) return '-'
      return Number(val).toFixed(2)
    },
    lineOption (title, yName, label, value, color) {
      return {
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(15, 23, 42, 0.92)',
          borderColor: 'rgba(255,255,255,0.08)',
          textStyle: { color: '#fff' }
        },
        grid: {
          left: 16,
          right: 24,
          bottom: 16,
          top: 24,
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: label,
          boundaryGap: false,
          axisLine: { lineStyle: { color: '#e2e8f0' } },
          axisLabel: { color: '#94a3b8' }
        },
        yAxis: {
          type: 'value',
          name: yName,
          nameTextStyle: { color: '#94a3b8' },
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: '#f1f5f9' } },
          axisLabel: { color: '#94a3b8' }
        },
        series: [{
          data: value,
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 7,
          lineStyle: { width: 3, color: color, shadowColor: color, shadowBlur: 12, shadowOffsetY: 6 },
          itemStyle: { color: color, borderWidth: 2, borderColor: '#fff' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: color + '55' },
                { offset: 1, color: color + '00' }
              ]
            }
          }
        }]
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 22px 26px 30px;
  min-height: 100%;
}

/* ---------- Hero ---------- */
.hero {
  position: relative;
  border-radius: 24px;
  overflow: hidden;
  padding: 40px 44px;
  margin-bottom: 24px;
  background: linear-gradient(120deg, #1b1f3a 0%, #3b2a5e 45%, #1b2a4a 100%);
  background-size: 200% 200%;
  animation: anime-aurora 18s ease infinite;
  box-shadow: 0 20px 50px rgba(27, 31, 58, 0.35);
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: url('@/assets/dashboard-hero.png') center/cover no-repeat;
  opacity: 0.22;
  mix-blend-mode: screen;
}

.hero-orbs {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(50px);
    opacity: 0.5;
  }

  .o1 {
    width: 260px;
    height: 260px;
    top: -60px;
    right: 8%;
    background: radial-gradient(circle, rgba(242, 167, 195, 0.9), transparent 70%);
    animation: anime-float 8s ease-in-out infinite;
  }

  .o2 {
    width: 200px;
    height: 200px;
    bottom: -70px;
    right: 30%;
    background: radial-gradient(circle, rgba(56, 189, 248, 0.85), transparent 70%);
    animation: anime-float 11s ease-in-out infinite reverse;
  }
}

.hero-content {
  position: relative;
  z-index: 1;
  color: #fff;

  .hero-kicker {
    display: inline-block;
    font-size: 11px;
    letter-spacing: 3px;
    color: rgba(255, 255, 255, 0.65);
    margin-bottom: 14px;
  }

  h1 {
    margin: 0 0 12px;
    font-size: 30px;
    font-weight: 800;
    letter-spacing: 1px;
    background: linear-gradient(90deg, #fff 0%, #ffd6e6 45%, #bfe6ff 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  p {
    margin: 0 0 18px;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.72);
    letter-spacing: 0.5px;
  }
}

.hero-tags {
  display: flex;
  gap: 10px;

  span {
    font-size: 12px;
    padding: 5px 14px;
    border-radius: 999px;
    color: #fff;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.16);
    backdrop-filter: blur(6px);
  }
}

/* ---------- Quick actions ---------- */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

@media (max-width: 1100px) {
  .quick-actions { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 620px) {
  .quick-actions { grid-template-columns: 1fr; }
}

.qa-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
  transition: transform 0.25s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.25s, background 0.25s;

  &:hover {
    transform: translateY(-4px);
    background: #fff;
    box-shadow: 0 18px 40px rgba(15, 23, 42, 0.12);
  }

  .qa-icon {
    font-size: 22px;
    width: 42px;
    height: 42px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, rgba(242, 167, 195, 0.15), rgba(217, 167, 224, 0.15));
  }

  .qa-text {
    display: flex;
    flex-direction: column;

    b {
      font-size: 14px;
      color: #0f172a;
      font-weight: 700;
    }

    small {
      margin-top: 3px;
      font-size: 11px;
      color: #94a3b8;
    }
  }
}

/* ---------- Stat cards ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 24px;
}

@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 680px) {
  .stat-grid { grid-template-columns: 1fr; }
}

.stat-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 22px 46px rgba(15, 23, 42, 0.14);
  }
}

.stat-icon {
  flex: 0 0 auto;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.16);
}

.icon-pink { background: linear-gradient(135deg, #f2a7c3, #f5c2d4); }
.icon-blue { background: linear-gradient(135deg, #38bdf8, #0ea5e9); }
.icon-green { background: linear-gradient(135deg, #34d399, #10b981); }
.icon-purple { background: linear-gradient(135deg, #d9a7e0, #c6a3d8); }

.stat-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 6px;
  letter-spacing: 0.02em;
}

.stat-value {
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;

  &.hot-video {
    font-size: 17px;
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.stat-glow {
  position: absolute;
  right: -40px;
  top: -40px;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  filter: blur(30px);
  opacity: 0.28;
}

.glow-pink { background: #f2a7c3; }
.glow-blue { background: #38bdf8; }
.glow-green { background: #10b981; }
.glow-purple { background: #d9a7e0; }

/* ---------- Charts / cards ---------- */
.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  margin-bottom: 24px;
}

@media (max-width: 1100px) {
  .chart-grid { grid-template-columns: 1fr; }
}

.chart-card,
.heat-card {
  padding: 20px 22px 12px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
    color: #0f172a;
    letter-spacing: 0.02em;
  }
}

.badge {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 999px;
  color: #64748b;
  background: #f1f5f9;
  letter-spacing: 0.05em;
}

.badge-hot {
  color: #fff;
  background: linear-gradient(135deg, #f2a7c3, #d9a7e0);
}

.chart {
  width: 100%;
  height: 340px;
}

.anime-table {
  border-radius: 14px;

  :deep(.el-table__header th) {
    background: #f8fafc !important;
    color: #475569;
    font-weight: 700;
  }

  :deep(.el-table__row:hover > td) {
    background-color: #fff1f7 !important;
  }
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-weight: 700;
  font-size: 13px;
  color: #64748b;
  background: #f1f5f9;
}

.rank-1 { background: linear-gradient(135deg, #ffd700, #ffaa00); color: #fff; }
.rank-2 { background: linear-gradient(135deg, #c0c0c0, #a0a0a0); color: #fff; }
.rank-3 { background: linear-gradient(135deg, #cd7f32, #b87333); color: #fff; }

.rate-num {
  display: inline-block;
  margin-left: 6px;
  color: #f2a7c3;
  font-weight: 700;
}

.heat-score {
  color: #d9a7e0;
  font-weight: 800;
}
</style>
