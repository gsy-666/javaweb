<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import type { SituationEvent, WorkOrder, WorkOrderProgress } from '../situation/types'
import { normalizeProgressEvents, synthesizeSlaEvent } from '../situation/normalize'
import { focusState, focusWorkOrder } from '../situation/focusState'

import PageContainer from '../components/PageContainer.vue'
import UiCard from '../components/UiCard.vue'
import EventStream from '../components/situation/EventStream.vue'
import SpatialPanel from '../components/situation/SpatialPanel.vue'
import ContextDrawer from '../components/situation/ContextDrawer.vue'

const loading = ref(false)
const err = ref<string | null>(null)
const workOrders = ref<WorkOrder[]>([])
const progressCache = ref(new Map<number, WorkOrderProgress[]>())

const filteredWorkOrders = computed(() => {
  const list = workOrders.value
  const f = focusState.filters

  return list.filter((wo) => {
    if (f.onlyEscalated && (wo.escalationLevel || 0) <= 0) return false
    if (f.tradeCode && String(wo.tradeCode || '') !== f.tradeCode) return false
    if (f.statuses.length && !f.statuses.includes(String(wo.status))) return false
    return true
  })
})

const pendingCnt = computed(
  () => filteredWorkOrders.value.filter((x) => String(x.status).includes('ASSIGN') || String(x.status).includes('PEND')).length
)
const doneCnt = computed(() => filteredWorkOrders.value.filter((x) => String(x.status).includes('DONE')).length)
const warnCnt = computed(() => filteredWorkOrders.value.filter((x) => (x.escalationLevel || 0) > 0).length)

async function loadList() {
  loading.value = true
  err.value = null
  try {
    const resp = await api.get('/work-orders')
    workOrders.value = resp.data?.data || []
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function ensureProgress(woId: number) {
  if (progressCache.value.has(woId)) return
  try {
    const resp = await api.get(`/work-orders/${woId}`)
    const data = resp.data?.data
    const progress = (data?.progress || []) as WorkOrderProgress[]
    progressCache.value.set(woId, progress)
  } catch {
    // ignore
  }
}

const events = computed<SituationEvent[]>(() => {
  const out: SituationEvent[] = []

  for (const wo of filteredWorkOrders.value) {
    const slaEv = synthesizeSlaEvent(wo)
    if (slaEv) out.push(slaEv)

    const progress = progressCache.value.get(wo.id)
    if (progress && progress.length) {
      out.push(...normalizeProgressEvents(progress))
    } else {
      out.push({
        id: `snap:${wo.id}:${String(wo.status)}`,
        ts: Date.now() - 1,
        workOrderId: wo.id,
        kind: 'NOTE',
        title: `当前状态：${String(wo.status)}`,
        severity: (wo.escalationLevel || 0) > 0 ? 'warn' : 'info',
        geo: wo.lng != null && wo.lat != null ? { lng: wo.lng, lat: wo.lat } : undefined,
        tags: wo.tradeCode ? [String(wo.tradeCode)] : undefined,
      })
    }
  }

  return out.sort((a, b) => b.ts - a.ts)
})

function onFocus(woId: number) {
  focusWorkOrder(woId)
  ensureProgress(woId)
}

const drawerOpen = computed(() => focusState.focusWorkOrderId != null)

onMounted(async () => {
  await loadList()
})
</script>

<template>
  <PageContainer>
    <div class="user-situation">
      <UiCard class="hero" padded>
        <template #title>
          <div class="ttl">
            <div class="h">服务进度</div>
            <div class="muted tiny">提交报修 · 跟踪处理 · 沟通与评价</div>
          </div>
        </template>
        <template #extra>
          <div class="row">
            <button class="primary" @click="loadList" :disabled="loading">{{ loading ? '同步中…' : '刷新' }}</button>
          </div>
        </template>

        <div v-if="err" class="error-text" style="margin-bottom: 10px">{{ err }}</div>

        <div class="kpis">
          <div class="kpi" data-k="a">
            <div class="num">{{ filteredWorkOrders.length }}</div>
            <div class="lab">全部</div>
          </div>
          <div class="kpi" data-k="b">
            <div class="num">{{ pendingCnt }}</div>
            <div class="lab">处理中</div>
          </div>
          <div class="kpi" data-k="c">
            <div class="num">{{ doneCnt }}</div>
            <div class="lab">已完成</div>
          </div>
          <div class="kpi" data-k="d">
            <div class="num">{{ warnCnt }}</div>
            <div class="lab">预警</div>
          </div>
        </div>

        <div class="muted tip">提示：点选工单可查看详情与处理记录；如已完成可进入详情进行评价。</div>
      </UiCard>

      <div class="panel-grid">
        <UiCard title="动态" class="panel">
          <EventStream :events="events" :focusWorkOrderId="focusState.focusWorkOrderId" @focus="onFocus" />
        </UiCard>
        <UiCard title="位置" class="panel">
          <SpatialPanel :workOrders="filteredWorkOrders" :focusWorkOrderId="focusState.focusWorkOrderId" @focus="onFocus" />
        </UiCard>
      </div>

      <ContextDrawer
        :open="drawerOpen"
        :workOrderId="focusState.focusWorkOrderId"
        :role="'USER' as any"
        @close="focusWorkOrder(null)"
        @changed="loadList"
      />
    </div>
  </PageContainer>
</template>

<style scoped>
.user-situation {
  display: grid;
  gap: 12px;
}

.hero {
  background: linear-gradient(135deg, color-mix(in srgb, var(--sr-brand2) 18%, transparent), transparent 55%),
    linear-gradient(180deg, var(--sr-panel), color-mix(in srgb, var(--sr-panel2) 70%, transparent));
}

.ttl {
  display: grid;
  gap: 2px;
}

.h {
  font-weight: 950;
}

.tiny {
  font-size: 12px;
}

.kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.kpi {
  border: 1px solid var(--sr-border);
  border-radius: 18px;
  padding: 12px;
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
}

.kpi[data-k='a'] {
  background: color-mix(in srgb, var(--sr-brand2) 10%, transparent);
}

.kpi[data-k='b'] {
  background: color-mix(in srgb, #34d399 10%, transparent);
}

.kpi[data-k='c'] {
  background: color-mix(in srgb, #a78bfa 10%, transparent);
}

.kpi[data-k='d'] {
  background: color-mix(in srgb, #fbbf24 10%, transparent);
}

.num {
  font-size: 22px;
  font-weight: 950;
}

.lab {
  margin-top: 4px;
  font-size: 12px;
  color: var(--sr-text2);
}

.tip {
  margin-top: 10px;
  font-size: 12px;
}

.panel-grid {
  display: grid;
  grid-template-columns: 1.25fr 1fr;
  gap: 12px;
  align-items: start;
}

.panel {
  min-width: 0;
}

@media (max-width: 1000px) {
  .kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
