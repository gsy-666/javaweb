<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import PageContainer from '../components/PageContainer.vue'
import UiCard from '../components/UiCard.vue';
import Admin3DScene from '../components/Admin3DScene.vue';

const now = ref(new Date())

const loading = ref(false)
const err = ref<string | null>(null)

const overview = ref<any | null>(null)

const kpi = computed(() => {
  const o = overview.value
  return {
    todayReported: o?.todayReported || 0,
    pending: o?.pending || 0,
    overdue: o?.overdue || 0,
    todayDone: o?.done || 0,
  }
})

const totalOrders = computed(() => overview.value?.total || 0)

const breakdown = computed(() => {
  const sc = (overview.value?.statusCounts || {}) as Record<string, number>
  const pending = (sc['NEW'] || 0) + (sc['ASSIGNED'] || 0)
  const inProgress = (sc['ACCEPTED'] || 0) + (sc['IN_PROGRESS'] || 0) + (sc['WAIT_USER'] || 0)
  const waitRate = sc['DONE_WAIT_RATE'] || 0
  const done = sc['CLOSED'] || 0

  const total = totalOrders.value || 1
  const pct = (n: number) => ((n / total) * 100).toFixed(2)

  return {
    pending: { n: pending, pct: pct(pending) },
    inProgress: { n: inProgress, pct: pct(inProgress) },
    waitRate: { n: waitRate, pct: pct(waitRate) },
    done: { n: done, pct: pct(done) },
  }
})

const avgStars = computed(() => Number(overview.value?.avgStars || 0).toFixed(2))
const starsCounts = computed(() => (overview.value?.starsCounts || {}) as Record<string, number>)
const starsPercent = computed(() => {
  const total = Object.values(starsCounts.value).reduce((a, b) => a + (b || 0), 0) || 1
  const out: Record<number, { cnt: number; pct: number }> = {} as any
  for (const s of [5, 4, 3, 2, 1]) {
    const cnt = Number((starsCounts.value as any)[s] || 0)
    out[s] = { cnt, pct: Math.round((cnt / total) * 100) }
  }
  return out
})

async function loadOverview() {
  loading.value = true
  err.value = null
  try {
    const resp = await api.get('/admin/overview')
    overview.value = resp.data?.data || null
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadOverview()
  window.setInterval(() => (now.value = new Date()), 1000)
})
</script>

