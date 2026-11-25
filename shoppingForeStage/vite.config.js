import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/** 开发代理：API 转发 Gateway:8080；/admin HTML 路由 bypass 给 Vue Router */
export default defineConfig({
  plugins: [vue()],
  server: {
    //historyApiFallback: true,  // 或者 appType: 'spa'
    port: 5173,
    host: '127.0.0.1',
    hmr: {
      host: '127.0.0.1',
      port: 5173
    },
    proxy: {
      '/auth': { target: 'http://localhost:8080', changeOrigin: true },
      '/goods': { target: 'http://localhost:8080', changeOrigin: true },
      '/cart': { target: 'http://localhost:8080', changeOrigin: true  , bypass(req) {
          const accept = req.headers.accept || ''
          if (accept.includes('text/html')) {
            return '/index.html'
          }
        }},
      '/order': { target: 'http://localhost:8080', changeOrigin: true },
      '/alipay': { target: 'http://localhost:8080', changeOrigin: true },
      '/user': { target: 'http://localhost:8080', changeOrigin: true },
      '/coupon': { target: 'http://localhost:8080', changeOrigin: true },
      '/admin': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        bypass(req) {
          // 浏览器访问 /admin/login 等页面时不代理，避免与后台 API 路径冲突
          const accept = req.headers.accept || ''
          if (accept.includes('text/html')) {
            return '/index.html'
          }
        }
      },
      '/ai': { target: 'http://localhost:8080', changeOrigin: true ,bypass(req) {
          const accept = req.headers.accept || ''
          if (accept.includes('text/html')) {
            return '/index.html'
          }
        } },
      '/ws': { target: 'http://localhost:8088', ws: true, changeOrigin: true }
    }
  }
})
