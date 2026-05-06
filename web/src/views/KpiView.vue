<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api'
import PageContainer from '../components/PageContainer.vue'
import UiCard from '../components/UiCard.vue'

const date = ref<string>(new Date().toISOString().slice(0, 10))
const list = ref<any[]>([])
const err = ref<string | null>(null)

async function load() {
  err.value = null
  try {
    const resp = await api.get('/kpi/workers', { params: { date: date.value } })
    list.value = resp.data?.data || []
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  }
}

function workerLabel(r: any) {
  const name = r?.workerName || ''
  const acc = r?.workerAccount || ''
  if (name && acc) return `${name}（${acc}）`
  return name || acc || `#${r?.workerId ?? '-'}`
}

onMounted(load)
</script>

<template>
  <PageContainer>
    <UiCard>
      <template #title>
        <div class="ttl">
          <div class="h">绩效排行</div>
          <div class="muted tiny">按日统计 · 支持查询</div>
        </div>
      </template>
      <template #extra>
        <div class="row">
          <input v-model="date" type="date" />
          <button class="primary" @click="load">查询</button>
        </div>
      </template>

      <div v-if="err" class="error-text" style="margin-bottom: 10px">{{ err }}</div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>维修工</th>
              <th>电话</th>
              <th>完成数</th>
              <th>平均接单(分钟)</th>
              <th>平均星级</th>
              <th>差评率</th>
              <th>综合分</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in list" :key="r.workerId">
              <td>
                <span class="chip">{{ workerLabel(r) }}</span>
              </td>
              <td>{{ r.workerPhone || '-' }}</td>
              <td>
                <b>{{ r.doneCnt }}</b>
              </td>
              <td>{{ r.avgAcceptMin?.toFixed?.(1) ?? '-' }}</td>
              <td>{{ r.avgRating?.toFixed?.(2) ?? '-' }}</td>
              <td>{{ r.badRate?.toFixed?.(2) ?? '-' }}</td>
              <td>
                <b>{{ r.kpiScore?.toFixed?.(1) ?? '-' }}</b>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </UiCard>
  </PageContainer>
</template>

<style scoped>
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

.table-wrap {
  overflow-x: auto;
}
</style>

