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
  { path: '/software-statement', component: () => import('./views/SoftwareStatement.vue') },
  { path: '/tutorial', component: () => import('./views/Tutorial.vue') }
]

const router = createRouter({
  history: createWebHistory('/app/'),
  routes
})

let sessionChecked = false
let sessionValid = false

async function hasSession() {
  try {
    // 如果已经检查过会话且会话有效，直接返回true
    if (sessionChecked && sessionValid) {
      return true
    }
    
    if (typeof window !== 'undefined' && window.SiyuBaoBackend && window.SiyuBaoBackend.isJcef) {
      const r = await window.SiyuBaoBackend.auth.me()
      const valid = !!(r && r.success && r.user && r.user.id)
      sessionChecked = true
      sessionValid = valid
      return valid
    }
    const res = await fetch('/api/auth/me')
    if (!res.ok) {
      sessionChecked = true
      sessionValid = false
      return false
    }
    const j = await res.json().catch(() => null)
    const valid = !!(j && j.success && j.user && j.user.id)
    sessionChecked = true
    sessionValid = valid
    return valid
  } catch {
    sessionChecked = true
    sessionValid = false
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
