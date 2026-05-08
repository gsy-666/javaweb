<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import { RouterLink, useRouter } from 'vue-router'

import PageContainer from '../components/PageContainer.vue'
import UiCard from '../components/UiCard.vue'

type WorkOrder = {
  id: number
  code: string
  tradeCode: string
  status: string
  address?: string
  createdAt?: string
  escalationLevel?: number
  assignedWorkerId?: number | null
}

type WorkerOption = {
  userId: number
  displayName: string
  tradeCode?: string | null
  acceptOrders?: number
}

const list = ref<WorkOrder[]>([])
const workers = ref<WorkerOption[]>([])

const router = useRouter()

const err = ref<string | null>(null)
const workerErr = ref<string | null>(null)
const assigningId = ref<number | null>(null)
const selectedWorkerId = ref<number | null>(null)

const pendingCnt = computed(
  () => list.value.filter((x) => String(x.status).includes('ASSIGN') || String(x.status).includes('PEND')).length
)
const doneCnt = computed(() => list.value.filter((x) => String(x.status).includes('DONE')).length)
const warnCnt = computed(() => list.value.filter((x) => (x.escalationLevel || 0) > 0).length)

function tradeLabel(code: string) {
  if (code === 'WATER_ELEC') return '水电'
  if (code === 'CARPENTER') return '木工'
  if (code === 'NETWORK') return '网络'
  return code
}

async function load() {
  err.value = null
  try {
    const resp = await api.get('/admin/work-orders')
    list.value = resp.data?.data || []
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  }
}

async function loadWorkers() {
  workerErr.value = null
  try {
    const resp = await api.get('/admin/workers')
    workers.value = resp.data?.data || []
  } catch (e: any) {
    workerErr.value = e?.message || '加载维修人员失败'
  }
}

function openAssign(id: number) {
  assigningId.value = id
  selectedWorkerId.value = null
  if (!workers.value.length) loadWorkers()
}

function closeAssign() {
  assigningId.value = null
  selectedWorkerId.value = null
}

async function confirmAssign() {
  if (!assigningId.value) return
  if (!selectedWorkerId.value) return
  err.value = null
  try {
    await api.post(`/admin/work-orders/${assigningId.value}/assign`, { workerId: selectedWorkerId.value })
    closeAssign()
    load()
  } catch (e: any) {
    err.value = e?.message || '分配失败'
  }
}

const selectedWorker = computed(() => {
  if (!selectedWorkerId.value) return null
  return workers.value.find((x) => x.userId === selectedWorkerId.value) || null
})

onMounted(() => {
  load()
  loadWorkers()
})
</script>

