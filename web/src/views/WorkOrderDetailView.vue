<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { api, getAssetUrl, getWsUrl } from '../api'
import { useRoute, useRouter } from 'vue-router'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { role, token } from '../user'

import PageContainer from '../components/PageContainer.vue'
import UiCard from '../components/UiCard.vue'

const route = useRoute()
const router = useRouter()
const id = computed(() => Number(route.params.id))

const detail = ref<any>(null)
const chat = ref<any[]>([])
const chatText = ref('')
const err = ref<string | null>(null)
const wsConnected = ref(false)

const etaInput = ref('')
const rateStars = ref(5)
const rateComment = ref('')
let client: Client | null = null

const canDelete = computed(() => {
  if (role.value !== 'USER') return false
  const s = String(detail.value?.workOrder?.status || '')
  return ['NEW', 'ASSIGNED', 'CANCELLED'].includes(s)
})

async function deleteMine() {
  if (!window.confirm('确认删除该工单？删除后在列表中将不可见。')) return
  try {
    await api.post(`/work-orders/${id.value}/delete`)
    await router.replace('/orders')
  } catch (e: any) {
    err.value = e?.message || '操作失败'
  }
}

async function load() {
  err.value = null
  try {
    const resp = await api.get(`/work-orders/${id.value}`)
    detail.value = resp.data?.data
    const chatResp = await api.get(`/work-orders/${id.value}/chat/messages`)
    chat.value = chatResp.data?.data || []

    const etaAt = detail.value?.workOrder?.etaAt
    etaInput.value = etaAt ? String(etaAt).slice(0, 16) : ''
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  }
}

function connectWs() {
  if (!Number.isFinite(id.value)) return
  if (!token.value) return

  const wsUrl = getWsUrl(`/ws?token=${encodeURIComponent(token.value)}`)

  client = new Client({
    webSocketFactory: () => new SockJS(wsUrl.replace(/^ws:/, 'http:').replace(/^wss:/, 'https:')),
    reconnectDelay: 3000,
    debug: (str) => console.log('[stomp]', str),
    onConnect: () => {
      wsConnected.value = true
      client?.subscribe(`/topic/wo/${id.value}`, (msg) => {
        try {
          chat.value.push(JSON.parse(msg.body))
        } catch {
          // ignore
        }
      })
    },
    onDisconnect: () => {
      wsConnected.value = false
    },
    onStompError: () => {
      wsConnected.value = false
    },
    onWebSocketClose: () => {
      wsConnected.value = false
    },
  })
  client.activate()
}

async function sendChat() {
  if (!chatText.value.trim()) return
  if (!wsConnected.value) {
    err.value = '聊天连接尚未就绪，请稍后再试'
    return
  }
  client?.publish({
    destination: `/app/wo/${id.value}/send`,
    body: JSON.stringify({ msgType: 'text', content: chatText.value }),
  })
  chatText.value = ''
}

async function setEta() {
  if (!etaInput.value) return
  try {
    await api.post(`/work-orders/${id.value}/eta`, { etaAt: etaInput.value + ':00' })
    await load()
  } catch (e: any) {
    err.value = e?.message || '操作失败'
  }
}

async function confirmFixed() {
  try {
    await api.post(`/work-orders/${id.value}/confirm-fixed`)
    await load()
  } catch (e: any) {
    err.value = e?.message || '操作失败'
  }
}

async function accept() {
  try {
    await api.post(`/work-orders/${id.value}/accept`)
    await load()
  } catch (e: any) {
    err.value = e?.message || '操作失败'
  }
}
async function reject() {
  try {
    await api.post(`/work-orders/${id.value}/reject`, null, { params: { reason: '忙碌/不方便' } })
    await load()
  } catch (e: any) {
    err.value = e?.message || '操作失败'
  }
}
async function progress() {
  try {
    await api.post(`/work-orders/${id.value}/progress`, { message: '已到达现场，开始处理' })
    await load()
  } catch (e: any) {
    err.value = e?.message || '操作失败'
  }
}
async function finish() {
  try {
    await api.post(`/work-orders/${id.value}/finish`, null, { params: { message: '已修复完成' } })
    await load()
  } catch (e: any) {
    err.value = e?.message || '操作失败'
  }
}
async function rate() {
  try {
    await api.post(`/work-orders/${id.value}/rating`, {
      stars: rateStars.value,
      tags: [],
      comment: rateComment.value || '',
    })
    await load()
  } catch (e: any) {
    err.value = e?.message || '操作失败'
  }
}

