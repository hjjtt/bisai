import { get, put } from '@/utils/request'
import type { Message, PageResponse, PageRequest } from '@/types'

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
