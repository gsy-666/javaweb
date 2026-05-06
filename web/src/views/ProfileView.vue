<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { role, account, userId, logout } from '../user'
import { api } from '../api'

import PageContainer from '../components/PageContainer.vue'
import UiCard from '../components/UiCard.vue'

const router = useRouter()

const loading = ref(false)
const err = ref<string | null>(null)

const serverMe = ref<{ userId: number; role: string; displayName?: string | null; account?: string | null } | null>(null)

const displayAccount = computed(() => {
  return serverMe.value?.account || account.value || '未填写'
})

const displayRole = computed(() => {
  return (serverMe.value?.role || role.value) as any
})

const displayName = computed(() => {
  return serverMe.value?.displayName || ''
})

const displayUserId = computed(() => {
  return serverMe.value?.userId || userId.value || 0
})

const initials = computed(() => {
  const a = (displayAccount.value || '').trim()
  if (!a || a === '未填写') return 'U'
  return a.slice(0, 1).toUpperCase()
})

function go() {
  if (role.value === 'ADMIN') router.replace('/admin')
  else if (role.value === 'WORKER') router.replace('/worker')
  else router.replace('/')
}

async function onLogout() {
  try {
    await api.post('/auth/logout')
  } catch {
    // ignore
  } finally {
    logout()
    router.replace('/login')
  }
}

onMounted(async () => {
  loading.value = true
  err.value = null
  try {
    const resp = await api.get('/users/me')
    serverMe.value = resp.data?.data || null
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <PageContainer>
    <div class="me-grid">
      <UiCard class="hero" padded>
        <template #title>
          <div class="ttl">
            <div class="h">个人中心</div>
            <div class="muted tiny">账号信息 · 身份说明 · 入口操作</div>
          </div>
        </template>
        <template #extra>
          <div class="row">
            <button class="primary" @click="go">进入系统</button>
            <button @click="onLogout" :disabled="loading">退出登录</button>
          </div>
        </template>

        <div v-if="err" class="error-text" style="margin-bottom: 10px">{{ err }}</div>

        <div class="profile">
          <div class="avatar">{{ initials }}</div>
          <div class="meta">
            <div class="line">
              <span class="k">账号</span>
              <span class="v">{{ displayAccount }}</span>
            </div>
            <div class="line">
              <span class="k">身份</span>
              <span class="v"><span class="chip">{{ displayRole }}</span></span>
            </div>
            <div class="line">
              <span class="k">ID</span>
              <span class="v">{{ displayUserId }}</span>
            </div>
            <div v-if="displayName" class="line">
              <span class="k">昵称</span>
              <span class="v">{{ displayName }}</span>
            </div>
          </div>
        </div>
      </UiCard>

      <UiCard title="身份权限说明" class="span2">
        <div class="tips">
          <div class="tip">
            <div class="row">
              <span class="chip">USER</span>
              <b>报修端</b>
            </div>
            <div class="muted">可创建报修工单、查看自己的工单与处理进度、评价服务。</div>
          </div>
          <div class="tip">
            <div class="row">
              <span class="chip">WORKER</span>
              <b>工人端</b>
            </div>
            <div class="muted">可接单、更新进度、上报位置，处理用户反馈。</div>
          </div>
          <div class="tip">
            <div class="row">
              <span class="chip">ADMIN</span>
              <b>管理端</b>
            </div>
            <div class="muted">可查看态势面板、统计与绩效排行（示例页）。</div>
          </div>
        </div>
      </UiCard>
    </div>
  </PageContainer>
</template>

<style scoped>
.me-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.span2 {
  grid-column: span 2;
}

.hero {
  grid-column: span 2;
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

.profile {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 12px;
  align-items: start;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  font-weight: 950;
  font-size: 22px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-brand) 10%, transparent);
}

.meta {
  display: grid;
  gap: 8px;
}

.line {
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 10px;
  align-items: center;
}

.k {
  font-size: 12px;
  font-weight: 800;
  color: var(--sr-text2);
}

.v {
  min-width: 0;
}

.tips {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.tip {
  border: 1px solid var(--sr-border);
  border-radius: 18px;
  padding: 12px;
  background: color-mix(in srgb, var(--sr-panel2) 72%, transparent);
}

@media (max-width: 1000px) {
  .tips {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .me-grid {
    grid-template-columns: 1fr;
  }
  .span2 {
    grid-column: span 1;
  }
  .hero {
    grid-column: span 1;
  }
}
</style>
