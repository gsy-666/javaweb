import { computed, reactive } from 'vue'
import type { Role } from './types'
import { role as authedRole } from '../user'

export type FocusFilters = {
  statuses: string[]
  tradeCode: string | null
  onlyEscalated: boolean
}

export const focusState = reactive({
  perspectiveRole: authedRole.value as Role,
  focusWorkOrderId: null as number | null,
  focusZone: null as string | null,
  filters: {
    statuses: [] as string[],
    tradeCode: null,
    onlyEscalated: false,
  } as FocusFilters,
})

export const effectiveRole = computed<Role>(() => {
  // 同一路由允许视角切换；默认等于登录角色
  return focusState.perspectiveRole
})

export function setPerspective(role: Role) {
  focusState.perspectiveRole = role
  focusState.focusWorkOrderId = null
  focusState.focusZone = null
}

export function focusWorkOrder(id: number | null) {
  focusState.focusWorkOrderId = id
}
