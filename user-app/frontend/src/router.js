import { createRouter, createWebHistory } from 'vue-router'
import Login from './views/Login.vue'
import ChatPreview from './views/ChatPreview.vue'
import CardMessage from './views/CardMessage.vue'
import RouteManage from './views/RouteManage.vue'

const routes = [
  { path: '/', redirect: '/chat-preview' },
  { path: '/login', component: Login },
  { path: '/chat-preview', component: ChatPreview },
  { path: '/membership', component: () => import('./views/Membership.vue') },
  { path: '/settings', component: () => import('./views/Settings.vue') },
  { path: '/support', component: () => import('./views/Support.vue') },
  { path: '/history', component: CardMessage },
  { path: '/card-message', redirect: '/history' },
  { path: '/route', component: RouteManage },
  { path: '/software-statement', component: () => import('./views/SoftwareStatement.vue') }
]

const router = createRouter({
  history: createWebHistory('/app/'),
  routes
})

async function hasSession() {
  try {
    if (typeof window !== 'undefined' && window.SiyuBaoBackend && window.SiyuBaoBackend.isJcef) {
      const r = await window.SiyuBaoBackend.auth.me()
      return !!(r && r.success && r.user && r.user.id)
    }
    const res = await fetch('/api/auth/me')
    if (!res.ok) return false
    const j = await res.json().catch(() => null)
    return !!(j && j.success && j.user && j.user.id)
  } catch {
    return false
  }
}

router.beforeEach(async (to) => {
  if (to.path === '/login') return true
  const ok = await hasSession()
  if (!ok) return { path: '/login' }
  return true
})

export default router
