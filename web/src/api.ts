import axios from 'axios'

function resolveApiBaseUrl(): string {
  const envBase = import.meta.env.VITE_API_BASE_URL as string | undefined
  if (envBase?.trim()) {
    return envBase.trim().replace(/\/$/, '')
  }
  return '/api'
}

export const apiBaseUrl = resolveApiBaseUrl()

export const api = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000,
})

export function getAssetUrl(path: string): string {
  if (!path) return apiBaseUrl

  // Backend exposes uploaded files at /uploads/** (not /api/uploads/**)
  if (path.startsWith('/uploads/')) {
    // In dev, Vite can proxy /uploads -> backend. If not, fall back to backend port.
    if (apiBaseUrl.startsWith('/')) {
      return window.location.port === '5173' ? `http://localhost:8082${path}` : path
    }
    return `${new URL(apiBaseUrl).origin}${path}`
  }

  return path.startsWith('http://') || path.startsWith('https://')
    ? path
    : `${apiBaseUrl}${path.startsWith('/') ? '' : '/'}${path}`
}

export function getWsUrl(path = '/ws'): string {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const host = apiBaseUrl.startsWith('/') ? window.location.host : new URL(apiBaseUrl).host
  return `${wsProtocol}//${host}${normalizedPath}`
}

let authToken = ''

export function setToken(token: string) {
  authToken = (token || '').trim()
}

function parseApiError(error: unknown): string {
  const e = error as any
  return (
    e?.response?.data?.message ||
    e?.response?.data?.msg ||
    e?.message ||
    '请求失败，请稍后重试'
  )
}

api.interceptors.request.use((config) => {
  if (authToken) {
    config.headers = config.headers || {}
    ;(config.headers as any)['Authorization'] = `Bearer ${authToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status
    if (status === 401 || status === 403) {
      const m = await import('./user')
      const r = await import('./router')
      m.logout()
      if (r.router.currentRoute.value.path !== '/login') {
        await r.router.replace({ path: '/login', query: { redirect: r.router.currentRoute.value.fullPath } })
      }
    }
    return Promise.reject(new Error(parseApiError(error)))
  }
)
