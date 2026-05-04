import { post } from '@/utils/request'

// 报表导出
export function exportStudentReport(submissionId: number, format: 'PDF' | 'WORD') {
  return post<{ fileId: number; fileName: string }>(`/reports/student/${submissionId}`, { format })
}

export function exportClassReport(taskId: number, format: 'PDF' | 'EXCEL') {
  return post<{ fileId: number; fileName: string }>(`/reports/class/${taskId}`, { format })
}

