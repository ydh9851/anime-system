import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import { fileURLToPath, URL } from 'node:url'
import path from 'node:path'

// https://vitejs.dev/config/
export default defineConfig({
  // 与旧 vue-cli 的 publicPath: './' 保持一致，产物可用相对路径部署
  base: './',
  plugins: [
    vue(),
    // svg 雪碧图：替代 webpack 的 svg-sprite-loader
    createSvgIconsPlugin({
      iconDirs: [path.resolve(process.cwd(), 'src/icons/svg')],
      symbolId: 'icon-[name]'
    })
  ],
  resolve: {
    // webpack 迁移过来大量省略 .vue 后缀的 import，这里补上扩展名解析
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue'],
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: true,
    port: 8002,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true
      },
      '/eda': {
        target: 'http://localhost:8000',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'admin',
    assetsDir: 'static',
    sourcemap: false,
    chunkSizeWarningLimit: 4000,
    // 每次构建前清空输出目录：资源名带内容哈希，残留的历史 chunk 会不断累积
    // （实测累积到 571 个文件 / 28 MB），清理后仅保留当前版本产物。
    emptyOutDir: true
  }
})
