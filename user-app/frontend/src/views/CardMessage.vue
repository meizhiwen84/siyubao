<template>
  <div class="bg-white border border-gray-200 rounded-lg">
    <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
      <div class="font-medium text-gray-800">对话内容管理</div>
      <div class="text-xs text-gray-600">共 {{ total }} 条</div>
    </div>
    <div class="p-4 space-y-3">
      <div v-if="error" class="text-sm text-red-600">{{ error }}</div>
      <form class="grid grid-cols-1 md:grid-cols-4 gap-3" @submit.prevent="search(0)">
        <div>
          <div class="text-sm text-gray-700 mb-1">线路</div>
          <select v-model="filters.line" class="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white">
            <option value="">全部</option>
            <option v-for="(r, idx) in routes" :key="r?.id ?? r?.routeValue ?? idx" :value="r?.routeValue ?? ''">
              {{ r?.routeName ?? '' }}
            </option>
          </select>
        </div>
        <div>
          <div class="text-sm text-gray-700 mb-1">平台</div>
          <select v-model="filters.platform" class="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white">
            <option value="">全部</option>
            <option value="dy">dy</option>
            <option value="xhs">xhs</option>
            <option value="sph">sph</option>
          </select>
        </div>
        <div>
          <div class="text-sm text-gray-700 mb-1">手机号/微信</div>
          <input v-model.trim="filters.phone" class="w-full border border-gray-300 rounded px-3 py-2 text-sm" placeholder="关键字" />
        </div>
        <div class="flex items-end space-x-2">
          <button class="px-3 py-2 rounded bg-blue-600 hover:bg-blue-700 text-white text-sm" :disabled="loading">
            {{ loading ? '查询中...' : '查询' }}
          </button>
          <button type="button" class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm" @click="reset" :disabled="loading">
            重置
          </button>
          <select v-model.number="size" class="h-9 border border-gray-300 rounded px-2 text-sm bg-white" :disabled="loading" @change="search(0)">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </div>
      </form>

      <div class="border border-gray-200 rounded-lg overflow-hidden">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-left text-gray-600">
            <tr>
              <th class="py-2 px-3">时间</th>
              <th class="py-2 px-3">线路</th>
              <th class="py-2 px-3">平台</th>
              <th class="py-2 px-3">手机号/微信</th>
              <th class="py-2 px-3">用户</th>
              <th class="py-2 px-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in rows" :key="m.id" class="border-t border-gray-100">
              <td class="py-2 px-3 whitespace-nowrap">{{ m.createTime || '' }}</td>
              <td class="py-2 px-3">{{ m.line || '' }}</td>
              <td class="py-2 px-3">{{ m.platform || '' }}</td>
              <td class="py-2 px-3">
                <div class="flex items-center space-x-2">
                  <div>{{ m.phone || '' }}</div>
                  <button
                    v-if="m.phone"
                    class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200"
                    @click="copy(m.phone)"
                  >
                    复制
                  </button>
                </div>
              </td>
              <td class="py-2 px-3">
                <div class="flex items-center space-x-2">
                  <img v-if="normalizeUrl(m.userPic)" :src="normalizeUrl(m.userPic)" class="w-8 h-8 rounded object-cover border border-gray-200" />
                  <div class="text-gray-800">{{ m.userName || '' }}</div>
                </div>
              </td>
              <td class="py-2 px-3">
                <div class="flex items-center space-x-2">
                  <button class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200" @click="open(m)">详情</button>
                  <button class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200" @click="openPreview(m)">截图</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!loading && rows.length === 0" class="p-4 text-sm text-gray-600">暂无数据</div>
      </div>

      <div class="flex items-center justify-between">
        <div class="text-xs text-gray-600">第 {{ page + 1 }} / {{ pages || 1 }} 页</div>
        <div class="space-x-2">
          <button class="px-3 py-1.5 rounded bg-gray-100 hover:bg-gray-200 text-sm" :disabled="loading || page <= 0" @click="search(page - 1)">
            上一页
          </button>
          <button
            class="px-3 py-1.5 rounded bg-gray-100 hover:bg-gray-200 text-sm"
            :disabled="loading || pages === 0 || page >= pages - 1"
            @click="search(page + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </div>
  </div>

  <div v-if="modalOpen" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
    <div class="bg-white rounded-lg w-full max-w-3xl border border-gray-200">
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-medium text-gray-800">对话详情</div>
        <button class="text-gray-500 hover:text-gray-700" @click="close">关闭</button>
      </div>
      <div class="p-4">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
          <div class="border border-gray-200 rounded">
            <div class="px-3 py-2 border-b border-gray-200 text-xs text-gray-600">消息预览</div>
            <div class="p-3 space-y-2 max-h-[60vh] overflow-auto bg-gray-50">
              <div v-for="(m, idx) in parsedMessages" :key="idx" class="flex" :class="m.msgType === 2 ? 'justify-end' : 'justify-start'">
                <div
                  class="max-w-[80%] rounded-lg px-3 py-2 text-sm border"
                  :class="m.msgType === 2 ? 'bg-blue-50 border-blue-200 text-gray-800' : 'bg-white border-gray-200 text-gray-800'"
                >
                  <div class="text-[11px] text-gray-500 mb-1">{{ m.dateTimeStr || '' }}</div>
                  <img v-if="m.contentType === 2 && normalizeUrl(m.msg)" :src="normalizeUrl(m.msg)" class="max-w-full rounded border border-gray-200" />
                  <div v-else class="whitespace-pre-wrap break-words">{{ m.msg || '' }}</div>
                </div>
              </div>
            </div>
          </div>
          <div class="border border-gray-200 rounded">
            <div class="px-3 py-2 border-b border-gray-200 text-xs text-gray-600 flex items-center justify-between">
              <div>JSON</div>
              <button class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200" @click="copy(detailJson)" :disabled="!detailJson">复制 JSON</button>
            </div>
            <pre class="text-xs bg-gray-50 p-3 overflow-auto max-h-[60vh]">{{ prettyJson }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div v-if="previewOpen" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
    <div
      class="bg-white rounded-lg w-full border border-gray-200 overflow-hidden"
      :class="previewFullscreen ? 'fixed inset-0 z-50 m-0 rounded-none border-0' : 'max-w-4xl'"
    >
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-medium text-gray-800">聊天截图（可编辑）</div>
        <div class="flex items-center space-x-2">
          <button
            class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200 disabled:opacity-60"
            :disabled="!previewHtml"
            @click="togglePreviewFullscreen"
          >
            {{ previewFullscreen ? '退出全屏' : '全屏' }}
          </button>
          <button
            class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200 disabled:opacity-60"
            :disabled="previewGenerating || !previewDirty"
            @click="applyPreviewEdits"
          >
            应用编辑
          </button>
          <button class="text-gray-500 hover:text-gray-700" @click="closePreview">关闭</button>
        </div>
      </div>
      <div :class="previewFullscreen ? 'h-[calc(100vh-49px)]' : 'h-[75vh]'" class="bg-gray-50">
        <iframe
          ref="previewFrame"
          v-if="previewHtml"
          :srcdoc="previewHtml"
          class="w-full h-full"
          frameborder="0"
        ></iframe>
      </div>
      <input ref="uploadInput" type="file" accept="image/*" class="hidden" @change="onPickUploadFile" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

const loading = ref(false)
const error = ref('')

const filters = ref({
  line: '',
  platform: '',
  phone: ''
})

const routes = ref([])
const rows = ref([])
const total = ref(0)
const pages = ref(0)
const page = ref(0)
const size = ref(10)

const modalOpen = ref(false)
const detailJson = ref('')
const previewOpen = ref(false)
const previewHtml = ref('')
const previewFrame = ref(null)
const previewDirty = ref(false)
const previewEditedPayload = ref(null)
const previewGenerating = ref(false)
const previewMessageId = ref(null)
const previewFullscreen = ref(false)
const uploadInput = ref(null)
const pendingUpload = ref(null)

const parsedMessages = computed(() => {
  if (!detailJson.value) return []
  try {
    const arr = JSON.parse(detailJson.value)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
})

const prettyJson = computed(() => {
  if (!detailJson.value) return ''
  try {
    return JSON.stringify(JSON.parse(detailJson.value), null, 2)
  } catch {
    return detailJson.value
  }
})

function reset() {
  filters.value = { line: '', platform: '', phone: '' }
  search(0)
}

async function loadRoutes() {
  try {
    const resp = await window.SiyuBaoBackend.routes.list()
    if (resp && resp.success) {
      routes.value = (resp.data || []).filter(Boolean)
    }
  } catch {
  }
}

async function search(p) {
  error.value = ''
  loading.value = true
  try {
    const resp = await window.SiyuBaoBackend.history.list({
      line: filters.value.line,
      platform: filters.value.platform,
      phone: filters.value.phone,
      page: p,
      size: size.value
    })
    if (!resp.success) throw new Error(resp.message || '查询失败')
    rows.value = resp.data || []
    total.value = resp.total || 0
    pages.value = resp.pages || 0
    page.value = resp.page || 0
    size.value = resp.size || 10
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

function open(m) {
  modalOpen.value = true
  detailJson.value = m && m.chatMessage ? m.chatMessage : ''
}

function close() {
  modalOpen.value = false
  detailJson.value = ''
}

function openPreview(item) {
  previewOpen.value = true
  previewDirty.value = false
  previewEditedPayload.value = null
  previewMessageId.value = item && item.id != null ? item.id : null
  const msgs = safeParseMessages(item && item.chatMessage ? item.chatMessage : '')
  previewGenerating.value = true
  window.SiyuBaoBackend.chat
    .regenerate({
      xianlu: item && item.line ? String(item.line) : '',
      platform: item && item.platform ? String(item.platform) : '',
      xianshiname: '',
      userName: item && item.userName ? String(item.userName) : '',
      userAvatar: item && item.userPic ? String(normalizeUrl(item.userPic)) : '',
      myAvatar: '',
      topTime: '',
      messageId: item && item.id != null ? item.id : null,
      chatMessages: msgs,
      editable: true
    })
    .then((resp) => {
      if (!resp || !resp.success) throw new Error(resp?.message || '生成失败')
      previewHtml.value = resp.html || ''
    })
    .catch((e) => {
      error.value = e?.message || String(e)
      closePreview()
    })
    .finally(() => {
      previewGenerating.value = false
    })
}

function closePreview() {
  previewOpen.value = false
  previewHtml.value = ''
  previewDirty.value = false
  previewEditedPayload.value = null
  previewMessageId.value = null
  previewFullscreen.value = false
  pendingUpload.value = null
}

function togglePreviewFullscreen() {
  previewFullscreen.value = !previewFullscreen.value
}

function safeParseMessages(s) {
  if (!s) return []
  try {
    const v = JSON.parse(String(s))
    return Array.isArray(v) ? v : []
  } catch {
    return []
  }
}

async function applyPreviewEdits() {
  error.value = ''
  previewGenerating.value = true
  try {
    if (!previewEditedPayload.value) throw new Error('没有可应用的编辑')
    const p = previewEditedPayload.value
    const resp = await window.SiyuBaoBackend.chat.regenerate({
      xianlu: p.xianlu || '',
      platform: p.platform || '',
      xianshiname: '',
      userName: p.userName || '',
      userAvatar: p.userAvatar || '',
      myAvatar: p.myAvatar || '',
      topTime: p.topTime || '',
      messageId: previewMessageId.value,
      chatMessages: (p.messages || []).map((m) => ({
        contentType: m.contentType || 1,
        msgType: m.msgType || 1,
        dateTimeStr: m.dateTimeStr || '',
        showTime: !!m.showTime,
        msg: m.msg || '',
        userName: p.userName || ''
      })),
      editable: true
    })
    if (!resp || !resp.success) throw new Error(resp?.message || '应用失败')
    previewHtml.value = resp.html || ''
    previewDirty.value = false

    if (previewMessageId.value != null) {
      const idx = rows.value.findIndex((x) => x && x.id === previewMessageId.value)
      if (idx >= 0) {
        const updated = {
          ...rows.value[idx],
          userName: p.userName || rows.value[idx].userName,
          userPic: p.userAvatar || rows.value[idx].userPic,
          chatMessage: JSON.stringify(
            (p.messages || []).map((m) => ({
              contentType: m.contentType || 1,
              msgType: m.msgType || 1,
              dateTimeStr: m.dateTimeStr || '',
              showTime: !!m.showTime,
              msg: m.msg || '',
              userName: p.userName || ''
            }))
          )
        }
        rows.value.splice(idx, 1, updated)
      }
    }
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    previewGenerating.value = false
  }
}

function requestPickFile(payload) {
  if (!payload) return
  const { type, ...rest } = payload
  pendingUpload.value = rest
  if (uploadInput.value) uploadInput.value.click()
}

async function onPickUploadFile(evt) {
  const file = evt?.target?.files?.[0]
  evt.target.value = ''
  if (!file || !pendingUpload.value) return
  const req = pendingUpload.value
  pendingUpload.value = null
  try {
    let resp
    if (window.SiyuBaoBackend && window.SiyuBaoBackend.upload) {
      if (window.SiyuBaoBackend.isJcef) {
        const base64 = await readFileAsBase64(file)
        resp = await window.SiyuBaoBackend.upload.image({ filename: file.name, contentBase64: base64 })
      } else {
        resp = await window.SiyuBaoBackend.upload.image({ file })
      }
    } else {
      throw new Error('upload not ready')
    }
    if (!resp || !resp.success || !resp.url) throw new Error(resp?.message || '上传失败')
    const win = previewFrame.value?.contentWindow
    win?.postMessage({ ...req, type: 'siyubao-uploaded', url: resp.url }, '*')
  } catch (e) {
    error.value = e?.message || String(e)
  }
}

function readFileAsBase64(file) {
  return new Promise((resolve, reject) => {
    const r = new FileReader()
    r.onload = () => {
      const s = typeof r.result === 'string' ? r.result : ''
      const idx = s.indexOf('base64,')
      resolve(idx >= 0 ? s.slice(idx + 7) : '')
    }
    r.onerror = () => reject(new Error('读取文件失败'))
    r.readAsDataURL(file)
  })
}

async function copy(text) {
  const t = text == null ? '' : String(text)
  if (!t) return
  try {
    await navigator.clipboard.writeText(t)
  } catch {
    window.prompt('复制内容', t)
  }
}

function normalizeUrl(url) {
  if (url == null) return ''
  const t = String(url).trim()
  if (!t) return ''
  if (t.startsWith('http://') || t.startsWith('https://') || t.startsWith('data:') || t.startsWith('blob:') || t.startsWith('//')) return t
  if (t.startsWith('/')) return t
  if (t.startsWith('./')) return '/' + t.slice(2)
  return '/' + t
}

onMounted(async () => {
  await loadRoutes()
  await search(0)

  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') previewFullscreen.value = false
  })

  window.addEventListener('message', (evt) => {
    if (!previewOpen.value) return
    if (evt?.source !== previewFrame.value?.contentWindow) return
    const d = evt?.data
    if (d && d.type === 'siyubao-request-upload') {
      requestPickFile(d)
      return
    }
    if (!d || d.type !== 'siyubao-edit') return
    previewEditedPayload.value = {
      xianlu: d.xianlu,
      platform: d.platform,
      userAvatar: d.userAvatar,
      myAvatar: d.myAvatar,
      userName: d.userName,
      topTime: d.topTime,
      messages: d.messages || []
    }
    previewDirty.value = true
  })
})
</script>
