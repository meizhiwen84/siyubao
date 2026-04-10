<template>
  <div class="bg-white border border-gray-200 rounded-lg p-6">
    <div class="text-lg font-medium text-gray-900">我的账号</div>
    <div class="mt-4 text-sm text-gray-700">
      <div>账号：{{ user?.username || '-' }}</div>
      <div class="mt-2">会员：{{ plan?.name || '-' }}</div>
      <div class="mt-2">今日已用：{{ todayUsed ?? '-' }}</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const user = ref(null)
const plan = ref(null)
const todayUsed = ref(null)

onMounted(async () => {
  try {
    const resp = await fetch('/api/auth/me')
    const r = await resp.json().catch(() => ({}))
    if (resp.ok && r && r.success) {
      user.value = r.user || null
      plan.value = r.plan || null
      todayUsed.value = r.todayUsed
      return
    }
  } catch {
  }
})
</script>

