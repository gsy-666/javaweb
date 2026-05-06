let p: Promise<any> | null = null

type AMapGlobal = any

declare global {
  interface Window {
    AMap?: AMapGlobal
  }
}

export function loadAMap() {
  if (window.AMap) return Promise.resolve(window.AMap)
  if (p) return p

  const key = (import.meta.env.VITE_AMAP_KEY as string | undefined)?.trim()
  if (!key) {
    return Promise.reject(new Error('Missing VITE_AMAP_KEY'))
  }

  p = new Promise((resolve, reject) => {
    const id = 'amap-js'
    const existed = document.getElementById(id) as HTMLScriptElement | null
    if (existed) {
      existed.addEventListener('load', () => resolve(window.AMap))
      existed.addEventListener('error', () => reject(new Error('Load AMap failed')))
      return
    }

    const s = document.createElement('script')
    s.id = id
    s.async = true
    s.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}&plugin=AMap.MarkerCluster`
    s.onload = () => {
      if (!window.AMap) {
        reject(new Error('AMap not found on window'))
        return
      }
      resolve(window.AMap)
    }
    s.onerror = () => reject(new Error('Load AMap failed'))
    document.head.appendChild(s)
  })

  return p
}
