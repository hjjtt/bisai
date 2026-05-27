<template>
  <div class="teacher-home">
    <section class="hero-panel">
      <div class="hero-copy">
        <div class="eyebrow">
          <el-icon><DataBoard /></el-icon>
          <span>教师工作台</span>
        </div>
        <h2>成果复核与风险处理</h2>
        <p>优先处理 AI 已评分成果和高风险核查项，把教师复核动作集中在最需要判断的地方。</p>
        <div class="hero-actions">
          <el-button type="primary" :icon="EditPen" @click="router.push('/teacher/submissions')">提交管理</el-button>
          <el-button :icon="Document" @click="router.push('/teacher/tasks')">任务管理</el-button>
          <el-button :icon="CircleCheck" @click="router.push('/teacher/check')">成果核查</el-button>
        </div>
      </div>

      <div class="focus-card" :class="{ 'is-risk': stats.highRisk > 0 }">
        <div class="focus-label">
          <span>当前优先级</span>
          <el-tag :type="stats.highRisk > 0 ? 'danger' : 'success'" effect="light">
            {{ stats.highRisk > 0 ? '需关注' : '运行平稳' }}
          </el-tag>
        </div>
        <strong>{{ focusTitle }}</strong>
        <p>{{ focusDescription }}</p>
        <el-button
          type="primary"
          plain
          :icon="ArrowRight"
          @click="router.push(stats.highRisk > 0 ? '/teacher/check' : '/teacher/submissions')"
        >
          {{ stats.highRisk > 0 ? '查看风险成果' : '查看待复核' }}
        </el-button>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="item in statCards" :key="item.title" class="stat-card" :class="item.tone">
        <div class="stat-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <span>{{ item.title }}</span>
          <strong v-if="loading"><el-icon class="is-loading"><Loading /></el-icon></strong>
          <strong v-else>{{ item.value }}</strong>
        </div>
      </article>
    </section>

    <section class="review-grid">
      <div class="work-panel" v-loading="loading">
        <div class="section-header">
          <div>
            <h3>待复核成果</h3>
            <p>AI 评分完成后，教师在这里完成最终确认。</p>
          </div>
          <el-button type="primary" link :icon="ArrowRight" @click="router.push('/teacher/submissions')">全部提交</el-button>
        </div>

        <el-table :data="pendingReviews">
          <el-table-column prop="studentName" label="学生" width="120" />
          <el-table-column label="任务名称" min-width="220">
            <template #default="{ row }">
              <div class="task-title">
                <strong>{{ getSubmissionTitle(row) }}</strong>
                <span>等待教师复核</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="170">
            <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="130" align="right">
            <template #default="{ row }">
              <el-button type="primary" text :icon="ArrowRight" @click="router.push(`/teacher/submissions/${row.id}/score`)">
                去复核
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && pendingReviews.length === 0" description="暂无待复核成果" :image-size="88" />
      </div>

      <div class="work-panel risk-panel" v-loading="loading">
        <div class="section-header">
          <div>
            <h3>高风险成果</h3>
            <p>核查异常、疑似不完整或风险较高的提交。</p>
          </div>
          <el-button type="danger" link :icon="Warning" @click="router.push('/teacher/check')">风险列表</el-button>
        </div>

        <el-table :data="highRiskSubmissions">
          <el-table-column prop="studentName" label="学生" width="120" />
          <el-table-column label="任务名称" min-width="190">
            <template #default="{ row }">
              <div class="task-title">
                <strong>{{ getSubmissionTitle(row) }}</strong>
                <span>{{ row.riskReason || '等待查看核查详情' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" align="right">
            <template #default="{ row }">
              <el-button type="danger" text :icon="ArrowRight" @click="router.push(`/teacher/submissions/${row.id}/check`)">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && highRiskSubmissions.length === 0" description="暂无高风险成果" :image-size="88" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  CircleCheck,
  DataBoard,
  Document,
  EditPen,
  Loading,
  Warning,
} from '@element-plus/icons-vue'
import { getTeacherStats } from '@/api/dashboard'
import type { Submission } from '@/types'
import { formatDate } from '@/utils/date'

type ReviewSubmission = Submission & {
  title?: string
  riskReason?: string
}

interface TeacherStats {
  pendingScore: number
  pendingReview: number
  highRisk: number
  completed: number
  pendingReviews: ReviewSubmission[]
  highRiskSubmissions: ReviewSubmission[]
}

const router = useRouter()
const loading = ref(true)
const stats = ref({
  pendingScore: 0,
  pendingReview: 0,
  highRisk: 0,
  completed: 0,
})

const pendingReviews = ref<ReviewSubmission[]>([])
const highRiskSubmissions = ref<ReviewSubmission[]>([])

const statCards = computed(() => [
  { title: '待评价', value: stats.value.pendingScore, icon: markRaw(EditPen), tone: 'is-amber' },
  { title: '待复核', value: stats.value.pendingReview, icon: markRaw(Document), tone: 'is-blue' },
  { title: '高风险成果', value: stats.value.highRisk, icon: markRaw(Warning), tone: 'is-red' },
  { title: '已完成', value: stats.value.completed, icon: markRaw(CircleCheck), tone: 'is-green' },
])

const focusTitle = computed(() => {
  if (stats.value.highRisk > 0) return `${stats.value.highRisk} 项高风险成果需要关注`
  if (stats.value.pendingReview > 0) return `${stats.value.pendingReview} 项成果等待复核`
  return '暂无紧急处理项'
})

const focusDescription = computed(() => {
  if (stats.value.highRisk > 0) return '建议先查看核查证据，再决定是否退回、复核或发布成绩。'
  if (stats.value.pendingReview > 0) return '可以按提交时间逐项确认评分，必要时补充教师评语。'
  return '当前班级成果处理节奏良好，可以继续维护任务与评价模板。'
})

function getSubmissionTitle(row: ReviewSubmission) {
  return row.taskTitle || row.title || '未命名任务'
}

async function loadStats() {
  loading.value = true
  try {
    const res = await getTeacherStats()
    const d = res.data as TeacherStats
    stats.value = {
      pendingScore: d.pendingScore || 0,
      pendingReview: d.pendingReview || 0,
      highRisk: d.highRisk || 0,
      completed: d.completed || 0,
    }
    pendingReviews.value = d.pendingReviews || []
    highRiskSubmissions.value = d.highRiskSubmissions || []
  } catch {
    // 请求错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style lang="scss" scoped>
.teacher-home {
  max-width: 1280px;
  margin: 0 auto;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 24px;
  margin-bottom: 24px;
}

.hero-copy,
.focus-card,
.stat-card,
.work-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.hero-copy {
  min-width: 0;
  padding: 30px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(242, 248, 255, 0.94)),
    url('@/assets/hero.png') right center / auto 100% no-repeat;

  .eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 14px;
    color: #2563eb;
    font-size: 13px;
    font-weight: 600;
  }

  h2 {
    margin-bottom: 10px;
    color: #0f172a;
    font-size: 30px;
    font-weight: 700;
  }

  p {
    max-width: 620px;
    color: #52657a;
    font-size: 15px;
    line-height: 1.8;
  }

  .hero-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 24px;
  }
}

