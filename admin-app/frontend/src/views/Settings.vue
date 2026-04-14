<template>
  <div class="card box">
    <div class="head">
      <div class="title">系统设置</div>
      <div class="meta">注册防刷参数、企业微信通知可配置</div>
    </div>
    <div class="body">
      <div class="card sub">
        <div class="sub-title">注册防刷配置</div>
        <div v-if="error" class="err">{{ error }}</div>
        <div v-if="ok" class="ok">保存成功</div>

        <div class="grid">
          <div class="row">
            <div class="label">同IP最小间隔(秒)</div>
            <input v-model.number="form.minuteIntervalSeconds" class="input" type="number" min="1" />
          </div>
          <div class="row">
            <div class="label">同IP每日上限(次)</div>
            <input v-model.number="form.dayLimit" class="input" type="number" min="1" />
          </div>
        </div>
      </div>

      <div class="card sub" style="margin-top:12px">
        <div class="sub-title">支付通知配置</div>

        <div class="grid" style="grid-template-columns: 1fr">
          <div class="row">
            <div class="label">企业微信 Webhook URL</div>
            <input v-model="form.wecomWebhookUrl" class="input" type="text" placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..." />
            <div class="hint">用于接收新订单通知</div>
          </div>
          <div class="row">
            <div class="label">管理端基础 URL</div>
            <input v-model="form.adminBaseUrl" class="input" type="text" placeholder="http://yourdomain.com/admin" />
            <div class="hint">用于生成订单处理链接和上传图片的完整 URL（包含 /admin）</div>
          </div>
          <div class="row">
            <div class="hint" style="color: rgba(59, 130, 246, 0.9);">
              💡 收款码管理已移至"收款码管理"页面，支持添加多个二维码并随机返回给用户
            </div>
          </div>
        </div>

        <div class="actions" style="margin-top:12px">
          <button class="btn" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
          <button class="btn primary" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { adminSettingsGet, adminSettingsUpdate } from '../api'

const loading = ref(false)
const saving = ref(false)
const error = ref('')
const ok = ref(false)

const form = ref({
  minuteIntervalSeconds: 60,
  dayLimit: 3,
  wecomWebhookUrl: '',
  adminBaseUrl: ''
})

async function load() {
  error.value = ''
  ok.value = false
  loading.value = true
  try {
    const r = await adminSettingsGet()
    if (!r || !r.success) throw new Error(r?.message || '加载失败')
    const d = r.data || {}
    form.value = {
      minuteIntervalSeconds: Number(d.minuteIntervalSeconds ?? 60),
      dayLimit: Number(d.dayLimit ?? 3),
      wecomWebhookUrl: d.wecomWebhookUrl || '',
      adminBaseUrl: d.adminBaseUrl || ''
    }
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

async function save() {
  error.value = ''
  ok.value = false
  saving.value = true
  try {
    const payload = { ...form.value }
    const r = await adminSettingsUpdate(payload)
    if (!r || !r.success) throw new Error(r?.message || '保存失败')
    ok.value = true
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    saving.value = false
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

.err {
  border: 1px solid rgba(248, 113, 113, 0.35);
  background: rgba(127, 29, 29, 0.28);
  color: rgba(254, 226, 226, 0.95);
  padding: 10px 12px;
  border-radius: 12px;
  margin-bottom: 12px;
  font-size: 13px;
}

.ok {
  border: 1px solid rgba(34, 197, 94, 0.35);
  background: rgba(20, 83, 45, 0.25);
  color: rgba(220, 252, 231, 0.95);
  padding: 10px 12px;
  border-radius: 12px;
  margin-bottom: 12px;
  font-size: 13px;
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
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

.actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.hint {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.7);
  margin-top: 4px;
}
</style>
