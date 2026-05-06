<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { api } from '../api'

import PageContainer from '../components/PageContainer.vue'
import UiCard from '../components/UiCard.vue'

const tradeCode = ref('WATER_ELEC')
const lng = ref<number | null>(null)
const lat = ref<number | null>(null)
const myOrders = ref<any[]>([])
const err = ref<string | null>(null)
const busyReport = ref(false)
const acceptOrders = ref(true)
let reportTimer: number | null = null

const tradeLabel = computed(() => {
  if (tradeCode.value === 'WATER_ELEC') return '水电'
  if (tradeCode.value === 'CARPENTER') return '木工'
  if (tradeCode.value === 'NETWORK') return '网络'
  return tradeCode.value
})

async function locate() {
  const p = await new Promise<GeolocationPosition>((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: false, timeout: 4000 })
  })
  lng.value = p.coords.longitude
  lat.value = p.coords.latitude
}

async function setBusy(v: boolean) {
  acceptOrders.value = !v
  err.value = null
  busyReport.value = true
  try {
    if (lng.value == null || lat.value == null) await locate()
    await api.post('/workers/me/location', {
      tradeCode: tradeCode.value,
      lng: lng.value,
      lat: lat.value,
      acceptOrders: acceptOrders.value,
    })
  } catch (e: any) {
    err.value = e?.message || '更新状态失败'
  } finally {
    busyReport.value = false
  }
}


async function loadOrders() {
  err.value = null
  try {
    const resp = await api.get('/work-orders')
    const list = (resp.data?.data || []) as any[]
    myOrders.value = list.filter((x) => String(x.status).includes('ASSIGN') || String(x.status).includes('ACCEPT') || String(x.status).includes('IN_PROGRESS') || String(x.status).includes('WAIT') || String(x.status).includes('DONE'))
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  }
}

function startHeartbeat() {
  if (reportTimer != null) window.clearInterval(reportTimer)
  reportTimer = window.setInterval(() => {
    if (!acceptOrders.value) return
    if (busyReport.value) return
    setBusy(false)
  }, 120000)
}

function stopHeartbeat() {
  if (reportTimer != null) window.clearInterval(reportTimer)
  reportTimer = null
}

watch(acceptOrders, (v) => {
  if (v) startHeartbeat()
  else stopHeartbeat()
})

onMounted(() => {
  loadOrders()
  if (acceptOrders.value) {
    // Kick off location update without blocking initial render.
    setTimeout(() => setBusy(false), 0)
    startHeartbeat()
  }
})

const toggleDisabled = computed(() => busyReport.value || lng.value == null || lat.value == null)

onBeforeUnmount(() => stopHeartbeat())
</script>

