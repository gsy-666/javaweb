export type Role = 'USER' | 'WORKER' | 'ADMIN'

export type WorkOrderStatus =
  | 'NEW'
  | 'ASSIGNED'
  | 'ACCEPTED'
  | 'IN_PROGRESS'
  | 'WAIT_USER'
  | 'DONE_WAIT_RATE'
  | 'CLOSED'
  | 'CANCELLED'

export type SituationSeverity = 'info' | 'warn' | 'danger'

export type SituationEventKind =
  | 'STATUS_CHANGE'
  | 'NOTE'
  | 'SLA_WARN'
  | 'SLA_BREACH'
  | 'DISPATCH_EXPLAIN'

export type GeoPoint = {
  lng: number
  lat: number
}

export type SituationEvent = {
  id: string
  ts: number
  workOrderId: number
  kind: SituationEventKind
  title: string
  subtitle?: string
  fromStatus?: WorkOrderStatus | null
  toStatus?: WorkOrderStatus | null
  severity: SituationSeverity
  geo?: GeoPoint
  tags?: string[]
}

export type WorkOrder = {
  id: number
  code?: string
  requesterId?: number
  tradeCode?: string
  description?: string
  address?: string
  lng?: number | null
  lat?: number | null
  status: WorkOrderStatus | string
  priority?: number | null
  assignedWorkerId?: number | null
  escalationLevel?: number | null
  createdAt?: string
  assignedAt?: string | null
  acceptedAt?: string | null
  finishedAt?: string | null
  closedAt?: string | null
  cancelledAt?: string | null
}

export type WorkOrderProgress = {
  id: number
  workOrderId: number
  fromStatus?: string | null
  toStatus?: string | null
  message?: string | null
  operatorId?: number | null
  createdAt?: string
}
