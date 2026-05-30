// 仪表盘统计类型（与后端 DashboardStats 对应）

// 学生首页统计
export interface StudentStats {
  ongoingTasks: number
  submittedCount: number
  pendingFeedback: number
  unreadMessages: number
  recentTasks: Record<string, unknown>[]
}

// 教师首页统计
export interface TeacherStats {
  pendingScore: number
  pendingReview: number
  highRisk: number
  completed: number
  pendingReviews: Record<string, unknown>[]
  highRiskSubmissions: Record<string, unknown>[]
}

// 管理员首页统计
export interface AdminStats {
  userCount: number
  userTrend: number
  classCount: number
  classTrend: number
  courseCount: number
  taskCount: number
  taskTrend: number
  submissionCount: number
  todayError: number
  errorTrend: number
  recentLogs: Record<string, unknown>[]
  systemStatus: Record<string, unknown>[]
  apiUsage: number
  serverLoad: number
  dates: string[]
  submissions: number[]
  parsed: number[]
  scored: number[]
}
