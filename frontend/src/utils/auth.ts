import type { UserInfo } from '@/types'

const TOKEN_KEY = 'bisai_token'
const USER_KEY = 'bisai_user'
const SIGNING_KEY = 'bisai-auth-signing-key-2024'

/**
 * 计算 payload 的简单签名，用于防篡改校验
 * 目的：防止普通用户通过浏览器控制台修改 localStorage 中的用户信息
 */
function computeSignature(payload: string): string {
  let hash = 0
  for (let i = 0; i < payload.length; i++) {
    const char = payload.charCodeAt(i)
    hash = ((hash << 5) - hash) + char + SIGNING_KEY.charCodeAt(i % SIGNING_KEY.length)
    hash = hash & hash
  }
  return hash.toString(36)
}

/**
 * 验证 localStorage 中用户数据的签名
 */
function verifySignature(payload: string, signature: string | null): boolean {
  if (!signature) return false
  return computeSignature(payload) === signature
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(USER_KEY + '_sig')
}

export function getUserRole(): string | null {
  const userStr = localStorage.getItem(USER_KEY)
  if (!userStr) return null
  const sig = localStorage.getItem(USER_KEY + '_sig')
  if (!verifySignature(userStr, sig)) return null
  try {
    const user: UserInfo = JSON.parse(userStr)
    return user.role
  } catch {
    return null
  }
}

export function setUserInfo(user: UserInfo): void {
  const payload = JSON.stringify(user)
  const signature = computeSignature(payload)
  localStorage.setItem(USER_KEY, payload)
  localStorage.setItem(USER_KEY + '_sig', signature)
}

export function getUserInfo(): UserInfo | null {
  const userStr = localStorage.getItem(USER_KEY)
  if (!userStr) return null
  const sig = localStorage.getItem(USER_KEY + '_sig')
  if (!verifySignature(userStr, sig)) return null
  try {
    return JSON.parse(userStr) as UserInfo
  } catch {
    return null
  }
}

export function clearAuth(): void {
  removeToken()
}
