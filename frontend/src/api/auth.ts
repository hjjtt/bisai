import { post } from '@/utils/request'
import type { LoginRequest, LoginResponse } from '@/types'

export function login(data: LoginRequest) {
  return post<LoginResponse>('/auth/login', data)
}

export function logout() {
  return post('/auth/logout')
}

