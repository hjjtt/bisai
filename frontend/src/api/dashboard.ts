import { get } from '@/utils/request'

export function getStudentStats() {
  return get('/dashboard/student')
}

export function getTeacherStats() {
  return get('/dashboard/teacher')
}

export function getAdminStats() {
  return get('/dashboard/admin')
}
