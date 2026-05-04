// 统一的状态映射工具函数

export function getParseStatusType(status: string): string {
  const map: Record<string, string> = { PENDING: 'info', PARSING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

export function getParseStatusLabel(status: string): string {
  const map: Record<string, string> = { PENDING: '待解析', PARSING: '解析中', SUCCESS: '已完成', FAILED: '失败' }
  return map[status] || status
}

export function getCheckStatusType(status?: string): string {
  const map: Record<string, string> = { NOT_CHECKED: 'info', CHECKING: 'warning', SUCCESS: 'success', CHECK_FAILED: 'danger' }
  return map[status || 'NOT_CHECKED'] || 'info'
}

export function getCheckStatusLabel(status?: string): string {
  const map: Record<string, string> = { NOT_CHECKED: '未核查', CHECKING: '核查中', SUCCESS: '已完成', CHECK_FAILED: '失败' }
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

export function getSubmitStatusType(status: string): string {
  const map: Record<string, string> = { '已提交': 'success', '未提交': 'info', '待提交': 'warning' }
  return map[status] || 'info'
}

export function getRoleLabel(role: string): string {
  const map: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
  return map[role] || role
}

export function getRoleType(role: string): string {
  const map: Record<string, string> = { STUDENT: 'info', TEACHER: 'success', ADMIN: 'warning' }
  return map[role] || 'info'
}

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

export function getResultType(result: string): string {
  const map: Record<string, string> = { PASS: 'success', WARNING: 'warning', FAIL: 'danger' }
  return map[result] || 'info'
}

export function getResultLabel(result: string): string {
  const map: Record<string, string> = { PASS: '通过', WARNING: '警告', FAIL: '不通过' }
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
  const map: Record<string, string> = { PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

export function getAsyncTaskStatusLabel(status: string): string {
  const map: Record<string, string> = { PENDING: '等待中', RUNNING: '处理中', SUCCESS: '已完成', FAILED: '失败' }
  return map[status] || status
}

export function getMessageTypeType(type: string): string {
  const map: Record<string, string> = {
    SUBMIT: 'success', RESUBMIT: 'warning', SCORE_COMPLETE: 'primary',
    SCORE_PUBLISH: 'success', BATCH_FAIL: 'danger', QUOTA_WARNING: 'warning',
  }
  return map[type] || 'info'
}

export function getMessageTypeLabel(type: string): string {
  const map: Record<string, string> = {
    SUBMIT: '提交通知', RESUBMIT: '重提通知', SCORE_COMPLETE: '评分完成',
    SCORE_PUBLISH: '成绩发布', BATCH_FAIL: '批量失败', QUOTA_WARNING: '配额警告',
  }
  return map[type] || type
}
