import './style.css'
import { createApp, watch } from 'vue'
import App from './App.vue'
import { router } from './router'

import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'

import { applyTheme, getTokens } from './theme'
import { role } from './user'

function syncTheme() {
  const app = role.value === 'ADMIN' ? 'admin' : role.value === 'WORKER' ? 'worker' : 'user'
  applyTheme(getTokens(app))
}

syncTheme()
watch(role, syncTheme)

createApp(App).use(router).use(Antd).mount('#app')
