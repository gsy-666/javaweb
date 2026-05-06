import { ref, watchEffect } from 'vue'
import { setToken } from './api'

export type Role = 'USER' | 'WORKER' | 'ADMIN'

const roleSet = new Set<Role>(['USER', 'WORKER', 'ADMIN'])

const storedRoleRaw = localStorage.getItem('sr_role')
const storedRole: Role = storedRoleRaw && roleSet.has(storedRoleRaw as Role) ? (storedRoleRaw as Role) : 'USER'

const storedId = Number(localStorage.getItem('sr_userId') || '0')
const storedAccount = (localStorage.getItem('sr_account') || '').trim()
const storedToken = (localStorage.getItem('sr_token') || '').trim()

export const userId = ref<number>(Number.isFinite(storedId) && storedId > 0 ? Math.trunc(storedId) : 0)
export const role = ref<Role>(storedRole)
export const account = ref<string>(storedAccount)
export const token = ref<string>(storedToken)

export const isAuthed = ref<boolean>(token.value.length > 0 && userId.value > 0)

export function setSession(next: { userId: number; role: Role; account?: string; token?: string }) {
  userId.value = Number.isFinite(next.userId) && next.userId > 0 ? Math.trunc(next.userId) : 0
  role.value = next.role
  account.value = (next.account || '').trim()
  token.value = (next.token || '').trim()
  isAuthed.value = token.value.length > 0 && userId.value > 0
}

export function logout() {
  userId.value = 0
  role.value = 'USER'
  account.value = ''
  token.value = ''
  isAuthed.value = false
}

watchEffect(() => {
  localStorage.setItem('sr_userId', String(userId.value || 0))
  localStorage.setItem('sr_role', role.value)
  localStorage.setItem('sr_account', account.value)
  localStorage.setItem('sr_token', token.value)

  setToken(token.value)
})
