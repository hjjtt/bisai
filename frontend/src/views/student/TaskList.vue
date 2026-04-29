<template>
  <div class="task-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实训任务列表</span>
          <el-select v-model="filter.courseId" placeholder="按课程筛选" clearable style="width: 200px" @change="loadTasks">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
      </template>
      <el-table :data="tasks" stripe v-loading="loading">
        <el-table-column prop="title" label="任务名称" min-width="180" />
        <el-table-column prop="courseName" label="所属课程" width="150" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="截止时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag>{{ getTaskStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/student/tasks/${row.id}`)">查看详情</el-button>
            <el-button type="success" link @click="$router.push(`/student/submit/${row.id}`)" v-if="row.status === 'PUBLISHED'">
              提交成果
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @change="loadTasks"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getTaskList } from '@/api/task'
import { getCourseList } from '@/api/course'
import type { TrainingTask } from '@/types'

const loading = ref(false)
const tasks = ref<TrainingTask[]>([])
const courses = ref<any[]>([])
const filter = reactive({ courseId: undefined as number | undefined })
const pagination = reactive({ page: 1, size: 20, total: 0 })

function getTaskStatus(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '进行中', CLOSED: '已关闭', ARCHIVED: '已归档' }
  return map[status] || status
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await getTaskList({ page: pagination.page, size: pagination.size, status: 'PUBLISHED', ...filter })
    tasks.value = res.data.items
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadCourses() {
  try {
    const res = await getCourseList({ size: 100 })
    courses.value = res.data.items
  } catch { /* ignore */ }
}

onMounted(() => {
  loadCourses()
  loadTasks()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
