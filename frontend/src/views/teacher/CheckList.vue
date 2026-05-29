<template>
  <div class="check-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实训成果核查</span>
          <div class="header-actions">
            <el-select v-model="filter.taskId" placeholder="按任务筛选" clearable style="width: 200px" @change="loadData">
              <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
            </el-select>
            <el-button type="success" @click="handleBatchCheck" :loading="batchLoading">批量核查</el-button>
          </div>
        </div>
      </template>

      <el-table :data="submissions" stripe v-loading="loading" @row-click="handleRowClick">
        <el-table-column prop="studentName" label="学生" min-width="100" align="center" />
        <el-table-column prop="taskTitle" label="任务名称" min-width="180" align="center" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" min-width="70" align="center" />
        <el-table-column label="提交时间" min-width="170" align="center">
          <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="核查状态" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getCheckStatusType(row.checkStatus)" size="small">{{ getCheckStatusLabel(row.checkStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click.stop="$router.push(`/teacher/submissions/${row.id}/check`)">核查详情</el-button>
            <el-button type="info" size="small" @click.stop="handleCheck(row.id)" :loading="aiLoading[row.id]">重新核查</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && submissions.length === 0" description="暂无待核查数据" />

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @change="loadData"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSubmissions, getTaskList, batchCheck, startCheck } from '@/api/task'
import { getCheckStatusType, getCheckStatusLabel } from '@/utils/status'
import { formatDate } from '@/utils/date'
import type { Submission, TrainingTask } from '@/types'

const router = useRouter()
const loading = ref(false)
const batchLoading = ref(false)
const aiLoading = reactive<Record<number, boolean>>({})
const polling = ref<number | null>(null)
const submissions = ref<Submission[]>([])
const tasks = ref<TrainingTask[]>([])
const filter = reactive({ taskId: undefined as number | undefined })
const pagination = reactive({ page: 1, size: 20, total: 0 })

async function loadData() {
  loading.value = true
  try {
    const res = await getSubmissions({ page: pagination.page, size: pagination.size, ...filter })
    submissions.value = res.data.items
    pagination.total = res.data.total
  } catch (e) {
    ElMessage.error('加载提交列表失败')
  } finally {
    loading.value = false
  }
}

function hasRunningAiTask() {
  return submissions.value.some(item => item.checkStatus === 'CHECKING')
}

function startPolling() {
  if (polling.value !== null) return
  polling.value = window.setInterval(async () => {
    await loadData()
    if (!hasRunningAiTask()) {
      stopPolling()
      batchLoading.value = false
      Object.keys(aiLoading).forEach(key => { aiLoading[Number(key)] = false })
    }
  }, 3000)
}

function stopPolling() {
  if (polling.value !== null) {
    window.clearInterval(polling.value)
    polling.value = null
  }
}

async function loadTasks() {
  try {
    const res = await getTaskList({ size: 100 })
    tasks.value = res.data.items
  } catch (e) {
  }
}

async function handleCheck(id: number) {
  aiLoading[id] = true
  try {
    await startCheck(id)
    ElMessage.success('AI 核查任务已启动')
    await loadData()
    startPolling()
  } catch (e) {
    ElMessage.error('AI 核查失败')
  } finally {
    if (!hasRunningAiTask()) aiLoading[id] = false
  }
}

async function handleBatchCheck() {
  if (!filter.taskId) { ElMessage.warning('请先选择任务'); return }
  batchLoading.value = true
  try {
    await batchCheck(filter.taskId)
    ElMessage.success('批量核查任务已启动')
    await loadData()
    startPolling()
  } catch (e) {
    ElMessage.error('批量核查失败')
  } finally {
    if (!hasRunningAiTask()) batchLoading.value = false
  }
}

function handleRowClick(row: Submission) {
  router.push(`/teacher/submissions/${row.id}/check`)
}

onMounted(() => {
  loadTasks()
  loadData().then(() => {
    if (hasRunningAiTask()) startPolling()
  })
})

onBeforeUnmount(stopPolling)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  gap: 12px;
}
.el-table__row {
  cursor: pointer;
}
</style>
