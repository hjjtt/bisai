import { post, get, put } from '@/utils/request'
import request from '@/utils/request'
import type { Message, PageResponse, PageRequest } from '@/types'

// 报表导出 - 直接下载文件
export function exportStudentReport(submissionId: number, format: 'PDF' | 'WORD') {
  return request.post(`/reports/student/${submissionId}`, { format }, {
    responseType: 'blob'
  })
}

export function exportClassReport(taskId: number, format: 'PDF' | 'EXCEL') {
  return request.post(`/reports/class/${taskId}`, { format }, {
    responseType: 'blob'
  })
}

// 消息
export function getMessages(params?: PageRequest & { type?: string; isRead?: boolean }) {
  return get<PageResponse<Message>>('/messages', params)
}

export function markMessageRead(id: number) {
  return put(`/messages/${id}/read`)
}

export function markAllMessagesRead() {
  return put('/messages/read-all')
}

export function getUnreadCount() {
  return get<number>('/messages/unread-count')
}
