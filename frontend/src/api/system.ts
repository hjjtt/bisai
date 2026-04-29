import { get, put, post } from '@/utils/request'

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

// 日志 - 改为调用 /api/logs
export function getOperationLogs(params?: any) {
  return get('/logs/operation', params)
}

export function getModelCallLogs(params?: any) {
  return get('/logs/model-call', params)
}