.focus-card {
  display: flex;
  min-height: 220px;
  flex-direction: column;
  justify-content: space-between;
  padding: 24px;
  border-color: #d7e6f7;

  &.is-risk {
    border-color: #fecaca;
    background: #fffafa;
  }

  .focus-label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #64748b;
    font-size: 13px;
    font-weight: 600;
  }

  strong {
    display: block;
    color: #0f172a;
    font-size: 22px;
    line-height: 1.35;
  }

  p {
    color: #64748b;
    font-size: 13px;
    line-height: 1.7;
  }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  min-height: 112px;
  align-items: center;
  gap: 16px;
  padding: 20px;

  .stat-icon {
    display: flex;
    width: 44px;
    height: 44px;
    flex: 0 0 44px;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    font-size: 22px;
  }

  span {
    display: block;
    margin-bottom: 8px;
    color: #64748b;
    font-size: 13px;
  }

  strong {
    color: #0f172a;
    font-size: 30px;
    font-weight: 700;
    line-height: 1;
  }

  &.is-blue .stat-icon {
    background: #eaf2ff;
    color: #2563eb;
  }

  &.is-green .stat-icon {
    background: #e8f7ef;
    color: #059669;
  }

  &.is-amber .stat-icon {
    background: #fff4df;
    color: #d97706;
  }

  &.is-red .stat-icon {
    background: #fff1f2;
    color: #dc2626;
  }
}

.review-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.78fr);
  gap: 24px;
}

.work-panel {
  min-width: 0;
  overflow: hidden;

  :deep(.el-table) {
    --el-table-border-color: #eef2f7;
    --el-table-header-bg-color: #ffffff;
    --el-table-row-hover-bg-color: #f8fbff;
  }

  :deep(.el-table__header th) {
    height: 52px;
    color: #6b7280;
    font-weight: 600;
  }
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid #eef2f7;

  h3 {
    margin-bottom: 6px;
    color: #0f172a;
    font-size: 18px;
    font-weight: 700;
  }

  p {
    color: #64748b;
    font-size: 13px;
  }
}

.task-title {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;

  strong {
    overflow: hidden;
    color: #111827;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    overflow: hidden;
    color: #64748b;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.risk-panel {
  border-color: #fee2e2;
}

@media (max-width: 1180px) {
  .hero-panel,
  .review-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .hero-copy {
    padding: 22px;
    background: #ffffff;

    h2 {
      font-size: 26px;
    }
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
