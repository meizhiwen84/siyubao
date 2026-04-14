<template>
  <div class="card box">
    <div class="head">
      <div class="title">支付订单管理</div>
      <div class="meta">支持：查询、批准、拒绝</div>
    </div>
    <div class="body">
      <div class="grid">
        <div class="card sub">
          <div class="sub-title">搜索</div>
          <div class="row" style="display: flex; gap: 8px; align-items: center;">
            <select v-model="statusFilter" class="input" style="width:120px; flex:0 0 auto;">
              <option value="">全部状态</option>
              <option value="PENDING">待处理</option>
              <option value="APPROVED">已批准</option>
              <option value="REJECTED">已拒绝</option>
            </select>
            <input v-model="keyword" class="input" placeholder="搜索订单号/交易号" style="flex:1;" />
            <button class="btn" :disabled="loading" @click="loadOrders">{{ loading ? '查询中…' : '查询' }}</button>
          </div>
        </div>
        <div class="card sub">
          <div class="sub-title">列表</div>
          <div v-if="error" class="err">{{ error }}</div>
          <div class="table">
            <table class="tbl">
              <thead>
                <tr>
                  <th>订单号</th>
                  <th>用户</th>
                  <th>套餐</th>
                  <th>金额</th>
                  <th>平台</th>
                  <th>交易号</th>
                  <th>收款码</th>
                  <th>状态</th>
                  <th>时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="o in orders" :key="o.id">
                  <td class="mono">{{ o.orderNo }}</td>
                  <td>{{ o.user?.username || '-' }}</td>
                  <td>{{ o.plan?.name || '-' }}</td>
                  <td>{{ formatPrice(o.amountCents) }}</td>
                  <td>{{ formatPlatform(o.platform) }}</td>
                  <td class="mono">{{ o.transactionId }}</td>
                  <td>
                    <template v-if="o.qrCodeName || o.qrCodeUrl">
                      <div v-if="o.qrCodeName" class="text-xs text-gray-300">{{ o.qrCodeName }}</div>
                      <a v-if="o.qrCodeUrl" :href="o.qrCodeUrl" target="_blank" class="text-xs text-blue-400 hover:text-blue-300">查看图片</a>
                    </template>
                    <template v-else>-</template>
                  </td>
                  <td>
                    <span :class="['status-badge', o.status.toLowerCase()]">{{ formatStatus(o.status) }}</span>
                  </td>
                  <td class="mono">{{ formatTime(o.createTime) }}</td>
                  <td>
                    <div style="display: flex; gap: 8px;">
                      <template v-if="o.status === 'PENDING'">
                        <button class="btn" @click="approve(o)">批准</button>
                        <button class="btn" @click="showRejectDialog(o)">拒绝</button>
                      </template>
                      <template v-else>
                        -
                      </template>
                    </div>
                  </td>
                </tr>
                <tr v-if="!loading && orders.length === 0">
                  <td colspan="10" class="placeholder">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="pager" v-if="pages > 1">
            <div class="pmeta">第 {{ page + 1 }} / {{ pages || 1 }} 页</div>
            <div style="display: flex; gap: 8px;">
              <button class="btn" :disabled="loading || page <= 0" @click="page--; loadOrders()">上一页</button>
              <button class="btn" :disabled="loading || pages === 0 || page >= pages - 1" @click="page++; loadOrders()">下一页</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="rejectDialogOpen" class="modal-overlay" @click.self="rejectDialogOpen = false">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title">拒绝订单</div>
          <button class="btn" @click="rejectDialogOpen = false">关闭</button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label>拒绝原因（可选）</label>
            <textarea v-model="rejectRemark" class="input" rows="3" placeholder="输入拒绝原因..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn" @click="rejectDialogOpen = false">取消</button>
          <button class="btn" @click="doReject">确认拒绝</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminOrders, adminOrderApprove, adminOrderReject } from '../api'

const loading = ref(false)
const error = ref('')
const orders = ref([])
const page = ref(0)
const pages = ref(1)
const size = 20
const statusFilter = ref('')
const keyword = ref('')

const rejectDialogOpen = ref(false)
const rejectOrder = ref(null)
const rejectRemark = ref('')

function formatPrice(cents) {
  const n = Number(cents || 0)
  if (n <= 0) return '免费'
  return `￥${(n / 100).toFixed(2)}`
}

function formatPlatform(p) {
  if (p === 'wechat') return '微信'
  if (p === 'alipay') return '支付宝'
  return p || '-'
}

function formatStatus(s) {
  if (s === 'PENDING') return '待处理'
  if (s === 'APPROVED') return '已批准'
  if (s === 'REJECTED') return '已拒绝'
  return s || '-'
}

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}

async function loadOrders() {
  error.value = ''
  loading.value = true
  try {
    const r = await adminOrders({ 
      status: statusFilter.value, 
      keyword: keyword.value, 
      page: page.value, 
      size: size 
    })
    orders.value = Array.isArray(r.data) ? r.data : []
    pages.value = Number(r.pages || 1)
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

async function approve(o) {
  if (!confirm(`确定批准订单 ${o.orderNo}？`)) return
  try {
    await adminOrderApprove(o.id)
    await loadOrders()
  } catch (e) {
    alert(e?.message || String(e))
  }
}

function showRejectDialog(o) {
  rejectOrder.value = o
  rejectRemark.value = ''
  rejectDialogOpen.value = true
}

async function doReject() {
  if (!rejectOrder.value) return
  try {
    await adminOrderReject(rejectOrder.value.id, rejectRemark.value)
    rejectDialogOpen.value = false
    await loadOrders()
  } catch (e) {
    alert(e?.message || String(e))
  }
}

onMounted(loadOrders)
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
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
}
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}
.status-badge.pending {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
}
.status-badge.approved {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.status-badge.rejected {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
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
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 100;
}
.modal {
  background: rgba(15, 23, 42, 0.98);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
}
.modal-header {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.modal-title {
  font-weight: 700;
}
.modal-body {
  padding: 16px;
}
.modal-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 13px;
  color: rgba(148, 163, 184, 0.9);
}
</style>
