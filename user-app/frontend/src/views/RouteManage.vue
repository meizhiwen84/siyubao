<template>
  <div class="bg-white border border-gray-200 rounded-lg">
    <div class="p-4 border-b border-gray-200 flex items-center justify-between">
      <div class="font-medium text-gray-800">图像管理</div>
      <div class="space-x-2">
        <button class="px-3 py-1.5 rounded bg-gray-100 hover:bg-gray-200 text-sm" @click="load">
          刷新
        </button>
        <button class="px-3 py-1.5 rounded bg-blue-600 hover:bg-blue-700 text-white text-sm" @click="openCreate">
          新增
        </button>
      </div>
    </div>
    <div class="p-4">
      <div v-if="error" class="mb-3 text-sm text-red-600">{{ error }}</div>
      <div v-if="loading" class="text-sm text-gray-600">加载中...</div>
      <table v-else class="w-full text-sm">
        <thead class="text-left text-gray-600">
          <tr>
            <th class="py-2">名称</th>
            <th class="py-2">值</th>
            <th class="py-2">抖音头像</th>
            <th class="py-2">视频号头像</th>
            <th class="py-2">小红书头像</th>
            <th class="py-2">欢迎语</th>
            <th class="py-2">状态</th>
            <th class="py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r, idx) in routes" :key="r?.id ?? idx" class="border-t border-gray-100">
            <td class="py-2">
              <div class="flex items-center space-x-2">
                <div class="text-gray-800">{{ r.routeName }}</div>
                <button class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200" @click="openEditRoute(r)">编辑</button>
              </div>
            </td>
            <td class="py-2">
              <div class="flex items-center space-x-2">
                <div class="text-gray-800">{{ r.routeValue }}</div>
              </div>
            </td>
            <td class="py-2">
              <div class="flex items-center space-x-2">
                <img v-if="r.douyinAvatar" :src="r.douyinAvatar" class="w-10 h-10 rounded object-cover border border-gray-200" />
                <div v-else class="w-10 h-10 rounded bg-gray-100 border border-gray-200"></div>
                <input :id="`f-${r.id}-douyin`" type="file" accept="image/*" class="hidden" @change="onUpload(r, 'douyin', $event)" />
                <button class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200" @click="pick(r, 'douyin')">上传</button>
                <button
                  class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200"
                  :disabled="!r.douyinAvatar"
                  @click="clear(r, 'douyin')"
                >
                  清除
                </button>
              </div>
            </td>
            <td class="py-2">
              <div class="flex items-center space-x-2">
                <img v-if="r.shipinAvatar" :src="r.shipinAvatar" class="w-10 h-10 rounded object-cover border border-gray-200" />
                <div v-else class="w-10 h-10 rounded bg-gray-100 border border-gray-200"></div>
                <input :id="`f-${r.id}-shipin`" type="file" accept="image/*" class="hidden" @change="onUpload(r, 'shipin', $event)" />
                <button class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200" @click="pick(r, 'shipin')">上传</button>
                <button
                  class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200"
                  :disabled="!r.shipinAvatar"
                  @click="clear(r, 'shipin')"
                >
                  清除
                </button>
              </div>
            </td>
            <td class="py-2">
              <div class="flex items-center space-x-2">
                <img v-if="r.xiaohongshuAvatar" :src="r.xiaohongshuAvatar" class="w-10 h-10 rounded object-cover border border-gray-200" />
                <div v-else class="w-10 h-10 rounded bg-gray-100 border border-gray-200"></div>
                <input
                  :id="`f-${r.id}-xiaohongshu`"
                  type="file"
                  accept="image/*"
                  class="hidden"
                  @change="onUpload(r, 'xiaohongshu', $event)"
                />
                <button class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200" @click="pick(r, 'xiaohongshu')">上传</button>
                <button
                  class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200"
                  :disabled="!r.xiaohongshuAvatar"
                  @click="clear(r, 'xiaohongshu')"
                >
                  清除
                </button>
              </div>
            </td>
            <td class="py-2 max-w-sm">
              <div class="flex items-center space-x-2">
                <div class="truncate text-gray-700" :title="r.welcomeMessage || ''">
                  {{ r.welcomeMessage || '' }}
                </div>
                <button class="px-2 py-1 rounded text-xs bg-gray-100 hover:bg-gray-200" @click="openEditWelcome(r)">编辑</button>
              </div>
            </td>
            <td class="py-2">
              <button
                class="px-2 py-1 rounded text-xs"
                :class="r.status ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'"
                @click="toggle(r)"
              >
                {{ r.status ? '启用' : '停用' }}
              </button>
            </td>
            <td class="py-2">
              <button class="px-2 py-1 rounded text-xs bg-red-100 text-red-700 hover:bg-red-200" @click="del(r)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!loading && routes.length === 0" class="text-sm text-gray-600">暂无数据</div>
    </div>
  </div>

  <div v-if="createModalOpen" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
    <div class="bg-white rounded-lg w-full max-w-md border border-gray-200">
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-medium text-gray-800">新增图像</div>
        <button class="text-gray-500 hover:text-gray-700" @click="closeCreate">关闭</button>
      </div>
      <form class="p-4 space-y-3" @submit.prevent="submitCreate">
        <div>
          <div class="text-sm text-gray-700 mb-1">业务图像名称</div>
          <input v-model.trim="createForm.routeName" class="w-full border border-gray-300 rounded px-3 py-2 text-sm" />
        </div>
        <div>
          <div class="text-sm text-gray-700 mb-1">业务图像英文名</div>
          <input v-model.trim="createForm.routeValue" class="w-full border border-gray-300 rounded px-3 py-2 text-sm" />
        </div>
        <div class="flex justify-end space-x-2 pt-2">
          <button type="button" class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm" @click="closeCreate">取消</button>
          <button class="px-3 py-2 rounded bg-blue-600 hover:bg-blue-700 text-white text-sm" :disabled="createSubmitting">
            {{ createSubmitting ? '提交中...' : '提交' }}
          </button>
        </div>
      </form>
    </div>
  </div>

  <div v-if="editRouteModalOpen" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
    <div class="bg-white rounded-lg w-full max-w-md border border-gray-200">
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-medium text-gray-800">编辑线路</div>
        <button class="text-gray-500 hover:text-gray-700" @click="closeEditRoute">关闭</button>
      </div>
      <form class="p-4 space-y-3" @submit.prevent="submitEditRoute">
        <div>
          <div class="text-sm text-gray-700 mb-1">线路名称</div>
          <input v-model.trim="editRouteForm.routeName" class="w-full border border-gray-300 rounded px-3 py-2 text-sm" />
        </div>
        <div>
          <div class="text-sm text-gray-700 mb-1">线路值</div>
          <input v-model.trim="editRouteForm.routeValue" class="w-full border border-gray-300 rounded px-3 py-2 text-sm" />
        </div>
        <div class="flex justify-end space-x-2 pt-2">
          <button type="button" class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm" @click="closeEditRoute">取消</button>
          <button class="px-3 py-2 rounded bg-blue-600 hover:bg-blue-700 text-white text-sm" :disabled="editRouteSubmitting">
            {{ editRouteSubmitting ? '保存中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>
  </div>

  <div v-if="editWelcomeModalOpen" class="fixed inset-0 bg-black/50 flex items-center justify-center p-4">
    <div class="bg-white rounded-lg w-full max-w-xl border border-gray-200">
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <div class="font-medium text-gray-800">编辑欢迎语</div>
        <button class="text-gray-500 hover:text-gray-700" @click="closeEditWelcome">关闭</button>
      </div>
      <form class="p-4 space-y-3" @submit.prevent="submitEditWelcome">
        <textarea
          v-model.trim="editWelcomeForm.welcomeMessage"
          class="w-full border border-gray-300 rounded px-3 py-2 text-sm resize-none"
          rows="8"
          maxlength="1000"
        ></textarea>
        <div class="flex justify-between items-center text-xs text-gray-500">
          <div>{{ (editWelcomeForm.welcomeMessage || '').length }}/1000</div>
          <div class="space-x-2">
            <button type="button" class="px-3 py-2 rounded bg-gray-100 hover:bg-gray-200 text-sm" @click="closeEditWelcome">取消</button>
            <button class="px-3 py-2 rounded bg-blue-600 hover:bg-blue-700 text-white text-sm" :disabled="editWelcomeSubmitting">
              {{ editWelcomeSubmitting ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const routes = ref([])
const loading = ref(false)
const error = ref('')

const createModalOpen = ref(false)
const createSubmitting = ref(false)
const createForm = ref({ routeName: '', routeValue: '' })

const editRouteModalOpen = ref(false)
const editRouteSubmitting = ref(false)
const editRouteForm = ref({ id: null, routeName: '', routeValue: '' })

const editWelcomeModalOpen = ref(false)
const editWelcomeSubmitting = ref(false)
const editWelcomeForm = ref({ id: null, welcomeMessage: '' })

async function load() {
  error.value = ''
  loading.value = true
  try {
    const resp = await window.SiyuBaoBackend.routes.list()
    if (!resp.success) throw new Error(resp.message || '加载失败')
    routes.value = (resp.data || []).filter(Boolean)
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  createForm.value = { routeName: '', routeValue: '' }
  createModalOpen.value = true
}

function closeCreate() {
  createModalOpen.value = false
}

async function submitCreate() {
  error.value = ''
  if (!createForm.value.routeName || !createForm.value.routeValue) {
    error.value = '请输入线路名称和线路值'
    return
  }
  createSubmitting.value = true
  try {
    const resp = await window.SiyuBaoBackend.routes.create(createForm.value.routeName, createForm.value.routeValue)
    if (!resp.success) throw new Error(resp.message || '创建失败')
    createModalOpen.value = false
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    createSubmitting.value = false
  }
}

async function toggle(r) {
  error.value = ''
  try {
    const resp = await window.SiyuBaoBackend.routes.updateStatus(r.id, !r.status)
    if (!resp.success) throw new Error(resp.message || '更新失败')
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  }
}

function pick(r, platform) {
  const el = document.getElementById(`f-${r.id}-${platform}`)
  if (el) el.click()
}

async function onUpload(r, platform, evt) {
  error.value = ''
  const file = evt?.target?.files?.[0]
  evt.target.value = ''
  if (!file) return
  try {
    const resp = await window.SiyuBaoBackend.routes.uploadAvatar(r.id, platform, file)
    if (!resp.success) throw new Error(resp.message || '上传失败')
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  }
}

async function clear(r, platform) {
  error.value = ''
  try {
    const resp = await window.SiyuBaoBackend.routes.clearAvatar(r.id, platform)
    if (!resp.success) throw new Error(resp.message || '清除失败')
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  }
}

function openEditWelcome(r) {
  editWelcomeForm.value = { id: r.id, welcomeMessage: r.welcomeMessage || '' }
  editWelcomeModalOpen.value = true
}

function closeEditWelcome() {
  editWelcomeModalOpen.value = false
}

async function submitEditWelcome() {
  error.value = ''
  editWelcomeSubmitting.value = true
  try {
    const resp = await window.SiyuBaoBackend.routes.updateWelcomeMessage(editWelcomeForm.value.id, editWelcomeForm.value.welcomeMessage || '')
    if (!resp.success) throw new Error(resp.message || '更新失败')
    editWelcomeModalOpen.value = false
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    editWelcomeSubmitting.value = false
  }
}

function openEditRoute(r) {
  editRouteForm.value = { id: r.id, routeName: r.routeName || '', routeValue: r.routeValue || '' }
  editRouteModalOpen.value = true
}

function closeEditRoute() {
  editRouteModalOpen.value = false
}

async function submitEditRoute() {
  error.value = ''
  if (!editRouteForm.value.routeName || !editRouteForm.value.routeValue) {
    error.value = '请输入线路名称和线路值'
    return
  }
  editRouteSubmitting.value = true
  try {
    const resp = await window.SiyuBaoBackend.routes.update(editRouteForm.value.id, editRouteForm.value.routeName, editRouteForm.value.routeValue)
    if (!resp.success) throw new Error(resp.message || '更新失败')
    editRouteModalOpen.value = false
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  } finally {
    editRouteSubmitting.value = false
  }
}

async function del(r) {
  error.value = ''
  if (!window.confirm('确定删除？')) return
  try {
    const resp = await window.SiyuBaoBackend.routes.delete(r.id)
    if (!resp.success) throw new Error(resp.message || '删除失败')
    await load()
  } catch (e) {
    error.value = e?.message || String(e)
  }
}

onMounted(load)
</script>
