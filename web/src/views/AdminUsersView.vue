<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '../api'
import PageContainer from '../components/PageContainer.vue'
import UiCard from '../components/UiCard.vue'

type Row = {
  userId: number
  role: string
  account?: string | null
  displayName?: string | null
  phone?: string | null
  enabled?: number | null
  tradeCode?: string | null
  acceptOrders?: number | null
  createdAt?: string | null
}

const loading = ref(false)
const err = ref<string | null>(null)
const roleFilter = ref<string>('')
const q = ref('')

const list = ref<Row[]>([])

async function load() {
  loading.value = true
  err.value = null
  try {
    const resp = await api.get('/admin/users', { params: { role: roleFilter.value || undefined } })
    list.value = resp.data?.data || []
  } catch (e: any) {
    err.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  const kw = q.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter((x) => {
    const s = `${x.userId} ${x.role || ''} ${x.account || ''} ${x.displayName || ''} ${x.phone || ''} ${x.tradeCode || ''}`.toLowerCase()
    return s.includes(kw)
  })
})

function roleLabel(r: string) {
  const rr = (r || '').toUpperCase()
  if (rr === 'ADMIN') return '管理员'
  if (rr === 'WORKER') return '工人'
  if (rr === 'USER') return '用户'
  return r
}

function tradeLabel(code?: string | null) {
  if (!code) return '-'
  if (code === 'WATER_ELEC') return '水电'
  if (code === 'CARPENTER') return '木工'
  if (code === 'NETWORK') return '网络'
  return code
}

onMounted(load)
</script>

<template>
  <PageContainer>
    <div class="grid">
      <UiCard class="hero" padded>
        <template #title>
          <div class="ttl">
            <div class="h">用户管理</div>
            <div class="muted tiny">管理员查看所有用户/工人信息</div>
          </div>
        </template>
        <template #extra>
          <div class="row">
            <select v-model="roleFilter" class="sel" @change="load">
              <option value="">全部角色</option>
              <option value="USER">用户</option>
              <option value="WORKER">工人</option>
              <option value="ADMIN">管理员</option>
            </select>
            <input v-model="q" class="inp" placeholder="搜索：ID/账号/姓名/手机号/工种" />
            <button class="primary" :disabled="loading" @click="load">刷新</button>
          </div>
        </template>

        <div v-if="err" class="error-text" style="margin-top: 10px">{{ err }}</div>
      </UiCard>

      <UiCard title="用户列表" class="span2">
        <div v-if="loading" class="muted tiny" style="margin-bottom: 8px">加载中...</div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th style="width: 90px">ID</th>
                <th style="width: 90px">角色</th>
                <th style="width: 170px">账号</th>
                <th style="width: 140px">姓名</th>
                <th style="width: 140px">手机号</th>
                <th style="width: 120px">工种</th>
                <th style="width: 90px">接单</th>
                <th style="width: 90px">启用</th>
                <th style="width: 190px">创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in filtered" :key="u.userId">
                <td><span class="chip">#{{ u.userId }}</span></td>
                <td><span class="chip" :data-r="String(u.role)">{{ roleLabel(u.role) }}</span></td>
                <td>{{ u.account || '-' }}</td>
                <td>{{ u.displayName || '-' }}</td>
                <td>{{ u.phone || '-' }}</td>
                <td>{{ tradeLabel(u.tradeCode) }}</td>
                <td>{{ u.acceptOrders == null ? '-' : u.acceptOrders ? '是' : '否' }}</td>
                <td>{{ u.enabled == null ? '-' : u.enabled ? '是' : '否' }}</td>
                <td class="muted tiny">{{ u.createdAt || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </UiCard>
    </div>
  </PageContainer>
</template>

<style scoped>
.grid {
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

.row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.sel {
  height: 36px;
  border-radius: 12px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel2) 75%, transparent);
  color: inherit;
  padding: 0 10px;
}

.inp {
  width: min(380px, 48vw);
  height: 36px;
  border-radius: 12px;
  border: 1px solid var(--sr-border);
  background: color-mix(in srgb, var(--sr-panel2) 75%, transparent);
  color: inherit;
  padding: 0 10px;
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

.table-wrap {
  overflow-x: auto;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .span2 {
    grid-column: span 1;
  }
  .row {
    flex-wrap: wrap;
  }
  .inp {
    width: 100%;
  }
}
</style>
