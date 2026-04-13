<template>
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
    <div class="bg-white border border-gray-200 rounded-lg lg:col-span-1">
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-medium text-gray-800">聊天预览</div>
      </div>
      <div class="p-4 space-y-3">
        <div v-if="error" class="text-sm text-red-600">{{ error }}</div>
        <input ref="uploadInput" type="file" accept="image/*" class="hidden" @change="onPickUploadFile" />
        <input ref="bgUploadInput" type="file" accept="image/*" class="hidden" @change="onPickBgFile" />
        <div class="grid grid-cols-2 gap-3">
          <div>
            <div class="text-sm text-gray-700 mb-1">平台</div>
            <select v-model="platform" class="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white">
              <option value="dy">抖音</option>
              <option value="sph">视频号</option>
              <option value="xhs">小红书</option>
            </select>
          </div>
          <div>
            <div class="text-sm text-gray-700 mb-1">图像</div>
            <select v-model="routeValue" class="w-full border border-gray-300 rounded px-3 py-2 text-sm bg-white">
              <option v-for="(r, idx) in routes" :key="r?.id ?? r?.routeValue ?? idx" :value="r?.routeValue ?? ''">
                {{ r?.routeName ?? '' }}
              </option>
            </select>
          </div>
        </div>

        <div>
          <div class="text-sm text-gray-700 mb-1">聊天背景</div>
          <div class="flex items-center gap-2 flex-wrap">
            <select v-model="bgColor" class="flex-1 border border-gray-300 rounded px-3 py-2 text-sm bg-white">
              <option v-for="opt in bgColorOptions" :key="opt.value" :value="opt.value ? opt.value : ''">
                {{ opt.label }}
              </option>
            </select>
            <button
              class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm"
              @click="pickBgFile"
            >
              上传背景
            </button>
            <button
              class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm"
              @click="clearBg"
            >
              清除
            </button>
          </div>
          <div v-if="bgImage" class="mt-2 text-xs text-gray-500">
            已设置背景图片
          </div>
        </div>

        <div>
          <div class="text-sm text-gray-700 mb-1">聊天内容</div>
          <textarea
            ref="chatTextarea"
            v-model="chatContent"
            class="w-full border border-gray-300 rounded px-3 py-2 text-sm resize-none"
            rows="10"
            @keydown="onChatKeydown"
          ></textarea>
          <div class="text-xs text-gray-500 mt-1">{{ chatContent.length }} 字符</div>
        </div>

        <div class="flex items-center space-x-2">
          <button
            class="px-3 py-2 rounded bg-blue-600 hover:bg-blue-700 text-white text-sm disabled:opacity-60"
            :disabled="generating || !chatContent.trim() || !routeValue"
            @click="generate"
          >
            {{ generating ? '生成中...' : '平台接入' }}
          </button>
          <label class="flex items-center space-x-2 text-sm text-gray-700 select-none">
            <input v-model="editMode" type="checkbox" class="border border-gray-300 rounded" />
            <span>编辑模式</span>
          </label>
          <button
            class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm disabled:opacity-60"
            :disabled="generating || !editMode || !dirtyEdits"
            @click="applyEdits"
          >
            应用编辑
          </button>
          <a class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm" href="/app/route" target="_blank">配置线路</a>
        </div>


      </div>
    </div>

    <div
      class="bg-white border border-gray-200 rounded-lg overflow-hidden"
      :class="fullscreen ? 'fixed inset-0 z-50 m-0 rounded-none border-0' : 'lg:col-span-2'"
    >
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-medium text-gray-800">预览</div>
        <div class="flex items-center space-x-2">
          <div class="text-xs text-gray-500 truncate max-w-[60vw]">{{ previewUrl }}</div>
          <button
            class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200 disabled:opacity-60"
            :disabled="exporting || !previewHtml"
            @click="exportHtml"
          >
            {{ exporting ? '导出中…' : '导出HTML' }}
          </button>
          <button
            class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200 disabled:opacity-60"
            :disabled="exporting || !previewHtml"
            @click="exportPng"
          >
            {{ exporting ? '导出中…' : '导出PNG' }}
          </button>
          <button
            class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200 disabled:opacity-60"
            :disabled="!previewHtml && !previewUrl"
            @click="toggleFullscreen"
          >
            {{ fullscreen ? '退出全屏' : '全屏' }}
          </button>
        </div>
      </div>
      <div :class="fullscreen ? 'h-[calc(100vh-49px)]' : 'h-[70vh]'">
        <iframe
          ref="previewFrame"
          v-if="previewHtml"
          :srcdoc="previewHtml"
          class="w-full h-full"
          frameborder="0"
          @load="onPreviewLoad"
        ></iframe>
        <iframe v-else-if="previewUrl" :src="previewUrl" class="w-full h-full" frameborder="0"></iframe>
        <div v-else class="h-full flex items-center justify-center text-sm text-gray-500">点击“平台接入”生成预览</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { toPng } from 'html-to-image'

