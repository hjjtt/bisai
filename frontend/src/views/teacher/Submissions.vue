<template>
  <div class="submissions">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>提交管理</span>
          <div class="header-actions">
            <el-select v-model="filter.taskId" placeholder="按任务筛选" clearable style="width: 200px" @change="loadData">
              <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
            </el-select>
            <el-button type="primary" @click="handleBatchParse" :loading="batchLoading">批量解析</el-button>
            <el-button type="success" @click="handleBatchScore" :loading="batchLoading">批量评分</el-button>
          </div>
        </div>
      </template>

      <el-table :data="submissions" stripe v-loading="loading">
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="title" label="任务名称" min-width="180" />
        <el-table-column prop="version" label="版本" width="70" />
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column label="解析状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getParseStatusType(row.parseStatus)" size="small">{{ getParseStatusLabel(row.parseStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核查状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getCheckStatusType(row.checkStatus)" size="small">{{ getCheckStatusLabel(row.checkStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分状态" width="130">
          <template #default="{ row }">
            <el-tag :type="getScoreStatusType(row.scoreStatus)" size="small">{{ getScoreStatusLabel(row.scoreStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" />
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/teacher/submissions/${row.id}/preview`)">预览</el-button>
            <el-button type="info" link @click="handleParse(row.id)" :loading="aiLoading[row.id]">解析</el-button>
            <el-button type="success" link @click="handleCheck(row.id)" :loading="aiLoading[row.id]">核查</el-button>
            <el-button type="warning" link @click="$router.push(`/teacher/submissions/${row.id}/score`)">评分</el-button>
            <el-button type="danger" link @click="handleReturn(row.id)">退回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && submissions.length === 0" description="暂无提交数据" />

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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSubmissions, getTaskList, batchParse, batchScore, returnSubmission, startParse, startCheck } from '@/api/task'
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

function getParseStatusType(status: string) {
  const map: Record<string, string> = { PENDING: 'info', PARSING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}
function getParseStatusLabel(status: string) {
  const map: Record<string, string> = { PENDING: '待解析', PARSING: '解析中', SUCCESS: '已完成', FAILED: '失败' }
  return map[status] || status
}
function getCheckStatusType(status?: string) {
  const map: Record<string, string> = { NOT_CHECKED: 'info', CHECKING: 'warning', SUCCESS: 'success', CHECK_FAILED: 'danger' }
  return map[status || 'NOT_CHECKED'] || 'info'
}
function getCheckStatusLabel(status?: string) {
  const map: Record<string, string> = { NOT_CHECKED: '未核查', CHECKING: '核查中', SUCCESS: '已完成', CHECK_FAILED: '失败' }
  return map[status || 'NOT_CHECKED'] || status || '未核查'
}
function getScoreStatusType(status: string) {
  const map: Record<string, string> = {
    NOT_SCORED: 'info', SCORING: 'warning', AI_SCORED: '', TEACHER_CONFIRMED: 'success',
    PUBLISHED: 'success', SCORE_FAILED: 'danger', RETURNED: 'warning',
  }
  return map[status] || 'info'
}
function getScoreStatusLabel(status: string) {
  const map: Record<string, string> = {
    NOT_SCORED: '未评分', SCORING: '评分中', AI_SCORED: 'AI已评分', TEACHER_CONFIRMED: '教师已确认',
    PUBLISHED: '已发布', SCORE_FAILED: '评分失败', RETURNED: '已退回',
  }
  return map[status] || status
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSubmissions({ page: pagination.page, size: pagination.size, ...filter })
    submissions.value = res.data.items
    pagination.total = res.data.total
  } catch {
    ElMessage.error('加载提交列表失败')
  } finally {
    loading.value = false
  }
}

function hasRunningAiTask() {
  return submissions.value.some(item => item.parseStatus === 'PARSING' || item.checkStatus === 'CHECKING' || item.scoreStatus === 'SCORING')
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
  } catch {
    // 忽略
  }
}

async function handleParse(id: number) {
  aiLoading[id] = true
  try {
    await startParse(id)
    ElMessage.success('AI 解析任务已启动')
    await loadData()
    startPolling()
  } catch {
    ElMessage.error('AI 解析失败')
  } finally {
    if (!hasRunningAiTask()) aiLoading[id] = false
  }
}

async function handleCheck(id: number) {
  aiLoading[id] = true
  try {
    await startCheck(id)
    ElMessage.success('AI 核查任务已启动')
    await loadData()
    startPolling()
  } catch {
    ElMessage.error('AI 核查失败')
  } finally {
    if (!hasRunningAiTask()) aiLoading[id] = false
  }
}

async function handleBatchParse() {
  if (!filter.taskId) { ElMessage.warning('请先选择任务'); return }
  batchLoading.value = true
  try {
    await batchParse(filter.taskId)
    ElMessage.success('批量解析任务已启动')
    await loadData()
    startPolling()
  } catch {
    ElMessage.error('批量解析失败')
  } finally {
    if (!hasRunningAiTask()) batchLoading.value = false
  }
}

async function handleBatchScore() {
  if (!filter.taskId) { ElMessage.warning('请先选择任务'); return }
  batchLoading.value = true
  try {
    await batchScore(filter.taskId)
    ElMessage.success('批量评分任务已启动')
    await loadData()
    startPolling()
  } catch {
    ElMessage.error('批量评分失败')
  } finally {
    if (!hasRunningAiTask()) batchLoading.value = false
  }
}

async function handleReturn(id: number) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入退回原因', '退回提交', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '请输入退回原因',
    })
    await returnSubmission(id, reason)
    ElMessage.success('已退回')
    loadData()
  } catch {
    // 用户取消
  }
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
</style>
