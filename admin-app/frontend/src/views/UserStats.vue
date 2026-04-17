<template>
  <div class="card box">
    <div class="head">
      <div class="title">用户统计</div>
      <div class="meta">用户：{{ userIdOrUserNo }}</div>
      <button class="btn" @click="goBack">返回</button>
    </div>
    <div class="body">
      <div v-if="error" class="err">{{ error }}</div>

      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab"
          :class="{ active: currentTab === tab.key }"
          @click="currentTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="currentTab === 'usage'" class="card sub">
        <div class="sub-title">每日使用记录</div>
        <div v-if="loadingUsage" class="loading">加载中…</div>
        <div v-else class="table">
          <table class="tbl">
            <thead>
              <tr>
                <th>日期</th>
                <th>使用次数</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in usageList" :key="item.date">
                <td>{{ item.date }}</td>
                <td>{{ item.count }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="!loadingUsage && usageList.length === 0" class="placeholder">暂无数据</div>
        </div>
        <div v-if="usageTotalPages > 1" class="pager">
          <div class="pmeta">第 {{ usagePage }} / {{ usageTotalPages }} 页，共 {{ usageTotal }} 条</div>
          <div style="display: flex; gap: 8px">
            <button class="btn" :disabled="loadingUsage || usagePage <= 1" @click="loadUsage(usagePage - 1)">上一页</button>
            <button class="btn" :disabled="loadingUsage || usagePage >= usageTotalPages" @click="loadUsage(usagePage + 1)">下一页</button>
          </div>
        </div>
      </div>

      <div v-if="currentTab === 'subscriptions'" class="card sub">
        <div class="sub-title">订阅记录</div>
        <div v-if="loadingSubs" class="loading">加载中…</div>
        <div v-else class="table">
          <table class="tbl">
            <thead>
              <tr>
                <th>套餐</th>
                <th>开始时间</th>
                <th>结束时间</th>
                <th>是否永久</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in subList" :key="item.id">
                <td>{{ item.planName || '-' }}</td>
                <td>{{ item.startTime }}</td>
                <td>{{ item.endTime || '-' }}</td>
                <td>{{ item.isLifetime ? '是' : '否' }}</td>
                <td>
                  <span :class="['px-2 py-1 rounded-full text-xs font-medium', 
                    item.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 
                    item.status === 'EXPIRED' ? 'bg-red-100 text-red-700' : 
                    'bg-yellow-100 text-yellow-700'
                  ]">
                    {{ formatStatus(item.status) }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!loadingSubs && subList.length === 0" class="placeholder">暂无数据</div>
        </div>
        <div v-if="subTotalPages > 1" class="pager">
          <div class="pmeta">第 {{ subPage }} / {{ subTotalPages }} 页，共 {{ subTotal }} 条</div>
          <div style="display: flex; gap: 8px">
            <button class="btn" :disabled="loadingSubs || subPage <= 1" @click="loadSubs(subPage - 1)">上一页</button>
            <button class="btn" :disabled="loadingSubs || subPage >= subTotalPages" @click="loadSubs(subPage + 1)">下一页</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { adminUserDailyUsage, adminUserSubscriptions } from '../api'

const router = useRouter()
const route = useRoute()

const tabs = [
  { key: 'usage', label: '使用记录' },
  { key: 'subscriptions', label: '订阅记录' }
]

const userIdOrUserNo = ref('')
const isUserNo = ref(false)
const currentTab = ref(route.query.tab || 'usage')

const loadingUsage = ref(false)
const usageList = ref([])
const usagePage = ref(1)
const usageTotalPages = ref(1)
const usageTotal = ref(0)

const loadingSubs = ref(false)
const subList = ref([])
const subPage = ref(1)
const subTotalPages = ref(1)
const subTotal = ref(0)

const error = ref('')

onMounted(() => {
  const qUserId = route.query ? route.query.userId : null
  const qUserNo = route.query ? route.query.userNo : null
  if (qUserNo != null && String(qUserNo).trim()) {
    userIdOrUserNo.value = String(qUserNo).trim()
    isUserNo.value = true
  } else if (qUserId != null && String(qUserId).trim()) {
    userIdOrUserNo.value = String(qUserId).trim()
    isUserNo.value = false
  }
  
  if (currentTab.value === 'usage') {
    loadUsage(1)
  } else if (currentTab.value === 'subscriptions') {
    loadSubs(1)
  }
})

async function loadUsage(pageNum = 1) {
  error.value = ''
  loadingUsage.value = true
  try {
    const r = await adminUserDailyUsage(userIdOrUserNo.value, pageNum, 30, isUserNo.value)
    if (!r || !r.success) throw new Error(r?.message || '加载失败')
    usageList.value = r.data || []
    usagePage.value = r.page || 1
    usageTotalPages.value = r.totalPages || 1
    usageTotal.value = r.total || 0
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loadingUsage.value = false
  }
}

async function loadSubs(pageNum = 1) {
  error.value = ''
  loadingSubs.value = true
  try {
    const r = await adminUserSubscriptions(userIdOrUserNo.value, pageNum, 20, isUserNo.value)
    if (!r || !r.success) throw new Error(r?.message || '加载失败')
    subList.value = r.data || []
    subPage.value = r.page || 1
    subTotalPages.value = r.totalPages || 1
    subTotal.value = r.total || 0
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loadingSubs.value = false
  }
}

function formatStatus(status) {
  if (status === 'ACTIVE') return '正常'
  if (status === 'EXPIRED') return '已过期'
  return status || '-'
}

function goBack() {
  router.push({ path: '/users' })
}

watch(currentTab, (newTab) => {
  if (newTab === 'usage' && usageList.value.length === 0) {
    loadUsage(1)
  } else if (newTab === 'subscriptions' && subList.value.length === 0) {
    loadSubs(1)
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
.sub {
  padding: 12px;
}
.sub-title {
  font-weight: 700;
  margin-bottom: 10px;
}
.err {
  border: 1px solid rgba(248, 113, 113, 0.35);
  background: rgba(127, 29, 29, 0.28);
  color: rgba(254, 226, 226, 0.95);
  padding: 10px 12px;
  border-radius: 12px;
  margin-bottom: 10px;
  font-size: 13px;
}
.loading {
  padding: 24px 12px;
  color: rgba(148, 163, 184, 0.8);
}
.placeholder {
  padding: 24px 12px;
  text-align: center;
  color: rgba(148, 163, 184, 0.6);
}
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}
.tab {
  padding: 8px 16px;
  border-radius: 8px;
  background: transparent;
  border: 1px solid rgba(148, 163, 184, 0.2);
  color: rgba(148, 163, 184, 0.9);
  cursor: pointer;
}
.tab.active {
  background: rgba(59, 130, 246, 0.2);
  border-color: rgba(59, 130, 246, 0.5);
  color: rgba(191, 219, 254, 1);
}
.table {
  overflow: auto;
}
.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.tbl th,
.tbl td {
  padding: 10px 12px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  text-align: left;
  white-space: nowrap;
}
.tbl thead th {
  border-top: 0;
  color: rgba(148, 163, 184, 0.95);
  font-weight: 700;
}
.pager {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.pmeta {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
}

.bg-green-100 {
  background-color: rgba(22, 163, 74, 0.15);
}
.text-green-700 {
  color: rgba(74, 222, 128, 1);
}
.bg-red-100 {
  background-color: rgba(220, 38, 38, 0.15);
}
.text-red-700 {
  color: rgba(248, 113, 113, 1);
}
.bg-yellow-100 {
  background-color: rgba(202, 138, 4, 0.15);
}
.text-yellow-700 {
  color: rgba(250, 204, 21, 1);
}
.rounded-full {
  border-radius: 999px;
}
.px-2 {
  padding-left: 0.5rem;
  padding-right: 0.5rem;
}
.py-1 {
  padding-top: 0.25rem;
  padding-bottom: 0.25rem;
}
.text-xs {
  font-size: 0.75rem;
}
.font-medium {
  font-weight: 500;
}
</style>
