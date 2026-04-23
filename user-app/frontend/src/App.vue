<template>
  <RouterView v-if="isLogin" />

  <div v-else class="min-h-screen bg-gray-50 flex flex-col">
    <header class="bg-white border-b border-gray-200">
      <div class="px-4 h-14 flex items-center justify-between">
        <div class="flex items-center space-x-2">
          <svg width="28" height="28" viewBox="0 0 256 256">
            <defs>
              <linearGradient id="sxjw-grad-head" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stop-color="#124baa" />
                <stop offset="0.55" stop-color="#00c8ff" />
                <stop offset="1" stop-color="#0d235a" />
              </linearGradient>
            </defs>
            <rect x="10" y="10" width="236" height="236" rx="48" fill="#070a12" />
            <path
              d="M60 64 L112 210 L136 152 L160 210 L212 64"
              fill="none"
              stroke="url(#sxjw-grad-head)"
              stroke-width="18"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <path
              d="M72 90 L204 90 L112 166 L204 166"
              fill="none"
              stroke="url(#sxjw-grad-head)"
              stroke-width="18"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <rect
              x="10"
              y="10"
              width="236"
              height="236"
              rx="48"
              fill="none"
              stroke="rgba(148,163,184,.35)"
              stroke-width="6"
            />
          </svg>
          <div class="font-semibold text-gray-900">仿聊大师</div>
        </div>

        <div class="text-sm text-gray-700 truncate max-w-[80vw]">
          当前账号：{{ me.user?.username || '未登录' }}（会员：{{ me.plan?.name || '-' }} / 到期：{{ formatExpireTime(me.subscription?.endTime) }} / 今日：{{ me.todayUsed ?? '-' }}）
        </div>

        <div class="flex items-center gap-3">
          <button 
            class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm text-gray-800 relative"
            @click="openAnnouncementList"
          >
            <span>🔔</span>
            <span v-if="hasNewAnnouncement" class="absolute -top-1 -right-1 w-2.5 h-2.5 bg-red-500 rounded-full"></span>
          </button>
          <button class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm text-gray-800" @click="logout">
            🚪退出登录
          </button>
        </div>
      </div>
    </header>

    <div class="flex-1 flex min-h-0">
      <aside class="w-56 bg-slate-900 border-r border-slate-800">
        <nav class="p-3 space-y-1">
          <button
            v-for="item in menu"
            :key="item.key"
            class="w-full text-left px-3 py-2 rounded text-sm"
            :class="
              isActive(item)
                ? 'bg-blue-600 text-white font-semibold'
                : 'text-slate-200 hover:bg-slate-800 hover:text-white'
            "
            @click="onMenuClick(item)"
          >
            {{ item.label }}
          </button>
        </nav>
      </aside>

      <main class="flex-1 min-w-0 p-4 overflow-auto">
        <RouterView />
      </main>
    </div>
  </div>

  <div v-if="kickDialogOpen" class="fixed inset-0 z-[60] bg-black/50 flex items-center justify-center p-4">
    <div class="bg-white rounded-lg shadow-xl w-full max-w-md border border-gray-200">
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-semibold text-gray-900">账号下线提示</div>
        <button class="px-2 py-1 rounded bg-gray-100 hover:bg-gray-200 text-sm" @click="confirmKick">关闭</button>
      </div>
      <div class="p-4 text-sm text-gray-700 space-y-2">
        <div>{{ kickDialogMessage }}</div>
        <div class="text-xs text-gray-500">可能原因：账号在另一台设备登录，或会话过期。</div>
      </div>
      <div class="px-4 pb-4 flex justify-end">
        <button class="px-3 py-2 rounded bg-blue-600 hover:bg-blue-700 text-white text-sm" @click="confirmKick">重新登录</button>
      </div>
    </div>
  </div>

  <div v-if="agreementDialogOpen" class="fixed inset-0 z-[70] bg-black/50 flex items-center justify-center p-4">
    <div class="bg-white rounded-lg shadow-xl w-full max-w-lg border border-gray-200">
      <div class="px-4 py-3 border-b border-gray-200">
        <div class="font-semibold text-gray-900 text-lg">用户协议与隐私政策</div>
      </div>
      <div class="p-4 text-sm text-gray-700 space-y-3 max-h-96 overflow-y-auto">
        <p>1. 本软件为模拟聊天界面截图工具，仅限娱乐、演示、自媒体创作使用，严禁用于伪造证据、诈骗、造谣、诽谤等违法用途，违者自行承担法律责任。</p>
        <p>2. 本软件仅在本地运行，不会上传您的任何截图、头像、聊天内容至服务器。</p>
        <p>3. 本软件的高级功能解锁为虚拟服务，激活后不予退款。</p>
        <p>4. 会员权益仅限个人使用，禁止倒卖、共享，违者封号。</p>
      </div>
      <div class="px-4 py-3 border-t border-gray-200 flex space-x-3">
        <button 
          class="flex-1 px-4 py-2 rounded border border-gray-300 text-gray-700 hover:bg-gray-50 text-sm font-medium"
          @click="rejectAgreement"
        >
          拒绝
        </button>
        <button 
          class="flex-1 px-4 py-2 rounded bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium"
          @click="acceptAgreement"
        >
          同意
        </button>
      </div>
    </div>
  </div>

  <div v-if="forcePopupAnnouncement" class="fixed inset-0 z-[80] bg-black/60 flex items-center justify-center p-4">
    <div class="bg-white rounded-lg shadow-xl w-full max-w-md border border-gray-200">
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between bg-red-50">
        <div class="font-semibold text-red-900 text-lg">重要公告</div>
      </div>
      <div class="p-4">
        <h3 class="font-bold text-gray-900 mb-2">{{ forcePopupAnnouncement.title }}</h3>
        <div class="text-sm text-gray-700 whitespace-pre-wrap">{{ forcePopupAnnouncement.content }}</div>
        <div class="text-xs text-gray-400 mt-3">{{ formatTime(forcePopupAnnouncement.createTime) }}</div>
      </div>
      <div class="px-4 py-3 border-t border-gray-200 flex justify-end">
        <button 
          class="px-4 py-2 rounded bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium"
          @click="closeForcePopup"
        >
          我知道了
        </button>
      </div>
    </div>
  </div>

  <div v-if="announcementListOpen" class="fixed inset-0 z-[70] bg-black/50 flex items-center justify-center p-4">
    <div class="bg-white rounded-lg shadow-xl w-full max-w-2xl border border-gray-200 flex flex-col max-h-[80vh]">
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-semibold text-gray-900 text-lg">公告列表</div>
        <button 
          class="px-3 py-1 rounded bg-gray-100 hover:bg-gray-200 text-sm text-gray-700"
          @click="closeAnnouncementList"
        >
          关闭
        </button>
      </div>
      <div class="p-4 flex-1 overflow-y-auto">
        <div v-if="announcements.length === 0" class="text-center text-gray-400 py-8">
          暂无公告
        </div>
        <div v-else class="space-y-3">
          <div v-for="item in announcements" :key="item.id" class="border border-gray-200 rounded-lg p-3">
            <div class="flex items-start justify-between mb-2">
              <div class="flex items-center gap-2">
                <span v-if="item.isTop" class="px-2 py-0.5 bg-red-100 text-red-700 text-xs rounded">置顶</span>
                <span v-if="item.isForcePopup" class="px-2 py-0.5 bg-yellow-100 text-yellow-700 text-xs rounded">重要</span>
                <span 
                  class="font-semibold text-gray-900"
                  :class="{ 'text-red-600': isNewestAnnouncement(item) }"
                >
                  {{ item.title }}
                </span>
              </div>
              <div class="text-xs text-gray-400">{{ formatTime(item.createTime) }}</div>
            </div>
            <div class="text-sm text-gray-700 whitespace-pre-wrap">{{ item.content }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const isLogin = computed(() => route.path === '/login')

const me = ref({ user: null, plan: null, todayUsed: null, subscription: null })
const kickDialogOpen = ref(false)
const kickDialogMessage = ref('')
const agreementDialogOpen = ref(false)
const AGREEMENT_KEY = 'sxjw-agreement-accepted'

const ANNOUNCEMENT_CACHE_KEY = 'sxjw-announcement-cache'
const ANNOUNCEMENT_READ_KEY = 'sxjw-announcement-read'
const USER_DATA_CACHE_KEY = 'sxjw-user-data-cache'
const announcements = ref([])
const announcementListOpen = ref(false)
const forcePopupAnnouncement = ref(null)
const hasNewAnnouncement = ref(false)

const menu = [
  { key: 'chat', label: '🏠聊天生成', path: '/chat-preview' },
  { key: 'picmanage', label: '⚙️图像管理', path: '/route' },
  { key: 'chatcontent', label: '⚙️对话内容', path: '/card-message' },
  { key: 'membership', label: '💎会员中心', path: '/membership' },
  { key: 'tutorial', label: '📖使用教程', path: '/tutorial' },
  { key: 'support', label: '📞联系客服', path: '/support' },
  { key: 'statement', label: '📜软件声明', path: '/software-statement' },
  { key: 'logout', label: '🚪退出登录', action: 'logout' }
]

function isActive(item) {
  if (item.action) return false
  return route.path === item.path
}

async function onMenuClick(item) {
  if (item.action === 'logout') {
    await logout()
    return
  }
  if (item.path && route.path !== item.path) {
    router.push(item.path)
  }
}

function formatExpireTime(t) {
  if (!t) return '永久'
  const expireDate = new Date(t)
  const now = new Date()
  if (expireDate < now) {
    return '已过期'
  }
  return expireDate.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function saveUserDataToLocal(data) {
  try {
    const toSave = {
      user: data.user,
      plan: data.plan,
      todayUsed: data.todayUsed,
      subscription: data.subscription,
      cacheTime: new Date().toISOString()
    }
    localStorage.setItem(USER_DATA_CACHE_KEY, JSON.stringify(toSave))
  } catch {
  }
}

function loadUserDataFromLocal() {
  try {
    const cached = localStorage.getItem(USER_DATA_CACHE_KEY)
    if (cached) {
      const data = JSON.parse(cached)
      return {
        user: data.user || null,
        plan: data.plan || null,
        todayUsed: data.todayUsed,
        subscription: data.subscription || null
      }
    }
  } catch {
  }
  return null
}

function updateTodayUsedLocally(newTodayUsed) {
  me.value.todayUsed = newTodayUsed
  const cached = loadUserDataFromLocal()
  if (cached) {
    saveUserDataToLocal({ ...cached, todayUsed: newTodayUsed })
  }
}

async function refreshMe(force = false) {
  try {
    if (!force) {
      const localData = loadUserDataFromLocal()
      if (localData && localData.user) {
        me.value = localData
        return
      }
    }
    
    const resp = await fetch('/api/auth/me', { method: 'GET' })
    const r = await resp.json().catch(() => ({}))
    if (resp.ok && r && r.success) {
      const data = { user: r.user || null, plan: r.plan || null, todayUsed: r.todayUsed, subscription: r.subscription || null }
      me.value = data
      saveUserDataToLocal(data)
      return
    }
  } catch {
  }
  me.value = { user: null, plan: null, todayUsed: null, subscription: null }
}

async function logout() {
  try {
    await fetch('/api/auth/logout', { method: 'POST' })
  } catch {
  }
  me.value = { user: null, plan: null, todayUsed: null, subscription: null }
  try {
    localStorage.removeItem(USER_DATA_CACHE_KEY)
  } catch {
  }
  router.push('/login')
}

async function confirmKick() {
  kickDialogOpen.value = false
  kickDialogMessage.value = ''
  await logout()
}

function checkAgreement() {
  const accepted = localStorage.getItem(AGREEMENT_KEY)
  if (!accepted) {
    agreementDialogOpen.value = true
  }
}

function acceptAgreement() {
  localStorage.setItem(AGREEMENT_KEY, 'true')
  agreementDialogOpen.value = false
}

function rejectAgreement() {
  if (window.SiyuBaoBackend && window.SiyuBaoBackend.exit) {
    window.SiyuBaoBackend.exit()
  } else {
    window.close()
  }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleString('zh-CN')
}

function isNewestAnnouncement(item) {
  if (announcements.value.length === 0) return false
  return announcements.value[0].id === item.id
}

function shouldFetchFromServer() {
  try {
    const cached = localStorage.getItem(ANNOUNCEMENT_CACHE_KEY)
    if (!cached) return true
    const data = JSON.parse(cached)
    const cacheTime = new Date(data.cacheTime)
    const now = new Date()
    const hoursDiff = (now - cacheTime) / (1000 * 60 * 60)
    if (hoursDiff >= 1) return true
    return false
  } catch {
    return true
  }
}

async function loadAnnouncements() {
  try {
    let data = null
    if (shouldFetchFromServer()) {
      const resp = await fetch('/api/announcement/latest', { method: 'GET' })
      const r = await resp.json().catch(() => ({}))
      if (resp.ok && r && r.success && r.data) {
        data = {
          list: r.data,
          cacheTime: new Date().toISOString()
        }
        localStorage.setItem(ANNOUNCEMENT_CACHE_KEY, JSON.stringify(data))
      }
    } else {
      const cached = localStorage.getItem(ANNOUNCEMENT_CACHE_KEY)
      if (cached) {
        data = JSON.parse(cached)
      }
    }

    if (data && data.list) {
      announcements.value = data.list
      checkNewAnnouncement(data.list)
      checkForcePopup(data.list)
    }
  } catch (e) {
    console.error('加载公告失败', e)
    try {
      const cached = localStorage.getItem(ANNOUNCEMENT_CACHE_KEY)
      if (cached) {
        const data = JSON.parse(cached)
        if (data && data.list) {
          announcements.value = data.list
          checkNewAnnouncement(data.list)
        }
      }
    } catch {}
  }
}

function checkNewAnnouncement(list) {
  if (!list || list.length === 0) {
    hasNewAnnouncement.value = false
    return
  }
  try {
    const readStr = localStorage.getItem(ANNOUNCEMENT_READ_KEY)
    const readIds = readStr ? JSON.parse(readStr) : []
    const latest = list[0]
    hasNewAnnouncement.value = !readIds.includes(latest.id)
  } catch {
    hasNewAnnouncement.value = list.length > 0
  }
}

function checkForcePopup(list) {
  if (!list || list.length === 0) return
  const forcePopup = list.find(item => item.isForcePopup)
  if (forcePopup) {
    try {
      const readStr = localStorage.getItem(ANNOUNCEMENT_READ_KEY)
      const readIds = readStr ? JSON.parse(readStr) : []
      if (!readIds.includes(forcePopup.id)) {
        forcePopupAnnouncement.value = forcePopup
      }
    } catch {
      forcePopupAnnouncement.value = forcePopup
    }
  }
}

function markAsRead(id) {
  try {
    const readStr = localStorage.getItem(ANNOUNCEMENT_READ_KEY)
    let readIds = readStr ? JSON.parse(readStr) : []
    if (!readIds.includes(id)) {
      readIds.push(id)
      localStorage.setItem(ANNOUNCEMENT_READ_KEY, JSON.stringify(readIds))
    }
  } catch {}
}

function openAnnouncementList() {
  announcementListOpen.value = true
  if (announcements.value.length > 0) {
    markAsRead(announcements.value[0].id)
    hasNewAnnouncement.value = false
  }
}

function closeAnnouncementList() {
  announcementListOpen.value = false
}

function closeForcePopup() {
  if (forcePopupAnnouncement.value) {
    markAsRead(forcePopupAnnouncement.value.id)
  }
  forcePopupAnnouncement.value = null
}

let heartbeatTimer = null
let onUserUpdated = null

watch(() => route.path, async () => {
  if (!isLogin.value) {
    const localData = loadUserDataFromLocal()
    if (!localData || !localData.user) {
      await refreshMe(true)
    } else {
      await refreshMe()
    }
  }
})

onMounted(async () => {
  checkAgreement()
  await refreshMe(true)
  await loadAnnouncements()
  
  // 监听两个事件：一个是更新 todayUsed，一个是全量刷新
  onUserUpdated = (event) => {
    if (event && event.detail && event.detail.todayUsed !== undefined) {
      updateTodayUsedLocally(event.detail.todayUsed)
    } else {
      refreshMe(true)
    }
  }
  window.addEventListener('sxjw-user-updated', onUserUpdated)

  heartbeatTimer = setInterval(async () => {
    try {
      if (isLogin.value) return
      if (!me.value.user || !me.value.user.id) return

      const resp = await fetch('/api/auth/heartbeat', { method: 'POST' })
      if (resp.ok) return

      if (kickDialogOpen.value) return

      let j = null
      try {
        j = await resp.json().catch(() => null)
      } catch {
      }

      const kicked = !!(j && j.kicked === true)
      if (resp.status === 401 && kicked) {
        kickDialogMessage.value = (j && j.message) ? String(j.message) : '当前账号已下线'
        kickDialogOpen.value = true
        return
      }

      if (resp.status === 401) {
        await refreshMe(true)
        if (!me.value.user || !me.value.user.id) {
          await logout()
        }
      }
    } catch {
    }
  }, 30000)

})

onUnmounted(() => {
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  if (onUserUpdated) {
    window.removeEventListener('sxjw-user-updated', onUserUpdated)
    onUserUpdated = null
  }
})
</script>
