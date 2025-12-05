import { createApp } from 'vue'
import './style.css'
import App from './App.vue'

// 引入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 引入Element Plus图标
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 引入Element Plus中文语言包
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

// 引入Vue Router
import router from './router'

const app = createApp(App)

// 注册Element Plus并配置中文语言
app.use(ElementPlus, {
  locale: zhCn
})

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 使用Vue Router
app.use(router)

app.mount('#app')
