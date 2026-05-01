import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { requiresAuth: true, title: '首页' }
      },
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/ProductList.vue'),
        meta: { requiresAuth: true, title: '理财产品' }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/Chat.vue'),
        meta: { requiresAuth: true, title: 'AI智能顾问' }
      },
      {
        path: 'asset',
        name: 'Asset',
        component: () => import('@/views/Asset.vue'),
        meta: { requiresAuth: true, title: '资产总览' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/Orders.vue'),
        meta: { requiresAuth: true, title: '订单记录' }
      },
      {
        path: 'risk',
        name: 'Risk',
        component: () => import('@/views/RiskAssessment.vue'),
        meta: { requiresAuth: true, title: '风险测评' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { requiresAuth: true, title: '个人中心' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/components/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/AdminDashboard.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '管理首页' }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/AdminUsers.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '用户管理' }
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/AdminProducts.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '产品管理' }
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/AdminOrders.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '订单管理' }
      },
      {
        path: 'assets',
        name: 'AdminAssets',
        component: () => import('@/views/admin/AdminAssets.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '资产管理' }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/admin/AdminProfile.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '个人中心' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = !!userStore.token
  
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && isLoggedIn) {
    const isAdmin = userStore.userInfo?.role === 'admin'
    next(isAdmin ? '/admin/dashboard' : '/dashboard')
  } else if (to.meta.requiresAdmin && userStore.userInfo?.role !== 'admin') {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
