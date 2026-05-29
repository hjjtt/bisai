import { get, put, post, del } from '@/utils/request'
import service from '@/utils/request'

export interface SystemConfigMap {
  'ai.api-key'?: string
  'ai.chat-model'?: string
  'ai.embedding-model'?: string
  'ai.rerank-model'?: string
  'ai.api-url'?: string
  'ai.max-tokens'?: string
  'ai.temperature'?: string
  'ai.timeout'?: string
  'ai.daily-token-limit'?: string
  'ai.daily-call-limit'?: string
  textModelApiUrl?: string
  textModelApiKey?: string
  model?: string
  maxTokens?: string
  temperature?: string
  timeout?: string
  [key: string]: string | undefined
}

// 系统配置
export function getSystemConfig() {
  return get<SystemConfigMap>('/system/config')
}

export function updateSystemConfig(data: Partial<SystemConfigMap>) {
  return put('/system/config', data)
}

// 测试模型连通性
export function testModelConnection(data: { apiUrl: string; apiKey: string; model?: string }) {
  return post<{ success: boolean; message: string }>('/system/test-model', data)
}

// 模型调用日志
export function getModelCallLogs(params?: { page?: number; size?: number }) {
  return get('/logs/model-call', params)
}

export function clearModelCallLogs() {
  return del('/logs/model-call')
}

// 文件下载（带认证）
export async function downloadFile(fileId: number): Promise<string> {
  const res = await service.get(`/files/${fileId}/download`, { responseType: 'blob' })
  const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
  // 检查是否返回了错误 JSON（如 403 被拦截）
  if (blob.type && blob.type.includes('application/json')) {
    const text = await blob.text()
    const json = JSON.parse(text)
    throw new Error(json.message || '下载失败')
  }
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const disposition = res.headers?.['content-disposition']
  let filename = `file_${fileId}`
  if (disposition) {
    const match = disposition.match(/filename\*?=(?:UTF-8'')?["']?([^;"'\n]+)/i)
    if (match) filename = decodeURIComponent(match[1])
  }
  a.download = filename
  a.click()
  window.URL.revokeObjectURL(url)
  return filename
}