const error = ref('')
const generating = ref(false)
const ocrRunning = ref(false)
const ocrHint = ref('')

const routes = ref([])
const platform = ref('dy')
const routeValue = ref('')
const chatContent = ref('')
const previewUrl = ref('')
const previewHtml = ref('')
const editMode = ref(false)
const dirtyEdits = ref(false)
const editedPayload = ref(null)
const previewFrame = ref(null)
const pendingRestoreScrollTop = ref(null)
const fullscreen = ref(false)
const currentMessageId = ref(null)
const chatTextarea = ref(null)
const exporting = ref(false)

const uploadInput = ref(null)
const pendingUpload = ref(null)
const bgUploadInput = ref(null)
const bgColor = ref('')
const bgImage = ref('')
const bgColorOptions = [
  { label: '默认灰色', value: '' },
  { label: '纯白', value: '#ffffff' },
  { label: '浅灰', value: '#f5f5f5' },
  { label: '浅蓝', value: '#e3f2fd' },
  { label: '浅绿', value: '#e8f5e9' },
  { label: '浅粉', value: '#fce4ec' },
  { label: '浅紫', value: '#f3e5f5' },
]

function getBgValueForApi() {
  if (bgImage.value) {
    return bgImage.value
  }
  return bgColor.value
}

const ocrFile = ref(null)
const ocrDataUrl = ref('')

async function loadRoutes() {
  error.value = ''
  try {
    const resp = await window.SiyuBaoBackend.routes.list()
    if (!resp.success) throw new Error(resp.message || '加载线路失败')
    routes.value = (resp.data || []).filter(Boolean)
    if (!routeValue.value && routes.value.length > 0) {
      routeValue.value = routes.value[0].routeValue
    }
  } catch (e) {
    error.value = e?.message || String(e)
  }
}

async function generate() {
  error.value = ''
  generating.value = true
  try {
    dirtyEdits.value = false
    editedPayload.value = null
    currentMessageId.value = null
    const resp = await window.SiyuBaoBackend.chat.generate(routeValue.value, platform.value, chatContent.value, '', editMode.value, getBgValueForApi())
    if (!resp.success) throw new Error(resp.message || '生成失败')
    if (resp.messageId != null) currentMessageId.value = resp.messageId
    if (resp.html) {
      previewHtml.value = resp.html
      previewUrl.value = ''
    } else if (resp.url) {
      previewUrl.value = resp.url
      previewHtml.value = ''
    } else {
      throw new Error('生成失败')
    }
    
    window.dispatchEvent(new CustomEvent('sxjw-user-updated'))
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    generating.value = false
  }
}