onMounted(async () => {
  await load()
  connectWs()
})

onBeforeUnmount(() => {
  if (client) {
    client.deactivate()
    client = null
  }
})
</script>

<template>
  <PageContainer>
    <UiCard>
      <template #title>
        <div class="hd">
          <button class="back" @click="router.back()">返回</button>
          <span>工单 #{{ id }}</span>
        </div>
      </template>
      <template #extra>
        <button @click="load">刷新</button>
        <button v-if="canDelete" class="danger" @click="deleteMine">删除</button>
      </template>

      <div v-if="err" class="error-text" style="margin-bottom: 10px">{{ err }}</div>

      <div v-if="detail" class="grid">
        <div class="kv">
          <div class="k">状态</div>
          <div class="v"><span class="chip">{{ detail.workOrder.status }}</span></div>
        </div>
        <div class="kv">
          <div class="k">工种</div>
          <div class="v">{{ detail.workOrder.tradeCode }}</div>
        </div>
        <div class="kv">
          <div class="k">指派</div>
          <div class="v">{{ detail.workOrder.assignedWorkerId || '-' }}</div>
        </div>
        <div class="kv span2">
          <div class="k">地址</div>
          <div class="v">{{ detail.workOrder.address || '-' }}</div>
        </div>
        <div class="kv span2">
          <div class="k">描述</div>
          <div class="v">{{ detail.workOrder.description }}</div>
        </div>

        <div v-if="detail.images?.length" class="kv span2">
          <div class="k">图片</div>
          <div class="v chips">
            <a v-for="img in detail.images" :key="img.id" class="pill" :href="getAssetUrl(img.url)" target="_blank">
              {{ img.url }}
            </a>
          </div>
        </div>

        <div class="kv span2" v-if="detail.requesterPhone || detail.workerPhone || detail.workOrder.etaAt">
          <div class="k">联系/上门</div>
          <div class="v">
            <template v-if="role === 'WORKER'">
              <span class="muted">用户手机号：</span>
              <a v-if="detail.requesterPhone" class="link" :href="`tel:${detail.requesterPhone}`">{{ detail.requesterPhone }}</a>
              <span v-else class="muted">(未填写)</span>
            </template>
            <template v-else>
              <span class="muted">维修人员手机号：</span>
              <a v-if="detail.workerPhone" class="link" :href="`tel:${detail.workerPhone}`">{{ detail.workerPhone }}</a>
              <span v-else class="muted">(未填写)</span>
            </template>

            <div v-if="detail.workOrder.etaAt" style="margin-top: 6px">
              <span class="muted">预计上门：</span>
              <span>{{ detail.workOrder.etaAt }}</span>
            </div>
          </div>
        </div>

        <div class="actions span2">
          <template v-if="role === 'WORKER'">
            <button class="primary" @click="accept">接单</button>
            <button @click="reject">拒单</button>
            <button @click="progress">更新进度</button>
            <button @click="finish">完工</button>

            <div class="eta" v-if="detail?.workOrder?.status !== 'CLOSED'">
              <input
                v-model="etaInput"
                type="datetime-local"
                class="chat-input"
                :min="new Date().toISOString().slice(0, 16)"
                placeholder="预计上门时间"
              />
              <button class="primary" :disabled="!etaInput" @click="setEta">保存ETA</button>
            </div>
          </template>
          <template v-else>
            <button v-if="detail.workOrder.status === 'DONE_WAIT_CONFIRM'" class="primary" @click="confirmFixed">
              确认已修好
            </button>

            <div v-if="detail.workOrder.status === 'DONE_WAIT_RATE'" class="rate">
              <div class="rate-row">
                <span class="muted">星级：</span>
                <select v-model.number="rateStars" class="chat-input" style="max-width: 140px">
                  <option :value="5">5</option>
                  <option :value="4">4</option>
                  <option :value="3">3</option>
                  <option :value="2">2</option>
                  <option :value="1">1</option>
                </select>
              </div>
              <input v-model="rateComment" class="chat-input" placeholder="评价（可选）" />
              <button class="primary" @click="rate">提交评价</button>
            </div>
          </template>
        </div>
      </div>
    </UiCard>

    <UiCard title="进度">
      <ul class="timeline">
        <li v-for="p in detail?.progress || []" :key="p.id">
          <span class="ts">{{ p.createdAt }}</span>
          <span class="muted">{{ p.fromStatus }} → {{ p.toStatus }}</span>
          <span>：{{ p.message }}</span>
        </li>
      </ul>
    </UiCard>

    <UiCard title="留言">
      <div class="chat" :data-on="wsConnected ? '1' : '0'">
        <div class="chat-hd">
          <span class="dot" />
          <span class="muted">{{ wsConnected ? '已连接' : '连接中' }}</span>
        </div>
        <div class="chat-bd">
          <div v-for="m in chat" :key="m.id" class="msg">
            <div class="meta"><b>#{{ m.senderId }}</b> <span class="muted">{{ m.createdAt }}</span></div>
            <div class="bubble">{{ m.content }}</div>
          </div>
        </div>
      </div>

      <div class="row" style="margin-top: 10px">
        <input v-model="chatText" class="chat-input" placeholder="输入留言..." @keydown.enter="sendChat" />
        <button class="primary" @click="sendChat">发送</button>
      </div>
    </UiCard>
  </PageContainer>
