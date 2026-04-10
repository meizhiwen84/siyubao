<template>
  <div class="bg-white border border-gray-200 rounded-lg p-6">
    <div class="text-lg font-medium text-gray-900">会员中心</div>
    <div class="mt-1 text-sm text-gray-500">价格与权益从远程获取</div>

    <div v-if="error" class="mt-4 text-sm text-red-600">{{ error }}</div>
    <div v-if="loading" class="mt-4 text-sm text-gray-600">加载中…</div>

    <div v-if="!loading" class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-3">
      <div
        v-for="p in plans"
        :key="p.id"
        class="border border-gray-200 rounded-lg p-4 hover:border-gray-300"
      >
        <div class="flex items-center justify-between">
          <div class="font-medium text-gray-900">{{ p.name }}</div>
          <div class="text-sm text-gray-600">{{ formatPrice(p.priceCents) }}</div>
        </div>
        <div class="mt-2 text-xs text-gray-500">
          时长：{{ p.durationDays }}天 · 设备：{{ p.deviceLimit }} · 水印：{{ p.watermark ? '有' : '无' }}
        </div>
        <div class="mt-2 text-xs text-gray-500" v-if="p.dailyFreeLimit && p.dailyFreeLimit > 0">
          免费：每日 {{ p.dailyFreeLimit }} 次
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const loading = ref(false)
const error = ref('')
const plans = ref([])

function formatPrice(cents) {
  const n = Number(cents || 0)
  if (n <= 0) return '免费'
  return `￥${(n / 100).toFixed(2)}`
}

onMounted(async () => {
  error.value = ''
  loading.value = true
  try {
    const resp = await fetch('/api/auth/plans')
    const r = await resp.json().catch(() => ({}))
    if (!resp.ok || !r.success) throw new Error(r.message || '获取失败')
    plans.value = Array.isArray(r.plans) ? r.plans : []
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
})
</script>

