<template>
  <div class="space-y-4">
    <div class="bg-white border border-gray-200 rounded-lg p-6">
      <div class="text-lg font-medium text-gray-900">系统设置</div>
      <div class="mt-2 text-sm text-gray-600">常用入口</div>
      <div class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-3">
        <button class="border border-gray-200 rounded-lg p-4 text-left hover:border-gray-300" @click="go('/route')">
          <div class="font-medium text-gray-900">图像管理</div>
          <div class="mt-1 text-xs text-gray-500">管理头像与欢迎语</div>
        </button>
        <button class="border border-gray-200 rounded-lg p-4 text-left hover:border-gray-300" @click="go('/card-message')">
          <div class="font-medium text-gray-900">对话内容</div>
          <div class="mt-1 text-xs text-gray-500">历史记录与编辑</div>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function go(p) {
  router.push(p)
}
</script>

