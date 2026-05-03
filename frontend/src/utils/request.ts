import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'
import router from '@/router'
import type { ApiResponse } from '@/types'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // blob 响应直接返回，不走业务状态码校验
    if (response.config.responseType === 'blob') {
      return response
    }

    const { code, message } = response.data
    if (code === 0) {
      return response.data as unknown as AxiosResponse
    }

    // 认证错误
    if (code >= 40100 && code <= 40199) {
      removeToken()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(new Error(message))
    }

    // 权限错误
    if (code >= 40300 && code <= 40399) {
      ElMessage.error('权限不足')
      return Promise.reject(new Error(message))
    }

    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (error) => {
    if (error.response?.status === 401) {
      removeToken()
      router.push('/login')
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)

export default service

// 封装请求方法
export function get<T>(url: string, params?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.get(url, { params, ...config }) as unknown as Promise<ApiResponse<T>>
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.post(url, data, config) as unknown as Promise<ApiResponse<T>>
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.put(url, data, config) as unknown as Promise<ApiResponse<T>>
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.delete(url, config) as unknown as Promise<ApiResponse<T>>
}

export function upload<T>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    ...config,
  }) as unknown as Promise<ApiResponse<T>>
}
