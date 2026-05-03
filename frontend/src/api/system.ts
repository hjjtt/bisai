import { get, put, post, del } from '@/utils/request'

// 系统配置
export function getSystemConfig() {
  return get<Record<string, any>>('/system/config')
}

export function updateSystemConfig(data: Record<string, any>) {
  return put('/system/config', data)
}

// 测试模型连通性
export function testModelConnection(data: { apiUrl: string; apiKey: string }) {
  return post<{ success: boolean; message: string }>('/system/test-model', data)
}

// 模型调用日志
export function getModelCallLogs(params?: any) {
  return get('/logs/model-call', params)
}

export function clearModelCallLogs() {
  return del('/logs/model-call')
}
