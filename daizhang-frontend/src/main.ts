import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import App from './App.vue'
import router from './router'
import './styles/global.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局错误处理器:捕获组件渲染期间未处理的JavaScript错误,
// 防止白屏并提供友好的错误提示(仅生产环境,开发环境由Vite覆盖层处理)
app.config.errorHandler = (err, _instance, _info) => {
  console.error('Vue全局错误:', err)
  if (import.meta.env.PROD) {
    ElMessage.error('页面渲染异常，请刷新页面重试')
  }
}

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
