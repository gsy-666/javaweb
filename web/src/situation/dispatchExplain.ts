export type DispatchWeights = {
  wDistance: number
  wLoad: number
  wPerformance: number
}

// Mirror backend defaults from DispatchProperties
export const defaultDispatchWeights: DispatchWeights = {
  wDistance: 0.55,
  wLoad: 0.35,
  wPerformance: 0.1,
}

export function explainDispatchScore(input: {
  distanceKm: number | null
  activeCount: number | null
  performanceScore: number | null
  weights?: DispatchWeights
}) {
  const weights = input.weights || defaultDispatchWeights
  const d = input.distanceKm == null ? 999 : input.distanceKm
  const load = input.activeCount == null ? 0 : input.activeCount
  const perf = input.performanceScore == null ? 0.5 : input.performanceScore

  const distanceTerm = weights.wDistance * (1 / (1 + d))
  const loadTerm = weights.wLoad * (1 / (1 + load))
  const perfTerm = weights.wPerformance * perf
  const finalScore = distanceTerm + loadTerm + perfTerm

  return {
    distanceTerm,
    loadTerm,
    perfTerm,
    finalScore,
    summary: `距离项 ${(distanceTerm * 100).toFixed(1)} + 负载项 ${(loadTerm * 100).toFixed(1)} + 绩效项 ${(perfTerm * 100).toFixed(1)} = ${(finalScore * 100).toFixed(1)}`,
  }
}
