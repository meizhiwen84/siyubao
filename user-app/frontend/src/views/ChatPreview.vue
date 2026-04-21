<template>
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
    <div class="bg-white border border-gray-200 rounded-lg lg:col-span-1">
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-medium text-gray-800">聊天预览</div>
      </div>
      <div class="p-4 space-y-3">
        <div v-if="error" class="text-sm text-red-600">{{ error }}</div>
        <div v-if="successMsg" class="text-sm text-green-600">{{ successMsg }}</div>
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
          <a class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm" href="/app/route">配置图像</a>
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
  
  <div class="mt-4 text-center text-xs text-gray-400">
    本截图为模拟生成，仅供娱乐演示，非真实记录
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { toPng } from 'html-to-image'

const error = ref('')
const successMsg = ref('')
const generating = ref(false)
const ocrRunning = ref(false)
const ocrHint = ref('')

const routes = ref([])
const platform = ref('dy')
const routeValue = ref('')
const chatContent = ref('')
const previewUrl = ref('')
const previewHtml = ref('')
const editMode = ref(true)
const dirtyEdits = ref(false)
const editedPayload = ref(null)
const previewFrame = ref(null)
const pendingRestoreScrollTop = ref(null)
const fullscreen = ref(false)
const currentMessageId = ref(null)
const chatTextarea = ref(null)
const exporting = ref(false)
const lastEditPayloadKey = ref('')
const suppressIncomingEditUntil = ref(0)

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
    
    // 直接从返回结果中获取 todayUsed 并更新，避免额外调用 me 接口
    if (resp.todayUsed !== undefined) {
      window.dispatchEvent(new CustomEvent('sxjw-user-updated', { 
        detail: { todayUsed: resp.todayUsed } 
      }))
    } else {
      window.dispatchEvent(new CustomEvent('sxjw-user-updated'))
    }
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
    const p = editedPayload.value || {}
    const rawMessages = Array.isArray(p.messages) ? p.messages : []
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
    const payload = {
      xianlu: p.xianlu ? String(p.xianlu) : routeValue.value,
      platform: p.platform ? String(p.platform) : platform.value,
      xianshiname: '',
      userName: p.userName == null ? '' : String(p.userName),
      userAvatar: p.userAvatar == null ? '' : String(p.userAvatar),
      myAvatar: p.myAvatar == null ? '' : String(p.myAvatar),
      topTime: p.topTime == null ? '' : String(p.topTime),
      readText: p.readText == null ? '已读' : String(p.readText),
      messageId: currentMessageId.value,
      chatMessages,
      chatBg: getBgValueForApi(),
      editable: true
    }
    suppressIncomingEditUntil.value = Date.now() + 1200
    const resp = await window.SiyuBaoBackend.chat.regenerate(JSON.parse(JSON.stringify(payload)))
    if (!resp.success) throw new Error(resp.message || '应用失败')
    previewHtml.value = resp.html || ''
    previewUrl.value = ''
    dirtyEdits.value = false
    editedPayload.value = null
    // 通知iframe清除工具栏和选中状态
    const win = previewFrame.value?.contentWindow
    win?.postMessage({ type: 'siyubao-clear-edit' }, '*')
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
    // 检查用户是否为VIP
    const isVip = await checkIsVip()
    
    const iframe = previewFrame.value
    const doc = iframe?.contentDocument
    if (!doc) throw new Error('预览未就绪')
    
    // 等待iframe内容完全加载（包括图片等资源）
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 计算正确的尺寸和选择容器
    let width, height, targetElement
    
    const isXhs = platform.value === 'xhs'
    
    if (isXhs) {
      // 小红书：直接使用body捕获整个页面
      targetElement = doc.body
      width = doc.body.scrollWidth
      height = doc.body.scrollHeight
      
      console.log('[DEBUG] 小红书body信息:', {
        scrollWidth: width,
        scrollHeight: height,
        offsetWidth: doc.body.offsetWidth,
        offsetHeight: doc.body.offsetHeight
      })
      console.log('[DEBUG] 最终使用高度:', height)
    } else {
      // 其他平台：使用主要容器
      const mobileContainer = doc.querySelector('.mobile-container') || 
                              doc.querySelector('.phone-container') ||
                              doc.body
      
      if (mobileContainer) {
        targetElement = mobileContainer
        const rect = mobileContainer.getBoundingClientRect()
        width = rect.width
        height = Math.max(rect.height, mobileContainer.scrollHeight)
      }
    }
    
    if (!targetElement) throw new Error('找不到聊天容器')
    
    // 创建画布
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    
    // 设置画布尺寸
    const scale = 2 // 高清输出
    canvas.width = width * scale
    canvas.height = height * scale
    ctx.scale(scale, scale)
    
    // 使用html-to-image捕获，设置CORS和资源加载选项
    const dataUrl = await toPng(targetElement, {
      cacheBust: true,
      backgroundColor: '#ffffff',
      pixelRatio: scale,
      useCORS: true,
      allowTaint: true,
      style: {
        transform: 'none'
      }
    })
    
    const img = new Image()
    await new Promise((resolve, reject) => {
      img.onload = resolve
      img.onerror = reject
      img.src = dataUrl
    })
    
    ctx.drawImage(img, 0, 0, width, height)
    
    // 如果是免费版用户，添加水印
    console.log('[DEBUG] 导出PNG - checkIsVip():', isVip)
    if (isVip) {
      console.log('[DEBUG] 免费版用户，添加水印')
      addWatermark(ctx, canvas.width, canvas.height)
    } else {
      console.log('[DEBUG] 付费用户，不添加水印')
    }
    
    // 导出为PNG
    const finalDataUrl = canvas.toDataURL('image/png')
    const filename = `sxjw-${platform.value}-${Date.now()}.png`
    
    // 尝试使用JCEF API保存文件
    if (window.SiyuBaoBackend && window.SiyuBaoBackend.saveFile) {
      try {
        // JCEF环境：使用后端API保存文件
        const base64 = finalDataUrl.split(',')[1]
        const resp = await window.SiyuBaoBackend.saveFile({
          filename: filename,
          contentBase64: base64
        })
        if (resp && resp.success) {
          successMsg.value = `图片已保存到：${resp.path || filename}`
          setTimeout(() => { successMsg.value = '' }, 5000)
        } else {
          throw new Error(resp?.message || '保存失败')
        }
      } catch (err) {
        // 如果JCEF保存失败，尝试浏览器下载
        const res = await fetch(finalDataUrl)
        const blob = await res.blob()
        downloadBlob(blob, filename)
        successMsg.value = `图片已导出：${filename}`
        setTimeout(() => { successMsg.value = '' }, 5000)
      }
    } else {
      // 浏览器环境：直接下载
      const res = await fetch(finalDataUrl)
      const blob = await res.blob()
      downloadBlob(blob, filename)
      successMsg.value = `图片已导出：${filename}`
      setTimeout(() => { successMsg.value = '' }, 5000)
    }
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    exporting.value = false
  }
}

