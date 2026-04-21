<template>
  <div class="w-full flex items-center justify-center py-10">
    <div class="w-full max-w-md bg-white border border-gray-200 rounded-2xl p-6 shadow-sm">
      <div class="flex flex-col items-center text-center">
        <svg width="64" height="64" viewBox="0 0 256 256" class="mb-3">
          <defs>
            <linearGradient id="sxjw-grad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#124baa" />
              <stop offset="0.55" stop-color="#00c8ff" />
              <stop offset="1" stop-color="#0d235a" />
            </linearGradient>
          </defs>
          <rect x="10" y="10" width="236" height="236" rx="48" fill="#070a12" />
          <path
            d="M60 64 L112 210 L136 152 L160 210 L212 64"
            fill="none"
            stroke="url(#sxjw-grad)"
            stroke-width="18"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <path
            d="M72 90 L204 90 L112 166 L204 166"
            fill="none"
            stroke="url(#sxjw-grad)"
            stroke-width="18"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <rect x="10" y="10" width="236" height="236" rx="48" fill="none" stroke="rgba(148,163,184,.35)" stroke-width="6" />
        </svg>
        <div class="text-xl font-semibold text-gray-900">仿聊大师</div>
        <div class="mt-1 text-sm text-gray-500">账号登录</div>
      </div>

      <div v-if="error" class="mt-4 text-sm text-red-600">{{ error }}</div>
      <div class="mt-4 flex items-center space-x-2">
        <button
          class="flex-1 px-3 py-2 rounded text-sm"
          :class="mode === 'login' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
          @click="mode = 'login'"
          type="button"
        >
          登录
        </button>
        <button
          class="flex-1 px-3 py-2 rounded text-sm"
          :class="mode === 'register' ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
          @click="mode = 'register'"
          type="button"
        >
          注册
        </button>
      </div>

      <form class="mt-4 space-y-3" @submit.prevent="submit">
        <input
          v-model.trim="username"
          class="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="用户名"
          autocomplete="off"
        />
        <input
          v-model.trim="password"
          type="password"
          class="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="密码"
          autocomplete="off"
        />
        <button
          class="w-full bg-blue-600 hover:bg-blue-700 text-white rounded px-3 py-2 text-sm disabled:opacity-60"
          :disabled="loading || !username || !password"
        >
          {{ loading ? (mode === 'login' ? '登录中...' : '注册中...') : mode === 'login' ? '登录' : '注册' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const mode = ref('login')
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

// 页面加载时自动填充保存的用户名
onMounted(() => {
  const savedUsername = localStorage.getItem('lastUsername')
  if (savedUsername) {
    username.value = savedUsername
  }
})

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const resp = await fetch(mode.value === 'login' ? '/api/auth/login' : '/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: username.value, password: password.value })
    })
    const json = await resp.json().catch(() => ({}))
    if (!resp.ok || !json.success) throw new Error(json.message || (mode.value === 'login' ? '登录失败' : '注册失败'))
    if (mode.value === 'register') {
      mode.value = 'login'
    } else {
      // 登录成功后保存用户名到localStorage
      try {
        localStorage.setItem('lastUsername', username.value)
        window.dispatchEvent(new Event('sxjw-user-updated'))
      } catch {
      }
    }
    await router.replace('/chat-preview')
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}
</script>
