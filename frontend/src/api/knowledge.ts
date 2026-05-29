import { get, del, put, upload } from '@/utils/request'
import type { PageResponse, PageRequest } from '@/types'

export interface KnowledgeDocument {
  id: number
  name: string
  knowledgeBaseId?: number
  taskId?: number
  taskName?: string
  courseName?: string
  parseStatus: string
  vectorStatus: string
  vectorized: boolean
  enabled: boolean
  updateTime: string
}

export function getKnowledgeList(params?: PageRequest & { keyword?: string }) {
  return get<PageResponse<KnowledgeDocument>>('/knowledge', params)
}

export function uploadKnowledge(file: File, options?: { courseId?: number; taskId?: number }) {
  const formData = new FormData()
  formData.append('file', file)
  if (options?.courseId !== undefined) {
    formData.append('courseId', String(options.courseId))
  }
  if (options?.taskId !== undefined) {
    formData.append('taskId', String(options.taskId))
  }
  return upload<KnowledgeDocument>('/knowledge/upload', formData)
}

export function deleteKnowledge(id: number) {
  return del<void>(`/knowledge/${id}`)
}

export function toggleKnowledgeStatus(id: number, enabled: boolean) {
  return put(`/knowledge/${id}/toggle`, { enabled })
}
