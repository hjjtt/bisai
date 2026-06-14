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
export type ParseStatus = 'PENDING' | 'PARSING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'

// 评分状态
export type ScoreStatus = 'NOT_SCORED' | 'SCORING' | 'AI_SCORED' | 'TEACHER_CONFIRMED' | 'PUBLISHED' | 'SCORE_FAILED' | 'RETURNED' | 'CANCELLED'

// 核查状态
export type CheckStatus = 'NOT_CHECKED' | 'CHECKING' | 'SUCCESS' | 'CHECK_FAILED' | 'CANCELLED'

// 异步任务状态
export type AsyncTaskStatus = 'PENDING' | 'RUNNING' | 'RETRYING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'

// 消息类型（与后端 Message.type 一致，参考 utils/status.ts 中的映射）
export type MessageType = 'SUBMISSION' | 'AI_PARSE' | 'AI_CHECK' | 'AI_SCORE' | 'SCORE_PUBLISHED' | 'SCORE_CORRECTED' | 'SUBMISSION_RETURNED' | 'AI_CHECK_REDFLAG' | 'AI_SCORE_AGENT'

// 批量任务进度（与后端 TaskService.getBatchProgress 返回结构对齐）
export interface BatchProgress {
  total: number
  /** 已解析完成的提交数 */
  parsed: number
  /** 已核查完成的提交数 */
  checked: number
  /** 已评分完成的提交数 */
  scored: number
  /** 解析失败数 */
  parseFailed: number
  /** 核查失败数 */
  checkFailed: number
  /** 评分失败数 */
  scoreFailed: number
  /** 三阶段失败总和（后端返回，便于展示） */
  totalFailed: number
  /** 执行中数量 */
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
