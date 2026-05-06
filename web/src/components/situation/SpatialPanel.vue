<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { WorkOrder } from '../../situation/types'
import { getStatusColor, getStatusLabel } from '../../situation/statusMeta'
import { loadAMap } from '../../amap'

const props = defineProps<{
  workOrders: WorkOrder[]
  focusWorkOrderId: number | null
}>()

const emit = defineEmits<{ (e: 'focus', workOrderId: number): void }>()

const mapEl = ref<HTMLElement | null>(null)
const amapReady = ref(true)

let AMap: any = null
let map: any = null
let cluster: any = null
let markers: any[] = []

function hasGeo(wo: WorkOrder) {
  return wo.lng != null && wo.lat != null
}

const geoOrders = computed(() => props.workOrders.filter(hasGeo))

function computeCenter() {
  const list = geoOrders.value
  if (!list.length) return [116.397428, 39.90923]
  const avgLng = list.reduce((s, x) => s + Number(x.lng), 0) / list.length
  const avgLat = list.reduce((s, x) => s + Number(x.lat), 0) / list.length
  return [avgLng, avgLat]
}

function buildMarkers() {
  if (!map || !AMap) return

  if (cluster) {
    try {
      cluster.setMap(null)
    } catch {
      // ignore
    }
    cluster = null
  }
  for (const m of markers) {
    try {
      m.setMap(null)
    } catch {
      // ignore
    }
  }
  markers = []

  const ms = geoOrders.value.map((wo) => {
    const m = new AMap.Marker({
      position: [Number(wo.lng), Number(wo.lat)],
      title: `#${wo.id} ${getStatusLabel(String(wo.status))}`,
      anchor: 'center',
      offset: new AMap.Pixel(-6, -6),
      content: `<div style="width:12px;height:12px;border-radius:999px;border:1px solid rgba(15,23,42,0.35);background:${getStatusColor(
        String(wo.status)
      )};box-shadow:0 0 0 4px rgba(0,0,0,0.05)"></div>`,
      extData: { id: wo.id },
    })
    m.on('click', () => emit('focus', wo.id))
    return m
  })

  markers = ms
  cluster = new AMap.MarkerCluster(map, markers, {
    gridSize: 70,
    renderClusterMarker: (ctx: any) => {
      const count = ctx.count || 0
      const div = document.createElement('div')
      div.style.width = '32px'
      div.style.height = '32px'
      div.style.borderRadius = '999px'
      div.style.background = 'rgba(255,255,255,0.92)'
      div.style.border = '1px solid rgba(15,23,42,0.18)'
      div.style.display = 'grid'
      div.style.placeItems = 'center'
      div.style.fontWeight = '900'
      div.style.color = 'rgba(15,23,42,0.9)'
      div.style.boxShadow = '0 10px 24px rgba(0,0,0,0.08)'
      div.innerText = String(count)
      ctx.marker.setContent(div)
    },
  })
}

function focusMarker(id: number | null) {
  if (!id || !map) return
  const wo = geoOrders.value.find((x) => x.id === id)
  if (!wo) return
  map.setZoomAndCenter(17, [Number(wo.lng), Number(wo.lat)])
}

onMounted(async () => {
  try {
    AMap = await loadAMap()
    if (!mapEl.value) return

    map = new AMap.Map(mapEl.value, {
      zoom: 15,
      center: computeCenter(),
      viewMode: '2D',
      dragEnable: true,
      zoomEnable: true,
    })

    buildMarkers()
    focusMarker(props.focusWorkOrderId)

    if (!props.focusWorkOrderId && geoOrders.value.length) {
      map.setFitView(markers, false, [28, 28, 28, 28], 16)
    }
  } catch {
    amapReady.value = false
  }
})

watch(
  () => geoOrders.value.map((x) => `${x.id}:${x.lng},${x.lat}:${x.status}`).join('|'),
  () => {
    if (!map) return
    buildMarkers()
    if (!props.focusWorkOrderId && geoOrders.value.length) {
      map.setFitView(markers, false, [28, 28, 28, 28], 16)
    }
  }
)

watch(
  () => props.focusWorkOrderId,
  (v) => focusMarker(v)
)

onBeforeUnmount(() => {
  try {
    if (cluster) cluster.setMap(null)
  } catch {
    // ignore
  }
  try {
    if (map) map.destroy()
  } catch {
    // ignore
  }
  cluster = null
  map = null
  markers = []
})
</script>

<template>
  <div class="spatial">
    <div class="head">
      <div class="t">空间分布</div>
      <div class="s">真实地图 · 点击点位聚焦工单</div>
    </div>

    <div v-if="!amapReady" class="fallback">地图加载失败：请配置 <b>VITE_AMAP_KEY</b></div>
    <div v-else ref="mapEl" class="map" />
  </div>
</template>

<style scoped>
.spatial {
  border-radius: 18px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(10px) saturate(140%);
  padding: 14px;
}

.head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

.t {
  font-weight: 900;
}

.s {
  color: var(--sr-text2);
  font-size: 12px;
}

.map {
  height: 220px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  overflow: hidden;
}

.fallback {
  height: 220px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  background: rgba(255, 255, 255, 0.55);
  display: grid;
  place-items: center;
  color: var(--sr-text2);
  font-size: 12px;
}
</style>
