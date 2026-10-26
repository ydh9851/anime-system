<template>
  <div class="screen-wrap">
    <div class="screen-stage" :style="stageStyle">
      <header class="top-bar">
        <div class="top-left">
          <span class="dot" />
          <span class="sys-name">番析 AniScope</span>
          <span class="sys-sub">动漫热度态势感知大屏</span>
        </div>
        <h1 class="top-title">动 漫 数 据 态 势 感 知 中 心</h1>
        <div class="top-right">
          <span class="status" :class="online ? 'ok' : 'off'">
            <i />{{ online ? '数据链路正常' : '数据链路异常' }}
          </span>
          <span class="clock">{{ clock }}</span>
          <span class="date">{{ today }}</span>
          <el-button size="small" round class="op-btn" @click="load">刷新</el-button>
          <el-button size="small" round class="op-btn" @click="toggleFull">
            {{ full ? '退出全屏' : '全屏' }}
          </el-button>
        </div>
      </header>

      <section class="cols">
        <div class="col">
          <div class="panel">
            <h3 class="panel-title">核心指标感知</h3>
            <div class="kpi-grid">
              <div class="kpi" v-for="k in kpis" :key="k.label">
                <div class="kpi-label">{{ k.label }}</div>
                <div class="kpi-value" :style="{ color: k.color }">
                  <count-to :start-val="0" :end-val="k.value" :duration="1800" />
                  <em v-if="k.unit">{{ k.unit }}</em>
                </div>
                <div class="kpi-sub">{{ k.sub }}</div>
              </div>
            </div>
          </div>

          <div class="panel">
            <h3 class="panel-title">动画类型结构占比</h3>
            <div ref="typePie" class="chart chart-md" />
          </div>

          <div class="panel">
            <h3 class="panel-title">近 20 年作品产出分布</h3>
            <div ref="yearBar" class="chart chart-md" />
          </div>
        </div>

        <div class="col">
          <div class="panel flex-1">
            <h3 class="panel-title">
              播放行为与用户活跃度实时感知
              <span class="legend">
                <i class="lg lg-a" />播放次数
                <i class="lg lg-b" />活跃度
              </span>
            </h3>
            <div ref="trendChart" class="chart chart-xl" />
          </div>

          <div class="panel rank-panel">
            <h3 class="panel-title">动漫热度实时榜单 TOP10</h3>
            <div class="rank-list">
              <div
                v-for="(item, idx) in heatTop"
                :key="item.videoId"
                class="rank-item"
                :style="{ animationDelay: idx * 0.06 + 's' }"
              >
                <span class="rank-no" :class="'no' + (idx + 1)">{{ idx + 1 }}</span>
                <span class="rank-name" :title="item.videoName">{{ item.videoName }}</span>
                <span class="rank-bar">
                  <i :style="{ width: barWidth(item) }" />
                </span>
                <span class="rank-heat">{{ fmt(item.heatScore) }}</span>
              </div>
              <el-empty v-if="!heatTop.length" description="暂无榜单数据" :image-size="60" />
            </div>
          </div>
        </div>

        <div class="col">
          <div class="panel">
            <h3 class="panel-title">题材热度 TOP10</h3>
            <div ref="genreBar" class="chart chart-md" />
          </div>

          <div class="panel">
            <h3 class="panel-title">用户评分档位感知</h3>
            <div ref="ratingPie" class="chart chart-md" />
          </div>

          <div class="panel">
            <h3 class="panel-title">原始语言分布</h3>
            <div ref="langPie" class="chart chart-md" />
          </div>
        </div>
      </section>

      <footer class="bottom-bar">
        <span>数据源：MyAnimeList 清洗数据集（24661 部） + 行为数据表 t_video_play / t_user_event_log</span>
        <span class="auto-refresh">
          自动刷新
          <el-switch v-model="autoRefresh" size="mini" />
          <i v-if="autoRefresh" class="cd" :style="{ width: progress + '%' }" />
          <em v-if="autoRefresh">{{ remain }}s</em>
        </span>
        <span>最后更新 {{ updatedAt || '--' }}</span>
      </footer>
    </div>
  </div>
