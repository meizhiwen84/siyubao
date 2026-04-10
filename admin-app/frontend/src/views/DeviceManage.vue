<template>
  <div class="card box">
    <div class="head">
      <div class="title">设备在线管理</div>
      <div class="meta">支持：按用户查询、踢下线</div>
    </div>
    <div class="body">
      <div class="card sub">
        <div class="sub-title">查询</div>
        <div class="row">
          <input v-model.trim="keyword" class="input" placeholder="用户ID 或 用户名关键字" />
        </div>
        <div style="display: flex; gap: 10px">
          <button class="btn" :disabled="loading" @click="load">{{ loading ? '查询中…' : '查询' }}</button>
          <button class="btn" :disabled="loading || !selectedUserId" @click="revokeAllUser">
            全部踢下线
          </button>
        </div>
      </div>
      <div class="card sub" style="margin-top: 12px">
        <div class="sub-title">列表</div>
        <div v-if="error" class="err">{{ error }}</div>
        <div class="table">
          <table class="tbl">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户</th>
                <th>deviceId</th>
                <th>lastSeen</th>
                <th>revoked</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in rows" :key="s.id">
                <td>{{ s.id }}</td>
                <td>{{ s.username || s.userId }}</td>
                <td>{{ s.deviceId }}</td>
                <td>{{ fmt(s.lastSeenTime) }}</td>
                <td>{{ s.revoked ? '是' : '否' }}</td>
                <td>
                  <div style="display: flex; gap: 8px">
                    <button class="btn" :disabled="s.revoked || revokingId === s.id" @click="revoke(s)">
                      {{ revokingId === s.id ? '处理中…' : '踢下线' }}
                    </button>
                    <button class="btn" :disabled="s.revoked || revokingId === s.id || !s.userId" @click="revokeOthers(s)">
                      踢除其它设备
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!loading && rows.length === 0" class="placeholder">暂无数据</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminDeviceRevoke, adminDevices, adminDevicesRevokeAll, adminDevicesRevokeOthers } from '../api'

const keyword = ref('')
const loading = ref(false)
const error = ref('')
const rows = ref([])
const revokingId = ref(null)
const selectedUserId = ref(null)

function parseUserId(s) {
  const t = String(s || '').trim()
  if (!t) return null
  const n = Number(t)
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : null
}

function fmt(v) {
  if (!v) return ''
  return String(v)
}

async function load() {
  error.value = ''
  loading.value = true
  try {
    const userId = parseUserId(keyword.value)
    selectedUserId.value = userId
    const r = await adminDevices({ userId, keyword: userId ? '' : keyword.value })
    if (!r || !r.success) throw new Error(r?.message || '查询失败')
    rows.value = r.data || []
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

async function revoke(s) {
  if (!s || !s.id) return
  error.value = ''
  revokingId.value = s.id
  try {
    const r = await adminDeviceRevoke(s.id)
    if (!r || !r.success) throw new Error(r?.message || '操作失败')
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    revokingId.value = null
  }
}

async function revokeOthers(s) {
  if (!s || !s.id || !s.userId) return
  error.value = ''
  revokingId.value = s.id
  try {
    const r = await adminDevicesRevokeOthers({ userId: s.userId, keepId: s.id })
    if (!r || !r.success) throw new Error(r?.message || '操作失败')
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    revokingId.value = null
  }
}

async function revokeAllUser() {
  if (!selectedUserId.value) return
  error.value = ''
  loading.value = true
  try {
    const r = await adminDevicesRevokeAll({ userId: selectedUserId.value })
    if (!r || !r.success) throw new Error(r?.message || '操作失败')
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
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
</style>
