// 用户角色
export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN'

// 用户状态
export type UserStatus = 'ENABLED' | 'DISABLED'

// 用户信息
export interface UserInfo {
  id: number
  username: string
  role: UserRole
  realName: string
  classId?: number
  status: UserStatus
}

// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 登录响应
export interface LoginResponse {
  token: string
  user: UserInfo
}
