<template>
  <div class="app-container">
    <el-alert type="primary" :closable="false" show-icon style="margin-bottom:14px"
              title="用户属性分析：统计追番用户的属性与评分行为（数据源 t_user_video_operation 用户评分行为表 + t_user）。配套底层算法：动漫用户画像（Personal_SVD / UserKNN / SVD 等基于用户历史评分）。"/>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="c in statCards" :key="c.label">
        <div class="stat-card">
          <div class="stat-num">{{ c.value }}</div>
          <div class="stat-label">{{ c.label }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="chart-grid">
      <div class="chart-box">
        <div class="chart-title">用户评分人数分布（1-5 分）</div>
        <div ref="ratingChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">最活跃用户 TOP10（评分次数）</div>
        <div ref="activeChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">用户评分次数分段分布</div>
        <div ref="countChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">用户平均评分档位分布（宽容/苛刻程度）</div>
        <div ref="avgChart" class="chart"></div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import analysisApi from '@/api/analysis'

export default {
  name: 'UserAttrAnalysis',
  data () {
    return {
      stats: {},
      ratingDist: [],
      activeUsers: [],
      countDist: [],
      avgDist: [],
      statCards: [],
      charts: []
    }
  },
  mounted () {
    this.loadData()
    window.addEventListener('resize', this.resizeCharts)
  },
  beforeDestroy () {
    window.removeEventListener('resize', this.resizeCharts)
    this.charts.forEach(c => c.dispose())
  },
  methods: {
    loadData () {
      analysisApi.userOverview().then(re => {
        const d = re.response || {}
        this.stats = d.stats || {}
        this.ratingDist = d.ratingDistribution || []
        this.activeUsers = d.activeUsers || []
        this.countDist = d.ratingCountDistribution || []
        this.avgDist = d.userAvgRatingDistribution || []
        this.buildStatCards()
        this.$nextTick(() => {
          this.renderRating()
          this.renderActive()
          this.renderCount()
          this.renderAvg()
        })
      }).catch(() => {})
    },
    buildStatCards () {
      const s = this.stats
      const total = Number(s.rating_count || 0)
      const high = Number(s.high_count || 0)
      this.statCards = [
        { label: '评分总条数', value: this.fmt(total) },
        { label: '参与评分用户数', value: this.fmt(Number(s.user_count || 0)) },
        { label: '全体平均评分', value: Number(s.avg_rating || 0).toFixed(2) },
        { label: '高分（≥4分）占比', value: total > 0 ? (high * 100 / total).toFixed(1) + '%' : '0%' }
      ]
    },
    initChart (ref) {
      const dom = this.$refs[ref]
      if (!dom) return null
      const chart = echarts.init(dom)
      this.charts.push(chart)
      return chart
    },
    barOption (names, values, color, rotate) {
      return {
        grid: { left: 60, right: 20, top: 30, bottom: 60 },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: names, axisLabel: { rotate: rotate || 0 } },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: values, barWidth: '45%',
          itemStyle: { color: color, borderRadius: [4, 4, 0, 0] },
          label: { show: true, position: 'top' } }]
      }
    },
    renderRating () {
      const chart = this.initChart('ratingChart')
      if (!chart) return
      const names = this.ratingDist.map(i => String(i.name) + ' 分')
      const values = this.ratingDist.map(i => i.value)
      chart.setOption(this.barOption(names, values, '#5470c6'))
    },
    renderActive () {
      const chart = this.initChart('activeChart')
      if (!chart) return
      const rows = this.activeUsers.slice(0, 10)
      const names = rows.map(i => i.name).reverse()
      const values = rows.map(i => i.value).reverse()
      chart.setOption({
        grid: { left: 120, right: 40, top: 20, bottom: 30 },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: names },
        series: [{ type: 'bar', data: values, barWidth: '55%',
          itemStyle: { color: '#91cc75', borderRadius: [0, 4, 4, 0] },
          label: { show: true, position: 'right' } }]
      })
    },
    renderCount () {
      const chart = this.initChart('countChart')
      if (!chart) return
      const names = this.countDist.map(i => i.name)
      const values = this.countDist.map(i => i.value)
      chart.setOption(this.barOption(names, values, '#fac858'))
    },
    renderAvg () {
      const chart = this.initChart('avgChart')
      if (!chart) return
      const names = this.avgDist.map(i => String(i.name) + ' 分档')
      const values = this.avgDist.map(i => i.value)
      chart.setOption(this.barOption(names, values, '#ee6666'))
    },
    resizeCharts () {
      this.charts.forEach(c => c.resize())
    },
    fmt (n) {
      if (n >= 10000) return (n / 10000).toFixed(1) + ' 万'
      return String(n)
    }
  }
}
</script>

<style scoped>
.stat-row {
  margin-bottom: 8px;
}
.stat-card {
  position: relative;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  border-radius: 18px;
  padding: 20px 22px;
  margin-bottom: 16px;
  border: 1px solid rgba(15, 23, 42, 0.05);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s;
  animation: anime-fade-up 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;

  &::after {
    content: '';
    position: absolute;
    right: -30px;
    top: -30px;
    width: 100px;
    height: 100px;
    border-radius: 50%;
    background: linear-gradient(135deg, rgba(242, 167, 195, 0.18), rgba(217, 167, 224, 0.14));
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 44px rgba(15, 23, 42, 0.13);
  }
}
.stat-num {
  font-size: 26px;
  font-weight: 800;
  background: linear-gradient(135deg, #f2a7c3, #d9a7e0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}
.chart-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}
.chart-box {
  width: 49%;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 18px;
  padding: 16px;
  box-sizing: border-box;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
  margin-bottom: 16px;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s;
  animation: anime-fade-up 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 44px rgba(15, 23, 42, 0.12);
  }
}
.chart-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 10px;
  padding-left: 10px;
  border-left: 3px solid #f2a7c3;
}
.chart {
  width: 100%;
  height: 320px;
}
</style>