<template>
  <PageContainer>
    <div class="admin-grid">
      <UiCard class="hero" padded>
        <div class="hero-top">
          <div>
            <div class="hello">您好，管理员</div>
            <div class="welcome muted">欢迎使用智慧后勤报修与满意度评价系统</div>
          </div>
          <div class="sys">
            <div class="dt">
              {{ now.getFullYear() }}-{{ String(now.getMonth() + 1).padStart(2, '0') }}-{{ String(now.getDate()).padStart(2, '0') }}
              {{ String(now.getHours()).padStart(2, '0') }}:{{ String(now.getMinutes()).padStart(2, '0') }}:{{
                String(now.getSeconds()).padStart(2, '0')
              }}
            </div>
            <div class="wx">周六 · 25°C 多云</div>
          </div>
        </div>

        <div v-if="err" class="error-text" style="margin-top: 12px">{{ err }}</div>

        <div class="cards">
          <div class="stat">
            <div class="ic" data-k="a">🧾</div>
            <div>
              <div class="num">{{ kpi.todayReported }}</div>
              <div class="lab">今日报修</div>
            </div>
          </div>
          <div class="stat">
            <div class="ic" data-k="b">🛠️</div>
            <div>
              <div class="num">{{ kpi.pending }}</div>
              <div class="lab">待处理工单</div>
            </div>
          </div>
          <div class="stat">
            <div class="ic" data-k="c">⏱️</div>
            <div>
              <div class="num">{{ kpi.overdue }}</div>
              <div class="lab">超时预警</div>
            </div>
          </div>
          <div class="stat">
            <div class="ic" data-k="d">✅</div>
            <div>
              <div class="num">{{ kpi.todayDone }}</div>
              <div class="lab">今日完工</div>
            </div>
          </div>
        </div>
      </UiCard>

      <UiCard title="工单处理概览" class="span2">
        <div class="chart-row">
          <div class="ring">
            <div class="ring-center">
              <div class="big">{{ totalOrders }}</div>
              <div class="muted tiny">总工单</div>
            </div>
          </div>
          <div class="legend">
            <div class="li"><span class="dot" style="--c:#22d3ee"></span>待处理 <b>{{ breakdown.pending.n }}</b> <span class="muted">({{ breakdown.pending.pct }}%)</span></div>
            <div class="li"><span class="dot" style="--c:#a78bfa"></span>处理中 <b>{{ breakdown.inProgress.n }}</b> <span class="muted">({{ breakdown.inProgress.pct }}%)</span></div>
            <div class="li"><span class="dot" style="--c:#fbbf24"></span>待评价 <b>{{ breakdown.waitRate.n }}</b> <span class="muted">({{ breakdown.waitRate.pct }}%)</span></div>
            <div class="li"><span class="dot" style="--c:#34d399"></span>已完成 <b>{{ breakdown.done.n }}</b> <span class="muted">({{ breakdown.done.pct }}%)</span></div>
          </div>
        </div>
      </UiCard>

      <UiCard class="span2 map-bento">
        <div class="bento-header">
          <h3>燕山大学数字孪生基座</h3>
          <p class="muted">Live 3D Visualization</p>
        </div>
        <div class="map-wrapper">
          <Admin3DScene />
        </div>
      </UiCard>

      <UiCard title="工单趋势分析" class="span2">
        <div class="line-skel">
          <div class="grid" />
          <div class="path" />
        </div>
      </UiCard>

      <UiCard title="满意度评价" class="span2">
        <div class="satis">
          <div class="score">
            <div class="meter">
              <div class="meter-inner" />
            </div>
            <div class="score-num">{{ avgStars }}</div>
            <div class="muted tiny">平均分</div>
          </div>
          <div class="bars">
            <div class="bar" v-for="x in [5, 4, 3, 2, 1]" :key="x">
              <div class="k">{{ x }}星</div>
              <div class="track"><div class="fill" :style="{ width: starsPercent[x].pct + '%' }" /></div>
              <div class="v">{{ starsPercent[x].cnt }}</div>
            </div>
          </div>
        </div>
      </UiCard>
    </div>
  </PageContainer>
</template>

<style scoped>
.admin-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  align-items: start;
}

.span2 {
  grid-column: span 2;
}

.hero {
  grid-column: span 2;
  background: linear-gradient(
      135deg,
      color-mix(in srgb, var(--sr-brand) 14%, transparent),
      transparent 45%
    ),
    linear-gradient(180deg, var(--sr-panel), color-mix(in srgb, var(--sr-panel2) 70%, transparent));
}

.hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.hello {
  font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
  font-weight: 950;
  letter-spacing: 0.02em;
  font-size: 18px;
}

.welcome {
  margin-top: 6px;
  font-size: 12px;
}

.sys {
  text-align: right;
  border: 1px solid var(--sr-border);
  border-radius: 16px;
  padding: 10px 12px;
  background: color-mix(in srgb, var(--sr-panel2) 70%, transparent);
}

.dt {
  font-variant-numeric: tabular-nums;
  font-weight: 900;
  font-size: 12px;
}

.wx {
  margin-top: 2px;
  font-size: 11px;
  color: var(--sr-text2);
}

.cards {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.stat {
  border: 1px solid var(--sr-border);
  border-radius: 18px;
  padding: 12px;
  display: flex;
  gap: 10px;
  align-items: center;
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
}

.ic {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-brand2) 10%, transparent);
}

