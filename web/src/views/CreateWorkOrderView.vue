<script setup lang="ts">
import { computed, ref } from 'vue'
import { api, getAssetUrl } from '../api'
import { useRouter } from 'vue-router'

import PageContainer from '../components/PageContainer.vue'
import UiCard from '../components/UiCard.vue'

const tradeCode = ref('WATER_ELEC')
const description = ref('')
const lng = ref<number | null>(null)
const lat = ref<number | null>(null)
const address = ref('')
const imageUrls = ref<string[]>([])
const lastCreatedId = ref<number | null>(null)
const err = ref<string | null>(null)
const busyLocate = ref(false)
const busySubmit = ref(false)
const router = useRouter()

const tradeLabel = computed(() => {
  if (tradeCode.value === 'WATER_ELEC') return '水电'
  if (tradeCode.value === 'CARPENTER') return '木工'
  if (tradeCode.value === 'NETWORK') return '网络'
  return tradeCode.value
})

async function locate() {
  err.value = null
  busyLocate.value = true
  try {
    const p = await new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 10000,
      })
    })
    lng.value = Number(p.coords.longitude.toFixed(6))
    lat.value = Number(p.coords.latitude.toFixed(6))
    const regeo = await api.get('/lbs/regeo', { params: { lng: lng.value, lat: lat.value } })
    address.value = regeo.data?.data || ''
  } catch (e: any) {
    err.value = e?.message || '定位失败，请检查浏览器定位权限'
  } finally {
    busyLocate.value = false
  }
}

async function upload(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files?.length) return
  err.value = null
  try {
    const f = input.files[0]
    const form = new FormData()
    form.append('file', f)
    const resp = await api.post('/files/upload', form)
    const url = resp.data?.data as string
    if (url) imageUrls.value.push(url)
    input.value = ''
  } catch (e: any) {
    err.value = e?.message || '上传失败'
  }
}

function removeImage(url: string) {
  imageUrls.value = imageUrls.value.filter((x) => x !== url)
}

async function submit() {
  err.value = null
  if (!description.value.trim()) {
    err.value = '请填写报修描述'
    return
  }
  if (lng.value == null || lat.value == null) {
    err.value = '请先定位（或手动填写经纬度）'
    return
  }
  busySubmit.value = true
  try {
    const resp = await api.post('/work-orders', {
      tradeCode: tradeCode.value,
      description: description.value,
      lng: lng.value,
      lat: lat.value,
      address: address.value,
      imageUrls: imageUrls.value,
    })
    lastCreatedId.value = resp.data?.data?.id ?? null
    description.value = ''
    imageUrls.value = []
    if (lastCreatedId.value != null) {
      await router.push(`/orders/${lastCreatedId.value}`)
    }
  } catch (e: any) {
    err.value = e?.message || '提交失败'
  } finally {
    busySubmit.value = false
  }
}
</script>

<template>
  <PageContainer>
    <UiCard>
      <template #title>
        <div class="title-row">
          <div class="headline">我要报修</div>
          <div class="sub">选择类型 → 定位 → 描述 → 提交</div>
        </div>
      </template>
      <template #extra>
        <span class="badge">{{ tradeLabel }}</span>
      </template>

      <div v-if="err" class="error-text" style="margin-bottom: 10px">{{ err }}</div>

      <div class="grid">
        <div class="block">
          <div class="block-title">报修类型</div>
          <div class="row">
            <label class="pick" :class="{ on: tradeCode === 'WATER_ELEC' }">
              <input v-model="tradeCode" type="radio" value="WATER_ELEC" />
              <span class="icon">💧</span>
              <span class="txt">水电</span>
            </label>
            <label class="pick" :class="{ on: tradeCode === 'CARPENTER' }">
              <input v-model="tradeCode" type="radio" value="CARPENTER" />
              <span class="icon">🪵</span>
              <span class="txt">木工</span>
            </label>
            <label class="pick" :class="{ on: tradeCode === 'NETWORK' }">
              <input v-model="tradeCode" type="radio" value="NETWORK" />
              <span class="icon">📶</span>
              <span class="txt">网络</span>
            </label>
          </div>
        </div>

        <div class="block">
          <div class="block-title">定位与地址</div>
          <div class="row">
            <button class="primary" @click="locate" :disabled="busyLocate">
              {{ busyLocate ? '定位中…' : '一键定位' }}
            </button>
            <span class="muted tiny">需要浏览器允许定位权限</span>
          </div>
          <div class="row mt">
            <label class="field">
              <span class="k">Lng</span>
              <input v-model.number="lng" type="number" step="0.000001" placeholder="经度" />
            </label>
            <label class="field">
              <span class="k">Lat</span>
              <input v-model.number="lat" type="number" step="0.000001" placeholder="纬度" />
            </label>
          </div>
          <div class="mt">
            <div class="k">地址（可选）</div>
            <input v-model="address" class="full" placeholder="自动填充或手动填写" />
          </div>
        </div>

        <div class="block span2">
          <div class="block-title">问题描述</div>
          <textarea v-model="description" class="desc" placeholder="例如：宿舍 3 楼走廊灯坏了，晚上无法通行"></textarea>
          <div class="muted tiny">尽量写清楚位置、现象、是否紧急。</div>
        </div>

        <div class="block span2">
          <div class="block-title">现场图片</div>
          <div class="row">
            <label class="upload">
              <input type="file" accept="image/*" @change="upload" />
              <span class="btn">上传图片</span>
              <span class="muted tiny">建议拍摄故障全景 + 特写</span>
            </label>
          </div>

          <div v-if="imageUrls.length" class="gallery">
            <div v-for="u in imageUrls" :key="u" class="thumb">
              <a :href="getAssetUrl(u)" target="_blank" class="img-wrap">
                <img :src="getAssetUrl(u)" alt="work order" />
              </a>
              <div class="thumb-bar">
                <a :href="getAssetUrl(u)" target="_blank" class="link">查看</a>
                <button class="ghost" type="button" @click="removeImage(u)">移除</button>
              </div>
            </div>
          </div>

          <div v-else class="empty">还没有上传图片</div>
        </div>

        <div class="span2 submit">
          <button class="primary big" @click="submit" :disabled="busySubmit">
            {{ busySubmit ? '提交中…' : '提交报修' }}
          </button>
          <div class="muted tiny">提交后可在「我的报修」查看进度与沟通。</div>
        </div>
      </div>
    </UiCard>
  </PageContainer>
