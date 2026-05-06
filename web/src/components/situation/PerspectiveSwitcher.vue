<script setup lang="ts">
import { computed } from 'vue'
import type { Role } from '../../situation/types'
import { account } from '../../user'

const props = defineProps<{ modelValue: Role }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: Role): void }>()

const roles: { key: Role; label: string; desc: string }[] = [
  { key: 'USER', label: '用户视角', desc: '报修提交 · 反馈评价 · 进度追踪' },
  { key: 'WORKER', label: '维修视角', desc: '接单处理 · 现场反馈 · 协同沟通' },
  { key: 'ADMIN', label: '调度视角', desc: '全局态势 · 风险预警 · 策略干预' },
]

const active = computed(() => props.modelValue)

function setRole(r: Role) {
  emit('update:modelValue', r)
}
</script>

<template>
  <div class="switcher">
    <div class="switcher-head">
      <div class="logo">SmartRepair</div>
      <div class="meta">
        <div class="meta-title">调度态势面板</div>
        <div class="meta-sub">账号：{{ account || '-' }}</div>
      </div>
    </div>

    <div class="switcher-block">
      <div class="block-title">角色视角</div>
      <div class="role-list">
        <button
          v-for="r in roles"
          :key="r.key"
          class="role-item"
          :class="{ active: active === r.key }"
          @click="setRole(r.key)"
        >
          <div class="role-top">
            <span class="dot" />
            <span class="role-label">{{ r.label }}</span>
          </div>
          <div class="role-desc">{{ r.desc }}</div>
        </button>
      </div>
    </div>

    <div class="switcher-block">
      <div class="block-title">信号</div>
      <div class="signals">
        <div class="signal">
          <span class="sig-k">告警</span>
          <span class="sig-v">—</span>
        </div>
        <div class="signal">
          <span class="sig-k">升级</span>
          <span class="sig-v">—</span>
        </div>
        <div class="signal">
          <span class="sig-k">在线</span>
          <span class="sig-v">—</span>
        </div>
      </div>
      <div class="hint">信号将由 SLA 与超时升级事件驱动联动。</div>
    </div>
  </div>
</template>

<style scoped>
.switcher {
  height: calc(100vh - 40px);
  position: sticky;
  top: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(2, 6, 23, 0.55);
  backdrop-filter: blur(12px) saturate(140%);
}

.switcher-head {
  display: flex;
  gap: 10px;
  align-items: center;
}

.logo {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: radial-gradient(120% 120% at 20% 20%, rgba(99, 102, 241, 0.35), rgba(34, 211, 238, 0.18) 55%, rgba(2, 6, 23, 0.0));
  border: 1px solid rgba(99, 102, 241, 0.28);
  color: rgba(248, 250, 252, 0.95);
  font-weight: 900;
  font-family: 'Space Grotesk', sans-serif;
  letter-spacing: 0.02em;
}

.meta-title {
  font-weight: 800;
  letter-spacing: 0.02em;
}

.meta-sub {
  color: rgba(148, 163, 184, 0.9);
  font-size: 12px;
  margin-top: 2px;
}

.switcher-block {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.45);
  padding: 12px;
}

.block-title {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
  margin-bottom: 10px;
  letter-spacing: 0.08em;
}

.role-list {
  display: grid;
  gap: 10px;
}

.role-item {
  width: 100%;
  text-align: left;
  border-radius: 14px;
  padding: 10px 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  transition: transform 160ms ease, border-color 160ms ease, background 160ms ease;
}

.role-item .role-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.9);
}

.role-label {
  font-weight: 700;
}

.role-desc {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(148, 163, 184, 0.9);
  line-height: 1.3;
}

.role-item.active {
  border-color: rgba(99, 102, 241, 0.40);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.18), rgba(34, 211, 238, 0.10), rgba(255, 255, 255, 0.03));
  transform: translateY(-1px);
}

.role-item.active .dot {
  background: var(--accent);
}

.signals {
  display: grid;
  gap: 8px;
}

.signal {
  display: flex;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 13px;
}

.sig-k {
  color: rgba(148, 163, 184, 0.95);
}

.sig-v {
  color: rgba(248, 250, 252, 0.95);
  font-weight: 800;
}

.hint {
  margin-top: 10px;
  font-size: 12px;
  color: rgba(148, 163, 184, 0.85);
  line-height: 1.35;
}
</style>
