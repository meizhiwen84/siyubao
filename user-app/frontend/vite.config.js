import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig({
  plugins: [vue()],
  base: '/app/',
  build: {
    outDir: path.resolve(__dirname, '../src/main/resources/static/app'),
    emptyOutDir: true
  }
})