async function checkIsVip() {
  try {
    const resp = await fetch('/api/auth/me')
    const r = await resp.json().catch(() => ({}))
    console.log('[DEBUG] auth.me 响应:', JSON.stringify(r))
    
    if (resp.ok && r && r.success) {
      // 检查用户计划
      const planName = r.plan?.name || r.planName || ''
      console.log('[DEBUG] 用户计划名称:', planName)
      
      // 如果计划名称包含"免费"，认为是免费版
      const isFree = planName.includes('免费')
      console.log('[DEBUG] 是否免费版:', isFree)
      
      // 根据需求：免费版应该添加水印，VIP不添加水印
      // 所以：return true表示"需要添加水印"（免费版），return false表示"不需要添加水印"（VIP）
      return isFree
    }
    console.log('[DEBUG] 响应不成功，默认返回true（免费版，需添加水印）')
    return true  // 默认返回true（免费版，需添加水印）
  } catch (e) {
    console.log('[DEBUG] checkIsVip 异常:', e)
    return true  // 出错时默认添加水印
  }
}

function addWatermark(ctx, width, height) {
  ctx.save()
  
  // 设置水印样式
  ctx.globalAlpha = 0.3
  ctx.fillStyle = '#ff0000'
  ctx.font = '24px Arial'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  
  // 计算水印位置（右下角）
  const padding = 20
  const watermarkText = '私信截图王'
  
  // 绘制多个水印（倾斜）
  ctx.translate(width / 2, height / 2)
  ctx.rotate(-Math.PI / 6) // 旋转-30度
  
  // 绘制水印网格
  const spacingX = 200
  const spacingY = 150
  const startX = -width
  const startY = -height
  
  for (let x = startX; x < width * 2; x += spacingX) {
    for (let y = startY; y < height * 2; y += spacingY) {
      ctx.fillText(watermarkText, x, y)
    }
  }
  
  ctx.restore()
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

  const onWindowKeydown = (e) => {
    if (e.key === 'Escape') fullscreen.value = false
  }
  window.addEventListener('keydown', onWindowKeydown)

  const onWindowMessage = (evt) => {
    if (generating.value) return
    if (Date.now() < suppressIncomingEditUntil.value) return
    if (evt?.source !== previewFrame.value?.contentWindow) return
    const d = evt?.data
    if (d && d.type === 'siyubao-request-upload') {
      requestPickFile(d)
      return
    }
    if (!d || d.type !== 'siyubao-edit') return
    let safeMessages = []
    if (Array.isArray(d.messages)) {
      try {
        // 通过 JSON 序列化裁剪潜在循环引用，避免响应式赋值时栈溢出
        safeMessages = JSON.parse(JSON.stringify(d.messages))
      } catch {
        safeMessages = []
      }
    }
    const payloadKey = JSON.stringify({
      xianlu: d.xianlu || '',
      platform: d.platform || '',
      userAvatar: d.userAvatar || '',
      myAvatar: d.myAvatar || '',
      userName: d.userName || '',
      topTime: d.topTime || '',
      readText: d.readText || '已读',
      messages: safeMessages
    })
    if (payloadKey === lastEditPayloadKey.value) return
    lastEditPayloadKey.value = payloadKey
    editedPayload.value = {
      xianlu: d.xianlu,
      platform: d.platform,
      userAvatar: d.userAvatar,
      myAvatar: d.myAvatar,
      userName: d.userName,
      topTime: d.topTime,
      messages: safeMessages,
      readText: d.readText || '已读'
    }
    dirtyEdits.value = true
  }
  window.addEventListener('message', onWindowMessage)

  onUnmounted(() => {
    window.removeEventListener('keydown', onWindowKeydown)
    window.removeEventListener('message', onWindowMessage)
  })
})
</script>
