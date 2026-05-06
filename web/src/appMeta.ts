import { computed } from 'vue'
import { account, role } from './user'

export type AppKind = 'admin' | 'worker' | 'user'

export const appKind = computed<AppKind>(() => {
  if (role.value === 'ADMIN') return 'admin'
  if (role.value === 'WORKER') return 'worker'
  return 'user'
})

export const appTitle = computed(() => {
  if (appKind.value === 'admin') return '智慧后勤报修与满意度评价系统'
  if (appKind.value === 'worker') return '智慧后勤报修与满意度评价系统'
  return '智慧后勤报修与满意度评价系统'
})

export const greeting = computed(() => {
  const name = (account.value || '').trim() || '同学'
  if (appKind.value === 'admin') return `您好，${name}管理员`
  if (appKind.value === 'worker') return `您好，${name}`
  return `您好，${name}`
})
