/**
 * 前端应用入口文件
 * 创建 Vue 应用实例并配置相关插件
 */
import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './tech-theme.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

/**
 * 创建 Vue 应用实例
 */
const app = createApp(App)

/**
 * 安装路由插件
 */
app.use(router)

/**
 * 安装 Element Plus UI 组件库
 */
app.use(ElementPlus)

/**
 * 全局注册 Element Plus 图标组件
 */
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

/**
 * 将应用挂载到 DOM 节点
 */
app.mount('#app')
