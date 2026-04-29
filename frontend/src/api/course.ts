import { get, post, put, del } from '@/utils/request'
import type { ClassInfo, Course, EvaluationTemplate, Indicator, PageResponse, PageRequest } from '@/types'

// 班级
export function getClassList(params?: PageRequest & { keyword?: string }) {
  return get<PageResponse<ClassInfo>>('/classes', params)
}

export function createClass(data: Partial<ClassInfo>) {
  return post<ClassInfo>('/classes', data)
}

export function updateClass(id: number, data: Partial<ClassInfo>) {
  return put<ClassInfo>(`/classes/${id}`, data)
}

// 课程
export function getCourseList(params?: PageRequest & { keyword?: string }) {
  return get<PageResponse<Course>>('/courses', params)
}

export function createCourse(data: Partial<Course>) {
  return post<Course>('/courses', data)
}

export function updateCourse(id: number, data: Partial<Course>) {
  return put<Course>(`/courses/${id}`, data)
}

// 评价模板
export function getTemplateList(params?: PageRequest) {
  return get<PageResponse<EvaluationTemplate>>('/templates', params)
}

export function getTemplate(id: number) {
  return get<EvaluationTemplate & { indicators: Indicator[] }>(`/templates/${id}`)
}

export function createTemplate(data: Partial<EvaluationTemplate>) {
  return post<EvaluationTemplate>('/templates', data)
}

export function updateTemplate(id: number, data: Partial<EvaluationTemplate>) {
  return put<EvaluationTemplate>(`/templates/${id}`, data)
}
