<template>
  <div class="app-container play-page">
    <el-card class="play-card fade-up" v-loading="formLoading">
      <template #header>
        <div class="play-head">
          <div>
            <h2 class="play-title">{{ form.videoName || '动漫播放' }}</h2>
            <p class="play-sub">番析 AniScope · 动漫在线播放</p>
          </div>
          <el-button round @click="$router.back()">返回</el-button>
        </div>
      </template>

      <div v-if="form.videoUrl" class="player-wrap">
        <video class="player" controls autoplay :src="form.videoUrl" />
      </div>
      <el-empty v-else description="该动漫暂无播放地址" />
    </el-card>
  </div>
</template>

<script>
import videoApi from '@/api/video'
export default {
  data () {
    return {
      form: {
        videoId: null,
        userName: '',
        videoName: '',
        videoCategory: null,
        videoUrl: '',
        videoTagList: []
      },
      formLoading: false
    }
  },
  created () {
    let id = this.$route.query.id
    let _this = this
    if (id && parseInt(id) !== 0) {
      _this.formLoading = true
      videoApi.selectVideo(id).then(re => {
        _this.form = re.response
        _this.formLoading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.play-page {
  max-width: 1100px;
  margin: 0 auto;
}

.play-card {
  border-radius: 20px;
}

.play-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.play-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.play-sub {
  margin: 6px 0 0;
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 0.5px;
}

.player-wrap {
  border-radius: 16px;
  overflow: hidden;
  background: #0b0f1a;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
}

.player {
  display: block;
  width: 100%;
  max-height: 70vh;
  background: #000;
}
</style>