</template>
<script>
import * as echarts from 'echarts'
import CountTo from '@/components/CountTo'
import dashboardApi from '@/api/dashboard'
import analysisApi from '@/api/analysis'

const AXIS = { color: '#9fb0d0' }
const REFRESH_SEC = 30

export default {
  name: 'CommandScreen',
  components: { CountTo },
  data () {
    return {
      full: false,
      online: true,
      autoRefresh: true,
      remain: REFRESH_SEC,
      progress: 0,
      updatedAt: '',
      clock: '',
      today: '',
      heatTop: [],
      kpis: [
        { label: '动漫库总量', value: 0, unit: '部', sub: '全量导入', color: '#7dd3fc' },
        { label: '本月播放次数', value: 0, unit: '次', sub: '播放行为累计', color: '#f2a7c3' },
        { label: '本月新增用户', value: 0, unit: '人', sub: '注册转化', color: '#a78bfa' },
        { label: '用户活跃度', value: 0, unit: '次', sub: '事件日志条数', color: '#34d399' }
      ],
      charts: {},
      scale: 1,
      timer: null,
      tickTimer: null
    }
  },
  computed: {
    stageStyle () {
      const s = this.scale
      const w = window.innerWidth
      const h = window.innerHeight
      return {
        width: '1920px',
        height: '1080px',
        transform: 'scale(' + s + ')',
        transformOrigin: 'top left',
        left: Math.max(0, (w - 1920 * s) / 2) + 'px',
        top: Math.max(0, (h - 1080 * s) / 2) + 'px'
      }
    }
  },
  mounted () {
    this.calcScale()
    this.tick()
    this.tickTimer = setInterval(this.tick, 1000)
    this.load()
    this.timer = setInterval(this.tickRefresh, 1000)
    window.addEventListener('resize', this.onResize)
  },
  beforeUnmount () {
    clearInterval(this.tickTimer)
    clearInterval(this.timer)
    window.removeEventListener('resize', this.onResize)
    Object.values(this.charts).forEach(c => c && c.dispose())
    this.charts = {}
  },
  methods: {
    tick () {
      const d = new Date()
      const p = n => String(n).padStart(2, '0')
      this.clock = p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds())
      const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
      this.today = d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' 星期' + week
    },
    tickRefresh () {
      if (!this.autoRefresh) {
        return
      }
      this.remain -= 1
      this.progress = Math.round(((REFRESH_SEC - this.remain) / REFRESH_SEC) * 100)
      if (this.remain <= 0) {
        this.remain = REFRESH_SEC
        this.progress = 0
        this.load()
      }
    },
    load () {
      Promise.all([
        dashboardApi.index().then(r => r.response || {}).catch(() => {}),
        dashboardApi.distribution().then(r => r.response || {}).catch(() => {}),
        analysisApi.overview().then(r => r.response || {}).catch(() => {})
      ]).then(([idx, dist, ana]) => {
        this.online = true
        this.render(idx, dist, ana)
        this.updatedAt = new Date().toLocaleTimeString('zh-CN')
      }).catch(() => {
        this.online = false
      })
    },
    render (idx, dist, ana) {
      this.heatTop = idx.heatTopList || []
      this.kpis[0].value = idx.videoTotal || 0
      this.kpis[1].value = idx.doPlayVideoCount || 0
      this.kpis[2].value = idx.newUserCount || 0
      this.kpis[3].value = (idx.mothDayUserActionValue || []).reduce((a, b) => a + b, 0)
      this.trend(idx.mothDayText || [], idx.mothDayVideoPlayValue || [], idx.mothDayUserActionValue || [])
      this.typePie(dist.typeDistribution || [])
      this.yearBar(dist.yearDistribution || [])
      this.genreBar(dist.genreDistribution || [])
      this.ratingPie(ana.ratingDistribution || [])
      this.langPie(dist.languageDistribution || [])
    },
    init (key) {
      if (this.charts[key]) {
        this.charts[key].dispose()
      }
      this.charts[key] = echarts.init(this.$refs[key], null, { renderer: 'canvas' })
      return this.charts[key]
    },    trend (labels, play, active) {
      this.init('trendChart').setOption({
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(12,16,28,.92)', borderColor: 'rgba(255,255,255,.12)', textStyle: { color: '#e6ebf5' } },
        legend: { show: false },
        grid: { left: 46, right: 46, top: 30, bottom: 34 },
        xAxis: {
          type: 'category',
          data: labels,
          axisLine: { lineStyle: { color: 'rgba(255,255,255,.15)' } },
          axisLabel: { color: AXIS.color, fontSize: 10 }
        },
        yAxis: [
          { type: 'value', name: '播放', nameTextStyle: { color: AXIS.color }, axisLabel: { color: AXIS.color }, splitLine: { lineStyle: { color: 'rgba(255,255,255,.06)' } } },
          { type: 'value', name: '活跃', nameTextStyle: { color: AXIS.color }, axisLabel: { color: AXIS.color }, splitLine: { show: false } }
        ],
        series: [
          {
            name: '播放次数',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            data: play,
            lineStyle: { width: 3, color: '#f2a7c3', shadowColor: 'rgba(242,167,195,.6)', shadowBlur: 14 },
            itemStyle: { color: '#f2a7c3' },
            areaStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [{ offset: 0, color: 'rgba(242,167,195,.45)' }, { offset: 1, color: 'rgba(242,167,195,0)' }]
              }
            }
          },
          {
            name: '活跃度',
            type: 'bar',
            yAxisIndex: 1,
            data: active,
            barMaxWidth: 16,
            itemStyle: { color: 'rgba(125,211,252,.55)', borderRadius: [4, 4, 0, 0] }
          }
        ]
      })
    },
    typePie (rows) {
      this.init('typePie').setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 部 ({d}%)' },
        legend: { bottom: 0, textStyle: { color: AXIS.color, fontSize: 10 }, itemWidth: 9, itemHeight: 9 },
        color: ['#8b5cf6', '#f2a7c3', '#38bdf8', '#34d399', '#fbbf24', '#f87171'],
        series: [{
          type: 'pie',
          radius: ['40%', '66%'],
          center: ['50%', '44%'],
          label: { show: false },
          emphasis: { label: { show: true, color: '#fff', formatter: '{b}\n{d}%' } },
          data: rows.map(i => ({ name: i.name, value: i.value }))
        }]
      })
    },
    yearBar (rows) {
      const top = rows.slice(-20)
      this.init('yearBar').setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 40, right: 16, top: 18, bottom: 34 },
        xAxis: { type: 'category', data: top.map(i => i.name), axisLabel: { color: AXIS.color, fontSize: 9, rotate: 40 }, axisLine: { lineStyle: { color: 'rgba(255,255,255,.15)' } } },
        yAxis: { type: 'value', axisLabel: { color: AXIS.color, fontSize: 9 }, splitLine: { lineStyle: { color: 'rgba(255,255,255,.06)' } } },
        series: [{
          type: 'bar',
          data: top.map(i => i.value),
          barMaxWidth: 16,
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#8b5cf6' }, { offset: 1, color: 'rgba(139,92,246,.15)' }] }
          }
        }]
      })
    },
    genreBar (rows) {
      const top = rows.slice(0, 10).reverse()
      this.init('genreBar').setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 70, right: 26, top: 12, bottom: 18 },
        xAxis: { type: 'value', axisLabel: { color: AXIS.color, fontSize: 9 }, splitLine: { lineStyle: { color: 'rgba(255,255,255,.06)' } } },
        yAxis: { type: 'category', data: top.map(i => i.name), axisLabel: { color: AXIS.color, fontSize: 9 }, axisLine: { show: false }, axisTick: { show: false } },
        series: [{
          type: 'bar',
          data: top.map(i => i.value),
          barMaxWidth: 14,
          itemStyle: {
            borderRadius: [0, 4, 4, 0],
            color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: 'rgba(56,189,248,.35)' }, { offset: 1, color: '#38bdf8' }] }
          },
          label: { show: true, position: 'right', color: '#cbd6ea', fontSize: 10 }
        }]
      })
    },    ratingPie (rows) {
      this.init('ratingPie').setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
        legend: { bottom: 0, textStyle: { color: AXIS.color, fontSize: 10 }, itemWidth: 9, itemHeight: 9 },
        color: ['#f87171', '#fbbf24', '#a78bfa', '#38bdf8', '#34d399'],
        series: [{
          type: 'pie',
          roseType: 'radius',
          radius: ['22%', '68%'],
          center: ['50%', '44%'],
          label: { color: '#cbd6ea', fontSize: 10, formatter: '{b}' },
          labelLine: { length: 6, length2: 6 },
          data: rows.map(i => ({ name: i.name + ' 分', value: i.value }))
        }]
      })
    },
    langPie (rows) {
      const top = rows.slice(0, 6)
      this.init('langPie').setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0, textStyle: { color: AXIS.color, fontSize: 10 }, itemWidth: 9, itemHeight: 9 },
        color: ['#f2a7c3', '#8b5cf6', '#38bdf8', '#34d399', '#fbbf24', '#a78bfa'],
        series: [{
          type: 'pie',
          radius: ['36%', '64%'],
          center: ['50%', '43%'],
          label: { show: false },
          emphasis: { label: { show: true, color: '#fff', formatter: '{b}\n{d}%' } },
          data: top.map(i => ({ name: i.name, value: i.value }))
        }]
      })
    },
    barWidth (item) {
      const max = Math.max.apply(null, this.heatTop.map(i => i.heatScore || 0).concat([1]))
      return Math.max(4, Math.round(((item.heatScore || 0) / max) * 100)) + '%'
    },
    fmt (v) {
      if (v == null) return '-'
      return Number(v).toFixed(2)
    },
    onResize () {
      this.calcScale()
      Object.values(this.charts).forEach(c => c && c.resize())
    },
    /** 按 1920x1080 设计稿等比缩放并居中 */
    calcScale () {
      const s = Math.min(window.innerWidth / 1920, window.innerHeight / 1080)
      this.scale = s > 0 ? Math.min(s, 1.6) : 1
    },
    toggleFull () {
      const el = this.$el
      if (!document.fullscreenElement) {
        el.requestFullscreen && el.requestFullscreen()
        this.full = true
      } else {
        document.exitFullscreen && document.exitFullscreen()
        this.full = false
      }
    }
  }
}
</script>
<style lang="scss" scoped>
.screen-wrap {
  width: 100%;
  /* 90 = navbar(56) + tags-view(34)，与 AppMain 的 min-height 保持一致 */
  height: calc(100vh - 90px);
  min-height: 620px;
  overflow: hidden;
  background: #070a12;
  position: relative;
}
.screen-stage {
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: top left;
  display: flex;
  flex-direction: column;
  padding: 0 18px 12px;
  color: #e6ebf5;
  background:
    radial-gradient(900px 420px at 12% 0%, rgba(139, 92, 246, .22), transparent 60%),
    radial-gradient(760px 380px at 88% 0%, rgba(56, 189, 248, .18), transparent 62%),
    linear-gradient(180deg, #0b1020 0%, #070a12 100%);
}
.screen-stage::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: repeating-linear-gradient(180deg, rgba(255, 255, 255, .02) 0 1px, transparent 1px 4px);
}
.top-bar {
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, .08);
}
.top-title {
  margin: 0;
  font-size: 30px;
  letter-spacing: 6px;
  font-weight: 800;
  background: linear-gradient(90deg, #fff 10%, #7dd3fc 45%, #f2a7c3 90%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.top-left,
.top-right {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 13px;
  color: #9fb0d0;
  min-width: 330px;
}
.top-right { justify-content: flex-end; }
.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 10px #34d399;
  animation: pulse 1.6s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: .35; transform: scale(.8); }
}
.sys-name { color: #e6ebf5; font-weight: 700; letter-spacing: 1px; }
.status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
}
.status i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
.status.ok { color: #34d399; background: rgba(52, 211, 153, .12); }
.status.off { color: #f87171; background: rgba(248, 113, 113, .12); }
.clock {
  font-size: 20px;
  font-weight: 700;
  color: #7dd3fc;
  font-variant-numeric: tabular-nums;
}
.date { font-size: 12px; }
.op-btn {
  background: rgba(255, 255, 255, .08);
  border-color: rgba(255, 255, 255, .16);
  color: #e6ebf5;
}
.op-btn:hover {
  background: rgba(125, 211, 252, .2);
  border-color: rgba(125, 211, 252, .45);
  color: #fff;
}
.cols {
  flex: 1;
  display: grid;
  grid-template-columns: 480px 1fr 480px;
  gap: 14px;
  padding: 12px 0;
  min-height: 0;
}
.col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}
.panel {
  background: rgba(255, 255, 255, .04);
  border: 1px solid rgba(255, 255, 255, .08);
  border-radius: 14px;
  padding: 10px 14px 12px;
  position: relative;
  min-height: 0;
}
.panel::before {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  top: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(125, 211, 252, .55), transparent);
}
.panel-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 700;
  color: #cbd6ea;
  letter-spacing: 1px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.legend { font-size: 11px; color: #9fb0d0; font-weight: 400; }
.lg { display: inline-block; width: 10px; height: 10px; border-radius: 2px; margin: 0 4px 0 10px; }
.lg-a { background: #f2a7c3; }
.lg-b { background: rgba(125, 211, 252, .8); }
.chart { width: 100%; }
.chart-md { height: 216px; }
.chart-xl { height: 330px; }
.flex-1 { flex: 1; display: flex; flex-direction: column; }
.flex-1 .chart { flex: 1; }.kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}
.kpi {
  background: rgba(255, 255, 255, .04);
  border: 1px solid rgba(255, 255, 255, .07);
  border-radius: 10px;
  padding: 10px 12px;
}
.kpi-label { font-size: 12px; color: #9fb0d0; }
.kpi-value {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.25;
  font-variant-numeric: tabular-nums;
}
.kpi-value em { font-size: 13px; font-style: normal; color: #9fb0d0; margin-left: 4px; }
.kpi-sub { font-size: 11px; color: #6b7a99; }
.rank-panel { flex: 1; display: flex; flex-direction: column; }
.rank-list {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
}
.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  animation: rankIn .5s ease both;
}
@keyframes rankIn {
  from { opacity: 0; transform: translateX(-14px); }
  to { opacity: 1; transform: none; }
}
.rank-no {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  background: rgba(255, 255, 255, .08);
  color: #cbd6ea;
}
.rank-no.no1 { background: #f59e0b; color: #1a1205; }
.rank-no.no2 { background: #cbd5e1; color: #1a2233; }
.rank-no.no3 { background: #b45309; color: #fff; }
.rank-name {
  width: 190px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #dbe4f5;
}
.rank-bar {
  flex: 1;
  height: 7px;
  border-radius: 999px;
  background: rgba(255, 255, 255, .07);
  overflow: hidden;
}
.rank-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #38bdf8, #f2a7c3);
  transition: width .6s ease;
}
.rank-heat {
  width: 54px;
  text-align: right;
  color: #7dd3fc;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.bottom-bar {
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7a99;
  border-top: 1px solid rgba(255, 255, 255, .07);
  padding-top: 6px;
}
.auto-refresh {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.auto-refresh .cd {
  display: block;
  height: 3px;
  background: linear-gradient(90deg, #38bdf8, #f2a7c3);
  border-radius: 999px;
  transition: width 1s linear;
}
.auto-refresh em { font-style: normal; color: #9fb0d0; width: 30px; }
</style>