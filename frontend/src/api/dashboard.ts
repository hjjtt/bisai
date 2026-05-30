import { get } from '@/utils/request'
import type { StudentStats, TeacherStats, AdminStats } from '@/types'

export function getStudentStats() {
  return get<StudentStats>('/dashboard/student')
}

export function getTeacherStats() {
  return get<TeacherStats>('/dashboard/teacher')
}

export function getAdminStats(days?: number) {
  return get<AdminStats>('/dashboard/admin', days ? { days } : undefined)
}
