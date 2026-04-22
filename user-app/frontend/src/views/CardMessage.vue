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
          <div class="text-sm text-gray-700 mb-1">业务名称</div>
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
              <th class="py-2 px-3">业务名称</th>
              <th class="py-2 px-3">平台</th>
              <th class="py-2 px-3">手机号/微信</th>
              <th class="py-2 px-3">用户</th>
              <th class="py-2 px-3">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in rows" :key="(m.id || '') + '_v' + (m._ev || 0)" class="border-t border-gray-100">
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
          @load="onPreviewLoad"
        ></iframe>
      </div>
      <input ref="uploadInput" type="file" accept="image/*" class="hidden" @change="onPickUploadFile" />
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'

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
const lastPreviewEditPayloadKey = ref('')
const suppressPreviewIncomingEditUntil = ref(0)
const previewApplyLock = ref(false)
const debugSeq = ref(0)
const appliedChatCache = ref({})

function debugLog(stage, extra) {
  const no = ++debugSeq.value
  const payload = extra == null ? '' : extra
  try {
    console.log(`[CardMessageDebug#${no}] ${new Date().toISOString()} ${stage}`, payload)
  } catch {
  }
}

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

const onPreviewLoad = () => {
    if (pendingRestoreScrollTop.value != null) {
      setPreviewScrollTop(pendingRestoreScrollTop.value)
      pendingRestoreScrollTop.value = null
    }
    suppressPreviewIncomingEditUntil.value = Date.now() + 5000
  }

  const openPreview = async (item) => {
    debugLog('openPreview:start', { id: item?.id, line: item?.line, platform: item?.platform })
    previewOpen.value = true
    previewDirty.value = false
    previewEditedPayload.value = null
    previewMessageId.value = item && item.id != null ? item.id : null

    var rawChatMsg = ''
    if (previewMessageId.value != null && appliedChatCache.value[previewMessageId.value]) {
      rawChatMsg = appliedChatCache.value[previewMessageId.value]
      debugLog('openPreview:use-cache', { id: previewMessageId.value })
    } else if (item && item.chatMessage) {
      rawChatMsg = item.chatMessage
    }

    const msgs = safeParseMessages(rawChatMsg)
    previewGenerating.value = true
    window.SiyuBaoBackend.chat
      .regenerate(JSON.parse(JSON.stringify({
        xianlu: item && item.line ? String(item.line) : '',
        platform: item && item.platform ? String(item.platform) : '',
        xianshiname: '',
        userName: item && item.userName ? String(item.userName) : '',
        userAvatar: item && item.userPic ? String(normalizeUrl(item.userPic)) : '',
        myAvatar: '',
        topTime: '',
        chatMessages: msgs,
        editable: true
      })))
      .then((resp) => {
        debugLog('openPreview:regenerate:done', { success: !!resp?.success, htmlLen: (resp?.html || '').length })
        if (!resp || !resp.success) throw new Error(resp?.message || '生成失败')
        previewHtml.value = resp.html || ''
      })
      .catch((e) => {
        debugLog('openPreview:regenerate:error', { message: e?.message || String(e), stack: e?.stack || '' })
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

function sanitizeEditMessages(messages) {
  if (!Array.isArray(messages)) return []
  try {
    return JSON.parse(JSON.stringify(messages))
  } catch {
    return []
  }
}

function buildMessagesFingerprint(messages) {
  if (!Array.isArray(messages)) return '0'
  const len = messages.length
  let acc = `${len}|`
  const limit = Math.min(len, 30)
  for (let i = 0; i < limit; i++) {
    const m = messages[i] || {}
    const msg = m.msg == null ? '' : String(m.msg)
    acc += `${m.msgType || 1}:${m.contentType || 1}:${msg.length}:${msg.slice(0, 16)}|`
  }
  return acc
}

async function applyPreviewEdits() {
  if (previewGenerating.value) return
  error.value = ''
  previewGenerating.value = true
  try {
    debugLog('apply:start', {
      previewOpen: previewOpen.value,
      previewDirty: previewDirty.value,
      hasPayload: !!previewEditedPayload.value,
      lock: previewApplyLock.value
    })
    if (!previewEditedPayload.value) throw new Error('没有可应用的编辑')
    const p = previewEditedPayload.value || {}
    const rawMessages = Array.isArray(p.messages) ? p.messages : []
    debugLog('apply:payload:raw', { rawMessagesLen: rawMessages.length, messageId: previewMessageId.value })
    const chatMessages = []
    for (let i = 0; i < rawMessages.length; i++) {
      const m = rawMessages[i] || {}
      chatMessages.push({
        contentType: Number(m.contentType) === 2 ? 2 : 1,
        msgType: Number(m.msgType) === 2 ? 2 : 1,
        dateTimeStr: m.dateTimeStr == null ? '' : String(m.dateTimeStr),
        showTime: !!m.showTime,
        msg: m.msg == null ? '' : String(m.msg),
        userName: p.userName == null ? '' : String(p.userName)
      })
    }

    try {
      var payload = {
        xianlu: p.xianlu == null ? '' : String(p.xianlu),
        platform: p.platform == null ? '' : String(p.platform),
        xianshiname: '',
        userName: p.userName == null ? '' : String(p.userName),
        userAvatar: p.userAvatar == null ? '' : String(p.userAvatar),
        myAvatar: p.myAvatar == null ? '' : String(p.myAvatar),
        topTime: p.topTime == null ? '' : String(p.topTime),
        messageId: previewMessageId.value == null ? null : Number(previewMessageId.value),
        chatMessages,
        editable: true
      }
    } catch (err) {
      throw new Error('构建payload失败: ' + (err?.message || String(err)))
    }

    debugLog('apply:payload:ready', {
      xianlu: payload.xianlu,
      platform: payload.platform,
      chatMessagesLen: chatMessages.length
    })

    previewApplyLock.value = true
    suppressPreviewIncomingEditUntil.value = Date.now() + 5000
    debugLog('apply:before-regenerate', { suppressUntil: suppressPreviewIncomingEditUntil.value })

    let resp
    try {
      resp = await window.SiyuBaoBackend.chat.regenerate(JSON.parse(JSON.stringify(payload)))
    } catch (err) {
      throw new Error('调用regenerate接口失败: ' + (err?.message || String(err)))
    }

    debugLog('apply:after-regenerate', { success: !!resp?.success, htmlLen: (resp?.html || '').length })
    if (!resp || !resp.success) throw new Error(resp?.message || '应用失败')

    try {
      previewHtml.value = resp.html || ''
    } catch (err) {
      throw new Error('赋值previewHtml失败: ' + (err?.message || String(err)))
    }

    previewDirty.value = false
    previewEditedPayload.value = null

    try {
      const win = previewFrame.value?.contentWindow
      win?.postMessage({ type: 'siyubao-clear-edit' }, '*')
    } catch (err) {
      console.warn('[CardMessage] send clear-edit failed:', err)
    }

    try {
      await nextTick()
    } catch (err) {
      console.warn('[CardMessage] nextTick failed:', err)
    }

    if (previewMessageId.value != null) {
      try {
        const idx = rows.value.findIndex(function (x) { return x && x.id === previewMessageId.value })
        if (idx >= 0) {
          var _userName = (p && p.userName) ? String(p.userName) : ((rows.value[idx] && rows.value[idx].userName) || '')
          var _userPic = (p && p.userAvatar) ? String(p.userAvatar) : ((rows.value[idx] && rows.value[idx].userPic) || '')
          var newChatMsgJson = JSON.stringify(chatMessages)
          var oldEv = (rows.value[idx] && rows.value[idx]._ev) || 0
          var updated = {
            xianlu: (rows.value[idx] && rows.value[idx].xianlu) || '',
            platform: (rows.value[idx] && rows.value[idx].platform) || '',
            line: (rows.value[idx] && rows.value[idx].line) || '',
            userName: _userName,
            userPic: _userPic,
            chatMessage: newChatMsgJson,
            id: (rows.value[idx] && rows.value[idx].id) || null,
            userNo: (rows.value[idx] && rows.value[idx].userNo) || '',
            phone: (rows.value[idx] && rows.value[idx].phone) || '',
            createTime: (rows.value[idx] && rows.value[idx].createTime) || '',
            _ev: oldEv + 1
          }
          rows.value.splice(idx, 1, updated)
          appliedChatCache.value[previewMessageId.value] = newChatMsgJson
          debugLog('apply:rows-updated', { rowIndex: idx, rowId: previewMessageId.value, chatMsgLen: chatMessages.length, newEv: oldEv + 1 })
        }
      } catch (err) {
        console.warn('[CardMessage] update rows failed:', err)
      }
    }
  } catch (e) {
    debugLog('apply:error', { message: e?.message || String(e), stack: e?.stack || '' })
    error.value = e?.message || String(e)
  } finally {
    setTimeout(function () {
      previewApplyLock.value = false
    }, 600)
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

  const onWindowKeydown = (e) => {
    if (e.key === 'Escape') previewFullscreen.value = false
  }
  window.addEventListener('keydown', onWindowKeydown)

  const onWindowMessage = (evt) => {
    if (previewApplyLock.value) return
    if (previewGenerating.value) return
    if (Date.now() < suppressPreviewIncomingEditUntil.value) return
    if (!previewOpen.value) return
    if (evt?.source !== previewFrame.value?.contentWindow) return
    const d = evt?.data
    if (d && d.type === 'siyubao-request-upload') {
      requestPickFile(d)
      return
    }
    if (!d || d.type !== 'siyubao-edit') return
    // 彻底净化整个payload，避免任何循环引用或响应式对象
    const safeD = JSON.parse(JSON.stringify(d))
    const safeMessages = sanitizeEditMessages(safeD.messages)
    const payloadKey = [
      safeD.xianlu || '',
      safeD.platform || '',
      safeD.userAvatar || '',
      safeD.myAvatar || '',
      safeD.userName || '',
      safeD.topTime || '',
      buildMessagesFingerprint(safeMessages)
    ].join('||')
    if (payloadKey === lastPreviewEditPayloadKey.value) return
    lastPreviewEditPayloadKey.value = payloadKey
    debugLog('window:message:edit', {
      safeMessagesLen: safeMessages.length,
      userName: safeD.userName || '',
      topTime: safeD.topTime || ''
    })
    // 使用净化后的对象，确保没有任何循环引用
    previewEditedPayload.value = {
      xianlu: safeD.xianlu,
      platform: safeD.platform,
      userAvatar: safeD.userAvatar,
      myAvatar: safeD.myAvatar,
      userName: safeD.userName,
      topTime: safeD.topTime,
      messages: safeMessages
    }
    previewDirty.value = true
  }
  window.addEventListener('message', onWindowMessage)

  const onWindowError = (evt) => {
    const msg = evt?.message || 'window error'
    const stack = evt?.error?.stack || ''
    debugLog('window:error', { message: msg, stack })
  }
  const onUnhandledRejection = (evt) => {
    const reason = evt?.reason
    debugLog('window:unhandledrejection', {
      message: reason?.message || String(reason || ''),
      stack: reason?.stack || ''
    })
  }
  window.addEventListener('error', onWindowError)
  window.addEventListener('unhandledrejection', onUnhandledRejection)

  onUnmounted(() => {
    window.removeEventListener('keydown', onWindowKeydown)
    window.removeEventListener('message', onWindowMessage)
    window.removeEventListener('error', onWindowError)
    window.removeEventListener('unhandledrejection', onUnhandledRejection)
  })
})
</script>