<template>
  <PageContainer>
    <div class="orders-grid">
      <UiCard class="hero" padded>
        <template #title>
          <div class="ttl">
            <div class="h">工单管理</div>
            <div class="muted tiny">管理员查看全量工单，并支持人工分配</div>
          </div>
        </template>
        <template #extra>
          <div class="row">
            <button class="primary" @click="load">刷新</button>
          </div>
        </template>

        <div v-if="err" class="error-text" style="margin-bottom: 10px">{{ err }}</div>

        <div class="kpis">
          <div class="kpi" data-k="a">
            <div class="num">{{ list.length }}</div>
            <div class="lab">全部</div>
          </div>
          <div class="kpi" data-k="b">
            <div class="num">{{ pendingCnt }}</div>
            <div class="lab">待处理</div>
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

        <div v-if="workerErr" class="error-text" style="margin-top: 10px">{{ workerErr }}</div>
      </UiCard>

      <UiCard title="工单列表" class="span2">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th style="width: 80px">ID</th>
                <th style="width: 100px">工种</th>
                <th style="width: 180px">状态</th>
                <th style="width: 100px">指派</th>
                <th>地址</th>
                <th style="width: 100px">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in list" :key="o.id" class="table-row" @click="router.push(`/admin/orders/${o.id}`)">
                <td class="id" @click.stop><RouterLink class="link" :to="`/admin/orders/${o.id}`">#{{ o.id }}</RouterLink></td>
                <td><span class="chip">{{ tradeLabel(o.tradeCode) }}</span></td>
                <td>
                  <div class="status">
                    <span class="chip" :data-s="String(o.status)">{{ o.status }}</span>
                    <span v-if="(o.escalationLevel || 0) > 0" class="chip warn">预警{{ o.escalationLevel }}</span>
                  </div>
                </td>
                <td>
                  <span class="muted tiny">{{ o.assignedWorkerId ? `#${o.assignedWorkerId}` : '-' }}</span>
                </td>
                <td class="addr">{{ o.address || '-' }}</td>
                <td class="op" @click.stop>
                  <button class="ghost" @click="openAssign(o.id)">分配</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </UiCard>

      <div v-if="assigningId" class="modal-mask" @click.self="closeAssign">
        <div class="modal">
          <div class="mh">
            <div class="mt">分配工单 #{{ assigningId }}</div>
            <button class="x" @click="closeAssign">×</button>
          </div>
          <div class="mb">
            <div class="row2">
              <div class="muted tiny">选择维修人员</div>
              <select v-model.number="selectedWorkerId" class="sel">
                <option :value="null" disabled>请选择</option>
                <option v-for="w in workers" :key="w.userId" :value="w.userId">
                  {{ w.displayName }} (#{{ w.userId }}){{ w.tradeCode ? ` · ${w.tradeCode}` : '' }}{{ (w.acceptOrders || 0) > 0 ? '' : ' · 不接单' }}
                </option>
              </select>
              <div v-if="selectedWorker" class="muted tiny" style="margin-top: 6px">
                将分配给：{{ selectedWorker.displayName }} (#{{ selectedWorker.userId }})
              </div>
            </div>
          </div>
          <div class="mf">
            <button class="ghost" @click="closeAssign">取消</button>
            <button class="primary" :disabled="!selectedWorkerId" @click="confirmAssign">确认分配</button>
          </div>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.orders-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.span2 {
  grid-column: span 2;
}

.hero {
  grid-column: span 2;
  background: linear-gradient(135deg, color-mix(in srgb, var(--sr-brand) 10%, transparent), transparent 55%),
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

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  table-layout: fixed;
  border-collapse: collapse;
}

table tr {
  display: table-row;
}

table th,

table td {
  display: table-cell;
}

td {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
  word-break: break-all;
}

.id {
  width: 80px;
}

.op {
  width: 120px;
}

.addr {
  white-space: normal;
  word-break: break-all;
  line-height: 1.5;
  color: var(--sr-text2);
}

.table-row td {
  vertical-align: middle;
}

.id {
  white-space: nowrap;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.table-row:hover {
  cursor: pointer;
  background: color-mix(in srgb, var(--sr-brand) 6%, transparent);
}

.link {
  color: inherit;
  text-decoration: none;
  font-weight: 900;
}

.link:hover {
  text-decoration: underline;
}

.chip.warn {
  margin-left: 8px;
  border-color: color-mix(in srgb, #fbbf24 45%, var(--sr-border));
  background: color-mix(in srgb, #fbbf24 12%, transparent);
}

.ghost {
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid var(--sr-border);
  background: transparent;
  color: inherit;
  font-weight: 800;
  cursor: pointer;
}

.ghost:hover {
  background: color-mix(in srgb, var(--sr-brand) 6%, transparent);
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: grid;
  place-items: center;
  z-index: 50;
}

.modal {
  width: min(560px, calc(100vw - 24px));
  border-radius: 18px;
  border: 1px solid var(--sr-border);
  background: var(--sr-panel);
  box-shadow: var(--sr-shadow2);
  overflow: hidden;
}

.mh {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--sr-border);
}

.mt {
  font-weight: 950;
}

.x {
  border: 1px solid var(--sr-border);
  background: transparent;
  color: inherit;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 900;
}

.mb {
  padding: 14px;
}

.row2 {
  display: grid;
  gap: 8px;
}

.sel {
  width: 100%;
  height: 36px;
  border-radius: 12px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel2) 75%, transparent);
  color: inherit;
  padding: 0 10px;
}

.mf {
  padding: 12px 14px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid var(--sr-border);
}

.primary {
  padding: 6px 12px;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--sr-brand) 45%, var(--sr-border));
  background: linear-gradient(135deg, color-mix(in srgb, var(--sr-brand) 20%, transparent), transparent);
  color: inherit;
  font-weight: 900;
  cursor: pointer;
}

.primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 1000px) {
  .kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .orders-grid {
    grid-template-columns: 1fr;
  }
  .span2 {
    grid-column: span 1;
  }
}
</style>
