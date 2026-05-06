import type { WorkOrder } from './types'

export type SlaThresholds = {
  assignTimeoutSeconds: number
  acceptTimeoutSeconds: number
  firstUpdateTimeoutSeconds: number
  finishTimeoutSeconds: number
}

// Mirror backend defaults in SlaProperties
export const defaultSla: SlaThresholds = {
  assignTimeoutSeconds: 60,
  acceptTimeoutSeconds: 300,
  firstUpdateTimeoutSeconds: 900,
  finishTimeoutSeconds: 7200,
}

const WARN_RATIO = 0.8

function parseTs(s?: string | null): number | null {
  if (!s) return null
  const t = Date.parse(s)
  return Number.isFinite(t) ? t : null
}

export function getSlaStage(wo: WorkOrder): 'ASSIGN' | 'ACCEPT' | 'FIRST_UPDATE' | 'FINISH' | null {
  const st = String(wo.status || '')
  if (st === 'NEW') return 'ASSIGN'
  if (st === 'ASSIGNED') return 'ACCEPT'
  if (st === 'ACCEPTED') return 'FIRST_UPDATE'
  if (st === 'IN_PROGRESS' || st === 'WAIT_USER') return 'FINISH'
  return null
}

export function getDeadlineMs(wo: WorkOrder, sla: SlaThresholds = defaultSla): number | null {
  const stage = getSlaStage(wo)
  const now = Date.now()
  if (!stage) return null

  if (stage === 'ASSIGN') {
    const base = parseTs(wo.createdAt) || now
    return base + sla.assignTimeoutSeconds * 1000
  }
  if (stage === 'ACCEPT') {
    const base = parseTs(wo.assignedAt) || parseTs(wo.createdAt) || now
    return base + sla.acceptTimeoutSeconds * 1000
  }
  if (stage === 'FIRST_UPDATE') {
    const base = parseTs(wo.acceptedAt) || now
    return base + sla.firstUpdateTimeoutSeconds * 1000
  }
  if (stage === 'FINISH') {
    const base = parseTs(wo.acceptedAt) || now
    return base + sla.finishTimeoutSeconds * 1000
  }
  return null
}

export function getSlaState(
  wo: WorkOrder,
  sla: SlaThresholds = defaultSla
): { state: 'ok' | 'warn' | 'breach'; remainingMs: number | null; deadlineMs: number | null } {
  const deadlineMs = getDeadlineMs(wo, sla)
  if (!deadlineMs) return { state: 'ok', remainingMs: null, deadlineMs: null }
  const remainingMs = deadlineMs - Date.now()
  if (remainingMs <= 0) return { state: 'breach', remainingMs, deadlineMs }

  const stage = getSlaStage(wo)
  let totalMs = 1
  if (stage === 'ASSIGN') totalMs = sla.assignTimeoutSeconds * 1000
  else if (stage === 'ACCEPT') totalMs = sla.acceptTimeoutSeconds * 1000
  else if (stage === 'FIRST_UPDATE') totalMs = sla.firstUpdateTimeoutSeconds * 1000
  else if (stage === 'FINISH') totalMs = sla.finishTimeoutSeconds * 1000

  const usedRatio = 1 - remainingMs / Math.max(1, totalMs)
  if (usedRatio >= WARN_RATIO) return { state: 'warn', remainingMs, deadlineMs }
  return { state: 'ok', remainingMs, deadlineMs }
}
