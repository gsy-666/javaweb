<template>
	  <header class="hdr">
	    <div class="brand">
	      <div class="logo">SR</div>
	      <div class="title-wrap">
	        <div class="title">{{ appTitle }}</div>
	        <div class="subtitle">{{ subtitle }}</div>
	      </div>
	    </div>

	    <div class="spacer" />

	    <div class="right">
	      <div class="sys">
	        <div class="dt">{{ nowText }}</div>
	        <div class="wx">{{ dayText }} · {{ weatherText }}</div>
	      </div>

	      <div class="user-area" ref="root" @pointerdown.stop>
	        <button class="user-btn" @click="toggle" type="button">
	          <div class="avatar">{{ initials }}</div>
	          <div class="meta">
	            <div class="account">{{ accountLabel }}</div>
	            <div class="role">{{ role }}</div>
	          </div>
	          <div class="chev">▾</div>
	        </button>
	      </div>
	    </div>

	    <Teleport to="body">
	      <div v-if="open" class="menu menu-tele" :style="menuStyle" @pointerdown.stop>
	        <button class="item" @click="goMe" type="button">个人中心</button>
	        <button class="item danger" @click="doLogout" type="button">退出登录</button>
	      </div>
	    </Teleport>
	  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { account, role, logout } from '../user'
import { api } from '../api'
import { appTitle } from '../appMeta'

const router = useRouter()
const route = useRoute()
const open = ref(false)
const root = ref<HTMLElement | null>(null)

watch(
  () => route.fullPath,
  () => {
    open.value = false
  }
)

const accountLabel = computed(() => account.value || '未登录')

const initials = computed(() => {
  const a = (account.value || '').trim()
  if (!a) return 'U'
  return a.slice(0, 1).toUpperCase()
})

const subtitle = computed(() => {
  if (route.path.startsWith('/admin')) return '管理端 · 调度态势面板'
  if (route.path.startsWith('/worker')) return '工人端 · 工作台'
  return '报修端 · 服务入口'
})

const now = ref(new Date())
let timer: number | null = null

const pad2 = (n: number) => String(n).padStart(2, '0')

const nowText = computed(() => {
  const d = now.value
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
})

const dayText = computed(() => {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[now.value.getDay()] || ''
})

const weatherText = computed(() => '25°C 多云')

const menuStyle = computed(() => {
  const btn = root.value?.querySelector('.user-btn') as HTMLElement | null
  if (!btn) return {}
  const r = btn.getBoundingClientRect()
  const width = 200
  return {
    position: 'fixed',
    left: `${Math.max(8, r.right - width)}px`,
    top: `${r.bottom + 8}px`,
    zIndex: 99999,
    width: `${width}px`,
  } as any
})

function toggle() {
  open.value = !open.value
}

function close() {
  open.value = false
}

function onDocClick(e: PointerEvent) {
  const el = root.value
  if (!el) return
  const path = (e as any).composedPath?.() as EventTarget[] | undefined
  if (path?.includes(el)) return
  if (e.target instanceof Node && el.contains(e.target)) return
  close()
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
}

async function doLogout() {
  try {
    await api.post('/auth/logout')
  } catch {
    // ignore
  } finally {
    logout()
    close()
    await router.replace('/login')
    window.location.assign('/login')
  }
}

function goMe() {
  close()
  router.push('/me')
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocClick)
  document.addEventListener('keydown', onKey)
  timer = window.setInterval(() => (now.value = new Date()), 1000)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocClick)
  document.removeEventListener('keydown', onKey)
  if (timer != null) window.clearInterval(timer)
})
</script>

<style scoped>
.hdr {
  position: sticky;
  top: 10px;
  margin-bottom: 20px;
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 16px;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--sr-border) 70%, transparent);
  background: linear-gradient(
      135deg,
      color-mix(in srgb, var(--sr-brand2) 10%, transparent),
      transparent 55%
    ),
    color-mix(in srgb, var(--sr-panel) 78%, transparent);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  padding: 12px 20px;
  transition: all 0.3s ease;
  box-shadow: var(--sr-shadow2);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.logo {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 14px;
  color: #38bdf8;
  background: rgba(56, 189, 248, 0.1);
  border: 1px solid rgba(56, 189, 248, 0.2);
  box-shadow: 0 0 12px rgba(56, 189, 248, 0.15);
  flex: 0 0 auto;
}

.title-wrap {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.title {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.03em;
  color: var(--sr-text1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.subtitle {
  font-size: 11px;
  color: var(--sr-text2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.spacer {
  flex: 1;
  min-width: 20px;
}

.right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sys {
  text-align: right;
  line-height: 1.2;
  padding: 6px 12px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--sr-panel2) 70%, transparent);
  border: 1px solid var(--sr-border);
}

.dt {
  font-variant-numeric: tabular-nums;
  font-weight: 800;
  font-size: 12px;
  color: var(--sr-text1);
}

.wx {
  margin-top: 2px;
  font-size: 10px;
  color: var(--sr-text2);
}

.user-area {
  position: relative;
  flex: 0 0 auto;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px 6px 6px;
  border-radius: 12px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel2) 70%, transparent);
  color: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  max-width: min(360px, 44vw);
}

.user-btn:hover {
  background: color-mix(in srgb, var(--sr-brand) 6%, transparent);
  border-color: var(--sr-border2);
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 12px;
  color: var(--sr-text1);
  background: color-mix(in srgb, var(--sr-brand) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--sr-brand) 22%, var(--sr-border));
  flex: 0 0 auto;
}

.meta {
  display: grid;
  text-align: left;
  gap: 2px;
  min-width: 0;
}

.account {
  font-size: 13px;
  font-weight: 900;
  color: var(--sr-text1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role {
  font-size: 10px;
  color: var(--sr-text2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.chev {
  opacity: 0.8;
  font-size: 12px;
  flex: 0 0 auto;
}

.menu {
  min-width: 200px;
  border-radius: 14px;
  border: 1px solid var(--sr-border);
  background: var(--sr-panel2);
  color: var(--sr-text1);
  backdrop-filter: blur(12px) saturate(140%);
  overflow: hidden;
  z-index: 1000;
  box-shadow: var(--sr-shadow2);
}

.item {
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  background: transparent;
  border: 0;
  color: inherit;
  cursor: pointer;
}

.item:hover {
  background: color-mix(in srgb, var(--sr-brand) 12%, transparent);
}

.item.danger {
  color: color-mix(in srgb, var(--sr-danger) 70%, var(--sr-text1));
}

@media (max-width: 820px) {
  .sys {
    display: none;
  }
}

@media (max-width: 520px) {
  .subtitle {
    display: none;
  }
  .user-btn .meta {
    display: none;
  }
}
</style>
