import { debounce } from '@/utils'

export default {
  data () {
    return {
      $_sidebarElm: null
    }
  },
  mounted () {
    this.$_initResizeEvent()
    this.$_initSidebarResizeEvent()
  },
  beforeUnmount () {
    this.$_destroyResizeEvent()
    this.$_destroySidebarResizeEvent()
  },
  // to fixed bug when cached by keep-alive
  // https://github.com/PanJiaChen/vue-element-admin/issues/2116
  activated () {
    this.$_initResizeEvent()
    this.$_initSidebarResizeEvent()
  },
  deactivated () {
    this.$_destroyResizeEvent()
    this.$_destroySidebarResizeEvent()
  },
  methods: {
    // use $_ for mixins properties
    // https://vuejs.org/v2/style-guide/index.html#Private-property-names-essential
    $_resizeHandler () {
      return debounce(() => {
        // 遍历所有图表实例，新增图表只需在 data 里注册即可，无需改这里
        ;['echartsUserAction', 'echartsQuestion', 'echartsYearBar', 'echartsTypePie'].forEach(key => {
          if (this[key]) {
            this[key].resize()
          }
        })
      }, 100)()
    },
    $_initResizeEvent () {
      window.addEventListener('resize', this.$_resizeHandler)
    },
    $_destroyResizeEvent () {
      window.removeEventListener('resize', this.$_resizeHandler)
    },
    $_sidebarResizeHandler (e) {
      if (e.propertyName === 'width') {
        this.$_resizeHandler()
      }
    },
    $_initSidebarResizeEvent () {
      this.$_sidebarElm = document.getElementsByClassName('sidebar-container')[0]
      this.$_sidebarElm && this.$_sidebarElm.addEventListener('transitionend', this.$_sidebarResizeHandler)
    },
    $_destroySidebarResizeEvent () {
      this.$_sidebarElm && this.$_sidebarElm.removeEventListener('transitionend', this.$_sidebarResizeHandler)
    }
  }
}
