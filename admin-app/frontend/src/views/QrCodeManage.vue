<template>
  <div class="card box">
    <div class="head">
      <div class="title">收款码管理</div>
      <div class="meta">支持：微信/支付宝收款码，随机返回给用户</div>
      <button class="btn" @click="showCreateDialog">添加收款码</button>
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

      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="filteredList.length === 0" class="placeholder">
        暂无{{ currentTab === 'wechat' ? '微信' : '支付宝' }}收款码
      </div>
      <div v-else class="grid-cards">
        <div v-for="item in filteredList" :key="item.id" class="card-item">
          <div class="card-item-header">
            <div class="card-item-title">{{ item.name }}</div>
            <div class="card-item-actions">
              <button class="btn" @click="toggleEnabled(item)">
                {{ item.enabled ? '停用' : '启用' }}
              </button>
              <button class="btn" @click="showEditDialog(item)">编辑</button>
              <button class="btn" @click="deleteItem(item)">删除</button>
            </div>
          </div>
          <div class="card-item-body">
            <img :src="item.imageUrl" :alt="item.name" class="qr-image" />
          </div>
          <div class="card-item-footer">
            <span :class="['status', item.enabled ? 'active' : 'inactive']">
              {{ item.enabled ? '生效中' : '已停用' }}
            </span>
            <span class="sort-order">排序：{{ item.sortOrder }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="dialogOpen" class="modal-overlay" @click.self="dialogOpen = false">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title">{{ editingItem ? '编辑收款码' : '添加收款码' }}</div>
          <button class="btn" @click="dialogOpen = false">关闭</button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label>平台</label>
            <select v-model="form.platform" class="input" :disabled="!!editingItem">
              <option value="wechat">微信</option>
              <option value="alipay">支付宝</option>
            </select>
          </div>
          <div class="field">
            <label>名称</label>
            <input v-model="form.name" class="input" placeholder="例如：微信收款码-1" />
          </div>
          <div class="field">
            <label>上传图片</label>
            <input ref="fileInput" type="file" accept="image/*" @change="handleFileChange" class="input" />
            <div v-if="uploading" class="hint">上传中…</div>
          </div>
          <div class="field">
            <label>图片 URL</label>
            <input v-model="form.imageUrl" class="input" placeholder="https://example.com/qrcode.png" />
          </div>
          <div class="field">
            <label>排序</label>
            <input v-model.number="form.sortOrder" type="number" class="input" placeholder="数字越小越靠前" />
          </div>
          <div class="field">
            <label class="checkbox-label">
              <input type="checkbox" v-model="form.enabled" />
              立即启用
            </label>
          </div>
          <div v-if="form.imageUrl" class="preview">
            <div class="preview-label">预览</div>
            <img :src="form.imageUrl" class="preview-image" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn" @click="dialogOpen = false">取消</button>
          <button class="btn" :class="{ primary: true }" :disabled="saving" @click="saveItem">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { adminQrCodesList, adminQrCodeCreate, adminQrCodeUpdate, adminQrCodeDelete, adminUpload } from '../api'

const tabs = [
  { key: 'wechat', label: '微信' },
  { key: 'alipay', label: '支付宝' }
]

const currentTab = ref('wechat')
const loading = ref(false)
const uploading = ref(false)
const error = ref('')
const list = ref([])
const dialogOpen = ref(false)
const editingItem = ref(null)
const saving = ref(false)
const form = ref({
  platform: 'wechat',
  name: '',
  imageUrl: '',
  sortOrder: 0,
  enabled: true
})
const fileInput = ref(null)

const filteredList = computed(() => {
  return list.value.filter(x => x.platform === currentTab.value)
})

async function loadList() {
  error.value = ''
  loading.value = true
  try {
    const r = await adminQrCodesList()
    if (!r || !r.success) throw new Error(r?.message || '加载失败')
    list.value = Array.isArray(r.data) ? r.data : []
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  editingItem.value = null
  form.value = {
    platform: currentTab.value,
    name: '',
    imageUrl: '',
    sortOrder: 0,
    enabled: true
  }
  dialogOpen.value = true
}

function showEditDialog(item) {
  editingItem.value = item
  form.value = {
    platform: item.platform,
    name: item.name,
    imageUrl: item.imageUrl,
    sortOrder: item.sortOrder,
    enabled: item.enabled
  }
  dialogOpen.value = true
}

async function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  
  uploading.value = true
  error.value = ''
  try {
    const r = await adminUpload(file)
    if (!r || !r.success) throw new Error(r?.message || '上传失败')
    form.value.imageUrl = r.data.url
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    uploading.value = false
  }
}

async function saveItem() {
  if (!form.value.name.trim() || !form.value.imageUrl.trim()) {
    alert('请填写完整信息')
    return
  }
  saving.value = true
  try {
    if (editingItem.value) {
      await adminQrCodeUpdate(editingItem.value.id, form.value)
    } else {
      await adminQrCodeCreate(form.value)
    }
    dialogOpen.value = false
    await loadList()
  } catch (e) {
    alert(e?.message || String(e))
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(item) {
  try {
    await adminQrCodeUpdate(item.id, { enabled: !item.enabled })
    await loadList()
  } catch (e) {
    alert(e?.message || String(e))
  }
}

async function deleteItem(item) {
  const ok = window.confirm(`确定删除收款码【${item.name}】吗？`)
  if (!ok) return
  try {
    await adminQrCodeDelete(item.id)
    await loadList()
  } catch (e) {
    alert(e?.message || String(e))
  }
}

onMounted(loadList)
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
  flex-wrap: wrap;
}
.title {
  font-weight: 800;
}
.meta {
  flex: 1;
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
.grid-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}
.card-item {
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  overflow: hidden;
}
.card-item-header {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.card-item-title {
  font-weight: 600;
  font-size: 14px;
}
.card-item-actions {
  display: flex;
  gap: 6px;
}
.card-item-body {
  padding: 12px;
  display: flex;
  justify-content: center;
}
.qr-image {
  width: 180px;
  height: 180px;
  border-radius: 8px;
  object-fit: cover;
  background: rgba(15, 23, 42, 0.8);
}
.card-item-footer {
  padding: 8px 12px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}
.status {
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 600;
}
.status.active {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
}
.status.inactive {
  background: rgba(148, 163, 184, 0.15);
  color: rgba(148, 163, 184, 0.8);
}
.sort-order {
  color: rgba(148, 163, 184, 0.7);
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
  max-width: 500px;
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
  margin-bottom: 12px;
}
.field label {
  font-size: 13px;
  color: rgba(148, 163, 184, 0.9);
}
.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.preview {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}
.preview-label {
  font-size: 13px;
  color: rgba(148, 163, 184, 0.9);
  margin-bottom: 8px;
}
.preview-image {
  width: 160px;
  height: 160px;
  border-radius: 8px;
  object-fit: cover;
  background: rgba(15, 23, 42, 0.8);
}
</style>
