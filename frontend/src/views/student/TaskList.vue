<template>
  <div class="task-list">
    <section class="page-hero">
      <div>
        <div class="eyebrow">
          <el-icon><Document /></el-icon>
          <span>实训任务</span>
        </div>
        <h2>选择任务并提交成果</h2>
        <p>按课程筛选当前发布的实训任务，查看要求后上传对应成果文件。</p>
      </div>
      <div class="hero-stat">
        <strong>{{ pagination.total }}</strong>
        <span>进行中任务</span>
      </div>
    </section>

    <section class="list-panel">
      <div class="toolbar">
        <div>
          <h3>任务列表</h3>
          <p>{{ activeFilterText }}</p>
        </div>
        <el-select
          v-model="filter.courseId"
          placeholder="按课程筛选"
          clearable
          class="course-filter"
          @change="handleFilterChange"
        >
          <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </div>

      <el-table :data="tasks" v-loading="loading">
        <el-table-column label="任务名称" min-width="240">
          <template #default="{ row }">
            <div class="task-name">
              <strong>{{ row.title }}</strong>
              <span>{{ row.requirements || '查看详情了解任务要求' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属课程" width="160">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.courseName || '未关联课程' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="任务周期" width="210">
          <template #default="{ row }">
            <div class="time-range">
              <span>{{ formatDate(row.startTime) }}</span>
              <small>{{ formatDate(row.endTime) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="截止提醒" width="130">
          <template #default="{ row }">
            <span :class="['deadline-pill', getDeadlineTone(row.endTime)]">
              <el-icon><Clock /></el-icon>
              {{ getDaysLeft(row.endTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusType(row.status)" effect="light">
              {{ getTaskStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="ArrowRight" @click="router.push(`/student/tasks/${row.id}`)">详情</el-button>
            <el-button
              v-if="row.status === 'PUBLISHED'"
              text
              type="success"
              :icon="UploadFilled"
              @click="router.push(`/student/submit/${row.id}`)"
            >
              提交
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && tasks.length === 0" description="暂无可提交的实训任务" :image-size="96" />

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @change="loadTasks"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Clock, Document, UploadFilled } from '@element-plus/icons-vue'
import { getTaskList } from '@/api/task'
import { getCourseList } from '@/api/course'
import { getTaskStatusLabel, getTaskStatusType } from '@/utils/status'
import { formatDate } from '@/utils/date'
import type { TrainingTask, Course } from '@/types'

const router = useRouter()
const loading = ref(false)
const tasks = ref<TrainingTask[]>([])
const courses = ref<Course[]>([])
const filter = reactive({ courseId: undefined as number | undefined })
const pagination = reactive({ page: 1, size: 20, total: 0 })

const activeFilterText = computed(() => {
  if (!filter.courseId) return '当前显示全部进行中的实训任务。'
  const course = courses.value.find(item => item.id === filter.courseId)
  return course ? `当前筛选：${course.name}` : '当前显示筛选后的实训任务。'
})

function handleFilterChange() {
  pagination.page = 1
  loadTasks()
}

function getDaysLeft(value: string | null | undefined) {
  if (!value) return '-'
  const end = new Date(value)
  if (Number.isNaN(end.getTime())) return '-'

  const start = new Date()
  start.setHours(0, 0, 0, 0)
  end.setHours(0, 0, 0, 0)
  const diff = Math.ceil((end.getTime() - start.getTime()) / 86400000)

  if (diff < 0) return '已截止'
  if (diff === 0) return '今天截止'
  if (diff === 1) return '明天截止'
  return `${diff} 天后`
}

function getDeadlineTone(value: string | null | undefined) {
  if (!value) return 'normal'
  const end = new Date(value)
  if (Number.isNaN(end.getTime())) return 'normal'

  const start = new Date()
  start.setHours(0, 0, 0, 0)
  end.setHours(0, 0, 0, 0)
  const diff = Math.ceil((end.getTime() - start.getTime()) / 86400000)

  if (diff < 0) return 'overdue'
  if (diff <= 1) return 'urgent'
  if (diff <= 3) return 'soon'
  return 'normal'
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await getTaskList({ page: pagination.page, size: pagination.size, status: 'PUBLISHED', ...filter })
    tasks.value = res.data.items
    pagination.total = res.data.total
  } catch {
    // 请求错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

async function loadCourses() {
  try {
    const res = await getCourseList({ size: 100 })
    courses.value = res.data.items
  } catch {
    // 请求错误已由拦截器处理
  }
}

onMounted(() => {
  loadCourses()
  loadTasks()
})
</script>

<style lang="scss" scoped>
.task-list {
  max-width: 1280px;
  margin: 0 auto;
}

.page-hero,
.list-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
  padding: 28px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(242, 248, 255, 0.94)),
    url('@/assets/hero.png') right center / auto 100% no-repeat;

  .eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    color: #2563eb;
    font-size: 13px;
    font-weight: 600;
  }

  h2 {
    margin-bottom: 8px;
    color: #0f172a;
    font-size: 28px;
    font-weight: 700;
  }

  p {
    color: #52657a;
    font-size: 14px;
    line-height: 1.8;
  }
}

.hero-stat {
  display: flex;
  min-width: 132px;
  min-height: 96px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.84);

  strong {
    color: #0f172a;
    font-size: 34px;
    line-height: 1;
  }

  span {
    margin-top: 8px;
    color: #64748b;
    font-size: 13px;
  }
}

.list-panel {
  overflow: hidden;
}

.toolbar {
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

  .course-filter {
    width: 220px;
  }
}

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
  height: 68px;
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
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    display: -webkit-box;
    overflow: hidden;
    color: #64748b;
    font-size: 12px;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 1;
  }
}

.time-range {
  display: flex;
  flex-direction: column;
  gap: 5px;
  color: #1f2937;
  font-size: 13px;

  small {
    color: #64748b;
  }
}

.deadline-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;

  &.urgent,
  &.overdue {
    color: #dc2626;
  }

  &.soon {
    color: #d97706;
  }
}

.el-pagination {
  justify-content: flex-end;
  padding: 18px 24px 22px;
}

@media (max-width: 860px) {
  .page-hero,
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-hero {
    padding: 22px;
    background: #ffffff;
  }

  .toolbar .course-filter {
    width: 100%;
  }
}
</style>