.ic[data-k='b'] {
  background: color-mix(in srgb, #34d399 10%, transparent);
}

.ic[data-k='c'] {
  background: color-mix(in srgb, #fbbf24 10%, transparent);
}

.ic[data-k='d'] {
  background: color-mix(in srgb, #a78bfa 10%, transparent);
}

.num {
  font-size: 22px;
  font-weight: 950;
}

.lab {
  font-size: 12px;
  color: var(--sr-text2);
  margin-top: 2px;
}

.chart-row {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 12px;
  align-items: center;
}

.ring {
  width: 280px;
  height: 280px;
  margin: 0 auto;
  border-radius: 999px;
  background: conic-gradient(#22d3ee, #a78bfa, #fbbf24, #34d399, #22d3ee);
  mask: radial-gradient(circle 104px, transparent 99px, #000 100px);
  position: relative;
}

.ring::after {
  content: '';
  position: absolute;
  inset: 22px;
  border-radius: 999px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel) 70%, transparent);
}

.ring-center {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  z-index: 1;
}

.big {
  font-size: 34px;
  font-weight: 950;
}

.legend {
  display: grid;
  gap: 10px;
}

.li {
  display: flex;
  gap: 10px;
  align-items: baseline;
  border: 1px solid var(--sr-border);
  border-radius: 16px;
  padding: 10px 12px;
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--c);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--c) 18%, transparent);
  margin-top: 4px;
}

.line-skel {
  height: 220px;
  border-radius: 18px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
  position: relative;
  overflow: hidden;
}

.line-skel .grid {
  position: absolute;
  inset: 0;
  background: linear-gradient(to right, rgba(255,255,255,0.05) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255,255,255,0.05) 1px, transparent 1px);
  background-size: 36px 36px;
  opacity: 0.55;
}

.line-skel .path {
  position: absolute;
  inset: 0;
  background: radial-gradient(900px 240px at 20% 60%, color-mix(in srgb, var(--sr-brand2) 22%, transparent), transparent 60%),
    radial-gradient(900px 240px at 70% 40%, color-mix(in srgb, var(--sr-brand) 18%, transparent), transparent 62%);
}

.satis {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 14px;
  align-items: center;
}

.score {
  text-align: center;
}

.meter {
  width: 160px;
  height: 160px;
  border-radius: 999px;
  margin: 0 auto 10px;
  background: conic-gradient(color-mix(in srgb, var(--sr-brand2) 85%, var(--sr-brand)), rgba(255,255,255,0.08) 0);
  mask: radial-gradient(circle 62px, transparent 60px, #000 61px);
  position: relative;
}

.meter-inner {
  position: absolute;
  inset: 18px;
  border-radius: 999px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel) 70%, transparent);
}

.score-num {
  font-size: 34px;
  font-weight: 950;
}

.bars {
  display: grid;
  gap: 10px;
}

.bar {
  display: grid;
  grid-template-columns: 34px 1fr 44px;
  gap: 10px;
  align-items: center;
}

.track {
  height: 10px;
  border-radius: 999px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
  overflow: hidden;
}

.fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--sr-brand2), var(--sr-brand));
}

.v {
  text-align: right;
  font-variant-numeric: tabular-nums;
  color: var(--sr-text2);
}

.tiny {
  font-size: 12px;
}

@media (max-width: 1000px) {
  .cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .chart-row {
    grid-template-columns: 1fr;
  }
  .satis {
    grid-template-columns: 1fr;
  }
}

.map-bento {
  padding: 0;
  border-radius: 24px;
  overflow: hidden;
  height: 500px;
  position: relative;
  box-shadow: 0 20px 40px rgba(0,0,0,0.4), inset 0 1px 0 rgba(255,255,255,0.1);
}
.map-bento .bento-header {
  padding: 20px 24px;
  position: absolute;
  top: 0; left: 0; right: 0;
  pointer-events: none;
}
.map-wrapper {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}
.map-wrapper > * {
  width: 100%;
  height: 100%;
}
</style>
