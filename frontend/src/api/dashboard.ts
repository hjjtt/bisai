import { get, post } from '@/utils/request'
import type { StudentStats, TeacherStats, AdminStats, CorrelationStats, PairwiseComparison, PairwiseRanking } from '@/types'

export function getStudentStats() {
  return get<StudentStats>('/dashboard/student')
}

export function getTeacherStats() {
  return get<TeacherStats>('/dashboard/teacher')
}

export function getAdminStats(days?: number) {
  return get<AdminStats>('/dashboard/admin', days ? { days } : undefined)
}

// P0.3 + P2.9: 评分一致性
export function getCorrelation(taskId?: number) {
  return get<CorrelationStats>('/consistency/correlation', taskId ? { taskId } : undefined)
}

export function getConsistencyTrend(days = 30) {
  return get<Record<string, unknown>>('/consistency/trend', { days })
}

export function getTasksConsistency() {
  return get<Record<string, unknown>[]>('/consistency/tasks-summary')
}

export function generateSnapshot() {
  return post<unknown>('/consistency/snapshot')
}

// P2.8: Pairwise 比较
export function runPairwise(taskId: number) {
  return post<number>(`/consistency/pairwise/task/${taskId}`)
}

export function getPairwiseResults(taskId: number) {
  return get<PairwiseComparison[]>(`/consistency/pairwise/task/${taskId}`)
}

export function getPairwiseRanking(taskId: number) {
  return get<PairwiseRanking[]>(`/consistency/pairwise/ranking/${taskId}`)
}