</template>

<style scoped>
.title-row {
  display: grid;
  gap: 2px;
}

.headline {
  font-weight: 950;
  letter-spacing: 0.02em;
}

.sub {
  font-size: 12px;
  color: var(--sr-text2);
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

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.block {
  border: 1px solid var(--sr-border);
  border-radius: 18px;
  padding: 14px;
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
}

.block-title {
  font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
  font-weight: 900;
  letter-spacing: 0.02em;
  margin-bottom: 10px;
}

.span2 {
  grid-column: span 2;
}

.pick {
  position: relative;
  display: inline-flex;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 16px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel) 70%, transparent);
  cursor: pointer;
  transition: all 180ms cubic-bezier(0.16, 1, 0.3, 1);
}

.pick input {
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
}

.pick:hover {
  transform: translateY(-1px);
  border-color: var(--sr-border2);
}

.pick.on {
  border-color: var(--sr-border2);
  box-shadow: 0 14px 30px color-mix(in srgb, var(--sr-brand) 10%, transparent);
  background: linear-gradient(135deg, color-mix(in srgb, var(--sr-brand) 14%, transparent), transparent);
}

.icon {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: color-mix(in srgb, var(--sr-brand) 12%, transparent);
}

.txt {
  font-weight: 800;
}

.field {
  display: grid;
  gap: 6px;
  flex: 1;
}

.k {
  font-size: 12px;
  font-weight: 800;
  color: var(--sr-text2);
}

.full {
  width: 100%;
}

.desc {
  width: 100%;
  min-height: 130px;
  resize: vertical;
}

.mt {
  margin-top: 10px;
}

.tiny {
  font-size: 12px;
}

.upload {
  display: flex;
  align-items: center;
  gap: 10px;
}

.upload input {
  display: none;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel) 70%, transparent);
  font-weight: 800;
  cursor: pointer;
}

.gallery {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.thumb {
  border-radius: 16px;
  border: 1px solid var(--sr-border);
  overflow: hidden;
  background: var(--sr-panel);
}

.img-wrap {
  display: block;
  aspect-ratio: 4 / 3;
  overflow: hidden;
}

img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 280ms cubic-bezier(0.16, 1, 0.3, 1);
}

.thumb:hover img {
  transform: scale(1.05);
}

.thumb-bar {
  display: flex;
  justify-content: space-between;
  padding: 10px 10px;
  border-top: 1px solid var(--sr-border);
}

.link {
  font-size: 12px;
  font-weight: 800;
  color: inherit;
  text-decoration: none;
  padding: 6px 10px;
  border-radius: 12px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel2) 70%, transparent);
}

.ghost {
  min-height: 0;
  padding: 6px 10px;
  border-radius: 12px;
  background: transparent;
}

.empty {
  margin-top: 10px;
  padding: 14px;
  border-radius: 16px;
  border: 1px dashed var(--sr-border);
  color: var(--sr-text2);
}

.submit {
  display: grid;
  justify-items: start;
  gap: 6px;
}

.big {
  min-height: 46px;
  padding: 10px 18px;
  border-radius: 16px;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .span2 {
    grid-column: span 1;
  }
  .gallery {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

