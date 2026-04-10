<template>
  <div class="card box">
    <div class="head">
      <div class="title">操作日志</div>
      <div class="meta">支持：筛选与分页</div>
    </div>
    <div class="body">
      <div v-if="error" class="err">{{ error }}</div>

      <div class="filters card">
        <div class="row">
          <div class="label">关键字</div>
          <input v-model.trim="keyword" class="input" placeholder="action / 管理员 / targetType / targetId" />
        </div>
        <div style="display: flex; gap: 10px; align-items: end">
          <button class="btn" :disabled="loading" @click="search(0)">{{ loading ? '查询中…' : '查询' }}</button>
          <select v-model.number="size" class="input" style="width: 120px" :disabled="loading" @change="search(0)">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </div>
      </div>

      <div class="table card">
        <table class="tbl">
          <thead>
            <tr>
              <th>时间</th>
              <th>管理员</th>
              <th>action</th>
              <th>target</th>
              <th>IP</th>
              <th>detail</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="l in rows" :key="l.id">
              <td>{{ fmt(l.createTime) }}</td>
              <td>{{ l.adminUsername }}（{{ l.adminUserId }}）</td>
              <td>{{ l.action }}</td>
              <td>{{ (l.targetType || '-') + ':' + (l.targetId || '-') }}</td>
              <td>{{ l.ip || '' }}</td>
              <td class="detail">
                <button class="btn" @click="openDetail(l)">查看</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!loading && rows.length === 0" class="empty">暂无数据</div>
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

  <div v-if="detailOpen" class="mask">
    <div class="card modal">
      <div class="mhead">
        <div class="mtitle">detail</div>
        <button class="btn" @click="closeDetail">关闭</button>
      </div>
      <pre class="pre">{{ detailText }}</pre>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminLogs } from '../api'

const keyword = ref('')
const rows = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const pages = ref(0)
const total = ref(0)
const size = ref(20)

const detailOpen = ref(false)
const detailText = ref('')

function fmt(v) {
  if (!v) return ''
  return String(v)
}

async function search(p) {
  error.value = ''
  loading.value = true
  try {
    const r = await adminLogs({ keyword: keyword.value, page: p, size: size.value })
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

function openDetail(l) {
  detailOpen.value = true
  detailText.value = l && l.detailJson ? String(l.detailJson) : ''
}

function closeDetail() {
  detailOpen.value = false
  detailText.value = ''
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

.err {
  border: 1px solid rgba(248, 113, 113, 0.35);
  background: rgba(127, 29, 29, 0.28);
  color: rgba(254, 226, 226, 0.95);
  padding: 10px 12px;
  border-radius: 12px;
  margin-bottom: 12px;
  font-size: 13px;
}

.filters {
  padding: 12px;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.row {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
}

.table {
  margin-top: 12px;
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

.empty {
  padding: 12px;
  color: rgba(148, 163, 184, 0.95);
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

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.modal {
  width: 100%;
  max-width: 900px;
  padding: 14px;
}

.mhead {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.mtitle {
  font-weight: 800;
}

.pre {
  margin: 0;
  padding-top: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.92);
}
</style>
