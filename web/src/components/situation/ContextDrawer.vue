<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { api } from '../../api'
import type { Role, WorkOrder, WorkOrderProgress } from '../../situation/types'
import { getAllowedActions, getStatusLabel } from '../../situation/statusMeta'
import { explainDispatchScore } from '../../situation/dispatchExplain'

const props = defineProps<{ open: boolean; workOrderId: number | null; role: Role }>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'changed'): void }>()

const loading = ref(false)
const err = ref<string | null>(null)
const detail = ref<any>(null)

const workOrder = computed<WorkOrder | null>(() => detail.value?.workOrder || null)
const progress = computed<WorkOrderProgress[]>(() => detail.value?.progress || [])

const allowed = computed(() => {
  if (!workOrder.value) return []
  return getAllowedActions(String(workOrder.value.status), props.role)
})

const dispatchExplain = computed(() => {
  const wo = workOrder.value
  if (!wo) return null
  if (String(wo.status) !== 'ASSIGNED') return null
  // We don't have assignment audit rows via API yet; approximate explainability with placeholders
  return explainDispatchScore({ distanceKm: null, activeCount: null, performanceScore: 0.5 })
})

async function load() {
  if (!props.workOrderId) return
  loading.value = true
  err.value = null
  try {
    const resp = await api.get(`/api/work-orders/${props.workOrderId}`)
    detail.value = resp.data?.data
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

watch(
  () => props.workOrderId,
  async () => {
    detail.value = null
    if (props.open && props.workOrderId) await load()
  }
)

watch(
  () => props.open,
  async (v) => {
    if (v && props.workOrderId) await load()
  }
)

onMounted(async () => {
  if (props.open && props.workOrderId) await load()
})

async function act(name: string) {
  const woId = props.workOrderId
  if (!woId) return
  err.value = null
  try {
    if (name === '接单') await api.post(`/api/work-orders/${woId}/accept`)
    else if (name === '拒单') await api.post(`/api/work-orders/${woId}/reject`, null, { params: { reason: '忙碌/不方便' } })
    else if (name === '更新进度') await api.post(`/api/work-orders/${woId}/progress`, { message: '已到达现场，开始处理' })
    else if (name === '完工') await api.post(`/api/work-orders/${woId}/finish`, null, { params: { message: '已修复完成' } })
    else if (name === '评价') await api.post(`/api/work-orders/${woId}/rating`, { stars: 5, tags: ['服务态度好'], comment: '很好' })
    else if (name === '取消') await api.post(`/api/work-orders/${woId}/cancel`)
    else return

    await load()
    emit('changed')
  } catch (e: any) {
    err.value = e?.message || '操作失败'
  }
}

function fmtTime(s?: string) {
  if (!s) return ''
  const t = Date.parse(s)
  if (!Number.isFinite(t)) return s
  const d = new Date(t)
  return d.toLocaleString()
}
</script>

<template>
  <a-drawer :open="open" placement="right" width="420" :closable="false" @close="emit('close')">
    <template #title>
      <div style="display: flex; justify-content: space-between; align-items: baseline; gap: 10px">
        <div style="font-weight: 900">工单上下文</div>
        <a-button size="small" @click="emit('close')">关闭</a-button>
      </div>
    </template>

    <div v-if="loading" class="muted">加载中...</div>
    <div v-if="err" class="error-text" style="margin-bottom: 10px">{{ err }}</div>

    <template v-if="workOrder">
      <div class="panel" style="padding: 12px">
        <div class="row" style="justify-content: space-between">
          <div style="font-weight: 900">#{{ workOrder.id }}</div>
          <span class="chip">{{ getStatusLabel(String(workOrder.status)) }}</span>
        </div>
        <div class="muted" style="margin-top: 8px">{{ workOrder.address || '-' }}</div>
        <div style="margin-top: 10px">{{ workOrder.description || '-' }}</div>
      </div>

      <div v-if="dispatchExplain" class="panel" style="padding: 12px; margin-top: 10px">
        <div style="font-weight: 800">派单解释</div>
        <div class="muted" style="margin-top: 6px">
          {{ dispatchExplain.summary }}
        </div>
      </div>

      <div class="panel" style="padding: 12px; margin-top: 10px">
        <div style="font-weight: 800; margin-bottom: 8px">下一步动作</div>
        <div class="row">
          <a-button v-for="a in allowed" :key="a" type="primary" @click="act(a)">{{ a }}</a-button>
        </div>
        <div class="muted" style="margin-top: 8px">动作集合由状态与角色驱动。</div>
      </div>

      <div class="panel" style="padding: 12px; margin-top: 10px">
        <div style="font-weight: 800; margin-bottom: 8px">事件时间轴</div>
        <ul style="margin: 0; padding-left: 18px; display: grid; gap: 8px">
          <li v-for="p in progress" :key="p.id">
            <span class="muted">{{ fmtTime(p.createdAt) }}</span>
            <span style="margin-left: 8px">
              {{ p.fromStatus }} → {{ p.toStatus }}：{{ p.message }}
            </span>
          </li>
        </ul>
      </div>
    </template>

    <div v-else-if="!loading" class="muted">请选择一条工单以查看上下文。</div>
  </a-drawer>
</template>
