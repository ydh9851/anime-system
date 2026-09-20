<template>
  <section class="app-main">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :is="Component" :key="key" />
        </keep-alive>
      </transition>
    </router-view>
  </section>
</template>

<script>
export default {
  name: 'AppMain',
  computed: {
    cachedViews () {
      return this.$store.state.tagsView.cachedViews
    },
    key () {
      return this.$route.path
    }
  }
}
</script>

<style lang="scss" scoped>
  .app-main {
    /* 56 = navbar height */
    min-height: calc(100vh - 56px);
    width: 100%;
    position: relative;
    overflow: hidden;
    background:
      radial-gradient(1100px 520px at 8% -12%, rgba(242, 167, 195, 0.12), transparent 62%),
      radial-gradient(900px 480px at 100% 0%, rgba(169, 217, 232, 0.14), transparent 58%),
      #fdf6f9;
  }

  .fixed-header+.app-main {
    padding-top: 56px;
  }

  .hasTagsView {
    .app-main {
      /* 90 = navbar + tags-view = 56 + 34 */
      min-height: calc(100vh - 90px);
    }

    .fixed-header+.app-main {
      padding-top: 90px;
    }
  }
</style>

<style lang="scss">
  // fix css style bug in open el-dialog
  .el-popup-parent--hidden {
    .fixed-header {
      padding-right: 15px;
    }
  }
</style>
