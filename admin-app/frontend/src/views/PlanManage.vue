<template>
  <div class="card box">
    <div class="head">
      <div class="title">会员套餐管理</div>
      <div style="display: flex; gap: 10px; align-items: center">
        <button class="btn" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
        <button class="btn primary" @click="openCreate">新增套餐</button>
      </div>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <div class="list card">
      <table class="tbl">
        <thead>
          <tr>
            <th>code</th>
            <th>名称</th>
            <th>价格(分)</th>
            <th>时长(天)</th>
            <th>设备数</th>
            <th>每日上限</th>
            <th>水印</th>
            <th>启用</th>
            <th>排序</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in rows" :key="p.id || p.code">
            <td>{{ p.code }}</td>
            <td>{{ p.name }}</td>
            <td>{{ fmtPrice(p.priceCents) }}</td>
            <td>{{ p.durationDays }}</td>
            <td>{{ p.deviceLimit }}</td>
            <td>{{ p.dailyFreeLimit }}</td>
            <td>{{ p.watermark ? '是' : '否' }}</td>
            <td>{{ p.enabled ? '是' : '否' }}</td>
            <td>{{ p.sortOrder }}</td>
            <td>
              <div style="display: flex; gap: 8px">
                <button class="btn" @click="openEdit(p)">编辑</button>
                <button class="btn" @click="toggle(p)">{{ p.enabled ? '停用' : '启用' }}</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!loading && rows.length === 0" class="empty">暂无数据</div>
    </div>

    <div class="hint">支持：新增/编辑/启用停用/排序</div>
  </div>

  <div v-if="modalOpen" class="mask">
    <div class="card modal">
      <div class="mhead">
        <div class="mtitle">{{ editingId ? '编辑套餐' : '新增套餐' }}</div>
        <button class="btn" @click="closeModal">关闭</button>
      </div>
      <div v-if="modalError" class="error">{{ modalError }}</div>
      <div class="grid">
        <div class="row">
          <div class="label">code</div>
          <input v-model.trim="form.code" class="input" :disabled="!!editingId" placeholder="例如 P_MONTH" />
        </div>
        <div class="row">
          <div class="label">名称</div>
          <input v-model.trim="form.name" class="input" placeholder="例如 个人会员·月卡" />
        </div>
        <div class="row">
          <div class="label">价格(分)</div>
          <input v-model.number="form.priceCents" class="input" type="number" min="0" />
        </div>
        <div class="row">
          <div class="label">时长(天)</div>
          <input v-model.number="form.durationDays" class="input" type="number" min="0" />
        </div>
        <div class="row">
          <div class="label">设备数</div>
          <input v-model.number="form.deviceLimit" class="input" type="number" min="1" />
        </div>
        <div class="row">
          <div class="label">每日上限</div>
          <input v-model.number="form.dailyFreeLimit" class="input" type="number" min="0" />
        </div>
        <div class="row">
          <div class="label">水印</div>
          <select v-model="form.watermark" class="input">
            <option :value="true">是</option>
            <option :value="false">否</option>
          </select>
        </div>
        <div class="row">
          <div class="label">启用</div>
          <select v-model="form.enabled" class="input">
            <option :value="true">是</option>
            <option :value="false">否</option>
          </select>
        </div>
        <div class="row">
          <div class="label">排序</div>
          <input v-model.number="form.sortOrder" class="input" type="number" min="0" />
        </div>
      </div>
      <div class="mactions">
        <button class="btn primary" :disabled="modalSaving" @click="save">{{ modalSaving ? '保存中…' : '保存' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminPlanCreate, adminPlanUpdate, adminPlans } from '../api'

const rows = ref([])
const loading = ref(false)
const error = ref('')

async function load() {
  error.value = ''
  loading.value = true
  try {
    const r = await adminPlans()
    rows.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)

function fmtPrice(cents) {
  const n = Number(cents || 0)
  return (n / 100).toFixed(2) + '元'
}

const modalOpen = ref(false)
const modalSaving = ref(false)
const modalError = ref('')
const editingId = ref(null)
const form = ref({
  code: '',
  name: '',
  priceCents: 0,
  durationDays: 0,
  deviceLimit: 1,
  dailyFreeLimit: 0,
  watermark: false,
  enabled: true,
  sortOrder: 0
})

function openCreate() {
  editingId.value = null
  form.value = {
    code: '',
    name: '',
    priceCents: 0,
    durationDays: 0,
    deviceLimit: 1,
    dailyFreeLimit: 0,
    watermark: false,
    enabled: true,
    sortOrder: 0
  }
  modalError.value = ''
  modalOpen.value = true
}

function openEdit(p) {
  if (!p) return
  editingId.value = p.id
  form.value = {
    code: p.code || '',
    name: p.name || '',
    priceCents: Number(p.priceCents || 0),
    durationDays: Number(p.durationDays || 0),
    deviceLimit: Number(p.deviceLimit || 1),
    dailyFreeLimit: Number(p.dailyFreeLimit || 0),
    watermark: !!p.watermark,
    enabled: !!p.enabled,
    sortOrder: Number(p.sortOrder || 0)
  }
  modalError.value = ''
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
  modalSaving.value = false
  modalError.value = ''
}

async function save() {
  modalError.value = ''
  modalSaving.value = true
  try {
    const payload = { ...form.value }
    if (editingId.value) {
      await adminPlanUpdate(editingId.value, payload)
    } else {
      await adminPlanCreate(payload)
    }
    closeModal()
    await load()
  } catch (e) {
    modalError.value = e?.message || String(e)
  } finally {
    modalSaving.value = false
  }
}

async function toggle(p) {
  if (!p || !p.id) return
  try {
    await adminPlanUpdate(p.id, { enabled: !p.enabled })
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  }
}
</script>

<style scoped>
.box {
  padding: 14px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}
.title {
  font-weight: 800;
}
.error {
  margin-top: 12px;
  border: 1px solid rgba(248, 113, 113, 0.35);
  background: rgba(127, 29, 29, 0.28);
  color: rgba(254, 226, 226, 0.95);
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 13px;
}
.list {
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
  padding: 14px;
  color: rgba(148, 163, 184, 0.95);
}
.hint {
  margin-top: 12px;
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
  max-width: 880px;
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

.grid {
  padding-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
}

.mactions {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}
</style>
