// 统一的状态映射工具函数

export function getParseStatusType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'info', PARSING: 'warning', SUCCESS: 'success', FAILED: 'danger',
    RETRYING: 'warning', CANCELLED: 'info',
  }
  return map[status] || 'info'
}

export function getParseStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待解析', PARSING: '解析中', SUCCESS: '已完成', FAILED: '失败',
    RETRYING: '重试中', CANCELLED: '已取消',
  }
  return map[status] || status
}

export function getCheckStatusType(status?: string): string {
  const map: Record<string, string> = {
    NOT_CHECKED: 'info', CHECKING: 'warning', SUCCESS: 'success', CHECK_FAILED: 'danger',
    RETRYING: 'warning', CANCELLED: 'info',
  }
  return map[status || 'NOT_CHECKED'] || 'info'
}

export function getCheckStatusLabel(status?: string): string {
  const map: Record<string, string> = {
    NOT_CHECKED: '未核查', CHECKING: '核查中', SUCCESS: '已完成', CHECK_FAILED: '失败',
    RETRYING: '重试中', CANCELLED: '已取消',
  }
  return map[status || 'NOT_CHECKED'] || status || '未核查'
}

export function getScoreStatusType(status: string): string {
  const map: Record<string, string> = {
    NOT_SCORED: 'info', SCORING: 'warning', AI_SCORED: 'primary', TEACHER_CONFIRMED: 'success',
    PUBLISHED: 'success', SCORE_FAILED: 'danger', RETURNED: 'warning',
  }
  return map[status] || 'info'
}

export function getScoreStatusLabel(status: string): string {
  const map: Record<string, string> = {
    NOT_SCORED: '未评分', SCORING: '评分中', AI_SCORED: 'AI已评分', TEACHER_CONFIRMED: '教师已确认',
    PUBLISHED: '已发布', SCORE_FAILED: '评分失败', RETURNED: '已退回',
  }
  return map[status] || status
}

export function getTaskStatusLabel(status: string): string {
  const map: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '进行中', CLOSED: '已关闭', ARCHIVED: '已归档' }
  return map[status] || status
}

export function getTaskStatusType(status: string): string {
  const map: Record<string, string> = { DRAFT: 'info', PUBLISHED: 'success', CLOSED: 'warning', ARCHIVED: 'info' }
  return map[status] || 'info'
}

export function getSubmitStatusType(status: string): string {
  const map: Record<string, string> = { '已提交': 'success', '未提交': 'info', '待提交': 'warning' }
  return map[status] || 'info'
}

export function getRoleLabel(role?: string | null): string {
  if (!role) return ''
  const map: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
  return map[role] || role
}

export function getRoleType(role?: string | null): string {
  if (!role) return 'info'
  const map: Record<string, string> = { STUDENT: 'info', TEACHER: 'success', ADMIN: 'warning' }
  return map[role] || 'info'
}

export const ROLE_OPTIONS = [
  { label: '学生', value: 'STUDENT' },
  { label: '教师', value: 'TEACHER' },
  { label: '管理员', value: 'ADMIN' }
]

export function getKnowledgeStatusType(status: string): string {
  switch (status) {
    case 'SUCCESS':
    case '已完成': return 'success'
    case 'PROCESSING':
    case 'PARSING':
    case '解析中': return 'warning'
    case 'FAILED':
    case '失败': return 'danger'
    default: return 'info'
  }
}

export function getKnowledgeStatusLabel(status: string): string {
  switch (status) {
    case 'SUCCESS': return '已完成'
    case 'PROCESSING':
    case 'PARSING': return '解析中'
    case 'FAILED': return '失败'
    case 'PENDING': return '等待中'
    default: return status
  }
}

export function getResultType(result: string): string {
  const map: Record<string, string> = {
    PASS: 'success', WARNING: 'warning', FAIL: 'danger',
    COMPLETED: 'success', PARTIAL: 'warning', NOT_COMPLETED: 'danger',
  }
  return map[result] || 'info'
}

export function getResultLabel(result: string): string {
  const map: Record<string, string> = {
    PASS: '通过', WARNING: '警告', FAIL: '不通过',
    COMPLETED: '已完成', PARTIAL: '部分完成', NOT_COMPLETED: '未完成',
  }
  return map[result] || result
}

export function getRiskType(level: string): string {
  const map: Record<string, string> = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }
  return map[level] || 'info'
}

export function getRiskLabel(level: string): string {
  const map: Record<string, string> = { LOW: '低', MEDIUM: '中', HIGH: '高' }
  return map[level] || level
}

export function getAsyncTaskStatusType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger',
    RETRYING: 'warning', CANCELLED: 'info',
  }
  return map[status] || 'info'
}

export function getAsyncTaskStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '等待中', RUNNING: '处理中', SUCCESS: '已完成', FAILED: '失败',
    RETRYING: '重试中', CANCELLED: '已取消',
  }
  return map[status] || status
}

export function getMessageTypeType(type: string): string {
  const map: Record<string, string> = {
    SUBMISSION: 'info',
    AI_PARSE: 'primary',
    AI_CHECK: 'warning',
    AI_SCORE: 'success',
    SCORE_PUBLISHED: 'success',
    SCORE_CORRECTED: 'warning',
  }
  return map[type] || 'info'
}

export function getMessageTypeLabel(type: string): string {
  const map: Record<string, string> = {
    SUBMISSION: '提交通知',
    AI_PARSE: '解析完成',
    AI_CHECK: '核查完成',
    AI_SCORE: '评分完成',
    SCORE_PUBLISHED: '成绩发布',
    SCORE_CORRECTED: '成绩修正',
  }
  return map[type] || type
}
