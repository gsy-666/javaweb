import type { SituationEvent, WorkOrder, WorkOrderProgress, SituationSeverity } from './types'
import { getSlaState } from './sla'
import { getStatusLabel } from './statusMeta'

function toMs(s?: string | null): number | null {
  if (!s) return null
  const t = Date.parse(s)
  return Number.isFinite(t) ? t : null
}

function severityFromProgress(p: WorkOrderProgress): SituationSeverity {
  const msg = (p.message || '').trim()
  if (msg.includes('超时升级')) return 'danger'
  if (p.toStatus && p.fromStatus && p.toStatus !== p.fromStatus) return 'info'
  return 'info'
}

export function normalizeProgressEvents(progress: WorkOrderProgress[]): SituationEvent[] {
  return (progress || [])
    .map((p) => {
      const ts = toMs(p.createdAt) || Date.now()
      const title = p.toStatus && p.fromStatus && p.toStatus !== p.fromStatus
        ? `${getStatusLabel(p.fromStatus)} → ${getStatusLabel(p.toStatus)}`
        : (p.message || '进度更新')

      return {
        id: `p:${p.id}`,
        ts,
        workOrderId: p.workOrderId,
        kind: p.toStatus && p.fromStatus && p.toStatus !== p.fromStatus ? 'STATUS_CHANGE' : 'NOTE',
        title,
        subtitle: p.message || undefined,
        fromStatus: (p.fromStatus as any) || null,
        toStatus: (p.toStatus as any) || null,
        severity: severityFromProgress(p),
      } satisfies SituationEvent
    })
    .sort((a, b) => a.ts - b.ts)
}

export function synthesizeSlaEvent(wo: WorkOrder): SituationEvent | null {
  const sla = getSlaState(wo)
  if (sla.state === 'ok') return null

  const kind = sla.state === 'breach' ? 'SLA_BREACH' : 'SLA_WARN'
  const severity: SituationSeverity = sla.state === 'breach' ? 'danger' : 'warn'

  const stage = String(wo.status)
  const title = sla.state === 'breach' ? `SLA超时：${getStatusLabel(stage)}` : `SLA临近：${getStatusLabel(stage)}`

  return {
    id: `sla:${wo.id}:${stage}`,
    ts: Date.now(),
    workOrderId: wo.id,
    kind,
    title,
    subtitle: sla.deadlineMs ? `截止 ${new Date(sla.deadlineMs).toLocaleString()}` : undefined,
    fromStatus: null,
    toStatus: stage as any,
    severity,
    geo: wo.lng != null && wo.lat != null ? { lng: wo.lng, lat: wo.lat } : undefined,
    tags: wo.escalationLevel && wo.escalationLevel > 0 ? [`升级L${wo.escalationLevel}`] : undefined,
  }
}
