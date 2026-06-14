import type { TaskStatus, ParseStatus, ScoreStatus, CheckStatus, FileType, AsyncTaskStatus } from './common'

// 实训任务
export interface TrainingTask {
  id: number
  courseId: number
  courseName?: string
  templateId: number
  templateName?: string
  title: string
  requirements: string
  startTime: string
  endTime: string
  allowResubmit: boolean
  allowedFileTypes?: FileType[]
  maxFileSize?: number
  status: TaskStatus
}

// 成果提交
export interface Submission {
  id: number
  taskId: number
  taskTitle?: string
  studentId: number
  studentName?: string
  submitTime: string
  version: number
  parseStatus: ParseStatus
  checkStatus?: CheckStatus
  scoreStatus: ScoreStatus
  totalScore?: number
  autoTotalScore?: number
  teacherComment?: string
  parseSummary?: string
  parseTopics?: string
  parseCompleteness?: string
  parseQuality?: string
  parseSuggestions?: string
  updatedAt?: string
}

// 文件
export interface FileInfo {
  id: number
  submissionId?: number
  originalName: string
  filePath: string
  fileType: string
  fileSize: number
  fileHash: string
  version: number
}

// 核查结果
export interface CheckResult {
  id: number
  submissionId: number
  checkType: string
  checkItem?: string
  result: 'PASS' | 'WARNING' | 'FAIL' | 'COMPLETED' | 'PARTIAL' | 'NOT_COMPLETED'
  description: string
  evidence?: string
  suggestion?: string
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH'
}

// 评分结果
export interface ScoreResult {
  id: number
  submissionId: number
  indicatorId?: number
  indicatorName?: string
  maxScore?: number
  autoScore?: number
  teacherScore?: number
  finalScore?: number
  reason?: string
  evidence?: string
  /** 多轮采样分数 JSON 数组，如 [85,82,88] */
  sampleScores?: string
  /** 结构化覆盖度分析 JSON */
  coverageDetails?: string
  /** 交叉模型（备用模型）评分 */
  crossModelScore?: number
  /** 主模型与交叉模型偏差（绝对值） */
  crossModelDivergence?: number
  /** 提交内容字数（冗长偏差修正用） */
  wordCount?: number
  /** 冗长偏差修正系数 */
  verbosityFactor?: number
}

// 结构化覆盖度分析项（解析 coverageDetails JSON）
export interface CoverageDetail {
  point: string
  status: 'covered' | 'partial' | 'missing'
  detail?: string
}

// 异步任务（与后端 AsyncTask 实体对齐）
export interface AsyncTask {
  id: number
  bizId: number
  taskType: string
  status: AsyncTaskStatus
  progress: number
  currentStep: string
  errorMessage?: string
  retryCount: number
  createdAt: string
  updatedAt: string
}

// 消息
export interface Message {
  id: number
  type: string
  title: string
  content: string
  isRead: boolean
  relatedId: number | null
  createdAt: string
}
