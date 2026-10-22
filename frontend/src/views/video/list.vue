<template>
  <div class="app-container video-page">
    <div class="hero fade-up">
      <div class="hero-search">
        <el-input v-model="queryParam.videoName" placeholder="搜索动漫名称，支持中英文" prefix-icon="el-icon-search"
                  clearable @keyup.enter.native="applyFilter" @clear="applyFilter" class="search-input" />
        <el-button type="primary" round icon="el-icon-search" @click="applyFilter">搜索</el-button>
      </div>
      <div class="hero-meta">
        <div class="stat"><b>{{ total }}</b><span>部作品</span></div>
        <div class="stat-divider" />
        <div class="sort-group">
          <span class="sort-label">排序</span>
          <button v-for="s in sortOptions" :key="s.value" class="sort-btn"
                  :class="{ active: queryParam.orderBy === s.value }" @click="setSort(s.value)">{{ s.label }}</button>
        </div>
      </div>
    </div>

    <transition name="slide">
      <div v-if="activeChips.length" class="chip-bar fade-up">
        <span class="chip-bar-label">已选</span>
        <el-tag v-for="c in activeChips" :key="c.key" :type="c.type" closable class="active-chip" @close="removeChip(c)">
          <em>{{ c.dim }}</em>{{ c.label }}
        </el-tag>
        <el-button type="text" class="clear-btn" @click="clearAll">清空全部</el-button>
      </div>
    </transition>

    <div class="filter-panel fade-up">
      <div class="filter-head" @click="panelCollapsed = !panelCollapsed">
        <span class="filter-title"><i class="el-icon-s-operation" /> 筛选
          <span class="filter-badge">{{ activeChips.length }}</span></span>
        <span class="filter-toggle">{{ panelCollapsed ? '展开' : '收起' }}
          <i :class="panelCollapsed ? 'el-icon-arrow-down' : 'el-icon-arrow-up'" /></span>
      </div>

      <div v-show="!panelCollapsed" class="filter-body">
        <div v-for="g in filterGroups" :key="g.key" class="filter-row">
          <div class="filter-dim">{{ g.label }}</div>
          <div class="filter-opts">
            <button class="opt" :class="{ active: !queryParam[g.key] || !queryParam[g.key].length }" @click="clearDim(g.key)">全部</button>
            <button v-for="o in g.options" :key="o.value" class="opt"
                    :class="{ active: isChecked(g.key, o.value), empty: o.count === 0 }"
                    @click="toggle(g.key, o)">
              {{ o.label }}<em v-if="showCount" class="opt-count">{{ o.count }}</em>
            </button>
          </div>
        </div>

        <div class="filter-row">
          <div class="filter-dim">年份 / 评分</div>
          <div class="filter-opts year-opts">
            <el-select v-model="yearRange" multiple collapse-tags size="small" class="year-select"
                       placeholder="不限年份" @change="applyFilter">
              <el-option v-for="y in yearOptions" :key="y" :label="y + ' 年'" :value="y" />
            </el-select>
            <el-select v-model="voteMin" clearable size="small" class="vote-select" placeholder="最低评分" @change="applyFilter">
              <el-option label="不限评分" :value="0" />
              <el-option v-for="s in scoreOptions" :key="s" :label="s + ' 分以上'" :value="s" />
            </el-select>
          </div>
        </div>
      </div>
    </div>

    <div v-loading="listLoading" class="card-grid" v-if="tableData.length">
      <div class="anime-card fade-up" v-for="(row, idx) in tableData" :key="row.videoId"
           :style="{ animationDelay: (idx % 12) * 35 + 'ms' }">
        <div class="poster-wrap">
          <img v-if="validPoster(row.posterPath)" :src="row.posterPath" :alt="row.videoName" loading="lazy" class="poster">
          <div v-else class="poster-placeholder"><span class="ph-char">番</span></div>
          <span v-if="row.premiered" class="premiered-badge">{{ row.premiered }}</span>
          <span v-if="countryName(row.country)" class="country-badge" :class="'c-' + (row.country || '')">{{ countryName(row.country) }}</span>
          <span v-if="row.originalLanguage" class="lang-badge">{{ languageName(row.originalLanguage) }}</span>
          <div class="poster-overlay">
            <router-link :to="{ path: '/video/details', query: { id: row.videoId } }">
              <el-button size="mini" type="primary" round plain>详情</el-button>
            </router-link>
            <el-button size="mini" round plain @click="collect(row)">{{ row.collected ? '已追番' : '追番' }}</el-button>
          </div>
        </div>
        <div class="card-body">
          <div class="name-row">
            <span class="name" :title="row.videoName">{{ row.videoName }}</span>
            <span v-if="row.videoType" class="type-tag">{{ typeName(row.videoType) }}</span>
          </div>
          <div class="genre-tags" v-if="row.genres && row.genres.length">
            <span v-for="gname in row.genres.slice(0, 3)" :key="gname" class="genre-tag">{{ genreName(gname) }}</span>
            <span v-if="row.genres.length > 3" class="genre-tag more">+{{ row.genres.length - 3 }}</span>
          </div>
          <div class="meta-row">
            <span class="score"><b>{{ row.voteAverage == null ? '-' : Number(row.voteAverage).toFixed(1) }}</b><i class="score-star">★</i></span>
            <span class="votes">{{ fmtCount(row.voteCount) }} 人评分</span>
          </div>
          <div class="meta-row sub">
            <span v-if="row.runtime" class="chips">◷ {{ row.runtime }} 分钟</span>
            <span v-if="row.releaseDate" class="chips">📅 {{ yearOf(row.releaseDate) }}</span>
            <span v-if="row.heatScore" class="chips heat">🔥 {{ fmtHeat(row.heatScore) }}</span>
          </div>
          <div class="card-actions">
            <el-button size="mini" icon="el-icon-edit" @click="editVideo(row)">编辑</el-button>
            <el-button size="mini" icon="el-icon-video-play" @click="playVideo(row)">播放</el-button>
            <el-button size="mini" type="danger" icon="el-icon-delete" @click="deleteVideo(row)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else-if="!listLoading" :description="activeChips.length ? '没有符合条件的结果' : '暂无动漫数据'" />

    <div class="pager-wrap">
      <pagination v-show="total>0" :total="total" v-model:page="queryParam.pageIndex" v-model:limit="queryParam.pageSize"
                  @pagination="search" />
    </div>
  </div>
