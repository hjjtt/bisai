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
    PENDING: '待解析', PARSING: '解析中', SUCCESS: '解析完成', FAILED: '解析失败',
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

// 第二个可选参数 parseStatus 用于上下文感知：
// 新流程下学生上传后只自动触发门禁+解析，核查/评分延后由教师触发。
// 当解析已完成但核查未触发（NOT_CHECKED），表达成「待教师核查」而非「未核查」，
// 避免给学生/教师"流程卡住"的误解。传 undefined 时保持原行为（向后兼容）。
export function getCheckStatusLabel(status?: string, parseStatus?: string): string {
  if (status === 'NOT_CHECKED' && parseStatus === 'SUCCESS') {
    return '待教师核查'
  }
  const map: Record<string, string> = {
    NOT_CHECKED: '未核查', CHECKING: '核查中', SUCCESS: '核查完成', CHECK_FAILED: '核查失败',
    RETRYING: '重试中', CANCELLED: '已取消',
  }
  return map[status || 'NOT_CHECKED'] || status || '未核查'
}

export function getScoreStatusType(status: string): string {
  const map: Record<string, string> = {
    NOT_SCORED: 'info', SCORING: 'warning', AI_SCORED: 'primary', TEACHER_CONFIRMED: 'success',
    PUBLISHED: 'success', SCORE_FAILED: 'danger', RETURNED: 'warning', CANCELLED: 'info',
  }
  return map[status] || 'info'
}

// 同 getCheckStatusLabel：解析完成后未评分表达成「待教师评分」，避免误解。
// 评分依赖核查完成，但核查状态前端多处不展示，故此处仅以 parseStatus 为上下文锚点，
// 教师侧展示更精确的「待评分」由 getScoreStatusLabel 的调用方按需处理。
export function getScoreStatusLabel(status: string, parseStatus?: string): string {
  if (status === 'NOT_SCORED' && parseStatus === 'SUCCESS') {
    return '待教师评分'
  }
  const map: Record<string, string> = {
    NOT_SCORED: '未评分', SCORING: '评分中', AI_SCORED: 'AI已评分', TEACHER_CONFIRMED: '教师已确认',
    PUBLISHED: '已发布', SCORE_FAILED: '评分失败', RETURNED: '已退回', CANCELLED: '已取消',
  }
  return map[status] || status
}

export function getTaskStatusLabel(status: string, endTime?: string | null): string {
  const map: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '进行中', CLOSED: '已关闭', ARCHIVED: '已归档' }
  // PUBLISHED 但已过截止时间 → 显示"已截止"
  if (status === 'PUBLISHED' && endTime && new Date(endTime).getTime() < Date.now()) {
    return '已截止'
  }
  return map[status] || status
}

export function getTaskStatusType(status: string, endTime?: string | null): string {
  const map: Record<string, string> = { DRAFT: 'info', PUBLISHED: 'success', CLOSED: 'warning', ARCHIVED: 'info' }
  if (status === 'PUBLISHED' && endTime && new Date(endTime).getTime() < Date.now()) {
    return 'danger'
  }
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
    case 'SUCCESS':
    case '已完成': return '已完成'
    case 'PROCESSING':
    case 'PARSING':
    case '解析中': return '解析中'
    case 'FAILED':
    case '失败': return '失败'
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
    SUBMISSION_RETURNED: 'danger',
    AI_CHECK_REDFLAG: 'danger',
    AI_SCORE_AGENT: 'success',
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
    SUBMISSION_RETURNED: '退回通知',
    AI_CHECK_REDFLAG: '红线熔断',
    AI_SCORE_AGENT: 'Agent评分',
  }
  return map[type] || type
}

// 启用/停用状态（账号、班级、课程、模板等通用）
export function getEnableStatusType(status?: string | null): string {
  return status === 'ENABLED' ? 'success' : 'danger'
}

export function getEnableStatusLabel(status?: string | null): string {
  return status === 'ENABLED' ? '启用' : '停用'
}

// 知识库向量化状态（补充 VECTORIZING，原 getKnowledgeStatus* 缺该分支）
export function getVectorStatusType(status: string): string {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'VECTORIZING':
    case 'PROCESSING':
    case 'PARSING': return 'warning'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}

export function getVectorStatusLabel(status: string): string {
  switch (status) {
    case 'SUCCESS': return '已完成'
    case 'VECTORIZING': return '向量化中'
    case 'PROCESSING':
    case 'PARSING': return '处理中'
    case 'FAILED': return '失败'
    case 'PENDING': return '等待中'
    default: return status
  }
}

// 异步任务类型标签
export function getTaskTypeLabel(type?: string | null): string {
  const map: Record<string, string> = {
    PRECHECK: '门禁校验',
    PARSE: '解析',
    CHECK: '核查',
    SCORE: '评分',
    // Agent 评分对学生展示同"评分"，避免暴露实现细节
    SCORE_AGENT: '评分',
  }
  return map[type || ''] || '处理'
}
