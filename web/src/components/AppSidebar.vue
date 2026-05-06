<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { role } from '../user'

type AppKind = 'admin' | 'worker' | 'user'

const route = useRoute()

const appKind = computed<AppKind>(() => {
  if (role.value === 'ADMIN') return 'admin'
  if (role.value === 'WORKER') return 'worker'
  return 'user'
})

const nav = computed(() => {
  if (appKind.value === 'admin') {
    return [
      { to: '/admin', label: '数据总览' },
      { to: '/admin/orders', label: '工单管理' },
      { to: '/admin/users', label: '用户管理' },
      { to: '/admin/kpi', label: '绩效统计' },
    ]
  }
  if (appKind.value === 'worker') {
    return [{ to: '/worker', label: '工作台' }]
  }
  return [
    { to: '/', label: '首页' },
    { to: '/create', label: '我要报修' },
    { to: '/orders', label: '我的报修' },
    { to: '/me', label: '个人中心' },
  ]
})

const active = computed(() => route.path)

const title = computed(() => {
  if (appKind.value === 'admin') return '管理端'
  if (appKind.value === 'worker') return '工人端'
  return '报修端'
})
</script>

<template>
  <aside class="side" :data-app="appKind">
    <div class="brand">
      <div class="logo">SR</div>
      <div class="meta">
        <div class="name">SmartRepair</div>
        <div class="sub">{{ title }}</div>
      </div>
    </div>

    <nav class="nav">
      <RouterLink v-for="it in nav" :key="it.to" class="nav-item" :class="{ active: active === it.to }" :to="it.to">
        <span class="dot" />
        <span class="label">{{ it.label }}</span>
      </RouterLink>
    </nav>

    <div class="hint">
      <div class="pill">快捷提示</div>
      <div class="txt">左侧为三端菜单风格示例，后续每个页面将统一卡片、表格、表单、详情布局。</div>
    </div>
  </aside>
</template>

<style scoped>
.side {
  position: sticky;
  top: 0;
  height: calc(100vh - 40px);
  border-radius: 18px;
  border: 1px solid var(--sr-border);
  background: linear-gradient(180deg, var(--sr-panel), color-mix(in srgb, var(--sr-panel2) 65%, transparent));
  backdrop-filter: blur(12px) saturate(150%);
  padding: 12px;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 12px;
  box-shadow: var(--sr-shadow2);
}

.brand {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 16px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel2) 70%, transparent);
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: 950;
  background: color-mix(in srgb, var(--sr-brand) 18%, transparent);
  border: 1px solid color-mix(in srgb, var(--sr-brand) 25%, transparent);
}

.name {
  font-weight: 950;
  letter-spacing: 0.02em;
}

.sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--sr-text2);
}

.nav {
  display: grid;
  gap: 8px;
}

.nav-item {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid transparent;
  text-decoration: none;
  color: inherit;
  background: transparent;
  transition: all 180ms cubic-bezier(0.16, 1, 0.3, 1);
}

.nav-item:hover {
  border-color: var(--sr-border);
  background: color-mix(in srgb, var(--sr-brand) 6%, transparent);
}

.nav-item.active {
  border-color: var(--sr-border2);
  background: linear-gradient(135deg, color-mix(in srgb, var(--sr-brand) 14%, transparent), transparent);
  box-shadow: 0 10px 24px color-mix(in srgb, var(--sr-brand) 10%, transparent);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--sr-brand2) 70%, var(--sr-brand));
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--sr-brand) 12%, transparent);
}

.label {
  font-weight: 700;
}

.hint {
  border-radius: 16px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
  padding: 10px;
}

.pill {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid var(--sr-border2);
  font-size: 12px;
  font-weight: 800;
}

.txt {
  margin-top: 8px;
  font-size: 12px;
  color: var(--sr-text2);
  line-height: 1.4;
}

@media (max-width: 1100px) {
  .side {
    position: relative;
    height: auto;
  }
}
</style>