async function applyEdits() {
  error.value = ''
  generating.value = true
  try {
    if (!editedPayload.value) throw new Error('没有可应用的编辑')
    pendingRestoreScrollTop.value = getPreviewScrollTop()
    const p = editedPayload.value
    const resp = await window.SiyuBaoBackend.chat.regenerate({
      xianlu: p.xianlu || routeValue.value,
      platform: p.platform || platform.value,
      xianshiname: '',
      userName: p.userName || '',
      userAvatar: p.userAvatar || '',
      myAvatar: p.myAvatar || '',
      topTime: p.topTime || '',
      readText: p.readText || '已读',
      messageId: currentMessageId.value,
      chatMessages: (p.messages || []).map((m) => ({
        contentType: m.contentType || 1,
        msgType: m.msgType || 1,
        dateTimeStr: m.dateTimeStr || '',
        showTime: !!m.showTime,
        msg: m.msg || '',
        userName: p.userName || ''
      })),
      chatBg: getBgValueForApi(),
      editable: true
    })
    if (!resp.success) throw new Error(resp.message || '应用失败')
    previewHtml.value = resp.html || ''
    previewUrl.value = ''
    dirtyEdits.value = false
    await nextTick()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    generating.value = false
  }
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

async function exportHtml() {
  if (!previewHtml.value) return
  exporting.value = true
  try {
    const blob = new Blob([previewHtml.value], { type: 'text/html;charset=utf-8' })
    const name = `sxjw-${platform.value}-${Date.now()}.html`
    downloadBlob(blob, name)
  } finally {
    exporting.value = false
  }
}

async function exportPng() {
  if (!previewHtml.value) return
  exporting.value = true
  try {
    const iframe = previewFrame.value
    const doc = iframe?.contentDocument
    const body = doc?.body
    if (!body) throw new Error('预览未就绪')
    const dataUrl = await toPng(body, { cacheBust: true, backgroundColor: '#ffffff' })
    const res = await fetch(dataUrl)
    const blob = await res.blob()
    const name = `sxjw-${platform.value}-${Date.now()}.png`
    downloadBlob(blob, name)
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    exporting.value = false
  }
}

function toggleFullscreen() {
  fullscreen.value = !fullscreen.value
}

function getPreviewScrollTop() {
  try {
    const doc = previewFrame.value?.contentDocument
    const el = doc?.scrollingElement || doc?.documentElement || doc?.body
    return el?.scrollTop ?? 0
  } catch {
    return 0
  }
}

function setPreviewScrollTop(y) {
  try {
    const doc = previewFrame.value?.contentDocument
    const el = doc?.scrollingElement || doc?.documentElement || doc?.body
    if (el) el.scrollTop = y
  } catch {
  }
}

function onPreviewLoad() {
  const y = pendingRestoreScrollTop.value
  if (y == null) return
  pendingRestoreScrollTop.value = null
  setPreviewScrollTop(y)
  setTimeout(() => setPreviewScrollTop(y), 50)
  setTimeout(() => setPreviewScrollTop(y), 200)
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

function pickOcrFile() {
  if (ocrFile.value) ocrFile.value.click()
}

function onPickOcrFile(evt) {
  const file = evt?.target?.files?.[0]
  evt.target.value = ''
  if (!file) return
  const r = new FileReader()
  r.onload = () => {
    ocrDataUrl.value = typeof r.result === 'string' ? r.result : ''
    ocrHint.value = `已选择：${file.name}`
  }
  r.onerror = () => {
    ocrHint.value = '读取图片失败'
  }
  r.readAsDataURL(file)
}

async function runOcr() {
  error.value = ''
  ocrRunning.value = true
  try {
    const resp = await window.SiyuBaoBackend.ocr.performDataUrl(ocrDataUrl.value, 'image.png')
    if (!resp.success) throw new Error(resp.message || '识别失败')
    if (resp.text) {
      chatContent.value = chatContent.value ? `${chatContent.value}\n${resp.text}` : resp.text
    }
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    ocrRunning.value = false
  }
}

function onChatKeydown(e) {
  const isCtrl = !!e.ctrlKey
  const isAlt = !!e.altKey
  const isShift = !!e.shiftKey
  const key = e.key
  if (!isCtrl) return

  if (!isAlt && !isShift && (key === 'y' || key === 'Y')) {
    e.preventDefault()
    deleteCurrentLine()
    return
  }

  if (isAlt && !isShift && (key === 'ArrowUp' || key === 'ArrowDown')) {
    e.preventDefault()
    moveCurrentLine(key === 'ArrowUp' ? -1 : 1)
  }
}

function deleteCurrentLine() {
  const el = chatTextarea.value
  if (!el) return
  const text = String(chatContent.value || '')
  const pos = typeof el.selectionStart === 'number' ? el.selectionStart : 0
  const { lineIndex, column } = cursorLineInfo(text, pos)
  const lines = text.split('\n')
  if (lines.length === 0) return
  if (lineIndex < 0 || lineIndex >= lines.length) return

  const nextLines = lines.slice(0, lineIndex).concat(lines.slice(lineIndex + 1))
  const normalized = nextLines.length === 0 ? [''] : nextLines
  const targetIndex = Math.min(lineIndex, normalized.length - 1)
  const nextText = normalized.join('\n')
  const nextPos = lineStartOffset(normalized, targetIndex) + Math.min(column, normalized[targetIndex].length)
  applyTextareaChange(nextText, nextPos, el.scrollTop)
}

function moveCurrentLine(dir) {
  const el = chatTextarea.value
  if (!el) return
  const text = String(chatContent.value || '')
  const pos = typeof el.selectionStart === 'number' ? el.selectionStart : 0
  const { lineIndex, column } = cursorLineInfo(text, pos)
  const lines = text.split('\n')
  if (lines.length <= 1) return
  if (lineIndex < 0 || lineIndex >= lines.length) return

  const to = lineIndex + dir
  if (to < 0 || to >= lines.length) return

  const nextLines = lines.slice()
  const tmp = nextLines[lineIndex]
  nextLines[lineIndex] = nextLines[to]
  nextLines[to] = tmp

  const nextText = nextLines.join('\n')
  const nextPos = lineStartOffset(nextLines, to) + Math.min(column, nextLines[to].length)
  applyTextareaChange(nextText, nextPos, el.scrollTop)
}

function cursorLineInfo(text, pos) {
  let idx = 0
  let start = 0
  for (let i = 0; i < pos && i < text.length; i++) {
    if (text.charAt(i) === '\n') {
      idx++
      start = i + 1
    }
  }
  return { lineIndex: idx, column: Math.max(0, pos - start) }
}

function lineStartOffset(lines, lineIndex) {
  let off = 0
  for (let i = 0; i < lineIndex; i++) off += lines[i].length + 1
  return off
}

function applyTextareaChange(nextText, nextPos, scrollTop) {
  chatContent.value = nextText
  nextTick(() => {
    const el = chatTextarea.value
    if (!el) return
    try {
      el.focus()
      el.setSelectionRange(nextPos, nextPos)
      if (typeof scrollTop === 'number') el.scrollTop = scrollTop
    } catch {
    }
  })
}

function pickBgFile() {
  if (bgUploadInput.value) bgUploadInput.value.click()
}

async function onPickBgFile(evt) {
  const file = evt?.target?.files?.[0]
  evt.target.value = ''
  if (!file) return
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
    bgImage.value = `url('${resp.url}') center/cover no-repeat`
  } catch (e) {
    error.value = e?.message || String(e)
  }
}

function clearBg() {
  bgColor.value = ''
  bgImage.value = ''
}

onMounted(async () => {
  await loadRoutes()

  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') fullscreen.value = false
  })

  window.addEventListener('message', (evt) => {
    const d = evt?.data
    if (d && d.type === 'siyubao-request-upload') {
      requestPickFile(d)
      return
    }
    if (!d || d.type !== 'siyubao-edit') return
    editedPayload.value = {
      xianlu: d.xianlu,
      platform: d.platform,
      userAvatar: d.userAvatar,
      myAvatar: d.myAvatar,
      userName: d.userName,
      topTime: d.topTime,
      messages: d.messages || [],
      readText: d.readText || '已读'
    }
    dirtyEdits.value = true
  })
})
</script>
