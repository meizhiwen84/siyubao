<template>
  <div class="card box">
    <div class="head">
      <div class="title">用户管理</div>
      <div class="meta">支持：查询与启用/禁用</div>
    </div>
    <div class="body">
      <div class="grid">
        <div class="card sub">
          <div class="sub-title">搜索</div>
          <div class="row">
            <input v-model.trim="keyword" class="input" placeholder="用户名关键词" />
          </div>
          <button class="btn" :disabled="loading" @click="search(0)">{{ loading ? '查询中…' : '查询' }}</button>
        </div>
        <div class="card sub">
          <div class="sub-title">列表</div>
          <div v-if="error" class="err">{{ error }}</div>
          <div class="table">
            <table class="tbl">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>用户名</th>
                  <th>角色</th>
                  <th>套餐</th>
                  <th>到期</th>
                  <th>启用</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="u in rows" :key="u.id">
                  <td>{{ u.id }}</td>
                  <td>{{ u.username }}</td>
                  <td>{{ u.role }}</td>
                  <td>{{ u.planName || '-' }}</td>
                  <td>{{ fmtEnd(u.subscriptionEndTime) }}</td>
                  <td>{{ u.enabled ? '是' : '否' }}</td>
                  <td>
                    <div style="display: flex; gap: 8px; flex-wrap: wrap">
                      <button class="btn" @click="viewUsage(u)">使用记录</button>
                      <button class="btn" @click="viewSubscriptions(u)">订阅记录</button>
                      <button class="btn" @click="viewPlan(u)">开通会员</button>
                      <button class="btn" @click="toggle(u)">{{ u.enabled ? '禁用' : '启用' }}</button>
                      <button class="btn" @click="resetPwd(u)">重置密码</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="!loading && rows.length === 0" class="placeholder">暂无数据</div>
          </div>
          <div class="pager">
            <div class="pmeta">第 {{ page + 1 }} / {{ pages || 1 }} 页，共 {{ total }} 条</div>
            <div style="display: flex; gap: 8px">
              <button class="btn" :disabled="loading || page <= 0" @click="search(page - 1)">上一页</button>
              <button class="btn" :disabled="loading || pages === 0 || page >= pages - 1" @click="search(page + 1)">下一页</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminUserResetPassword, adminUserSetEnabled, adminUsers } from '../api'

const router = useRouter()
const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const pages = ref(0)
const total = ref(0)
const size = ref(20)

function fmtEnd(v) {
  if (!v) return '-'
  return String(v)
}

function viewPlan(u) {
  if (!u || !u.id) return
  router.push({ path: '/subscriptions', query: { userId: String(u.id) } })
}

function viewUsage(u) {
  if (!u || !u.id) return
  router.push({ path: '/user-stats', query: { userId: String(u.id), tab: 'usage' } })
}

function viewSubscriptions(u) {
  if (!u || !u.id) return
  router.push({ path: '/user-stats', query: { userId: String(u.id), tab: 'subscriptions' } })
}

async function search(p) {
  error.value = ''
  loading.value = true
  try {
    const r = await adminUsers({ keyword: keyword.value, page: p, size: size.value })
    if (!r || !r.success) throw new Error(r?.message || '查询失败')
    rows.value = r.data || []
    page.value = r.page || 0
    pages.value = r.pages || 0
    total.value = r.total || 0
    size.value = r.size || 20
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

async function toggle(u) {
  if (!u || !u.id) return
  error.value = ''
  try {
    await adminUserSetEnabled(u.id, !u.enabled)
    await search(page.value)
  } catch (e) {
    error.value = e?.message || String(e)
  }
}

async function resetPwd(u) {
  if (!u || !u.id) return
  error.value = ''
  try {
    const ok = window.confirm(`确定重置用户【${u.username}】的密码吗？`)
    if (!ok) return
    const r = await adminUserResetPassword(u.id)
    if (!r || !r.success) throw new Error(r?.message || '重置失败')
    window.alert(`新密码：${r.newPassword}`)
  } catch (e) {
    error.value = e?.message || String(e)
  }
}

onMounted(() => search(0))
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
  grid-template-columns: 1fr 2fr;
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
  margin-bottom: 10px;
}
.placeholder {
  font-size: 13px;
  color: rgba(226, 232, 240, 0.9);
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
</style>
