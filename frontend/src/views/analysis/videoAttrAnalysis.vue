<template>
  <div class="app-container" v-loading="loading">
    <el-alert type="success" :closable="false" show-icon style="margin-bottom:14px"
              title="动漫属性分析：统计动漫内容属性（数据源 t_video_info，覆盖全量 24661 部动漫作品，含国家/制作公司/原作来源/动画类型维度）。配套底层算法：LightGBM/RandomForest 等热度预测的特征取自这些动漫属性（语言、集数、热度、时长等）。"/>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="4" v-for="c in statCards" :key="c.label">
        <div class="stat-card">
          <div class="stat-num">{{ c.value }}</div>
          <div class="stat-label">{{ c.label }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="chart-grid">
      <div class="chart-box">
        <div class="chart-title">制作国家/地区分布</div>
        <div ref="countryChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">制作公司 TOP10</div>
        <div ref="studioChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">原作来源分布 TOP12</div>
        <div ref="sourceChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">动画类型分布</div>
        <div ref="typeChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">动漫类型分布（标签 TOP12）</div>
        <div ref="genreChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">发行年份分布</div>
        <div ref="yearChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">集数 - 热度散点关系</div>
        <div ref="budgetChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">动漫时长分段分布</div>
        <div ref="runtimeChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">主要语言分布 TOP10</div>
        <div ref="langChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">热度 TOP10 动漫</div>
        <el-table :data="topRevenue" size="small" height="340" border style="width:100%">
          <el-table-column prop="videoName" label="片名" min-width="150" show-overflow-tooltip/>
          <el-table-column label="热度($)" width="110">
            <template v-slot="{ row }">{{ fmtUsd(row.revenue) }}</template>
          </el-table-column>
          <el-table-column label="集数" width="100">
            <template v-slot="{ row }">{{ fmtUsd(row.budget) }}</template>
          </el-table-column>
          <el-table-column prop="voteAverage" label="评分" width="70"/>
          <el-table-column prop="runtime" label="时长" width="70"/>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import analysisApi from '@/api/analysis'

const GENRE_MAP = {
  Action: '动作', Adventure: '冒险', Comedy: '喜剧', Drama: '剧情', Fantasy: '奇幻',
  Horror: '恐怖', Mystery: '悬疑', Romance: '恋爱', 'Sci-Fi': '科幻', 'Slice of Life': '日常',
  Sports: '运动', Supernatural: '超自然', Thriller: '惊悚', Music: '音乐', Mecha: '机甲',
  School: '校园', Shounen: '少年', Shoujo: '少女', Seinen: '青年', Josei: '女性',
  Ecchi: '福利', Hentai: '成人', Game: '游戏', Psychological: '心理', Police: '警察',
  Military: '军事', Space: '太空', Vampire: '吸血鬼', Demons: '恶魔', Historical: '历史',
  Isekai: '异世界', 'Martial Arts': '武术', Magic: '魔法', 'Super Power': '超能力',
  'Award Winning': '获奖作品', 'Avant Garde': '先锋', Gourmet: '美食', Kids: '儿童',
  Parody: '恶搞', Samurai: '武士', 'Shoujo Ai': '百合', 'Shounen Ai': '耽美',
  'Time Travel': '穿越', 'Video Game': '电子游戏', 'Boys Love': '耽美', 'Girls Love': '百合',
  'Mahou Shoujo': '魔法少女', 'Idols (Female)': '女性偶像', 'Idols (Male)': '男性偶像',
  'Performing Arts': '表演艺术', 'Love Polygon': '多角恋', Detective: '侦探',
  Mythology: '神话', 'Organized Crime': '黑帮', 'Otaku Culture': '宅文化',
  Reincarnation: '转生', Survival: '生存', 'Team Sports': '团队运动',
  'Strategy Game': '策略游戏', 'Adult Cast': '成人向', Anthropomorphic: '拟人',
  CGDCT: '可爱日常', Combat: '战斗', 'Combat Sports': '格斗运动', Educational: '教育',
  'Gag Humor': '搞笑', 'High Stakes Game': '高风险游戏', Idol: '偶像', Iyashikei: '治愈',
  Pets: '宠物', Racing: '赛车', Revenge: '复仇', 'Reverse Harem': '逆后宫',
  Showbiz: '演艺圈', Suspense: '悬疑', Workplace: '职场', 'Visual Arts': '视觉艺术',
  'Childcare': '育儿', 'Crossdressing': '女装', Delinquents: '不良少年'
}

const LANG_MAP = {
  en: '英语', zh: '中文', ja: '日语', fr: '法语', de: '德语', es: '西班牙语',
  it: '意大利语', ko: '韩语', hi: '印地语', ru: '俄语', pt: '葡萄牙语',
  sv: '瑞典语', da: '丹麦语', ar: '阿拉伯语', nl: '荷兰语', no: '挪威语', pl: '波兰语'
}

const COUNTRY_MAP = { JP: '日本', CN: '中国', KR: '韩国' }

const TYPE_MAP = {
  TV: 'TV动画', Movie: '剧场版', OVA: 'OVA', ONA: '网络动画', Special: '特别篇', Music: '音乐'
}

const SOURCE_MAP = {
  Original: '原创', Manga: '漫画', Novel: '小说', 'Light novel': '轻小说',
  'Web manga': '网络漫画', Game: '游戏', 'Visual novel': '视觉小说',
  Music: '音乐', 'Picture book': '绘本', Other: '其他'
}

const PALETTE = ['#a78bfa', '#f2a7c3', '#60a5fa', '#34d399', '#fbbf24', '#f87171',
  '#22d3ee', '#a3e635', '#fb923c', '#c084fc', '#f472b6', '#4ade80']

export default {
  name: 'VideoAttrAnalysis',
  data () {
    return {
      stats: {},
      genreDist: [],
      yearDist: [],
      budgetRevenue: [],
      runtimeDist: [],
      langDist: [],
      topRevenue: [],
      countryDist: [],
      studioTop: [],
      sourceDist: [],
      typeDist: [],
      statCards: [],
      charts: [],
      loading: true
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
      this.loading = true
      analysisApi.videoOverview().then(re => {
        const d = re.response || {}
        this.stats = d.stats || {}
        this.genreDist = d.genreDistribution || []
        this.yearDist = d.yearDistribution || []
        this.budgetRevenue = d.budgetRevenue || []
        this.runtimeDist = d.runtimeDistribution || []
        this.langDist = d.languageDistribution || []
        this.topRevenue = d.topRevenue || []
        this.countryDist = d.countryDistribution || []
        this.studioTop = d.studioTop || []
        this.sourceDist = d.sourceDistribution || []
        this.typeDist = d.typeDistribution || []
        this.buildStatCards()
        this.$nextTick(() => {
          this.renderCountry()
          this.renderStudio()
          this.renderSource()
          this.renderType()
          this.renderGenre()
          this.renderYear()
          this.renderBudget()
          this.renderRuntime()
          this.renderLang()
        })
        this.loading = false
      }).catch(() => {
        // 错误提示由响应拦截器统一弹出，这里保证失败后不会停在骨架屏
        this.loading = false
      })
    },
    buildStatCards () {
      const s = this.stats
      this.statCards = [
        { label: '动漫总数', value: this.fmt(Number(s.video_count || 0)) },
        { label: '平均集数', value: this.fmtUsd(Number(s.avg_budget || 0)) },
        { label: '平均热度(成员数)', value: this.fmtUsd(Number(s.avg_revenue || 0)) },
        { label: '平均评分', value: Number(s.avg_vote || 0).toFixed(1) },
        { label: '平均单集时长(分钟)', value: this.fmt(Number(s.avg_runtime || 0)) },
        { label: '平均人气', value: this.fmt(Number(s.avg_popularity || 0)) }
      ]
    },
    initChart (ref) {
      const dom = this.$refs[ref]
      if (!dom) return null
      const chart = echarts.init(dom)
      this.charts.push(chart)
      return chart
    },
    renderCountry () {
      const chart = this.initChart('countryChart')
      if (!chart) return
      const data = this.countryDist.map((i, idx) => ({
        name: COUNTRY_MAP[i.name] || i.name,
        value: i.value,
        itemStyle: { color: PALETTE[idx % PALETTE.length] }
      }))
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 部 ({d}%)' },
        legend: { type: 'scroll', bottom: 0, textStyle: { fontSize: 11 } },
        series: [{
          name: '国家/地区', type: 'pie', radius: ['30%', '62%'], center: ['50%', '45%'],
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { fontSize: 11, formatter: '{b}\n{c}部' },
          data
        }]
      })
    },
    renderStudio () {
      const chart = this.initChart('studioChart')
      if (!chart) return
      const rows = this.studioTop.slice(0, 10)
      const names = rows.map(i => i.name)
      const values = rows.map(i => i.value)
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 110, right: 40, top: 16, bottom: 36 },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: names.reverse(), axisLabel: { fontSize: 11 } },
        series: [{
          type: 'bar', data: values.reverse(), barWidth: '58%',
          itemStyle: { color: '#a78bfa', borderRadius: [0, 6, 6, 0] },
          label: { show: true, position: 'right', fontSize: 10 }
        }]
      })
    },
    renderSource () {
      const chart = this.initChart('sourceChart')
      if (!chart) return
      const rows = this.sourceDist.slice(0, 12)
      const names = rows.map(i => SOURCE_MAP[i.name] || i.name)
      const values = rows.map(i => i.value)
      chart.setOption({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 90, right: 40, top: 16, bottom: 36 },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: names.reverse(), axisLabel: { fontSize: 11 } },
        series: [{
          type: 'bar', data: values.reverse(), barWidth: '58%',
          itemStyle: { color: '#38bdf8', borderRadius: [0, 6, 6, 0] },
          label: { show: true, position: 'right', fontSize: 10 }
        }]
      })
    },
    renderType () {
      const chart = this.initChart('typeChart')
      if (!chart) return
      const rows = this.typeDist.slice(0, 6)
      const data = rows.map((i, idx) => ({
        name: TYPE_MAP[i.name] || i.name,
        value: i.value,
        itemStyle: { color: PALETTE[idx % PALETTE.length] }
      }))
      chart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} 部 ({d}%)' },
        legend: { type: 'scroll', bottom: 0, textStyle: { fontSize: 11 } },
        series: [{
          name: '动画类型', type: 'pie', radius: ['30%', '62%'], center: ['50%', '45%'],
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { fontSize: 11, formatter: '{b}\n{c}部' },
          data
        }]
      })
    },
    renderGenre () {
      const chart = this.initChart('genreChart')
      if (!chart) return
      const rows = this.genreDist.slice(0, 12)
      const names = rows.map(i => GENRE_MAP[i.name] || i.name)
      const values = rows.map(i => i.value)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { type: 'scroll', bottom: 0, textStyle: { fontSize: 10 } },
        series: [{
          type: 'pie', radius: ['25%', '68%'], center: ['50%', '45%'],
          itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 1 },
          label: { fontSize: 10 },
          data: names.map((n, idx) => ({ name: n, value: values[idx] }))
        }]
      })
    },
    renderYear () {
      const chart = this.initChart('yearChart')
      if (!chart) return
      chart.setOption({
        grid: { left: 50, right: 20, top: 30, bottom: 70 },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: this.yearDist.map(i => i.name), axisLabel: { rotate: 45, fontSize: 10 } },
        yAxis: { type: 'value' },
        dataZoom: [{ type: 'inside' }],
        series: [{ type: 'bar', data: this.yearDist.map(i => i.value),
          itemStyle: { color: '#5470c6' }, barMaxWidth: 10 }]
      })
    },
    renderBudget () {
      const chart = this.initChart('budgetChart')
      if (!chart) return
      const m = 1000000
      const data = this.budgetRevenue.map(i => [Number(i.budget) / m, Number(i.revenue) / m])
      chart.setOption({
        tooltip: {
          trigger: 'item',
          formatter: p => {
            const v = p.value
            return '集数: ' + (v[0] * m / 100000000).toFixed(1) + ' 亿<br/>热度: ' + (v[1] * m / 100000000).toFixed(1) + ' 亿'
          }
        },
        grid: { left: 60, right: 30, top: 30, bottom: 50 },
        xAxis: { type: 'value', name: '集数' },
        yAxis: { type: 'value', name: '热度' },
        series: [{
          type: 'scatter', symbolSize: 5, data: data,
          itemStyle: { color: 'rgba(84,112,198,0.6)' },
          markLine: { silent: true, lineStyle: { type: 'dashed', color: '#ccc' },
            label: { show: false },
            data: [{ yAxis: 0 }] }
        }]
      })
    },
    renderRuntime () {
      const chart = this.initChart('runtimeChart')
      if (!chart) return
      const names = this.runtimeDist.map(i => i.name)
      const values = this.runtimeDist.map(i => i.value)
      chart.setOption({
        grid: { left: 60, right: 20, top: 30, bottom: 80 },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: names, axisLabel: { fontSize: 11, rotate: 25 } },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: values, barWidth: '45%',
          itemStyle: { color: '#91cc75', borderRadius: [4, 4, 0, 0] },
          label: { show: true, position: 'top' } }]
      })
    },
    renderLang () {
      const chart = this.initChart('langChart')
      if (!chart) return
      const rows = this.langDist.slice(0, 10)
      const names = rows.map(i => LANG_MAP[i.name] || i.name)
      const values = rows.map(i => i.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 90, right: 40, top: 20, bottom: 40 },
        xAxis: { type: 'value' },
        yAxis: { type: 'category', data: names.reverse() },
        series: [{ type: 'bar', data: values.reverse(), barWidth: '55%',
          itemStyle: { color: '#fac858', borderRadius: [0, 4, 4, 0] },
          label: { show: true, position: 'right' } }]
      })
    },
    resizeCharts () {
      this.charts.forEach(c => c.resize())
    },
    fmt (n) {
      if (n >= 10000) return (n / 10000).toFixed(1) + ' 万'
      return String(n)
    },
    fmtUsd (n) {
      if (!n) return '-'
      if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
      if (n >= 10000) return (n / 10000).toFixed(0) + '万'
      return String(n)
    }
  }
}
</script>

<style scoped>
.stat-card {
  position: relative;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  border-radius: 18px;
  padding: 16px 18px;
  margin-bottom: 16px;
  border: 1px solid rgba(15, 23, 42, 0.05);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s;
  animation: anime-fade-up 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;

  &::after {
    content: '';
    position: absolute;
    right: -28px;
    top: -28px;
    width: 90px;
    height: 90px;
    border-radius: 50%;
    background: linear-gradient(135deg, rgba(56, 189, 248, 0.18), rgba(217, 167, 224, 0.14));
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 20px 44px rgba(15, 23, 42, 0.13);
  }
}
.stat-num {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  white-space: nowrap;
  position: relative;
  z-index: 1;
}
.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
  position: relative;
  z-index: 1;
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
  border-left: 3px solid #38bdf8;
}
.chart {
  width: 100%;
  height: 330px;
}
</style>
