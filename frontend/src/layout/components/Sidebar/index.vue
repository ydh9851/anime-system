<template>
  <div class="has-logo">
    <logo :collapse="isCollapse" />
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="variables.menuBg"
        :text-color="variables.menuText"
        :unique-opened="false"
        :active-text-color="variables.menuActiveText"
        :collapse-transition="false"
        mode="vertical"
      >
        <!--
          分组扁平菜单：把所有多级路由拍平成一级菜单项，
          用「分组标题」做视觉分隔，避免层层折叠导致功能藏得太深；
          点击分组标题可整组展开 / 收起（保留多级菜单的交互能力）。
        -->
        <template v-for="group in menuGroups" :key="group.title || 'home'">
          <li
            v-if="group.title && !isCollapse"
            class="sidebar-group-title"
            :class="{ 'is-collapsed': isGroupCollapsed(group.title) }"
            @click="toggleGroup(group.title)"
          >
            <svg-icon v-if="group.icon" :icon-class="group.icon" class="group-icon" />
            <span class="group-label">{{ group.title }}</span>
            <svg-icon icon-class="arrow-down" class="group-arrow" />
          </li>

          <div v-show="showGroup(group.title)">
            <sidebar-item
              v-for="item in group.items"
              :key="item.path"
              :item="item.route"
              :base-path="item.path"
            />
          </div>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Logo from './Logo'
import SidebarItem from './SidebarItem'
import variables from '@/styles/variables.js'

/**
 * 分组配置：决定顶层路由的展示顺序与分组名。
 * 只写「顺序 + 分组」，菜单项标题、图标仍从路由 meta 取，避免两处维护。
 */
const GROUP_CONFIG = [
  { keys: ['/'], title: '', icon: '' },
  { keys: ['/video', '/model'], title: '数据与模型', icon: 'component' },
  { keys: ['/user', '/collection'], title: '用户与运营', icon: 'user' },
  { keys: ['/analysis'], title: '数据分析', icon: 'chart' },
  { keys: ['/recommend', '/predict'], title: '算法服务', icon: 'skill' }
]

/** 拼接父子路径并去掉多余斜杠：'/' + 'dashboard' -> '/dashboard' */
function joinPath (base, sub) {
  if (!sub) {
    return base
  }
  if (sub.startsWith('/')) {
    return sub
  }
  return (base.replace(/\/$/, '') + '/' + sub).replace(/\/{2,}/g, '/')
}

export default {
  components: { SidebarItem, Logo },
  data () {
    return {
      // 记录被收起的分组；默认为空 = 全部展开，一眼能看到所有功能入口
      collapsedGroups: []
    }
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'routes'
    ]),
    activeMenu () {
      const route = this.$route
      const { meta, path } = route
      // 详情/编辑类页面通过 meta.activeMenu 指向列表项，保证高亮跟得住
      if (meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    },
    variables () {
      return variables
    },
    isCollapse () {
      return !this.sidebar.opened
    },
    /** 把 routes 拍平成「分组 -> 菜单项」的两级结构 */
    menuGroups () {
      const pool = (this.routes || []).filter(r => !r.hidden)
      return GROUP_CONFIG.map(group => {
        const items = []
        pool.forEach(route => {
          if (!group.keys.includes(route.path)) {
            return
          }
          const children = (route.children || []).filter(c => !c.hidden)
          if (children.length) {
            children.forEach(child => {
              items.push({
                path: joinPath(route.path, child.path),
                route: child
              })
            })
          } else {
            items.push({
              path: route.redirect || route.path,
              route: route
            })
          }
        })
        return { title: group.title, icon: group.icon, items }
      }).filter(group => group.items.length)
    }
  },
  methods: {
    isGroupCollapsed (title) {
      return this.collapsedGroups.indexOf(title) > -1
    },
    showGroup (title) {
      // 无标题的分组（如「主页」）不可收起
      if (!title) {
        return true
      }
      return !this.isGroupCollapsed(title)
    },
    toggleGroup (title) {
      const i = this.collapsedGroups.indexOf(title)
      if (i > -1) {
        this.collapsedGroups.splice(i, 1)
      } else {
        this.collapsedGroups.push(title)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.sidebar-group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 20px 6px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  color: rgba(148, 163, 184, 0.65);
  white-space: nowrap;
  list-style: none;
  cursor: pointer;
  user-select: none;
  transition: color 0.2s;

  &:hover {
    color: rgba(242, 167, 195, 0.9);
  }

  .group-icon {
    width: 12px;
    height: 12px;
    fill: currentColor;
  }

  .group-label {
    flex: 1;
  }

  // 收起状态箭头翻转，点击分组即可整组展开 / 收起
  .group-arrow {
    width: 10px;
    height: 10px;
    fill: currentColor;
    opacity: 0.7;
    transition: transform 0.25s;
  }

  &.is-collapsed .group-arrow {
    transform: rotate(-90deg);
  }
}
</style>