</template>

<script>
import Pagination from "@/components/Pagination"
import videoApi from "@/api/video"
import collectionApi from "@/api/collection"

const COUNTRY_MAP = { JP: "日本", CN: "中国", KR: "韩国", US: "欧美" }
const LANGUAGE_MAP = { ja: "日语", zh: "国语", ko: "韩语", en: "英语" }
const TYPE_MAP = {
  TV: "TV动画", Movie: "剧场版", OVA: "OVA", ONA: "网络动画", Special: "特别篇", Music: "音乐"
}
const GENRE_MAP = {
  Action: "动作", Adventure: "冒险", Comedy: "喜剧", Drama: "剧情", Fantasy: "奇幻",
  Horror: "恐怖", Mystery: "推理", Romance: "恋爱", "Sci-Fi": "科幻",
  "Slice of Life": "日常", Sports: "运动", Supernatural: "超自然", Suspense: "悬疑"
}

const DIM_META = {
  countries: { label: "地区", type: "" },
  languages: { label: "语言", type: "success" },
  types: { label: "类型", type: "warning" },
  genres: { label: "题材", type: "danger" }
}

export default {
  name: "VideoList",
  components: { Pagination },
  data () {
    return {
      queryParam: {
        videoName: "",
        countries: [],
        languages: [],
        types: [],
        genres: [],
        yearFrom: null,
        yearTo: null,
        voteMin: null,
        orderBy: "default",
        pageIndex: 1,
        pageSize: 12
      },
      yearRange: [],
      voteMin: null,
      options: { countries: [], languages: [], types: [], genres: [] },
      minYear: null,
      maxYear: null,
      showCount: true,
      panelCollapsed: false,
      listLoading: true,
      tableData: [],
      total: 0,
      sortOptions: [
        { label: "综合", value: "default" },
        { label: "热度", value: "heat" },
        { label: "评分", value: "vote" },
        { label: "最新", value: "release" }
      ],
      scoreOptions: [6, 7, 8, 9]
    }
  },

  computed: {
    filterGroups () {
      return [
        { key: "countries", label: "地区", options: this.options.countries },
        { key: "languages", label: "语言", options: this.options.languages },
        { key: "types", label: "类型", options: this.options.types },
        { key: "genres", label: "题材", options: this.options.genres }
      ]
    },
    yearOptions () {
      if (this.minYear == null || this.maxYear == null) return []
      const arr = []
      for (let y = this.maxYear; y >= this.minYear; y--) arr.push(y)
      return arr
    },
    activeChips () {
      const chips = []
      Object.keys(DIM_META).forEach(key => {
        (this.queryParam[key] || []).forEach(v => {
          const opt = this.options[key].find(o => o.value === v)
          chips.push({
            key: key + ":" + v,
            dim: DIM_META[key].label,
            label: (opt && opt.label) || v,
            type: DIM_META[key].type,
            value: v,
            dimKey: key
          })
        })
      })
      if (this.yearRange && this.yearRange.length) {
        chips.push({
          key: "year",
          dim: "年份",
          label: this.yearRange.length === 1
            ? this.yearRange[0] + " 年"
            : Math.min.apply(null, this.yearRange) + " - " + Math.max.apply(null, this.yearRange),
          type: "info",
          dimKey: "year"
        })
      }
      if (this.voteMin) {
        chips.push({ key: "vote", dim: "评分", label: this.voteMin + " 分以上", type: "info", dimKey: "vote" })
      }
      return chips
    }
  },
  created () {
    this.loadOptions()
    this.search()
  },
  methods: {
    loadOptions () {
      videoApi.getFilterOptions().then(data => {
        const o = data.response || {}
        this.options.countries = o.countries || []
        this.options.languages = o.languages || []
        this.options.types = o.types || []
        this.options.genres = o.genres || []
        this.minYear = o.minYear
        this.maxYear = o.maxYear
      }).catch(() => {})
    },
    isChecked (dim, value) {
      return (this.queryParam[dim] || []).indexOf(value) > -1
    },
    toggle (dim, opt) {
      if (opt.count === 0) {
        this.$message.warning("「" + opt.label + "」下暂无作品")
        return
      }
      const arr = this.queryParam[dim] || []
      const i = arr.indexOf(opt.value)
      if (i > -1) arr.splice(i, 1)
      else arr.push(opt.value)
      this.applyFilter()
    },
    clearDim (dim) {
      this.queryParam[dim] = []
      this.applyFilter()
    },
    removeChip (chip) {
      if (chip.dimKey === "year") this.yearRange = []
      else if (chip.dimKey === "vote") this.voteMin = null
      else {
        const arr = this.queryParam[chip.dimKey] || []
        const i = arr.indexOf(chip.value)
        if (i > -1) arr.splice(i, 1)
      }
      this.applyFilter()
    },
    clearAll () {
      this.queryParam.countries = []
      this.queryParam.languages = []
      this.queryParam.types = []
      this.queryParam.genres = []
      this.yearRange = []
      this.voteMin = null
      this.applyFilter()
    },
    setSort (v) {
      this.queryParam.orderBy = v
      this.applyFilter()
    },
    applyFilter () {
      const ys = (this.yearRange || []).slice().sort((a, b) => a - b)
      this.queryParam.yearFrom = ys.length ? ys[0] : null
      this.queryParam.yearTo = ys.length ? ys[ys.length - 1] : null
      this.queryParam.voteMin = this.voteMin || null
      this.queryParam.pageIndex = 1
      this.search()
    },
    search () {
      this.listLoading = true
      videoApi.getVideoPageList(this.queryParam).then(data => {
        const re = data.response || {}
        this.tableData = re.list || []
        this.total = re.total || 0
        this.queryParam.pageIndex = re.pageNum || this.queryParam.pageIndex
        this.listLoading = false
      }).catch(() => {
        this.listLoading = false
      })
    },

    editVideo (row) {
      this.$router.push({ path: "/video/edit", query: { id: row.videoId } })
    },
    playVideo (row) {
      this.$router.push({ path: "/video/play", query: { id: row.videoId } })
    },
    deleteVideo (row) {
      this.$confirm("确定删除「" + row.videoName + "」吗？", "提示", {
        confirmButtonText: "删除", cancelButtonText: "取消", type: "warning"
      }).then(() => {
        videoApi.deleteVideo(row.videoId).then(re => {
          if (re.code === 1) {
            this.$message.success(re.message)
            this.search()
          } else {
            this.$message.error(re.message)
          }
        }).catch(() => {})
      }).catch(() => {})
    },
    collect (row) {
      collectionApi.toggle(row.videoId).then(re => {
        row.collected = re.response === 1
        this.$message.success(row.collected ? "已加入我的追番" : "已取消追番")
      }).catch(() => {})
    },
    validPoster (p) {
      return p && String(p).startsWith("http")
    },
    countryName (c) {
      return c ? (COUNTRY_MAP[c] || c) : ""
    },
    languageName (l) {
      return l ? (LANGUAGE_MAP[l] || l) : ""
    },
    typeName (t) {
      return TYPE_MAP[t] || t || ""
    },
    genreName (g) {
      return GENRE_MAP[g] || g
    },
    yearOf (d) {
      return d ? String(d).slice(0, 4) : ""
    },
    fmtCount (v) {
      if (v == null) return "-"
      return Number(v).toLocaleString("en-US")
    },
    fmtHeat (v) {
      if (v == null) return "-"
      const n = Number(v)
      if (n >= 1e8) return (n / 1e8).toFixed(1) + " 亿"
      if (n >= 1e4) return (n / 1e4).toFixed(1) + " 万"
      return String(Math.round(n))
    }
  }
}
</script>

