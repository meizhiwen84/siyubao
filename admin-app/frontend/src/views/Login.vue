<template>
  <div class="wrap">
    <div class="card panel">
      <div class="title">私信截图王 · 管理端登录</div>
      <div v-if="error" class="error">{{ error }}</div>

      <div class="row">
        <div class="label">用户名</div>
        <input v-model.trim="username" class="input" autocomplete="username" />
      </div>
      <div class="row">
        <div class="label">密码</div>
        <input v-model.trim="password" class="input" type="password" autocomplete="current-password" />
      </div>

      <div class="actions">
        <button class="btn primary" :disabled="loading || !username || !password" @click="doLogin">
          {{ loading ? '登录中…' : '登录' }}
        </button>
      </div>

      <div class="hint">请使用管理员账号登录</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, me } from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function doLogin() {
  error.value = ''
  loading.value = true
  try {
    await login(username.value, password.value)
    const r = await me()
    const role = r && r.user ? r.user.role : ''
    if (role !== 'ADMIN') throw new Error('非管理员账号')
    router.push('/users')
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px;
}

.panel {
  width: 100%;
  max-width: 420px;
  padding: 18px;
}

.title {
  font-weight: 800;
  letter-spacing: 0.3px;
  margin-bottom: 14px;
}

.row {
  margin-bottom: 12px;
}

.label {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
  margin-bottom: 6px;
}

.actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.error {
  border: 1px solid rgba(248, 113, 113, 0.35);
  background: rgba(127, 29, 29, 0.28);
  color: rgba(254, 226, 226, 0.95);
  padding: 10px 12px;
  border-radius: 12px;
  margin-bottom: 12px;
  font-size: 13px;
}

.hint {
  margin-top: 12px;
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
}
</style>

