import { get, post } from '@/utils/request'
import type { LoginRequest, LoginResponse } from '@/types'

export function login(data: LoginRequest) {
  return post<LoginResponse>('/auth/login', data)
}

export function getCaptcha() {
  return get<{ uuid: string; image: string }>('/auth/captcha')
}

export function logout() {
  return post('/auth/logout')
}

export function changePassword(oldPassword: string, newPassword: string) {
  return post('/auth/change-password', { oldPassword, newPassword })
}

