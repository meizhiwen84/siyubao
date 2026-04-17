<template>
  <div class="page">
    <div class="page-header">
      <h2>用户反馈管理</h2>
    </div>
    <div class="card">
      <div class="card-header">
        <h3>反馈列表</h3>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <template v-else>
        <table class="tbl">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户编号</th>
              <th>用户名</th>
              <th>反馈内容</th>
              <th>联系方式</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="f in feedbacks" :key="f.id">
              <td>{{ f.id }}</td>
              <td class="mono">{{ f.userNo || '-' }}</td>
              <td>{{ f.username || '-' }}</td>
              <td class="max-w-md break-words">{{ f.content }}</td>
              <td>{{ f.contact || '-' }}</td>
              <td class="mono">{{ formatTime(f.createTime) }}</td>
            </tr>
            <tr v-if="feedbacks.length === 0">
              <td colspan="6" class="placeholder">暂无数据</td>
            </tr>
          </tbody>
        </table>
        <div v-if="total > 0" class="pagination">
          <button class="btn" :disabled="page === 0" @click="load(page - 1)">上一页</button>
          <span>第 {{ page + 1 }} / {{ pages }} 页，共 {{ total }} 条</span>
          <button class="btn" :disabled="page + 1 >= pages" @click="load(page + 1)">下一页</button>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const loading = ref(false)
const feedbacks = ref([])
const page = ref(0)
const size = ref(20)
const total = ref(0)
const pages = ref(0)

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

async function load(p = 0) {
  loading.value = true
  try {
    const r = await api('GET', `/api/admin/feedbacks/list?page=${p}&size=${size.value}`)
    feedbacks.value = r.data || []
    total.value = r.total || 0
    pages.value = r.pages || 0
    page.value = p
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page {
  padding: 24px;
}
.page-header {
  margin-bottom: 16px;
}
.card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}
.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}
.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.tbl thead {
  background: #f9fafb;
}
.tbl th,
.tbl td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}
.tbl th {
  font-weight: 600;
  color: #374151;
}
.placeholder {
  color: #6b7280;
  text-align: center;
}
.loading {
  padding: 48px;
  text-align: center;
  color: #6b7280;
}
.pagination {
  padding: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
}
.pagination span {
  color: #374151;
}
.btn {
  border: 1px solid #d1d5db;
  background: white;
  color: #111827;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.btn:hover:not(:disabled) {
  background: #f3f4f6;
}
.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}
.max-w-md {
  max-width: 448px;
}
.break-words {
  word-break: break-word;
}
</style>
