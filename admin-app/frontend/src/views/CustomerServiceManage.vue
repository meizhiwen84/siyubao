<template>
  <div class="page">
    <div class="page-header">
      <h2>客服信息管理</h2>
    </div>
    <div class="card">
      <h3>配置客服信息</h3>
      <div class="form-group">
        <label>客服微信二维码</label>
        <div class="flex gap-4 items-start">
          <div v-if="wechatQrCodeUrl" class="w-40">
            <img :src="wechatQrCodeUrl" alt="客服微信" class="w-full border border-gray-200 rounded-lg" />
          </div>
          <div class="flex-1">
            <input v-model="wechatQrCodeUrl" type="text" placeholder="输入微信二维码图片 URL 或上传图片" />
            <div class="mt-2">
              <input ref="wechatFileInput" type="file" accept="image/*" style="display: none" @change="handleWechatUpload" />
              <button class="btn" @click="uploadWechat">上传图片</button>
            </div>
          </div>
        </div>
      </div>
      <div class="form-group">
        <label>公众号二维码</label>
        <div class="flex gap-4 items-start">
          <div v-if="officialAccountQrCodeUrl" class="w-40">
            <img :src="officialAccountQrCodeUrl" alt="公众号二维码" class="w-full border border-gray-200 rounded-lg" />
          </div>
          <div class="flex-1">
            <input v-model="officialAccountQrCodeUrl" type="text" placeholder="输入公众号二维码图片 URL 或上传图片" />
            <div class="mt-2">
              <input ref="officialFileInput" type="file" accept="image/*" style="display: none" @change="handleOfficialUpload" />
              <button class="btn" @click="uploadOfficial">上传图片</button>
            </div>
          </div>
        </div>
      </div>
      <div class="form-group">
        <label>客服邮箱</label>
        <input v-model="email" type="email" placeholder="请输入客服邮箱" />
      </div>
      <div class="form-actions">
        <button class="btn primary" @click="save">保存配置</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminUpload, api } from '../api'

const wechatQrCodeUrl = ref('')
const officialAccountQrCodeUrl = ref('')
const email = ref('')
const wechatFileInput = ref(null)
const officialFileInput = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const r = await api('GET', '/api/customer-service/info')
    if (r.data) {
      wechatQrCodeUrl.value = r.data.wechatQrCodeUrl || ''
      officialAccountQrCodeUrl.value = r.data.officialAccountQrCodeUrl || ''
      email.value = r.data.email || ''
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function save() {
  try {
    await api('POST', '/api/admin/customer-service/update', {
      wechatQrCodeUrl: wechatQrCodeUrl.value,
      officialAccountQrCodeUrl: officialAccountQrCodeUrl.value,
      email: email.value
    })
    alert('保存成功！')
  } catch (e) {
    alert(e.message || '保存失败')
  }
}

function uploadWechat() {
  wechatFileInput.value?.click()
}

function handleWechatUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  doUpload(file, (url) => {
    wechatQrCodeUrl.value = url
  })
  e.target.value = ''
}

function uploadOfficial() {
  officialFileInput.value?.click()
}

function handleOfficialUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  doUpload(file, (url) => {
    officialAccountQrCodeUrl.value = url
  })
  e.target.value = ''
}

async function doUpload(file, onSuccess) {
  try {
    const r = await adminUpload(file)
    if (!r || !r.success) throw new Error(r?.message || '上传失败')
    onSuccess(r.data.url)
  } catch (e) {
    alert('上传失败: ' + (e.message || String(e)))
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page {
  padding: 24px;
  max-width: 900px;
}
.page-header {
  margin-bottom: 16px;
}
.card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
}
.card h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}
.form-group {
  margin-bottom: 16px;
}
.form-group label {
  display: block;
  font-size: 14px;
  color: #374151;
  margin-bottom: 6px;
  font-weight: 500;
}
input[type="text"],
input[type="email"],
select,
textarea {
  width: 100%;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 14px;
  outline: none;
  transition: border 0.15s, box-shadow 0.15s;
}
input:focus,
select:focus,
textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59,130,246,0.1);
}
.form-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
.btn {
  border: 1px solid #d1d5db;
  background: white;
  color: #111827;
  border-radius: 6px;
  padding: 8px 14px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.btn:hover { background: #f3f4f6; }
.btn.primary {
  border-color: #3b82f6;
  background: #3b82f6;
  color: white;
}
.btn.primary:hover { background: #2563eb; }
.btn.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.flex { display: flex; }
.gap-4 { gap: 16px; }
.items-start { align-items: flex-start; }
.flex-1 { flex: 1; }
.w-40 { width: 160px; }
.w-full { width: 100%; }
.mt-2 { margin-top: 8px; }
</style>
