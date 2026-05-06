export type ThemeMode = 'dark' | 'light'
export type AppKind = 'admin' | 'worker' | 'user'

export type ThemeTokens = {
  mode: ThemeMode
  app: AppKind

  bg0: string
  bg1: string
  panel: string
  panel2: string
  border: string
  border2: string

  text1: string
  text2: string
  text3: string

  brand: string
  brand2: string
  accent: string
  danger: string
  warn: string
  ok: string

  radius: string
  shadow: string
  shadow2: string
}

export function getTokens(app: AppKind): ThemeTokens {
  if (app === 'admin') {
    return {
      mode: 'dark',
      app,
      bg0: '#060816',
      bg1: '#0a0f24',
      panel: 'rgba(15, 23, 42, 0.62)',
      panel2: 'rgba(2, 6, 23, 0.55)',
      border: 'rgba(255, 255, 255, 0.10)',
      border2: 'rgba(99, 102, 241, 0.45)',
      text1: 'rgba(248, 250, 252, 0.98)',
      text2: 'rgba(148, 163, 184, 0.92)',
      text3: 'rgba(100, 116, 139, 0.95)',
      brand: '#6366f1',
      brand2: '#22d3ee',
      accent: '#a78bfa',
      danger: '#fb7185',
      warn: '#fbbf24',
      ok: '#34d399',
      radius: '16px',
      shadow: '0 10px 40px rgba(0,0,0,0.35)',
      shadow2: '0 18px 70px rgba(0,0,0,0.45)',
    }
  }

  if (app === 'worker') {
    return {
      mode: 'light',
      app,
      bg0: '#f5f7ff',
      bg1: '#eef2ff',
      panel: 'rgba(255, 255, 255, 0.80)',
      panel2: 'rgba(255, 255, 255, 0.92)',
      border: 'rgba(15, 23, 42, 0.10)',
      border2: 'rgba(79, 70, 229, 0.18)',
      text1: 'rgba(15, 23, 42, 0.95)',
      text2: 'rgba(51, 65, 85, 0.88)',
      text3: 'rgba(71, 85, 105, 0.88)',
      brand: '#2563eb',
      brand2: '#06b6d4',
      accent: '#7c3aed',
      danger: '#ef4444',
      warn: '#f59e0b',
      ok: '#16a34a',
      radius: '16px',
      shadow: '0 12px 32px rgba(15, 23, 42, 0.10)',
      shadow2: '0 18px 60px rgba(15, 23, 42, 0.12)',
    }
  }

  // user/repair
  return {
    mode: 'light',
    app: 'user',
    bg0: '#fff7ed',
    bg1: '#fff1f2',
    panel: 'rgba(255, 255, 255, 0.82)',
    panel2: 'rgba(255, 255, 255, 0.94)',
    border: 'rgba(15, 23, 42, 0.10)',
    border2: 'rgba(249, 115, 22, 0.22)',
    text1: 'rgba(15, 23, 42, 0.95)',
    text2: 'rgba(51, 65, 85, 0.88)',
    text3: 'rgba(71, 85, 105, 0.88)',
    brand: '#f97316',
    brand2: '#fb7185',
    accent: '#0ea5e9',
    danger: '#ef4444',
    warn: '#f59e0b',
    ok: '#16a34a',
    radius: '16px',
    shadow: '0 12px 32px rgba(15, 23, 42, 0.10)',
    shadow2: '0 18px 60px rgba(15, 23, 42, 0.12)',
  }
}

export function applyTheme(tokens: ThemeTokens) {
  const root = document.documentElement

  root.dataset.srApp = tokens.app
  root.dataset.srMode = tokens.mode

  const map: Record<string, string> = {
    '--sr-bg0': tokens.bg0,
    '--sr-bg1': tokens.bg1,
    '--sr-panel': tokens.panel,
    '--sr-panel2': tokens.panel2,
    '--sr-border': tokens.border,
    '--sr-border2': tokens.border2,
    '--sr-text1': tokens.text1,
    '--sr-text2': tokens.text2,
    '--sr-text3': tokens.text3,
    '--sr-brand': tokens.brand,
    '--sr-brand2': tokens.brand2,
    '--sr-accent': tokens.accent,
    '--sr-danger': tokens.danger,
    '--sr-warn': tokens.warn,
    '--sr-ok': tokens.ok,
    '--sr-radius': tokens.radius,
    '--sr-shadow': tokens.shadow,
    '--sr-shadow2': tokens.shadow2,
  }

  for (const [k, v] of Object.entries(map)) root.style.setProperty(k, v)
}
