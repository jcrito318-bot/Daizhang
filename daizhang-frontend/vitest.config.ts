import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// Vitest 配置:
// - environment: jsdom(模拟 DOM,供 Vue 组件挂载)
// - globals: 启用全局 describe/it/expect,简化测试代码
// - coverage: v8 provider,统计测试覆盖率
// - alias: 与 vite.config.ts 保持一致,使用 @ 指向 src
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{js,ts}'],
    exclude: ['node_modules', 'dist', 'target', 'src/**/*.bench.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{ts,vue}'],
      exclude: ['src/**/*.d.ts', 'src/main.ts', 'src/**/*.spec.ts', 'src/**/*.test.ts']
    },
    // mock 处理:css 文件等静态资源在 jsdom 下不需要真实内容
    server: {
      deps: {
        inline: [/element-plus/, /@element-plus/]
      }
    }
  }
})
