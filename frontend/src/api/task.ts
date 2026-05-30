import { get, post, put, upload } from '@/utils/request'
import service from '@/utils/request'
import type { TrainingTask, Submission, ScoreResult, CheckResult, FileInfo, PageResponse, PageRequest, AsyncTask } from '@/types'

// 创建/更新任务的请求参数
export interface TaskFormData {
  courseId?: number
  templateId?: number
  title?: string
  requirements?: string
  startTime?: string
  endTime?: string
  allowResubmit?: boolean
  allowedFileTypes?: string[]
  maxFileSize?: number
}

// 实训任务
export function getTaskList(params?: PageRequest & { courseId?: number; status?: string }) {
  return get<PageResponse<TrainingTask>>('/tasks', params)
}

export function getTask(id: number) {
  return get<TrainingTask>(`/tasks/${id}`)
}

export function createTask(data: TaskFormData) {
  return post<TrainingTask>('/tasks', data)
}

export function updateTask(id: number, data: TaskFormData) {
  return put<TrainingTask>(`/tasks/${id}`, data)
}

export function publishTask(id: number) {
  return put<void>(`/tasks/${id}/publish`)
}

export function closeTask(id: number) {
  return put<void>(`/tasks/${id}/close`)
}

// 成果提交
export function uploadFiles(taskId: number, formData: FormData) {
  return upload(`/submissions/${taskId}/files`, formData)
}

export function getSubmissions(params?: PageRequest & { taskId?: number; studentId?: number }) {
  return get<PageResponse<Submission>>('/submissions', params)
}

export function getSubmission(id: number) {
  return get<Submission>(`/submissions/${id}`)
}

// 文件预览
export function getFilePreview(fileId: number) {
  return service.get(`/files/${fileId}/preview`, { responseType: 'blob' })
}

export function getFileList(submissionId: number) {
  return get<FileInfo[]>(`/submissions/${submissionId}/files`)
}

// 智能解析
export function startParse(submissionId: number) {
  return post<void>(`/submissions/${submissionId}/parse`)
}

// 智能核查
export function startCheck(submissionId: number) {
  return post<void>(`/submissions/${submissionId}/check`)
}

export function getCheckResults(submissionId: number) {
  return get<CheckResult[]>(`/submissions/${submissionId}/check-results`)
}

// 智能评分
export function startScore(submissionId: number) {
  return post<void>(`/submissions/${submissionId}/score`)
}

export function getScoreResults(submissionId: number) {
  return get<ScoreResult[]>(`/submissions/${submissionId}/scores`)
}

// 教师复核
export function saveTeacherScores(submissionId: number, data: { scores: ScoreResult[]; comment?: string; expectedUpdatedAt?: string }) {
  return put<void>(`/submissions/${submissionId}/scores`, data)
}

export function publishScore(submissionId: number) {
  return put<void>(`/submissions/${submissionId}/publish`)
}

// 退回提交
export function returnSubmission(submissionId: number, reason: string) {
  return put<void>(`/submissions/${submissionId}/return`, { reason })
}

// 批量操作
export function batchParse(taskId: number) {
  return post<{ total: number; created: number; skipped: number }>(`/tasks/${taskId}/batch-parse`)
}

export function batchScore(taskId: number) {
  return post<{ total: number; created: number; skipped: number }>(`/tasks/${taskId}/batch-score`)
}

export function batchCheck(taskId: number) {
  return post<{ total: number; created: number; skipped: number }>(`/tasks/${taskId}/batch-check`)
}

export function getBatchProgress(taskId: number) {
  return get<{ total: number; parsed: number; checked: number; scored: number; parseFailed: number; checkFailed: number; scoreFailed: number; totalFailed: number; running: number }>(`/tasks/${taskId}/batch-progress`)
}

// 客观评分
export function getObjectiveScore(submissionId: number) {
  return get<{ scores: Record<string, number>; total: number }>(`/submissions/${submissionId}/objective-score`)
}

// 成绩修正
export function correctScore(submissionId: number, data: { indicatorId?: number; newScore: number; reason: string }) {
  return put<void>(`/submissions/${submissionId}/correct`, data)
}

// 异步任务
export function getAsyncTasksByBizId(bizId: number) {
  return get<AsyncTask[]>(`/async-tasks/biz/${bizId}`)
}

export function retryAsyncTask(taskId: number) {
  return post<void>(`/async-tasks/${taskId}/retry`)
}

export function cancelAsyncTask(taskId: number) {
  return post<void>(`/async-tasks/${taskId}/cancel`)
}

// 学生查看已发布成绩
export function getStudentScores(submissionId: number) {
  return get<ScoreResult[]>(`/submissions/${submissionId}/student-scores`)
}

// 评分校准
export interface CalibrationItem {
  indicatorId: number
  score: number
  reason: string
  advantages: string
  problems: string
  deductionBasis: string
}

export interface CalibrationSaveData {
  taskId: number
  submissionId: number
  items: CalibrationItem[]
}

export interface CalibrationRecord {
  id: number
  taskId: number
  submissionId: number
  indicatorId: number
  score: number
  reason: string
  advantages: string
  problems: string
  deductionBasis: string
  createdAt: string
}

export function saveCalibration(data: CalibrationSaveData) {
  return post<void>('/calibration', data)
}

export function getCalibrations(taskId: number) {
  return get<CalibrationRecord[]>(`/calibration/task/${taskId}`)
}

