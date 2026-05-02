import type { UserInfo } from '@/types'

const TOKEN_KEY = 'bisai_token'
const USER_KEY = 'bisai_user'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getUserRole(): string | null {
  const userStr = localStorage.getItem(USER_KEY)
  if (!userStr) return null
  try {
    const user: UserInfo = JSON.parse(userStr)
    return user.role
  } catch {
    return null
  }
}

export function setUserInfo(user: UserInfo): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function getUserInfo(): UserInfo | null {
  const userStr = localStorage.getItem(USER_KEY)
  if (!userStr) return null
  try {
    return JSON.parse(userStr) as UserInfo
  } catch {
    return null
  }
}

export function clearAuth(): void {
  removeToken()
}
