<template>
  <div class="space-y-6">
    <div class="bg-white border border-gray-200 rounded-lg p-6">
      <div class="text-lg font-medium text-gray-900 mb-4">📞 联系客服</div>
      
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
          <div class="flex items-center space-x-3">
            <div class="w-10 h-10 bg-green-500 rounded-full flex items-center justify-center text-white text-lg">
              💬
            </div>
            <div>
              <div class="font-medium text-gray-900">微信客服</div>
              <div class="text-sm text-gray-500">扫码添加微信</div>
            </div>
          </div>
          <div class="mt-3 text-center">
            <div v-if="loading" class="inline-block bg-gray-100 p-4 rounded">
              <div class="text-gray-400 text-sm">加载中...</div>
            </div>
            <div v-else-if="csInfo?.wechatQrCodeUrl" class="inline-block">
              <img :src="csInfo.wechatQrCodeUrl" alt="微信客服" class="w-48 h-48 border border-gray-200 rounded-lg" />
            </div>
            <div v-else class="inline-block bg-gray-100 p-4 rounded">
              <div class="text-gray-400 text-sm">暂未配置</div>
            </div>
          </div>
        </div>

        <div class="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
          <div class="flex items-center space-x-3">
            <div class="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center text-white text-lg">
              📧
            </div>
            <div>
              <div class="font-medium text-gray-900">邮箱支持</div>
              <div class="text-sm text-gray-500">24小时内回复</div>
            </div>
          </div>
          <div class="mt-3">
            <div class="text-center text-blue-600 font-mono text-sm">
              {{ csInfo?.email || 'support@siyubao.com' }}
            </div>
          </div>
        </div>

        <div class="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
          <div class="flex items-center space-x-3">
            <div class="w-10 h-10 bg-purple-500 rounded-full flex items-center justify-center text-white text-lg">
              📱
            </div>
            <div>
              <div class="font-medium text-gray-900">公众号二维码</div>
              <div class="text-sm text-gray-500">关注获取更多</div>
            </div>
          </div>
          <div class="mt-3 text-center">
            <div v-if="loading" class="inline-block bg-gray-100 p-4 rounded">
              <div class="text-gray-400 text-sm">加载中...</div>
            </div>
            <div v-else-if="csInfo?.officialAccountQrCodeUrl" class="inline-block">
              <img :src="csInfo.officialAccountQrCodeUrl" alt="公众号" class="w-48 h-48 border border-gray-200 rounded-lg" />
            </div>
            <div v-else class="inline-block bg-gray-100 p-4 rounded">
              <div class="text-gray-400 text-sm">暂未配置</div>
            </div>
          </div>
        </div>

        <div class="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
          <div class="flex items-center space-x-3">
            <div class="w-10 h-10 bg-orange-500 rounded-full flex items-center justify-center text-white text-lg">
              🐛
            </div>
            <div>
              <div class="font-medium text-gray-900">问题反馈</div>
              <div class="text-sm text-gray-500">提交Bug和建议</div>
            </div>
          </div>
          <div class="mt-3">
            <button 
              @click="showFeedback = !showFeedback"
              class="w-full px-3 py-2 bg-orange-100 text-orange-700 rounded hover:bg-orange-200 text-sm"
            >
              {{ showFeedback ? '收起' : '提交反馈' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showFaq" class="bg-white border border-gray-200 rounded-lg p-6">
      <div class="text-lg font-medium text-gray-900 mb-4">常见问题解答</div>
      
      <div class="space-y-4">
        <div class="border-b border-gray-100 pb-4">
          <div class="font-medium text-gray-800">Q: 如何使用聊天生成功能？</div>
          <div class="text-sm text-gray-600 mt-2">
            A: 点击"聊天生成"菜单，选择线路，填写用户信息后即可生成聊天截图。
          </div>
        </div>

        <div class="border-b border-gray-100 pb-4">
          <div class="font-medium text-gray-800">Q: 每日生成次数有限制吗？</div>
          <div class="text-sm text-gray-600 mt-2">
            A: 免费用户每日有限制，升级会员可获得更多或无限次数。详情请查看"会员中心"。
          </div>
        </div>

        <div class="border-b border-gray-100 pb-4">
          <div class="font-medium text-gray-800">Q: 如何升级会员？</div>
          <div class="text-sm text-gray-600 mt-2">
            A: 进入"会员中心"，选择合适的套餐进行购买即可。
          </div>
        </div>

        <div class="border-b border-gray-100 pb-4">
          <div class="font-medium text-gray-800">Q: 可以在多台设备上登录吗？</div>
          <div class="text-sm text-gray-600 mt-2">
            A: 根据您的会员等级，有不同的设备数量限制。详情请查看"会员中心"的套餐说明。
          </div>
        </div>

        <div>
          <div class="font-medium text-gray-800">Q: 生成的图片有水印吗？</div>
          <div class="text-sm text-gray-600 mt-2">
            A: 免费用户生成的图片会有水印，部分会员套餐可去除水印。
          </div>
        </div>
      </div>
    </div>

    <div v-if="showFeedback" class="bg-white border border-gray-200 rounded-lg p-6">
      <div class="text-lg font-medium text-gray-900 mb-4">问题反馈</div>
      
      <form @submit.prevent="submitFeedback" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">问题类型</label>
          <select v-model="feedback.type" class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500">
            <option value="bug">Bug反馈</option>
            <option value="feature">功能建议</option>
            <option value="usage">使用问题</option>
            <option value="other">其他</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">问题描述</label>
          <textarea 
            v-model="feedback.description" 
            rows="4" 
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="请详细描述您遇到的问题或建议..."
          ></textarea>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">联系方式（选填）</label>
          <input 
            v-model="feedback.contact" 
            type="text" 
            class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="留下您的联系方式，方便我们跟进"
          >
        </div>

        <div class="flex space-x-3">
          <button 
            type="submit" 
            :disabled="submitting"
            class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ submitting ? '提交中...' : '提交反馈' }}
          </button>
          <button 
            type="button" 
            @click="resetFeedback"
            class="px-4 py-2 bg-gray-200 text-gray-700 rounded-md hover:bg-gray-300"
          >
            重置
          </button>
        </div>
      </form>

      <div v-if="submitSuccess" class="mt-4 p-4 bg-green-50 border border-green-200 rounded-md text-green-700">
        ✓ 反馈提交成功！感谢您的反馈，我们会尽快处理。
      </div>
    </div>

    <div class="bg-white border border-gray-200 rounded-lg p-6">
      <div class="text-lg font-medium text-gray-900 mb-4">⏰ 服务时间</div>
      <div class="text-sm text-gray-600 space-y-1">
        <div>• 在线客服：周一至周日 9:00 - 21:00</div>
        <div>• 邮件支持：24小时内回复</div>
        <div>• 紧急问题：请优先使用微信客服</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const showFaq = ref(false)
