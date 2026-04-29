<template>
  <div class="task-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>任务管理</span>
          <el-button type="primary" @click="$router.push('/teacher/tasks/create')">创建任务</el-button>
        </div>
      </template>

      <el-table :data="tasks" stripe v-loading="loading">
        <el-table-column prop="title" label="任务名称" min-width="180" />
        <el-table-column prop="courseName" label="所属课程" width="150" />
        <el-table-column prop="templateName" label="评价模板" width="150" />
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="endTime" label="截止时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/teacher/tasks/${row.id}/edit`)">编辑</el-button>
            <el-button type="success" link v-if="row.status === 'DRAFT'" @click="handlePublish(row.id)">发布</el-button>
            <el-button type="warning" link v-if="row.status === 'PUBLISHED'" @click="handleClose(row.id)">关闭</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTaskList, publishTask, closeTask } from '@/api/task'
import type { TrainingTask } from '@/types'

const loading = ref(false)
const tasks = ref<TrainingTask[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })

function getStatusLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已关闭', ARCHIVED: '已归档' }
  return map[status] || status
}

function getStatusType(status: string) {
  const map: Record<string, string> = { DRAFT: 'info', PUBLISHED: 'success', CLOSED: 'warning', ARCHIVED: '' }
  return map[status] || ''
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await getTaskList({ page: pagination.page, size: pagination.size })
    tasks.value = res.data.items
    pagination.total = res.data.total
  } finally {
    loading.value = false
  }
}

async function handlePublish(id: number) {
  await ElMessageBox.confirm('确定发布该任务？发布后学生可查看并提交。', '确认发布')
  await publishTask(id)
  ElMessage.success('发布成功')
  loadTasks()
}

async function handleClose(id: number) {
  await ElMessageBox.confirm('确定关闭该任务？关闭后学生不可继续提交。', '确认关闭')
  await closeTask(id)
  ElMessage.success('已关闭')
  loadTasks()
}

onMounted(loadTasks)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
