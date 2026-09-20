<template>
  <div class="app-container">
    <div class="head fade-up">
      <h2>我的追番</h2>
      <span class="count">{{ list.length }} 部</span>
    </div>
    <div v-if="list.length" class="grid stagger">
      <div v-for="item in list" :key="item.videoId" class="card">
        <div class="cover">
          <img v-if="poster(item)" :src="poster(item)" alt="">
          <span v-else class="ph">番</span>
        </div>
        <div class="body">
          <div class="name" :title="item.videoName">{{ item.videoName }}</div>
          <div class="meta">评分 {{ item.voteAverage == null ? '-' : item.voteAverage }} · 热度 {{ item.revenue == null ? '-' : item.revenue }}</div>
          <el-button size="small" round @click="remove(item)">取消追番</el-button>
        </div>
      </div>
    </div>
    <el-empty v-else description="还没有追番，去动漫列表点「追番」吧" />
  </div>
</template>

<script>
import collectionApi from '@/api/collection'
export default {
  name: 'Collection',
  data () {
    return { list: [] }
  },
  created () {
    this.load()
  },
  methods: {
    load () {
      collectionApi.list().then(re => { this.list = re.response || [] }).catch(() => {})
    },
    poster (item) {
      const p = item.posterPath
      return p && String(p).startsWith('http') ? p : ''
    },
    remove (item) {
      collectionApi.toggle(item.videoId).then(() => this.load())
    }
  }
}
</script>

<style lang="scss" scoped>
.head { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.head h2 { margin: 0; color: #0f172a; font-size: 20px; }
.count { background: linear-gradient(135deg,#f2a7c3,#d9a7e0); color: #fff; border-radius: 999px; padding: 2px 12px; font-size: 12px; }
.grid { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 16px; }
@media (max-width: 1200px) { .grid { grid-template-columns: repeat(2, minmax(0,1fr)); } }
.card { background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 12px 30px rgba(15,23,42,.07); transition: transform .25s, box-shadow .25s; }
.card:hover { transform: translateY(-5px); box-shadow: 0 20px 44px rgba(15,23,42,.13); }
.cover { height: 180px; background: #f8eef4; display: flex; align-items: center; justify-content: center; }
.cover img { width: 100%; height: 100%; object-fit: cover; }
.ph { font-size: 40px; color: #f2a7c3; font-weight: 800; }
.body { padding: 12px 14px 16px; }
.name { font-weight: 700; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.meta { color: #94a3b8; font-size: 12px; margin: 8px 0 12px; }
</style>
