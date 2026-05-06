import type { SituationSeverity, Role } from './types'

export type StatusMeta = {
  label: string
  color: string
  severity: SituationSeverity
  pulse?: 'none' | 'slow' | 'fast'
  actions: {
    USER: string[]
    WORKER: string[]
    ADMIN: string[]
  }
}

export const statusMeta: Record<string, StatusMeta> = {
  NEW: {
    label: '待派单',
    color: '#38bdf8',
    severity: 'info',
    pulse: 'none',
    actions: { USER: ['取消'], WORKER: [], ADMIN: ['人工派单(占位)'] },
  },
  ASSIGNED: {
    label: '待接单',
    color: '#f59e0b',
    severity: 'warn',
    pulse: 'slow',
    actions: { USER: [], WORKER: ['接单', '拒单'], ADMIN: ['改派(占位)'] },
  },
  ACCEPTED: {
    label: '已接单',
    color: '#a78bfa',
    severity: 'info',
    pulse: 'none',
    actions: { USER: [], WORKER: ['更新进度'], ADMIN: [] },
  },
  IN_PROGRESS: {
    label: '处理中',
    color: '#22c55e',
    severity: 'info',
    pulse: 'none',
    actions: { USER: [], WORKER: ['更新进度', '完工'], ADMIN: [] },
  },
  WAIT_USER: {
    label: '待用户确认',
    color: '#10b981',
    severity: 'info',
    pulse: 'none',
    actions: { USER: ['确认(占位)'], WORKER: ['更新进度'], ADMIN: [] },
  },
  DONE_WAIT_RATE: {
    label: '待评价',
    color: '#06b6d4',
    severity: 'info',
    pulse: 'none',
    actions: { USER: ['评价'], WORKER: [], ADMIN: [] },
  },
  CLOSED: {
    label: '已关闭',
    color: '#94a3b8',
    severity: 'info',
    pulse: 'none',
    actions: { USER: [], WORKER: [], ADMIN: [] },
  },
  CANCELLED: {
    label: '已取消',
    color: '#64748b',
    severity: 'info',
    pulse: 'none',
    actions: { USER: [], WORKER: [], ADMIN: [] },
  },
}

export function getStatusLabel(status: string): string {
  return statusMeta[status]?.label || status
}

export function getStatusColor(status: string): string {
  return statusMeta[status]?.color || '#94a3b8'
}

export function getAllowedActions(status: string, role: Role): string[] {
  return statusMeta[status]?.actions?.[role] || []
}
