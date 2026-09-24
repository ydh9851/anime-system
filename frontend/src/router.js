import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '@/layout'

const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index')
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    hidden: true,
    component: () => import('@/views/login/index'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        component: () => import('@/views/dashboard/index'),
        name: 'Dashboard',
        meta: { title: '主页', icon: 'home', affix: true }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    name: 'UserPage',
    meta: {
      title: '用户管理',
      icon: 'users'
    },
    children: [
      {
        path: 'user/list',
        component: () => import('@/views/user/user/list'),
        name: 'UserPageList',
        meta: { title: '用户列表', noCache: true }
      },
      {
        path: 'user/edit',
        component: () => import('@/views/user/user/edit'),
        name: 'UserEdit',
        meta: {
          title: '用户编辑',
          noCache: true,
          activeMenu: '/user/user/list'
        },
        hidden: true
      },
      {
        path: 'user/details',
        component: () => import('@/views/user/user/details'),
        name: 'UserDetails',
        meta: {
          title: '用户详情',
          noCache: true,
          activeMenu: '/user/user/list'
        },
        hidden: true
      },
      {
        path: 'admin/list',
        component: () => import('@/views/user/admin/list'),
        name: 'UserAdminPageList',
        meta: { title: '管理员列表', noCache: true }
      },
      {
        path: 'admin/edit',
        component: () => import('@/views/user/admin/edit'),
        name: 'UserAdminEdit',
        meta: {
          title: '管理员编辑',
          noCache: true,
          activeMenu: '/user/admin/list'
        },
        hidden: true
      }
    ]
  },
  {
    path: '/video',
    component: Layout,
    name: 'ExamPage',
    meta: {
      title: '动漫管理',
      icon: 'exam'
    },
    children: [
      {
        path: 'list',
        component: () => import('@/views/video/list'),
        name: 'VideoPageList',
        meta: { title: '动漫列表', noCache: true }
      },
      {
        path: 'edit',
        component: () => import('@/views/video/edit'),
        name: 'Edit',
        meta: { title: '动漫编辑', noCache: true, activeMenu: '/video/list' },
        hidden: true
      },
      {
        path: 'details',
        component: () => import('@/views/video/details'),
        name: 'Details',
        meta: { title: '动漫详情', noCache: true, activeMenu: '/video/list' },
        hidden: true
      },
      {
        path: 'play',
        component: () => import('@/views/video/play'),
        name: 'Play',
        meta: { title: '动漫播放', noCache: true, activeMenu: '/video/list' },
        hidden: true
      },
    ]
  },
  {
    path: '/collection',
    component: Layout,
    children: [
      {
        path: 'index',
        component: () => import('@/views/collection/index'),
        name: 'CollectionIndex',
        meta: { title: '我的追番', icon: 'star', noCache: true }
      }
    ]
  },
  {
    path: '/analysis',
    component: Layout,
    name: 'Analysis',
    meta: {
      title: '动漫数据分析',
      icon: 'task'
    },
    alwaysShow: true,
    children: [
      {
        path: 'list',
        component: () => import('@/views/analysis/userAttrAnalysis'),
        name: 'TaskListPage',
        meta: { title: '用户属性分析', noCache: true }
      },
      {
        path: 'edit',
        component: () => import('@/views/analysis/videoAttrAnalysis'),
        name: 'TaskEditPage',
        meta: { title: '动漫属性分析', noCache: true }
      },
      {
        path: 'eda',
        component: () => import('@/views/analysis/edaAnalysis'),
        name: 'EdaAnalysisPage',
        meta: { title: '动漫数据探索（EDA）', noCache: true }
      },
      {
        path: 'userAnalysis',
        component: () => import('@/views/analysis/userAnalysis'),
        name: 'userAnalysis',
        meta: {
          title: '用户分析',
          noCache: true,
          activeMenu: '/analysis/list'
        },
        hidden: true
      },
      {
        path: 'screen',
        component: () => import('@/views/analysis/screen'),
        name: 'Screen',
        meta: {
          title: '数据可视化分析',
          noCache: true,
          activeMenu: '/analysis/list'
        },
        hidden: true
      }
    ]
  },
  {
    path: '/recommend',
    component: Layout,
    name: 'RecommendPage',
    meta: {
      title: '动漫推荐',
      icon: 'education'
    },
    alwaysShow: true,
    children: [
      {
        path: 'UserList',
        component: () => import('@/views/recommend/index'),
        name: 'RecommendIndex',
        meta: { title: '用户属性推荐', noCache: true, mode: 'user' }
      },
      {
        path: 'subject/edit',
        component: () => import('@/views/recommend/index'),
        name: 'RecommendMovieIndex',
        meta: { title: '动漫属性推荐', noCache: true, mode: 'video' }
      }
    ]
  },
  {
    path: '/predict',
    component: Layout,
    name: 'PredictPage',
    meta: {
      title: '热度预测',
      icon: 'money'
    },
    alwaysShow: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/predict/index'),
        name: 'PredictIndex',
        meta: { title: '热度预测', noCache: true }
      }
    ]
  },

  {
    path: '/profile',
    component: Layout,
    hidden: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/profile/index'),
        name: 'Profile',
        meta: { title: '个人简介', icon: 'user', noCache: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    hidden: true,
    component: () => import('@/views/error-page/404'),
    meta: { title: '404', noCache: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes
})

export { constantRoutes, router }
