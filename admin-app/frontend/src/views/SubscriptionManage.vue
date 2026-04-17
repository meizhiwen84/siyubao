<template>
  <div class="card box">
    <div class="head">
      <div class="title">用户开通/续费/升级</div>
      <div class="meta">支持：开通、续费叠加、升级保留剩余时长</div>
    </div>
    <div class="body">
      <div v-if="error" class="err">{{ error }}</div>
      <div class="grid">
        <div class="card sub">
          <div class="sub-title">选择用户</div>
          <input v-model.trim="userInput" class="input" placeholder="用户ID或用户编号" />
        </div>
        <div class="card sub">
          <div class="sub-title">选择套餐</div>
          <select v-model.number="planId" class="input">
            <option :value="0">请选择</option>
            <option v-for="p in plans" :key="p.id" :value="p.id">
              {{ p.name }}（{{ p.code }}）
            </option>
          </select>
        </div>
        <div class="card sub">
          <div class="sub-title">操作</div>
          <div class="row">
            <button class="btn primary" :disabled="submitting || !userId || !planId" @click="grant">
              {{ submitting ? '提交中…' : '开通' }}
            </button>
          </div>
          <div class="tip">开通会覆盖当前有效订阅</div>
        </div>
      </div>

      <div class="card sub" style="margin-top: 12px">
        <div class="sub-title">当前订阅</div>
        <div v-if="subLoading" class="tip">加载中…</div>
        <div v-else-if="!activeSub" class="tip">暂无有效订阅</div>
        <div v-else class="subinfo">
          <div>套餐：{{ activeSub.plan?.name || activeSub.planId }}</div>
          <div>开始：{{ fmt(activeSub.startTime) }}</div>
          <div>到期：{{ activeSub.endTime ? fmt(activeSub.endTime) : '永久' }}</div>
          <div>状态：{{ activeSub.status }}</div>
        </div>
        <button class="btn" style="margin-top: 10px" :disabled="subLoading || !userId" @click="loadActive">刷新订阅</button>
      </div>

      <div class="grid2" style="margin-top: 12px">
        <div class="card sub">
          <div class="sub-title">续费叠加</div>
          <div class="row">
            <input v-model.number="renewDays" class="input" type="number" min="1" placeholder="增加天数" />
          </div>
          <button class="btn primary" :disabled="submitting || !userId || renewDays <= 0" @click="renew">
            {{ submitting ? '提交中…' : '续费' }}
          </button>
          <div class="tip">永久订阅不可续费</div>
        </div>

        <div class="card sub">
          <div class="sub-title">升级</div>
          <div class="row">
            <select v-model.number="upgradePlanId" class="input">
              <option :value="0">请选择升级目标套餐</option>
              <option v-for="p in plans" :key="p.id" :value="p.id">
                {{ p.name }}（{{ p.code }}）
              </option>
            </select>
          </div>
          <button class="btn primary" :disabled="submitting || !userId || !upgradePlanId" @click="upgrade">
            {{ submitting ? '提交中…' : '升级' }}
          </button>
          <div class="tip">升级会保留剩余时长并加上目标套餐时长</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { adminGrantSubscription, adminPlans, adminSubscriptionActive, adminSubscriptionRenew, adminSubscriptionUpgrade } from '../api'

const route = useRoute()
const error = ref('')
const submitting = ref(false)
const plans = ref([])

const userInput = ref('')
const planId = ref(0)
const upgradePlanId = ref(0)
const renewDays = ref(30)

const subLoading = ref(false)
const activeSub = ref(null)

const isUserNo = computed(() => {
  const t = String(userInput.value || '').trim()
  if (!t) return false
  const n = Number(t)
  return !(Number.isFinite(n) && n > 0)
})

const userIdOrUserNo = computed(() => {
  const t = String(userInput.value || '').trim()
  if (!t) return null
  const n = Number(t)
  if (Number.isFinite(n) && n > 0) {
    return Math.floor(n)
  }
  return t
})

function fmt(v) {
  if (!v) return ''
  return String(v)
}

async function loadPlans() {
  try {
    const r = await adminPlans()
    plans.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {
    error.value = e?.message || String(e)
  }
}

async function loadActive() {
  error.value = ''
  activeSub.value = null
  if (!userIdOrUserNo.value) return
  subLoading.value = true
  try {
    const r = await adminSubscriptionActive(userIdOrUserNo.value, isUserNo.value)
    if (!r || !r.success) throw new Error(r?.message || '查询失败')
    activeSub.value = r.data || null
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    subLoading.value = false
  }
}

async function grant() {
  error.value = ''
  submitting.value = true
  try {
    const params = { planId: planId.value }
    if (isUserNo.value) {
      params.userNo = userIdOrUserNo.value
    } else {
      params.userId = userIdOrUserNo.value
    }
    const r = await adminGrantSubscription(params)
    if (!r || !r.success) throw new Error(r?.message || '开通失败')
    await loadActive()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    submitting.value = false
  }
}

async function renew() {
  error.value = ''
  submitting.value = true
  try {
    const params = { addDays: renewDays.value }
    if (isUserNo.value) {
      params.userNo = userIdOrUserNo.value
    } else {
      params.userId = userIdOrUserNo.value
    }
    const r = await adminSubscriptionRenew(params)
    if (!r || !r.success) throw new Error(r?.message || '续费失败')
    activeSub.value = r.data || null
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    submitting.value = false
  }
}

async function upgrade() {
  error.value = ''
  submitting.value = true
  try {
    const params = { planId: upgradePlanId.value }
    if (isUserNo.value) {
      params.userNo = userIdOrUserNo.value
    } else {
      params.userId = userIdOrUserNo.value
    }
    const r = await adminSubscriptionUpgrade(params)
    if (!r || !r.success) throw new Error(r?.message || '升级失败')
    activeSub.value = r.data || null
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadPlans()
  const qUserId = route.query ? route.query.userId : null
  const qUserNo = route.query ? route.query.userNo : null
  if (qUserNo != null && String(qUserNo).trim()) {
    userInput.value = String(qUserNo).trim()
    await loadActive()
  } else if (qUserId != null && String(qUserId).trim()) {
    userInput.value = String(qUserId).trim()
    await loadActive()
  }
})
</script>

<style scoped>
.box {
  padding: 14px;
}
.head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}
.title {
  font-weight: 800;
}
.meta {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
}
.body {
  padding-top: 14px;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12px;
}

.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.sub {
  padding: 12px;
}
.sub-title {
  font-weight: 700;
  margin-bottom: 10px;
}
.row {
  margin-bottom: 8px;
}
.tip {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
}

.subinfo {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  font-size: 13px;
  color: rgba(226, 232, 240, 0.92);
}

.err {
  border: 1px solid rgba(248, 113, 113, 0.35);
  background: rgba(127, 29, 29, 0.28);
  color: rgba(254, 226, 226, 0.95);
  padding: 10px 12px;
  border-radius: 12px;
  margin-bottom: 12px;
  font-size: 13px;
}
</style>
