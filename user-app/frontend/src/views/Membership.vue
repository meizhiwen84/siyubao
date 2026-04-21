<template>
  <div class="max-w-5xl mx-auto space-y-6">
    
    <div class="bg-white border border-gray-200 rounded-lg p-6">
      <h2 class="text-xl font-bold text-gray-900 mb-4">👤 我的账号</h2>
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div class="bg-gray-50 rounded-lg p-4">
          <div class="text-sm text-gray-500 mb-1">账号</div>
          <div class="text-lg font-semibold text-gray-900">{{ me?.user?.username || '-' }}</div>
        </div>
        <div class="bg-gray-50 rounded-lg p-4">
          <div class="text-sm text-gray-500 mb-1">当前会员</div>
          <div class="text-lg font-semibold text-blue-600">{{ me?.plan?.name || '免费版' }}</div>
        </div>
        <div class="bg-gray-50 rounded-lg p-4">
          <div class="text-sm text-gray-500 mb-1">到期时间</div>
          <div class="text-lg font-semibold text-gray-900">{{ formatExpireTime(me?.subscription?.endTime) }}</div>
        </div>
        <div class="bg-gray-50 rounded-lg p-4">
          <div class="text-sm text-gray-500 mb-1">今日已用</div>
          <div class="text-lg font-semibold text-gray-900">{{ me?.todayUsed ?? '-' }}</div>
        </div>
      </div>
    </div>

    <div class="bg-white border border-gray-200 rounded-lg p-6">
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-xl font-bold text-gray-900">💎 会员套餐</h2>
        <span class="text-sm text-gray-500">升级套餐解锁更多功能</span>
      </div>

      <div v-if="error" class="mb-4 text-sm text-red-600 bg-red-50 p-3 rounded">{{ error }}</div>
      <div v-if="loading" class="mb-4 text-sm text-gray-600">加载中…</div>

      <div v-if="!loading" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="p in plans"
          :key="p.id"
          class="border border-gray-200 rounded-xl p-5 hover:border-blue-400 hover:shadow-lg transition-all"
          :class="{ 'border-blue-500 ring-2 ring-blue-100': me?.plan?.id === p.id }"
        >
          <div class="flex items-center justify-between mb-3">
            <div class="font-bold text-lg text-gray-900">{{ p.name }}</div>
            <div v-if="me?.plan?.id === p.id" class="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded-full">当前</div>
          </div>
          
          <div class="text-3xl font-bold text-gray-900 mb-3">
            {{ formatPrice(p.priceCents) }}
          </div>

          <div class="space-y-2 text-sm text-gray-600 mb-5">
            <div class="flex items-center gap-2">
              <span class="text-green-500">✓</span>
              <span>时长：{{ p.durationDays ? p.durationDays + '天' : '永久' }}</span>
            </div>
            <div class="flex items-center gap-2">
              <span class="text-green-500">✓</span>
              <span>设备限制：{{ p.deviceLimit }}台</span>
            </div>
            <div class="flex items-center gap-2">
              <span class="text-green-500">✓</span>
              <span>水印：{{ p.watermark ? '有' : '无' }}</span>
            </div>
            <div v-if="p.dailyFreeLimit && p.dailyFreeLimit > 0" class="flex items-center gap-2">
              <span class="text-green-500">✓</span>
              <span>免费：每日 {{ p.dailyFreeLimit }} 次</span>
            </div>
          </div>

          <button
            v-if="me?.plan?.id !== p.id && p.priceCents > 0"
            class="w-full py-2.5 rounded-lg font-medium transition-all"
            :class="me?.plan?.id === p.id 
              ? 'bg-gray-100 text-gray-400 cursor-not-allowed' 
              : 'bg-blue-600 hover:bg-blue-700 text-white'"
            @click="showUpgradeDialog(p)"
          >
            立即升级
          </button>
          <button
            v-else-if="me?.plan?.id === p.id"
            class="w-full py-2.5 rounded-lg font-medium bg-gray-100 text-gray-500 cursor-default"
            disabled
          >
            当前套餐
          </button>
          <button
            v-else
            class="w-full py-2.5 rounded-lg font-medium bg-gray-100 text-gray-500 cursor-default"
            disabled
          >
            免费版
          </button>
        </div>
      </div>
    </div>

    <div class="bg-white border border-gray-200 rounded-lg p-6">
      <h2 class="text-xl font-bold text-gray-900 mb-4">📋 我的订单</h2>
      
      <div v-if="ordersLoading" class="text-sm text-gray-600">加载中…</div>
      <div v-else-if="myOrders.length === 0" class="text-sm text-gray-500 text-center py-8">暂无订单记录</div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-gray-200">
              <th class="text-left py-3 px-2 text-gray-500 font-medium">订单号</th>
              <th class="text-left py-3 px-2 text-gray-500 font-medium">套餐</th>
              <th class="text-left py-3 px-2 text-gray-500 font-medium">金额</th>
              <th class="text-left py-3 px-2 text-gray-500 font-medium">交易号</th>
              <th class="text-left py-3 px-2 text-gray-500 font-medium">状态</th>
              <th class="text-left py-3 px-2 text-gray-500 font-medium">时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in myOrders" :key="o.id" class="border-b border-gray-100">
              <td class="py-3 px-2 font-mono text-xs text-gray-600">{{ o.orderNo }}</td>
              <td class="py-3 px-2">{{ o.plan?.name || '-' }}</td>
              <td class="py-3 px-2">{{ formatPrice(o.amountCents) }}</td>
              <td class="py-3 px-2 font-mono text-xs text-gray-600">{{ o.transactionId }}</td>
              <td class="py-3 px-2">
                <span :class="['px-2 py-1 rounded-full text-xs font-medium', 
                  o.status === 'PENDING' ? 'bg-yellow-100 text-yellow-700' : 
                  o.status === 'APPROVED' ? 'bg-green-100 text-green-700' : 
                  'bg-red-100 text-red-700'
                ]">
                  {{ formatOrderStatus(o.status) }}
                </span>
              </td>
              <td class="py-3 px-2 text-gray-500 text-xs">{{ formatTime(o.createTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="upgradeDialogOpen" class="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4" @click.self="upgradeDialogOpen = false">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg border border-gray-200">
        <div class="px-5 py-4 border-b border-gray-200 flex items-center justify-between">
          <div class="font-semibold text-gray-900 text-lg">升级套餐</div>
          <button class="text-gray-400 hover:text-gray-600" @click="upgradeDialogOpen = false">✕</button>
        </div>
        <div class="p-5">
          <div class="mb-4 p-4 bg-blue-50 rounded-lg">
            <div class="font-medium text-blue-900 mb-1">{{ selectedPlan?.name }}</div>
            <div class="text-2xl font-bold text-blue-600">{{ formatPrice(selectedPlan?.priceCents) }}</div>
          </div>

          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">选择支付平台</label>
              <select v-model="upgradeForm.platform" class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                <option 
                  v-for="p in availablePlatforms" 
                  :key="p.key" 
                  :value="p.key"
                >
                  {{ p.name }}
                </option>
              </select>
            </div>

            <div v-if="paymentConfig" class="text-center">
              <div v-if="currentQrCode" class="inline-block">
                <div class="text-sm text-gray-600 mb-2">
                  {{ upgradeForm.platform === 'wechat' ? '微信扫码付款' : '支付宝扫码付款' }}
                </div>
                <img 
                  :key="currentQrCode.id + '-' + upgradeForm.platform" 
                  :src="currentQrCode.url" 
                  :alt="upgradeForm.platform === 'wechat' ? '微信收款码' : '支付宝收款码'" 
                  class="max-w-48 max-h-48 border border-gray-200 rounded-lg mx-auto" 
                />
              </div>
              <div v-else class="text-sm text-gray-500">
                请联系客服获取收款码
              </div>
            </div>

            <div class="p-4 bg-yellow-50 rounded-lg text-sm text-yellow-800">
              <div class="font-medium mb-1">⚠️ 付款说明</div>
              <ol class="list-decimal list-inside space-y-1">
                <li>请使用 {{ upgradeForm.platform === 'wechat' ? '微信' : '支付宝' }} 扫码付款</li>
                <li>付款金额：{{ formatPrice(selectedPlan?.priceCents) }}</li>
                <li>付款完成后，复制交易订单号</li>
                <li>将订单号粘贴到下方输入框</li>
                <li>点击"提交订单"，我们会尽快为您开通</li>
              </ol>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">交易订单号</label>
              <input 
                v-model="upgradeForm.transactionId" 
                type="text" 
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm"
                placeholder="请输入微信/支付宝交易单号"
              />
            </div>

            <div v-if="upgradeError" class="text-sm text-red-600">{{ upgradeError }}</div>
          </div>
        </div>
        <div class="px-5 py-4 border-t border-gray-200 flex space-x-3">
          <button class="flex-1 py-2.5 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 font-medium" @click="upgradeDialogOpen = false">
            取消
          </button>
          <button 
            class="flex-1 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="upgradeSubmitting || !upgradeForm.transactionId.trim()"
            @click="submitUpgrade"
          >
            {{ upgradeSubmitting ? '提交中…' : '提交订单' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'

const me = ref({ user: null, plan: null, todayUsed: null, subscription: null })
const loading = ref(false)
const error = ref('')
const plans = ref([])
const myOrders = ref([])
const ordersLoading = ref(false)
const paymentConfig = ref(null)

const upgradeDialogOpen = ref(false)
const selectedPlan = ref(null)
const upgradeForm = ref({
  platform: 'wechat',
  transactionId: '',
  qrCodeId: null,
  qrCodeName: '',
  qrCodeUrl: ''
})
const upgradeSubmitting = ref(false)
const upgradeError = ref('')

const availablePlatforms = computed(() => {
  const platforms = []
  if (paymentConfig.value?.wechatQr) {
    platforms.push({ key: 'wechat', name: '微信支付' })
  }
  if (paymentConfig.value?.alipayQr) {
    platforms.push({ key: 'alipay', name: '支付宝' })
  }
  return platforms
})

const currentQrCode = computed(() => {
  if (!paymentConfig.value) return null
  if (upgradeForm.value.platform === 'wechat') {
    return paymentConfig.value.wechatQr
  } else if (upgradeForm.value.platform === 'alipay') {
    return paymentConfig.value.alipayQr
  }
  return null
})

watch(() => upgradeForm.value.platform, () => {
  updateQrCodeInfo()
})

function formatPrice(cents) {
  const n = Number(cents || 0)
  if (n <= 0) return '免费'
  return `￥${(n / 100).toFixed(2)}`
}

function formatOrderStatus(s) {
  if (s === 'PENDING') return '待处理'
  if (s === 'APPROVED') return '已批准'
  if (s === 'REJECTED') return '已拒绝'
  return s || '-'
}

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
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

async function loadPaymentConfig() {
  try {
    const resp = await fetch('/api/payment/config')
    const r = await resp.json().catch(() => ({}))
    if (resp.ok && r && r.success) {
      paymentConfig.value = r.data
    }
  } catch {
  }
}

async function refreshMe() {
  try {
    const resp = await fetch('/api/auth/me')
    const r = await resp.json().catch(() => ({}))
    if (resp.ok && r && r.success) {
      me.value = { 
        user: r.user || null, 
        plan: r.plan || null, 
        todayUsed: r.todayUsed,
        subscription: r.subscription || null
      }
    }
  } catch {
  }
}

async function loadPlans() {
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
}

async function loadMyOrders() {
  ordersLoading.value = true
  try {
    const resp = await fetch('/api/payment/my-orders')
    const r = await resp.json().catch(() => ({}))
    if (resp.ok && r && r.success) {
      myOrders.value = Array.isArray(r.data) ? r.data : []
    }
  } catch {
  } finally {
    ordersLoading.value = false
  }
}

function updateQrCodeInfo() {
  if (!paymentConfig.value) return
  
  if (upgradeForm.value.platform === 'wechat' && paymentConfig.value.wechatQr) {
    upgradeForm.value.qrCodeId = paymentConfig.value.wechatQr.id
    upgradeForm.value.qrCodeName = paymentConfig.value.wechatQr.name
    upgradeForm.value.qrCodeUrl = paymentConfig.value.wechatQr.url
  } else if (upgradeForm.value.platform === 'alipay' && paymentConfig.value.alipayQr) {
    upgradeForm.value.qrCodeId = paymentConfig.value.alipayQr.id
    upgradeForm.value.qrCodeName = paymentConfig.value.alipayQr.name
    upgradeForm.value.qrCodeUrl = paymentConfig.value.alipayQr.url
  } else {
    upgradeForm.value.qrCodeId = null
    upgradeForm.value.qrCodeName = ''
    upgradeForm.value.qrCodeUrl = ''
  }
}

async function showUpgradeDialog(plan) {
  selectedPlan.value = plan
  upgradeForm.value = {
    platform: 'wechat',
    transactionId: '',
    qrCodeId: null,
    qrCodeName: '',
    qrCodeUrl: ''
  }
  upgradeError.value = ''
  upgradeDialogOpen.value = true
  
  // 每次打开对话框时重新获取支付配置，获取新的随机二维码
  await loadPaymentConfig()
  
  // 重新获取配置后，选择第一个可用平台
  const defaultPlatform = availablePlatforms.value.length > 0 ? availablePlatforms.value[0].key : 'wechat'
  upgradeForm.value.platform = defaultPlatform
  updateQrCodeInfo()
}

async function submitUpgrade() {
  if (!selectedPlan.value) return
  upgradeError.value = ''
  upgradeSubmitting.value = true
  try {
    const payload = {
      planId: selectedPlan.value.id,
      platform: upgradeForm.value.platform,
      transactionId: upgradeForm.value.transactionId.trim()
    }
    
    if (upgradeForm.value.qrCodeId) {
      payload.qrCodeId = upgradeForm.value.qrCodeId
    }
    if (upgradeForm.value.qrCodeName) {
      payload.qrCodeName = upgradeForm.value.qrCodeName
    }
    if (upgradeForm.value.qrCodeUrl) {
      payload.qrCodeUrl = upgradeForm.value.qrCodeUrl
    }
    
    const resp = await fetch('/api/payment/submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    })
    const r = await resp.json().catch(() => ({}))
    if (!resp.ok || !r.success) throw new Error(r.message || '提交失败')
    
    upgradeDialogOpen.value = false
    await loadMyOrders()
    await refreshMe()
    
    window.dispatchEvent(new CustomEvent('sxjw-user-updated'))
  } catch (e) {
    upgradeError.value = e?.message || String(e)
  } finally {
    upgradeSubmitting.value = false
  }
}

onMounted(async () => {
  await refreshMe()
  await loadPlans()
  await loadMyOrders()
  await loadPaymentConfig()
})
</script>
