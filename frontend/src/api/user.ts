import { get, post, put, del } from '@/utils/request'
import type { UserInfo, PageResponse, PageRequest } from '@/types'

export function getUserList(params?: PageRequest & { role?: string; keyword?: string }) {
  return get<PageResponse<UserInfo>>('/users', params)
}

export function getUser(id: number) {
  return get<UserInfo>(`/users/${id}`)
}

export function createUser(data: Partial<UserInfo> & { password: string }) {
  return post<UserInfo>('/users', data)
}

export function updateUser(id: number, data: Partial<UserInfo>) {
  return put<UserInfo>(`/users/${id}`, data)
}

export function resetPassword(id: number) {
  return post(`/users/${id}/reset-password`)
}

export function toggleUserStatus(id: number, status: string) {
  return put(`/users/${id}/status`, { status })
}