<template>
  <PageContainer>
    <div class="worker-grid">
      <UiCard class="hero" padded>
        <template #title>
          <div class="ttl">
            <div class="h">工人工作台</div>
            <div class="muted tiny">专注执行 · 快速接单 · 高效处理</div>
          </div>
        </template>
        <template #extra>
          <div class="head-ctl">
            <div class="toggle" :data-on="acceptOrders ? '1' : '0'" :data-dis="toggleDisabled ? '1' : '0'" @click="toggleDisabled ? null : setBusy(acceptOrders)">
              <span class="knob" />
              <span class="txt left">空闲</span>
              <span class="txt right">忙碌</span>
            </div>
            <span class="badge">{{ tradeLabel }}</span>
          </div>
        </template>

        <div v-if="err" class="error-text" style="margin-bottom: 10px">{{ err }}</div>

        <div class="kpis">
          <div class="kpi" data-k="a">
            <div class="num">{{ myOrders.length }}</div>
            <div class="lab">我的工单</div>
          </div>
          <div class="kpi" data-k="b">
            <div class="num">{{ myOrders.filter((x) => String(x.status).includes('ASSIGN') || String(x.status).includes('PEND')).length }}</div>
            <div class="lab">待处理</div>
          </div>
          <div class="kpi" data-k="c">
            <div class="num">{{ myOrders.filter((x) => String(x.status).includes('DONE')).length }}</div>
            <div class="lab">已完成</div>
          </div>
          <div class="kpi" data-k="d">
            <div class="num">{{ myOrders.filter((x) => (x.escalationLevel || 0) > 0).length }}</div>
            <div class="lab">预警</div>
          </div>
        </div>

          <div class="row" style="margin-top: 12px">
            <label class="field">
              <span class="k">工种</span>
              <select v-model="tradeCode">
                <option value="WATER_ELEC">水电</option>
                <option value="CARPENTER">木工</option>
                <option value="NETWORK">网络</option>
              </select>
            </label>
            <label class="field">
              <span class="k">Lng</span>
              <input v-model.number="lng" type="number" step="0.000001" placeholder="经度" />
            </label>
            <label class="field">
              <span class="k">Lat</span>
              <input v-model.number="lat" type="number" step="0.000001" placeholder="纬度" />
            </label>

            <button class="primary" @click="setBusy(false)" :disabled="busyReport || !acceptOrders">手动上报位置</button>
            <button @click="loadOrders">刷新我的工单</button>
          </div>

        <div class="muted tip">说明：空闲状态会持续上报位置并参与派单；忙碌状态不会被派单。</div>
      </UiCard>

      <UiCard title="我的任务列表" class="span2">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>工种</th>
                <th>状态</th>
                <th>地址</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in myOrders" :key="o.id" class="row-link" @click="$router.push(`/worker/orders/${o.id}`)">
                <td><span class="link">#{{ o.id }}</span></td>
                <td>{{ o.tradeCode }}</td>
                <td><span class="chip">{{ o.status }}</span></td>
                <td>{{ o.address || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </UiCard>
    </div>
  </PageContainer>
</template>

<style scoped>
.worker-grid {
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

.badge {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  border: 1px solid var(--sr-border2);
  background: color-mix(in srgb, var(--sr-brand) 10%, transparent);
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 900;
}

.head-ctl {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.toggle {
  height: 32px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--sr-border2) 85%, transparent);
  background: linear-gradient(
      135deg,
      color-mix(in srgb, var(--sr-panel) 78%, transparent),
      color-mix(in srgb, var(--sr-panel2) 65%, transparent)
    );
  backdrop-filter: blur(16px) saturate(160%);
  -webkit-backdrop-filter: blur(16px) saturate(160%);
  position: relative;
  width: 116px;
  padding: 3px;
  cursor: pointer;
  user-select: none;
  font-weight: 900;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.08);
}

.toggle[data-dis='1'] {
  opacity: 0.5;
  cursor: not-allowed;
}

.knob {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 56px;
  height: 26px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.12);
  transform: translateX(0);
  transition: transform 180ms cubic-bezier(0.16, 1, 0.3, 1),
    background 180ms cubic-bezier(0.16, 1, 0.3, 1),
    border-color 180ms cubic-bezier(0.16, 1, 0.3, 1);
}

.toggle[data-on='1'] .knob {
  background: rgba(16, 185, 129, 0.92);
  border-color: rgba(16, 185, 129, 0.95);
}

.toggle[data-on='0'] .knob {
  transform: translateX(54px);
  background: rgba(239, 68, 68, 0.92);
  border-color: rgba(239, 68, 68, 0.95);
}

.txt {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  width: 56px;
  text-align: center;
  padding: 0 6px;
  border-radius: 999px;
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.22);
  pointer-events: none;
  opacity: 0;
  transition: opacity 120ms ease;
}

.txt.left {
  left: 3px;
}

.txt.right {
  left: 57px;
}

.toggle[data-on='1'] .txt.left {
  opacity: 1;
  color: rgba(255, 255, 255, 0.96);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
}

.toggle[data-on='0'] .txt.right {
  opacity: 1;
  color: rgba(255, 255, 255, 0.96);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
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

.field {
  display: grid;
  gap: 6px;
}

.k {
  font-size: 12px;
  font-weight: 800;
  color: var(--sr-text2);
}

.tip {
  margin-top: 10px;
  font-size: 12px;
}

.table-wrap {
  overflow-x: auto;
}

.row-link {
  cursor: pointer;
}

.row-link:hover {
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

@media (max-width: 1000px) {
  .kpis {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .worker-grid {
    grid-template-columns: 1fr;
  }
  .span2 {
    grid-column: span 1;
  }
}
</style>