const showFeedback = ref(false)
const submitting = ref(false)
const submitSuccess = ref(false)
const loading = ref(false)
const csInfo = ref(null)

const feedback = ref({
  type: 'bug',
  description: '',
  contact: ''
})

async function loadCsInfo() {
  loading.value = true
  try {
    const resp = await fetch('/api/customer-service/info', {
      method: 'GET',
      headers: { 'Accept': 'application/json' }
    })
    const r = await resp.json().catch(() => ({}))
    if (r.success && r.data) {
      csInfo.value = r.data
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function submitFeedback() {
  if (!feedback.value.description.trim()) {
    alert('请填写问题描述')
    return
  }

  submitting.value = true
  try {
    const content = `【${feedback.value.type === 'bug' ? 'Bug反馈' : feedback.value.type === 'feature' ? '功能建议' : feedback.value.type === 'usage' ? '使用问题' : '其他'}】\n${feedback.value.description}`
    const resp = await fetch('/api/feedback/submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify({
        content: content,
        contact: feedback.value.contact || null
      })
    })
    const r = await resp.json().catch(() => ({}))
    if (!resp.ok || !r.success) throw new Error(r.message || '提交失败')

    submitSuccess.value = true
    resetFeedback()

    setTimeout(() => {
      submitSuccess.value = false
    }, 3000)
  } catch (e) {
    alert(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function resetFeedback() {
  feedback.value = {
    type: 'bug',
    description: '',
    contact: ''
  }
}

onMounted(() => {
  loadCsInfo()
})
</script>
