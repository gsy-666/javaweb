<script setup lang="ts">
import { computed, watch } from 'vue'
import type { SituationEvent } from '../../situation/types'

const props = defineProps<{
  events: SituationEvent[]
  focusWorkOrderId: number | null
}>()

const emit = defineEmits<{ (e: 'focus', workOrderId: number): void }>()

const grouped = computed(() => {
  const map = new Map<number, SituationEvent[]>()
  for (const e of props.events) {
    if (!map.has(e.workOrderId)) map.set(e.workOrderId, [])
    map.get(e.workOrderId)!.push(e)
  }
  return Array.from(map.entries())
    .map(([workOrderId, list]) => ({ workOrderId, list: list.sort((a, b) => b.ts - a.ts) }))
    .sort((a, b) => (b.list[0]?.ts || 0) - (a.list[0]?.ts || 0))
})

function fmt(ts: number) {
  const d = new Date(ts)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

watch(
  () => props.events.length,
  () => {
    // CSS animation handles insertion; keep logic simple
  }
)
</script>

<template>
  <div class="stream">
    <div class="stream-title">
      <div class="t">事件流</div>
      <div class="s">以工单为中心的叙事主线</div>
    </div>

    <div class="wo-group" v-for="g in grouped" :key="g.workOrderId">
      <button class="wo-head" :class="{ active: focusWorkOrderId === g.workOrderId }" @click="emit('focus', g.workOrderId)">
        <div class="wo-id">#{{ g.workOrderId }}</div>
        <div class="wo-last">
          <span class="time">{{ fmt(g.list[0].ts) }}</span>
          <span class="title">{{ g.list[0].title }}</span>
        </div>
        <span class="pill" :class="g.list[0].severity">{{ g.list[0].severity }}</span>
      </button>

      <ul class="event-list" :class="{ dim: focusWorkOrderId != null && focusWorkOrderId !== g.workOrderId }">
        <li v-for="e in g.list.slice(0, 8)" :key="e.id" class="event-item" :class="e.severity">
          <span class="dot" />
          <span class="time">{{ fmt(e.ts) }}</span>
          <span class="content">
            <span class="main">{{ e.title }}</span>
            <span v-if="e.subtitle" class="sub">{{ e.subtitle }}</span>
          </span>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.stream {
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(10px) saturate(140%);
  padding: 14px;
}

.stream-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}

.t {
  font-weight: 900;
  letter-spacing: 0.02em;
}

.s {
  color: var(--sr-text2);
  font-size: 12px;
}

.wo-group {
  margin-top: 12px;
}

.wo-head {
  width: 100%;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
  border-radius: 14px;
  padding: 10px 12px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255, 255, 255, 0.60);
}

.wo-head.active {
  border-color: color-mix(in srgb, var(--sr-brand) 35%, rgba(15, 23, 42, 0.12));
  background: linear-gradient(135deg, color-mix(in srgb, var(--sr-brand2) 10%, white), rgba(255, 255, 255, 0.62));
}

.wo-id {
  font-weight: 900;
  color: var(--sr-text1);
}

.wo-last {
  display: flex;
  gap: 10px;
  align-items: baseline;
  overflow: hidden;
}

.time {
  font-variant-numeric: tabular-nums;
  color: var(--sr-text2);
  font-size: 12px;
  white-space: nowrap;
}

.title {
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pill {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.pill.info {
  color: #2563eb;
}

.pill.warn {
  color: #b45309;
}

.pill.danger {
  color: #b91c1c;
}

.event-list {
  margin: 8px 0 0;
  padding: 0 0 0 10px;
  list-style: none;
  display: grid;
  gap: 6px;
}

.event-list.dim {
  opacity: 0.55;
}

.event-item {
  display: grid;
  grid-template-columns: 10px 44px 1fr;
  gap: 10px;
  align-items: start;
  padding: 6px 8px;
  border-radius: 12px;
  animation: in 220ms ease;
}

.event-item:hover {
  background: rgba(15, 23, 42, 0.04);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  margin-top: 5px;
  background: rgba(100, 116, 139, 0.8);
}

.event-item.warn .dot {
  background: #f59e0b;
}

.event-item.danger .dot {
  background: #ef4444;
}

.content {
  display: grid;
  gap: 2px;
}

.main {
  font-weight: 650;
}

.sub {
  color: var(--sr-text2);
  font-size: 12px;
  line-height: 1.25;
}

@keyframes in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
