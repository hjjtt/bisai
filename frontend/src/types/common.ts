// 统一响应结构
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页请求参数
export interface PageRequest {
  page?: number
  size?: number
  sort?: string
  order?: 'asc' | 'desc'
}

// 分页响应结构
export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  total: number
}

// 文件类型
export type FileType = 'DOC' | 'DOCX' | 'PDF' | 'JPG' | 'JPEG' | 'PNG' | 'XLS' | 'XLSX' | 'ZIP'

// 任务状态
export type TaskStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED'

// 解析状态
export type ParseStatus = 'PENDING' | 'PARSING' | 'SUCCESS' | 'FAILED'

// 评分状态
export type ScoreStatus = 'NOT_SCORED' | 'SCORING' | 'AI_SCORED' | 'TEACHER_CONFIRMED' | 'PUBLISHED' | 'SCORE_FAILED' | 'RETURNED'

// 核查状态
export type CheckStatus = 'NOT_CHECKED' | 'CHECKING' | 'SUCCESS' | 'CHECK_FAILED'

// 消息类型
export type MessageType = 'SUBMIT' | 'RESUBMIT' | 'SCORE_COMPLETE' | 'SCORE_PUBLISH' | 'BATCH_FAIL' | 'QUOTA_WARNING'

// 批量任务进度
export interface BatchProgress {
  total: number
  success: number
  failed: number
  running: number
}

// 仪表盘统计基础类型
export interface BaseStats {
  userCount?: number
  userTrend?: number
  classCount?: number
  classTrend?: number
  taskCount?: number
  taskTrend?: number
  todayError?: number
  errorTrend?: number
  apiUsage?: number
  serverLoad?: number
  systemStatus?: SystemStatusItem[]
  recentLogs?: LogEntry[]
  dates?: string[]
  submissions?: number[]
  parsed?: number[]
  scored?: number[]
}

export interface SystemStatusItem {
  name: string
  type: 'success' | 'warning' | 'danger' | 'info'
  text: string
}

export interface LogEntry {
  time: string
  user: string
  type: string
  content: string
  status?: string
}
