<template>
  <div class="student-home">
    <section class="hero-panel">
      <div class="hero-copy">
        <div class="eyebrow">
          <el-icon><DataBoard /></el-icon>
          <span>学生工作台</span>
        </div>
        <h2>你好，{{ displayName }}</h2>
        <p class="subtitle">今天可以先处理最近的实训任务，提交后系统会自动完成解析、核查与评分。</p>
        <div class="hero-actions">
          <el-button type="primary" :icon="Document" @click="router.push('/student/tasks')">查看实训任务</el-button>
          <el-button :icon="UploadFilled" @click="router.push('/student/submissions')">我的提交</el-button>
        </div>
      </div>

      <div class="focus-card">
        <div class="focus-header">
          <span>当前关注</span>
          <el-tag v-if="primaryTask" :type="getSubmitStatusType(primaryTask.submitStatus || '待提交')" size="small">
            {{ primaryTask.submitStatus || '待提交' }}
          </el-tag>
        </div>
        <template v-if="primaryTask">
          <h3>{{ primaryTask.title }}</h3>
          <p>{{ primaryTask.courseName || '未关联课程' }}</p>
          <div class="focus-meta">
            <span :class="['deadline-pill', getDeadlineTone(primaryTask.endTime)]">
              <el-icon><Clock /></el-icon>
              {{ getDaysLeft(primaryTask.endTime) }}
            </span>
            <span>{{ formatDateShort(primaryTask.endTime) }}</span>
          </div>
          <el-button type="primary" plain :icon="ArrowRight" @click="openTask(primaryTask)">
            {{ getTaskActionLabel(primaryTask) }}
          </el-button>
        </template>
        <template v-else>
          <div class="focus-empty">
            <el-icon><CircleCheck /></el-icon>
            <span>暂无待处理任务</span>
          </div>
        </template>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="item in statCards" :key="item.title" class="stat-card" :class="item.tone">
        <div class="stat-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <span class="stat-label">{{ item.title }}</span>
          <div class="stat-content" v-if="loading">
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
          <div class="stat-content" v-else>
            <strong>{{ item.value }}</strong>
            <span>项</span>
          </div>
        </div>
      </article>
    </section>

    <section class="dashboard-grid">
      <div class="task-panel">
        <div class="section-header">
          <div>
            <h3>近期实训任务</h3>
            <p>按截止时间关注提交状态与成绩反馈。</p>
          </div>
          <el-button type="primary" link :icon="ArrowRight" @click="router.push('/student/tasks')">全部任务</el-button>
        </div>

        <div class="task-table" v-loading="loading">
          <el-table :data="recentTasks" style="width: 100%" row-class-name="task-row">
            <el-table-column prop="title" label="任务名称" min-width="220">
              <template #default="{ row }">
                <div class="task-name">
                  <strong>{{ row.title }}</strong>
                  <span>{{ row.courseName || '未关联课程' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="截止日期" width="160">
              <template #default="{ row }">
                <div class="deadline-cell">
                  <span>{{ formatDateShort(row.endTime) }}</span>
                  <small :class="getDeadlineTone(row.endTime)">{{ getDaysLeft(row.endTime) }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="提交状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getSubmitStatusType(row.submitStatus || '待提交')" effect="light">
                  {{ row.submitStatus || '待提交' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="评价得分" width="110" align="right">
              <template #default="{ row }">
                <span class="score-text">{{ row.score || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="110" align="right">
              <template #default="{ row }">
                <el-button text type="primary" :icon="ArrowRight" @click="openTask(row)">
                  {{ getTaskActionLabel(row) }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!loading && recentTasks.length === 0" class="empty-state">
            <el-empty description="暂无进行中的实训任务" :image-size="96" />
          </div>
        </div>
      </div>

      <aside class="side-panel">
        <div class="date-card">
          <span class="day">{{ today.getDate() }}</span>
          <div>
            <strong>{{ today.getMonth() + 1 }} 月 {{ today.getFullYear() }}</strong>
            <span>{{ weekdayLabel }}</span>
          </div>
        </div>

        <div class="review-card">
          <div class="review-icon">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div>
            <h3>{{ feedbackTitle }}</h3>
            <p>{{ feedbackDescription }}</p>
          </div>
          <el-button :icon="Bell" @click="router.push('/student/messages')">查看消息</el-button>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Bell,
  ChatDotRound,
  CircleCheck,
  Clock,
  DataBoard,
  Document,
  DocumentChecked,
  Loading,
  Medal,
  UploadFilled,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getStudentStats } from '@/api/dashboard'
import { getSubmitStatusType, getRoleLabel } from '@/utils/status'
import { formatDateShort, getDaysLeft, getDeadlineTone } from '@/utils/date'
import type { TrainingTask } from '@/types'

type StudentTask = TrainingTask & {
  submitStatus?: string
  score?: number | string | null
}

interface StudentStats {
  ongoingTasks: number
  submittedCount: number
  pendingFeedback: number
  unreadMessages: number
  recentTasks: StudentTask[]
}

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const displayName = computed(() => userInfo.value?.realName || userInfo.value?.username || getRoleLabel(userStore.role || '') || '同学')

const today = new Date()
const weekdayLabel = new Intl.DateTimeFormat('zh-CN', { weekday: 'long' }).format(today)
const loading = ref(true)

const stats = ref({
  ongoingTasks: 0,
  submittedCount: 0,
  pendingFeedback: 0,
  unreadMessages: 0,
})

const recentTasks = ref<StudentTask[]>([])
const primaryTask = computed(() => recentTasks.value[0])

const statCards = computed(() => [
  { title: '进行中任务', value: stats.value.ongoingTasks, icon: markRaw(Document), tone: 'is-blue' },
  { title: '已提交成果', value: stats.value.submittedCount, icon: markRaw(DocumentChecked), tone: 'is-green' },
  { title: '待评价反馈', value: stats.value.pendingFeedback, icon: markRaw(Medal), tone: 'is-amber' },
  { title: '系统通知', value: stats.value.unreadMessages, icon: markRaw(Bell), tone: 'is-slate' },
])

const feedbackTitle = computed(() => {
  if (stats.value.pendingFeedback > 0) return `${stats.value.pendingFeedback} 项反馈待查看`
  if (stats.value.unreadMessages > 0) return `${stats.value.unreadMessages} 条系统消息`
  return '学习状态稳定'
})

const feedbackDescription = computed(() => {
  if (stats.value.pendingFeedback > 0) return '评分发布后可以在消息中心查看教师反馈。'
  if (stats.value.unreadMessages > 0) return '有新的任务或系统通知等待处理。'
  return '保持提交节奏，新的反馈会在这里提醒。'
})

function getTaskActionLabel(task: StudentTask) {
  return task.submitStatus === '已提交' ? '详情' : '提交'
}

function openTask(task: StudentTask) {
  if (task.submitStatus === '已提交') {
    router.push(`/student/tasks/${task.id}`)
    return
  }
  router.push(`/student/submit/${task.id}`)
}

async function loadStats() {
  loading.value = true
  try {
    const res = await getStudentStats()
    const d = res.data as StudentStats
    stats.value = {
      ongoingTasks: d.ongoingTasks || 0,
      submittedCount: d.submittedCount || 0,
      pendingFeedback: d.pendingFeedback || 0,
      unreadMessages: d.unreadMessages || 0,
    }
    recentTasks.value = d.recentTasks || []
  } catch {
    // 请求错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<style lang="scss" scoped>
.student-home {
  max-width: 1280px;
  margin: 0 auto;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 24px;
  margin-bottom: 24px;
  padding: 28px;
  border: 1px solid #dde7f0;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(244, 248, 251, 0.95)),
    url('@/assets/hero.png') right center / auto 100% no-repeat;
  box-shadow: 0 16px 40px rgba(30, 41, 59, 0.08);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;

  .eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    width: fit-content;
    margin-bottom: 14px;
    color: #2563eb;
    font-size: 13px;
    font-weight: 600;
  }

  h2 {
    margin-bottom: 10px;
    color: #0f172a;
    font-size: 32px;
    font-weight: 700;
    line-height: 1.25;
  }

  .subtitle {
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
  min-height: 220px;
  padding: 22px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(8px);

  .focus-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    color: #64748b;
    font-size: 13px;
    font-weight: 600;
  }

  h3 {
    margin-bottom: 8px;
    color: #111827;
    font-size: 18px;
    line-height: 1.45;
  }

  p {
    margin-bottom: 18px;
    color: #64748b;
    font-size: 13px;
  }

  .focus-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    margin-bottom: 18px;
    color: #64748b;
    font-size: 13px;
  }
}

.deadline-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #2563eb;
  font-weight: 600;

  &.urgent,
  &.overdue {
    color: #dc2626;
  }

  &.soon {
    color: #d97706;
  }
}

.focus-empty {
  display: flex;
  min-height: 142px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #64748b;
  font-weight: 600;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 112px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);

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

  .stat-body {
    min-width: 0;
  }

  .stat-label {
    display: block;
    margin-bottom: 8px;
    color: #64748b;
    font-size: 13px;
  }

  .stat-content {
    display: flex;
    align-items: baseline;
    gap: 5px;

    strong {
      color: #0f172a;
      font-size: 30px;
      font-weight: 700;
      line-height: 1;
    }

    span {
      color: #94a3b8;
      font-size: 12px;
    }
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

  &.is-slate .stat-icon {
    background: #edf2f7;
    color: #475569;
  }
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
}

.task-panel,
.side-panel > div {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.task-panel {
  min-width: 0;
  overflow: hidden;
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

.task-table {
  padding: 0 24px 18px;

  :deep(.el-table) {
    --el-table-border-color: #eef2f7;
    --el-table-header-bg-color: #ffffff;
    --el-table-row-hover-bg-color: #f8fbff;
  }

  :deep(.el-table__header th) {
    height: 54px;
    color: #6b7280;
    font-weight: 600;
  }

  :deep(.el-table__body td) {
    height: 66px;
  }
}

.task-name {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;

  strong {
    overflow: hidden;
    color: #111827;
    font-size: 14px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: #64748b;
    font-size: 12px;
  }
}

.deadline-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
  color: #1f2937;

  small {
    color: #64748b;

    &.urgent,
    &.overdue {
      color: #dc2626;
    }

    &.soon {
      color: #d97706;
    }
  }
}

.score-text {
  color: #2563eb;
  font-weight: 700;
}

.empty-state {
  padding: 42px 0;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.date-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px;

  .day {
    color: #0f172a;
    font-size: 42px;
    font-weight: 800;
    line-height: 1;
  }

  div {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding-left: 16px;
    border-left: 2px solid #2dd4bf;
  }

  strong {
    color: #1f2937;
    font-size: 14px;
  }

  span:last-child {
    color: #64748b;
    font-size: 12px;
  }
}

.review-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;

  .review-icon {
    display: flex;
    width: 44px;
    height: 44px;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: #ecfeff;
    color: #0891b2;
    font-size: 22px;
  }

  h3 {
    margin-bottom: 8px;
    color: #111827;
    font-size: 17px;
    font-weight: 700;
  }

  p {
    color: #64748b;
    font-size: 13px;
    line-height: 1.7;
  }
}

@media (max-width: 1180px) {
  .hero-panel,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .side-panel {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .student-home {
    max-width: none;
  }

  .hero-panel {
    padding: 22px;
    background: #ffffff;
  }

  .hero-copy h2 {
    font-size: 26px;
  }

  .stat-grid,
  .side-panel {
    grid-template-columns: 1fr;
  }

  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .task-table {
    padding: 0 14px 14px;
  }
}
</style>
