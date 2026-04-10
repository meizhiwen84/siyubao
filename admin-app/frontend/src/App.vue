<template>
  <RouterView v-if="isLogin" />

  <div v-else class="layout">
    <header class="topbar">
      <div class="brand">
        <div class="logo">私信截图王</div>
        <div class="sub">管理端</div>
      </div>
      <div class="who">{{ meState.user?.username || '未登录' }}</div>
      <button class="btn" @click="logout">退出登录</button>
    </header>

    <div class="body">
      <aside class="sidebar">
        <nav class="menu">
          <button
            v-for="item in menu"
            :key="item.key"
            class="menu-item"
            :class="isActive(item) ? 'active' : ''"
            @click="go(item)"
          >
            {{ item.label }}
          </button>
        </nav>
      </aside>
      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { clearToken, me as apiMe } from './api'

const route = useRoute()
const router = useRouter()
const isLogin = computed(() => route.path === '/login')

const meState = ref({ user: null })

const menu = [
  { key: 'users', label: '用户管理', path: '/users' },
  { key: 'plans', label: '会员套餐管理', path: '/plans' },
  { key: 'subs', label: '用户开通/续费', path: '/subscriptions' },
  { key: 'devices', label: '设备在线管理', path: '/devices' },
  { key: 'logs', label: '操作日志', path: '/logs' },
  { key: 'settings', label: '系统设置', path: '/settings' }
]

function isActive(item) {
  return item.path === route.path
}

function go(item) {
  if (item.path) router.push(item.path)
}

async function refreshMe() {
  try {
    const r = await apiMe()
    meState.value = { user: r.user || null }
  } catch {
    meState.value = { user: null }
  }
}

async function logout() {
  clearToken()
  meState.value = { user: null }
  router.push('/login')
}

onMounted(refreshMe)
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  color: #e2e8f0;
}

.topbar {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(2, 6, 23, 0.6);
  backdrop-filter: blur(10px);
}

.brand {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.logo {
  font-weight: 700;
  letter-spacing: 0.5px;
}

.sub {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
}

.who {
  font-size: 13px;
  color: rgba(226, 232, 240, 0.92);
  max-width: 60vw;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.body {
  flex: 1;
  min-height: 0;
  display: flex;
}

.sidebar {
  width: 230px;
  border-right: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(2, 6, 23, 0.55);
  backdrop-filter: blur(10px);
}

.menu {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-item {
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(15, 23, 42, 0.35);
  color: rgba(226, 232, 240, 0.92);
  cursor: pointer;
}

.menu-item:hover {
  background: rgba(30, 41, 59, 0.6);
}

.menu-item.active {
  background: linear-gradient(135deg, rgba(29, 78, 216, 0.95), rgba(6, 182, 212, 0.85));
  border-color: rgba(255, 255, 255, 0.22);
  color: #fff;
  font-weight: 700;
}

.content {
  flex: 1;
  min-width: 0;
  padding: 14px;
  overflow: auto;
}
</style>

