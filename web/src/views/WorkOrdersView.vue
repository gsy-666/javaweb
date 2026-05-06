<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import { RouterLink } from 'vue-router'

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
}

const list = ref<WorkOrder[]>([])
const err = ref<string | null>(null)

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
    const resp = await api.get('/work-orders')
    list.value = resp.data?.data || []
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  }
}

onMounted(load)
</script>

<template>
  <PageContainer>
    <div class="orders-grid">
      <UiCard class="hero" padded>
        <template #title>
          <div class="ttl">
            <div class="h">我的工单</div>
            <div class="muted tiny">进度追踪 · 在线沟通 · 评价反馈</div>
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

        <div class="muted tip">提示：点击工单进入详情，可查看处理进度、与维修工沟通并进行评价。</div>
      </UiCard>

      <UiCard title="工单列表" class="span2">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th style="width: 80px">ID</th>
                <th style="width: 120px">工种</th>
                <th style="width: 140px">状态</th>
                <th>地址</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in list" :key="o.id">
                <td><RouterLink class="link" :to="`/orders/${o.id}`">#{{ o.id }}</RouterLink></td>
                <td><span class="chip">{{ tradeLabel(o.tradeCode) }}</span></td>
                <td>
                  <span class="chip" :data-s="String(o.status)">{{ o.status }}</span>
                  <span v-if="(o.escalationLevel || 0) > 0" class="chip warn">预警{{ o.escalationLevel }}</span>
                </td>
                <td class="addr">{{ o.address || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </UiCard>
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

.tip {
  margin-top: 10px;
  font-size: 12px;
}

.table-wrap {
  overflow-x: auto;
}

.link {
  color: inherit;
  text-decoration: none;
  font-weight: 900;
}

.link:hover {
  text-decoration: underline;
}

.addr {
  min-width: 260px;
}

.chip.warn {
  margin-left: 8px;
  border-color: color-mix(in srgb, #fbbf24 45%, var(--sr-border));
  background: color-mix(in srgb, #fbbf24 12%, transparent);
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

