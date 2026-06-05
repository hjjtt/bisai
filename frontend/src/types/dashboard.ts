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
  // P2.9: 评分一致性看板
  consistency?: ConsistencyData
}

// 评分一致性趋势数据
export interface ConsistencyData {
  dates: string[]
  pearsonCorrelation: number[]
  mae: number[]
  avgDivergence: number[]
  latestPearson?: number
  latestSpearman?: number
  latestMae?: number
  latestRmse?: number
  totalEvaluated?: number
}

// 校准相关性统计
export interface CorrelationStats {
  sampleSize: number
  pearsonCorrelation: number
  spearmanCorrelation: number
  rmse: number
  mae: number
  avgDivergence: number
  biasDirection: string
  crossModelAgreement?: number
  indicatorAnalysis?: IndicatorCorrelation[]
}

export interface IndicatorCorrelation {
  indicatorId: number
  indicatorName?: string
  sampleSize: number
  pearsonCorrelation: number
  mae: number
}

// Pairwise 排名
export interface PairwiseRanking {
  rank: number
  submissionId: number
  studentName: string
  aiScore: number
  wins: number
  losses: number
  netWins: number
}

export interface PairwiseComparison {
  id: number
  submissionAId: number
  submissionBId: number
  winner: string
  reasoning: string
  studentAName: string
  studentBName: string
  scoreA: number
  scoreB: number
}
