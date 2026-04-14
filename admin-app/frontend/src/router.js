import { createRouter, createWebHistory } from 'vue-router'
import { clearToken, me } from './api'

const routes = [
  { path: '/', redirect: '/users' },
  { path: '/login', component: () => import('./views/Login.vue') },
  { path: '/users', component: () => import('./views/UserManage.vue') },
  { path: '/orders', component: () => import('./views/OrderManage.vue') },
  { path: '/qr-codes', component: () => import('./views/QrCodeManage.vue') },
  { path: '/plans', component: () => import('./views/PlanManage.vue') },
  { path: '/subscriptions', component: () => import('./views/SubscriptionManage.vue') },
  { path: '/devices', component: () => import('./views/DeviceManage.vue') },
  { path: '/user-stats', component: () => import('./views/UserStats.vue') },
  { path: '/logs', component: () => import('./views/Logs.vue') },
  { path: '/settings', component: () => import('./views/Settings.vue') }
]

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes
})

router.beforeEach(async (to) => {
  if (to.path === '/login') return true
  try {
    const r = await me()
    const role = r && r.user ? r.user.role : ''
    if (role !== 'ADMIN') throw new Error('非管理员账号')
    return true
  } catch {
    clearToken()
    return { path: '/login' }
  }
})

export default router

