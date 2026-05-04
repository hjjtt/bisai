// 班级
export interface ClassInfo {
  id: number
  name: string
  grade?: string
  major?: string
  status: string
}

// 课程
export interface Course {
  id: number
  name: string
  teacherId: number
  teacherName?: string
  classId: number
  className?: string
  status: string
}

// 评价模板
export interface EvaluationTemplate {
  id: number
  name: string
  description?: string
  totalScore: number
  creatorId: number
  status: string
  createdAt?: string
  updatedAt?: string
}

// 评价指标
export interface Indicator {
  id: number
  templateId: number
  parentId?: number
  name: string
  weight: number
  maxScore: number
  scoreRule?: string
  children?: Indicator[]
}