</template>

<style scoped>
.hd {
  display: flex;
  align-items: center;
  gap: 10px;
}

.back {
  padding: 6px 10px;
  border-radius: 12px;
  border: 1px solid var(--sr-border);
  background: transparent;
  color: inherit;
  font-weight: 900;
  cursor: pointer;
}

.danger {
  padding: 6px 10px;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--sr-danger) 40%, var(--sr-border));
  background: color-mix(in srgb, var(--sr-danger) 10%, transparent);
  color: inherit;
  font-weight: 900;
  cursor: pointer;
}

.danger:hover {
  background: color-mix(in srgb, var(--sr-danger) 14%, transparent);
}

.back:hover {
  background: color-mix(in srgb, var(--sr-brand) 6%, transparent);
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.kv {
  border: 1px solid var(--sr-border);
  border-radius: 16px;
  padding: 12px;
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
}

.k {
  font-size: 12px;
  font-weight: 800;
  color: var(--sr-text2);
}

.v {
  margin-top: 6px;
  font-weight: 700;
}

.span2 {
  grid-column: span 2;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.rate {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.rate-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.eta {
  display: flex;
  gap: 10px;
  align-items: center;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--sr-border2);
  background: color-mix(in srgb, var(--sr-brand) 8%, transparent);
  text-decoration: none;
  color: inherit;
  font-size: 12px;
}

.timeline {
  margin: 0;
  padding-left: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.timeline li {
  border: 1px solid var(--sr-border);
  border-radius: 16px;
  padding: 10px 12px;
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
}

.ts {
  font-variant-numeric: tabular-nums;
  font-size: 12px;
  color: var(--sr-text2);
  margin-right: 10px;
}

.chat {
  border: 1px solid var(--sr-border);
  border-radius: 16px;
  overflow: hidden;
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
}

.chat-hd {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--sr-border);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--sr-warn);
}

.chat[data-on='1'] .dot {
  background: var(--sr-ok);
}

.chat-bd {
  max-height: 300px;
  overflow: auto;
  padding: 12px;
  display: grid;
  gap: 10px;
}

.msg .meta {
  font-size: 12px;
  margin-bottom: 6px;
}

.bubble {
  border: 1px solid var(--sr-border);
  border-radius: 14px;
  padding: 10px 12px;
  background: var(--sr-panel);
}

.chat-input {
  flex: 1;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .span2 {
    grid-column: span 1;
  }
}
</style>

