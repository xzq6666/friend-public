import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import permissionDirective from './directives/permission'
import './assets/styles/variables.css'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus, { size: 'default' })
app.use(createPinia())
app.use(router)
app.use(permissionDirective)

const appInstance = app.mount('#app')

// 初始化用户认证状态
import { useUserStore } from './stores/user'
const userStore = useUserStore()
userStore.initializeAuth()

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('全局错误:', err, info)
  // 打印更详细的错误信息
  if (err?.message) {
    console.error('错误消息:', err.message)
  }
  if (err?.stack) {
    console.error('错误堆栈:', err.stack)
  }
  if (instance) {
    console.error('出错组件:', instance.$options?.name || instance.$options?.__name || '未知组件')
  }
}

export default appInstance
