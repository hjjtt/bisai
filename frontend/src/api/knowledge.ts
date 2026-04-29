import { get, del, post, upload } from '@/utils/request'
import type { PageResponse, PageRequest } from '@/types'

export interface KnowledgeDocument {
  id: number
  name: string
  knowledgeBaseId?: number
  courseName?: string
  parseStatus: string
  vectorStatus: string
  vectorized: boolean
  updateTime: string
}

export function getKnowledgeList(params?: PageRequest & { keyword?: string }) {
  return get<PageResponse<KnowledgeDocument>>('/knowledge', params)
}

export function uploadKnowledge(file: File, courseId?: number) {
  const formData = new FormData()
  formData.append('file', file)
  if (courseId !== undefined) {
    formData.append('courseId', String(courseId))
  }
  return upload<KnowledgeDocument>('/knowledge/upload', formData)
}

export function deleteKnowledge(id: number) {
  return del(`/knowledge/${id}`)
}
