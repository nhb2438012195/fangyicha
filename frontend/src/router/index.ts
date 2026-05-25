import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

/** 路由配置 */
const routes: RouteRecordRaw[] = [
  // 公开页面
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/register/RegisterView.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/',
    redirect: '/login'
  },

  // ===== 开发商路由（带侧边栏布局） =====
  {
    path: '/developer',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { role: 'ROLE_DEVELOPER' },
    children: [
      {
        path: 'dashboard',
        name: 'DeveloperDashboard',
        component: () => import('../views/developer/DashboardView.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'properties',
        name: 'DeveloperProperties',
        component: () => import('../views/developer/PropertyListView.vue'),
        meta: { title: '房产管理', icon: 'HomeFilled' }
      },
      {
        path: 'properties/create',
        name: 'DeveloperPropertyCreate',
        component: () => import('../views/developer/PropertyFormView.vue'),
        meta: { title: '添加房产', icon: 'Plus' }
      },
      {
        path: 'properties/:id/edit',
        name: 'DeveloperPropertyEdit',
        component: () => import('../views/developer/PropertyFormView.vue'),
        meta: { title: '编辑房产', icon: 'Edit' }
      },
      {
        path: 'analytics',
        name: 'DeveloperAnalytics',
        component: () => import('../views/developer/AnalyticsView.vue'),
        meta: { title: '空置率分析', icon: 'DataAnalysis' }
      },
      {
        path: 'profile',
        name: 'DeveloperProfile',
        component: () => import('../views/developer/ProfileView.vue'),
        meta: { title: '公司信息', icon: 'InfoFilled' }
      },
      {
        path: 'suggestions',
        name: 'DeveloperSuggestions',
        component: () => import('../views/developer/SuggestionsView.vue'),
        meta: { title: '客户建议', icon: 'ChatDotSquare' }
      }
    ]
  },

  // ===== 客户路由（带侧边栏布局） =====
  {
    path: '/customer',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { role: 'ROLE_CUSTOMER' },
    children: [
      {
        path: 'dashboard',
        name: 'CustomerDashboard',
        component: () => import('../views/customer/DashboardView.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'properties',
        name: 'CustomerProperties',
        component: () => import('../views/customer/PropertySearchView.vue'),
        meta: { title: '房产查询', icon: 'Search' }
      },
      {
        path: 'wizard',
        name: 'CustomerWizard',
        component: () => import('../views/customer/WizardView.vue'),
        meta: { title: '引导查询', icon: 'Guide' }
      },
      {
        path: 'developers',
        name: 'CustomerDevelopers',
        component: () => import('../views/customer/DeveloperListView.vue'),
        meta: { title: '开发商列表', icon: 'OfficeBuilding' }
      },
      {
        path: 'developers/:id',
        name: 'CustomerDeveloperDetail',
        component: () => import('../views/customer/DeveloperDetailView.vue'),
        meta: { title: '开发商详情', hidden: true }
      },
      {
        path: 'suggestions',
        name: 'CustomerSuggestions',
        component: () => import('../views/customer/SuggestionsView.vue'),
        meta: { title: '我的建议', icon: 'ChatDotSquare' }
      },
      {
        path: 'suggestions/new',
        name: 'CustomerNewSuggestion',
        component: () => import('../views/customer/NewSuggestionView.vue'),
        meta: { title: '提交建议', icon: 'Edit' }
      },
      {
        path: 'profile',
        name: 'CustomerProfile',
        component: () => import('../views/customer/ProfileView.vue'),
        meta: { title: '个人资料', icon: 'UserFilled' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/** 路由守卫：权限验证 */
router.beforeEach((to, _from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || '房易查'} - 房地产客户购房查询系统`

  const token = localStorage.getItem('token')
  const userInfoStr = localStorage.getItem('userInfo')
  let role = ''
  if (userInfoStr) {
    try {
      role = JSON.parse(userInfoStr).role || ''
    } catch {
      role = ''
    }
  }

  // 公开页面直接访问
  if (to.meta.public) {
    // 如果已登录，跳转到对应的仪表盘
    if (token && to.path === '/login') {
      if (role === 'ROLE_DEVELOPER') {
        next('/developer/dashboard')
      } else if (role === 'ROLE_CUSTOMER') {
        next('/customer/dashboard')
      } else {
        next()
      }
    } else {
      next()
    }
    return
  }

  // 需要认证的页面
  if (!token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 角色校验
  const requiredRole = to.meta.role as string
  if (requiredRole && role !== requiredRole) {
    // 角色不匹配，重定向到对应角色的仪表盘
    if (role === 'ROLE_DEVELOPER') {
      next('/developer/dashboard')
    } else if (role === 'ROLE_CUSTOMER') {
      next('/customer/dashboard')
    } else {
      next('/login')
    }
    return
  }

  next()
})

export default router
