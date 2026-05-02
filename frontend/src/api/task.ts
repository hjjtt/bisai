import { get, post, put, upload } from '@/utils/request'
import type { TrainingTask, Submission, ScoreResult, CheckResult, FileInfo, PageResponse, PageRequest } from '@/types'

// 实训任务
export function getTaskList(params?: PageRequest & { courseId?: number; status?: string }) {
  return get<PageResponse<TrainingTask>>('/tasks', params)
}

export function getTask(id: number) {
  return get<TrainingTask>(`/tasks/${id}`)
}

export function createTask(data: Partial<TrainingTask>) {
  return post<TrainingTask>('/tasks', data)
}

export function updateTask(id: number, data: Partial<TrainingTask>) {
  return put<TrainingTask>(`/tasks/${id}`, data)
}

export function publishTask(id: number) {
  return put(`/tasks/${id}/publish`)
}

export function closeTask(id: number) {
  return put(`/tasks/${id}/close`)
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
export function getFilePreviewUrl(fileId: number) {
  return `/api/files/${fileId}/preview`
}

export function getFileList(submissionId: number) {
  return get<FileInfo[]>(`/submissions/${submissionId}/files`)
}

// 智能解析
export function startParse(submissionId: number) {
  return post(`/submissions/${submissionId}/parse`)
}

// 智能核查
export function startCheck(submissionId: number) {
  return post(`/submissions/${submissionId}/check`)
}

export function getCheckResults(submissionId: number) {
  return get<CheckResult[]>(`/submissions/${submissionId}/check-results`)
}

// 智能评分
export function startScore(submissionId: number) {
  return post(`/submissions/${submissionId}/score`)
}

export function getScoreResults(submissionId: number) {
  return get<ScoreResult[]>(`/submissions/${submissionId}/scores`)
}

// 教师复核
export function saveTeacherScores(submissionId: number, data: { scores: ScoreResult[]; comment?: string }) {
  return put(`/submissions/${submissionId}/scores`, data)
}

export function publishScore(submissionId: number) {
  return put(`/submissions/${submissionId}/publish`)
}

// 退回提交
export function returnSubmission(submissionId: number, reason: string) {
  return put(`/submissions/${submissionId}/return`, { reason })
}

// 批量操作
export function batchParse(taskId: number) {
  return post(`/tasks/${taskId}/batch-parse`)
}

export function batchScore(taskId: number) {
  return post(`/tasks/${taskId}/batch-score`)
}

export function getBatchProgress(taskId: number) {
  return get<{ total: number; success: number; failed: number; running: number }>(`/tasks/${taskId}/batch-progress`)
}

// 客观评分
export function getObjectiveScore(submissionId: number) {
  return get<{ scores: Record<string, number>; total: number }>(`/submissions/${submissionId}/objective-score`)
}

// 成绩修正
export function correctScore(submissionId: number, data: { indicatorId?: number; newScore: number; reason: string }) {
  return put(`/submissions/${submissionId}/correct`, data)
}

// 下载报告
export function downloadReport(fileName: string) {
  return `/api/reports/download/report/${fileName}`
}