<style lang="scss" scoped>
.video-page {
  .hero {
    background: linear-gradient(120deg, rgba(168, 85, 247, .1), rgba(56, 189, 248, .08));
    border: 1px solid rgba(148, 163, 184, .18);
    border-radius: 16px;
    padding: 18px 20px;
    margin-bottom: 14px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 14px;
  }
  .hero-search { display: flex; gap: 10px; align-items: center; }
  .search-input { width: 320px; }
  .hero-meta { display: flex; align-items: center; gap: 16px; }
  .stat { display: flex; align-items: baseline; gap: 4px; color: #64748b; font-size: 13px;
    b { font-size: 22px; color: #a855f7; font-weight: 800; } }
  .stat-divider { width: 1px; height: 20px; background: rgba(148, 163, 184, .3); }
  .sort-group { display: flex; align-items: center; gap: 6px; }
  .sort-label { font-size: 13px; color: #94a3b8; margin-right: 2px; }
  .sort-btn {
    border: 1px solid rgba(148, 163, 184, .3);
    background: rgba(255, 255, 255, .7);
    color: #64748b; font-size: 13px; padding: 4px 14px; border-radius: 999px;
    cursor: pointer; transition: all .2s;
    &:hover { border-color: #a855f7; color: #a855f7; }
    &.active { background: linear-gradient(135deg, #a855f7, #7c3aed); color: #fff; border-color: transparent;
      box-shadow: 0 4px 12px rgba(168, 85, 247, .3); }
  }

  .chip-bar {
    display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
    background: rgba(255, 255, 255, .8); border: 1px dashed rgba(168, 85, 247, .35);
    border-radius: 12px; padding: 10px 14px; margin-bottom: 14px;
  }
  .chip-bar-label { font-size: 12px; color: #94a3b8; }
  .active-chip {
    em { font-style: normal; opacity: .7; margin-right: 4px; font-size: 11px; }
  }
  .clear-btn { font-size: 12px; color: #ef4444; }

  .filter-panel {
    background: rgba(255, 255, 255, .92);
    border: 1px solid rgba(148, 163, 184, .18);
    border-radius: 14px;
    margin-bottom: 18px;
    box-shadow: 0 6px 20px rgba(15, 23, 42, .05);
    overflow: hidden;
  }
  .filter-head {
    display: flex; align-items: center; justify-content: space-between;
    padding: 12px 16px; cursor: pointer; user-select: none;
    border-bottom: 1px solid rgba(148, 163, 184, .12);
    background: linear-gradient(90deg, rgba(168, 85, 247, .07), transparent 60%);
  }
  .filter-title { font-size: 14px; font-weight: 700; color: #334155; display: flex; align-items: center; gap: 6px; }
  .filter-badge {
    background: #a855f7; color: #fff; font-size: 11px; min-width: 18px; height: 18px;
    border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; padding: 0 5px;
  }
  .filter-toggle { font-size: 12px; color: #94a3b8; display: flex; align-items: center; gap: 4px; }
  .filter-body { padding: 6px 16px 14px; }
  .filter-row {
    display: flex; align-items: flex-start; gap: 14px;
    padding: 10px 0;
    border-bottom: 1px dashed rgba(148, 163, 184, .16);
    &:last-child { border-bottom: none; }
  }
  .filter-dim {
    flex: 0 0 78px; font-size: 13px; font-weight: 600; color: #64748b;
    padding-top: 5px; letter-spacing: 1px;
  }
  .filter-opts { flex: 1; display: flex; flex-wrap: wrap; gap: 8px; }
  .opt {
    border: 1px solid rgba(148, 163, 184, .3);
    background: rgba(248, 250, 252, .9);
    color: #475569; font-size: 13px; padding: 4px 12px; border-radius: 8px;
    cursor: pointer; transition: all .18s; display: inline-flex; align-items: center; gap: 5px;
    &:hover { border-color: #a855f7; color: #a855f7; transform: translateY(-1px); }
    &.active {
      background: linear-gradient(135deg, #a855f7, #8b5cf6); color: #fff; border-color: transparent;
      box-shadow: 0 3px 10px rgba(139, 92, 246, .32);
      .opt-count { color: rgba(255, 255, 255, .85); }
    }
    &.empty { opacity: .42; cursor: not-allowed; }
  }
  .opt-count { font-style: normal; font-size: 11px; color: #94a3b8; }
  .year-opts { align-items: center; }
  .year-select { width: 260px; }
  .vote-select { width: 140px; }
}

.slide-enter-active, .slide-leave-active { transition: all .25s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; transform: translateY(-6px); }

.card-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}
@media (max-width: 1400px) { .card-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 1000px) { .card-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) { .card-grid { grid-template-columns: repeat(1, minmax(0, 1fr)); } }

.anime-card {
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.07);
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s;
  animation: anime-fade-up 0.55s cubic-bezier(0.22, 1, 0.36, 1) both;
  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 24px 48px rgba(168, 85, 247, 0.18);
    .poster-overlay { opacity: 1; }
    .genre-tag { background: rgba(168, 85, 247, 0.14); border-color: rgba(168, 85, 247, 0.3); }
  }
}

.poster-wrap { position: relative; aspect-ratio: 3 / 4; background: linear-gradient(135deg, #f8eef4, #eef2ff); overflow: hidden; }
.poster { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .45s cubic-bezier(.22,1,.36,1); }
.anime-card:hover .poster { transform: scale(1.06); }
.poster-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
.ph-char { font-size: 64px; font-weight: 800; color: #f2a7c3; }

.premiered-badge {
  position: absolute; left: 10px; top: 10px; background: rgba(15, 23, 42, .72); color: #fff;
  font-size: 11px; padding: 3px 10px; border-radius: 999px; backdrop-filter: blur(4px);
}
.country-badge {
  position: absolute; right: 10px; top: 10px; font-size: 11px; font-weight: 700;
  padding: 3px 10px; border-radius: 999px; backdrop-filter: blur(4px);
  &.c-JP { background: rgba(255, 87, 87, .9); color: #fff; }
  &.c-CN { background: rgba(249, 115, 22, .9); color: #fff; }
  &.c-KR { background: rgba(99, 102, 241, .9); color: #fff; }
}
.lang-badge {
  position: absolute; right: 10px; top: 38px; font-size: 10px; font-weight: 600; color: #fff;
  background: rgba(56, 189, 248, .88); padding: 2px 8px; border-radius: 999px; backdrop-filter: blur(4px);
}

.poster-overlay {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; gap: 10px;
  background: rgba(15, 23, 42, .32); opacity: 0; transition: opacity .25s;
  :deep(.el-button) { background: rgba(255,255,255,.94) !important; color: #0f172a; border: none; font-weight: 600; }
}

.card-body { padding: 12px 14px 14px; }
.name-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.name { font-size: 14px; font-weight: 700; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.type-tag {
  flex: 0 0 auto; font-size: 10px; color: #a855f7; background: rgba(168,85,247,.1);
  border: 1px solid rgba(168,85,247,.25); padding: 2px 7px; border-radius: 999px;
}
.genre-tags { display: flex; flex-wrap: wrap; gap: 5px; margin-top: 8px; }
.genre-tag {
  font-size: 10px; color: #64748b; background: rgba(241,245,249,.9);
  border: 1px solid rgba(226,232,240,.9); padding: 2px 7px; border-radius: 6px; transition: all .2s;
  &.more { color: #a855f7; background: rgba(168,85,247,.08); }
}

.meta-row { display: flex; align-items: center; gap: 10px; margin-top: 8px; &.sub { gap: 6px; flex-wrap: wrap; } }
.score { display: inline-flex; align-items: baseline; gap: 3px;
  b { font-size: 16px; font-weight: 800; color: #a855f7; }
  .score-star { font-style: normal; color: #f2a7c3; font-size: 12px; }
}
.votes { font-size: 11px; color: #94a3b8; }
.chips { font-size: 11px; color: #64748b; background: #f1f5f9; padding: 2px 8px; border-radius: 999px;
  &.heat { color: #f97316; background: #fff7ed; }
}

.card-actions { display: flex; gap: 6px; margin-top: 12px;
  :deep(.el-button) { margin-left: 0; padding: 5px 9px; }
}

.pager-wrap { margin-top: 18px; display: flex; justify-content: center; }
</style>

