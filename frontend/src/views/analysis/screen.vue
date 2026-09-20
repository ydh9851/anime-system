<template>
  <div class="screen-board">
    <div class="screen-head">
      <h1>番析 AniScope · 动漫数据可视化大屏</h1>
      <div class="ops">
        <el-button size="small" round @click="load">刷新数据</el-button>
        <el-button size="small" round @click="toggleFull">{{ full ? '退出全屏' : '全屏投屏' }}</el-button>
      </div>
    </div>
    <div class="screen-grid">
      <div class="box"><h3>动漫题材分布 TOP10</h3><div class="chart" ref="genreChart" /></div>
      <div class="box"><h3>上映年份分布</h3><div class="chart" ref="yearChart" /></div>
      <div class="box"><h3>集数 - 热度散点</h3><div class="chart" ref="budgetChart" /></div>
      <div class="box"><h3>用户评分分布</h3><div class="chart" ref="ratingChart" /></div>
      <div class="box wide"><h3>活跃用户 TOP</h3><div class="chart" ref="activeChart" /></div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import analysisApi from '@/api/analysis'
const AXIS = { color: '#9fb0d0' }
export default {
  name: 'AnalysisScreen',
  data () {
    return { full: false, charts: [] }
  },
  mounted () {
    this.load()
    window.addEventListener('resize', this.resize)
  },
  beforeUnmount () {
    window.removeEventListener('resize', this.resize)
    this.charts.forEach(c => c.dispose())
  },
  methods: {
    load () {
      analysisApi.overview().then(re => {
        const d = re.response || {}
        this.$nextTick(() => {
          this.charts.forEach(c => c.dispose())
          this.charts = []
          this.pie('genreChart', '动漫题材分布', d.genreDistribution || [])
          this.bar('yearChart', '上映年份', d.yearDistribution || [])
          this.scatter('budgetChart', d.budgetRevenue || [])
          this.bar('ratingChart', '评分', d.ratingDistribution || [])
          this.bar('activeChart', '评分次数', d.activeUsers || [])
        })
      }).catch(() => {})
    },
    init (ref) {
      const c = echarts.init(this.$refs[ref], null, { renderer: 'canvas' })
      this.charts.push(c)
      return c
    },
    pie (ref, name, rows) {
      this.init(ref).setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0, textStyle: { color: AXIS.color, fontSize: 10 } },
        series: [{ type: 'pie', radius: ['35%', '65%'], center: ['50%', '45%'], data: rows.map(i => ({ name: i.name, value: i.value })), label: { color: '#cbd6ea' } }]
      })
    },
    bar (ref, name, rows) {
      this.init(ref).setOption({
        grid: { left: 40, right: 20, top: 20, bottom: 50 },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: rows.map(i => i.name), axisLabel: { color: AXIS.color, rotate: 35, fontSize: 10 } },
        yAxis: { type: 'value', axisLabel: { color: AXIS.color }, splitLine: { lineStyle: { color: 'rgba(255,255,255,.08)' } } },
        series: [{ type: 'bar', data: rows.map(i => i.value), itemStyle: { color: '#f2a7c3', borderRadius: [6, 6, 0, 0] } }]
      })
    },
    scatter (ref, rows) {
      const data = rows.filter(i => i.budget != null && i.revenue != null).map(i => [i.budget, i.revenue])
      this.init(ref).setOption({
        grid: { left: 50, right: 20, top: 20, bottom: 40 },
        tooltip: { trigger: 'item' },
        xAxis: { type: 'value', name: '集数', axisLabel: { color: AXIS.color }, splitLine: { lineStyle: { color: 'rgba(255,255,255,.08)' } } },
        yAxis: { type: 'value', name: '热度', axisLabel: { color: AXIS.color }, splitLine: { lineStyle: { color: 'rgba(255,255,255,.08)' } } },
        series: [{ type: 'scatter', symbolSize: 6, data, itemStyle: { color: 'rgba(242,167,195,.7)' } }]
      })
    },
    resize () { this.charts.forEach(c => c.resize()) },
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
.screen-board { min-height: 100%; padding: 20px 24px; background: radial-gradient(1200px 600px at 20% 0%, #2a2140, #12141c 60%); color: #e6ebf5; }
.screen-board:fullscreen { padding: 24px 32px; }
.screen-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
.screen-head h1 { margin: 0; font-size: 22px; background: linear-gradient(90deg,#fff,#f2a7c3,#d9a7e0); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.screen-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.box { background: rgba(255,255,255,.05); border: 1px solid rgba(255,255,255,.08); border-radius: 16px; padding: 12px 14px; }
.box.wide { grid-column: span 2; }
.box h3 { margin: 0 0 8px; font-size: 13px; color: #cbd6ea; font-weight: 600; }
.chart { width: 100%; height: 300px; }
.box.wide .chart { height: 260px; }
</style>
