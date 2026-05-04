<template>
  <div class="batch-progress">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>批量任务进度</span>
          <div class="header-actions">
            <el-select v-model="selectedTaskId" placeholder="选择任务" @change="onTaskChange" style="width: 200px">
              <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
            </el-select>
            <el-button type="primary" :disabled="!selectedTaskId" :loading="parseLoading" @click="handleBatchParse">
              批量解析
            </el-button>
            <el-button type="success" :disabled="!selectedTaskId" :loading="scoreLoading" @click="handleBatchScore">
              批量评分
            </el-button>
            <el-button :icon="Refresh" @click="refreshData">刷新</el-button>
            <span class="polling-indicator">
              <el-icon v-if="isPolling" class="rotating"><Loading /></el-icon>
              {{ isPolling ? '自动刷新中...' : '已停止自动刷新' }}
            </span>
          </div>
        </div>
      </template>

      <el-row :gutter="20" v-if="progress">
        <el-col :span="6">
          <el-statistic title="总数" :value="progress.total" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="成功" :value="progress.success" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="失败" :value="progress.failed" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="执行中" :value="progress.running" />
        </el-col>
      </el-row>

      <el-progress
        v-if="progress"
        :percentage="Math.round(((progress.success + progress.failed) / progress.total) * 100)"
        style="margin-top: 20px"
      />

      <!-- 提交列表 -->
      <div v-if="progress && selectedTaskId" style="margin-top: 24px">
        <el-table
          :data="submissions"
          :loading="listLoading"
          stripe
          border
          style="width: 100%"
        >
          <el-table-column prop="studentName" label="学生姓名" min-width="120" />
          <el-table-column prop="submitTime" label="提交时间" min-width="160">
            <template #default="{ row }">
              {{ formatDate(row.submitTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="parseStatus" label="解析状态" min-width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getParseStatusType(row.parseStatus)" size="small">
                {{ getParseStatusLabel(row.parseStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="scoreStatus" label="评分状态" min-width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getScoreStatusType(row.scoreStatus)" size="small">
                {{ getScoreStatusLabel(row.scoreStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalScore" label="总分" min-width="80" align="center">
            <template #default="{ row }">
              <span v-if="row.totalScore != null" style="font-weight: 600; color: #409eff">
                {{ row.totalScore }}
              </span>
              <span v-else style="color: #c0c4cc">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.parseStatus === 'FAILED'"
                type="warning"
                size="small"
                :loading="actionLoading.get(row.id)"
                @click="retryParse(row.id)"
              >
                重试解析
              </el-button>
              <el-button
                v-if="row.scoreStatus === 'SCORE_FAILED'"
                type="warning"
                size="small"
                :loading="actionLoading.get(row.id)"
                @click="retryScore(row.id)"
              >
                重试评分
              </el-button>
              <el-button
                v-if="row.parseStatus === 'PARSING' || row.scoreStatus === 'SCORING'"
                type="danger"
                size="small"
                :loading="actionLoading.get(row.id)"
                @click="cancelTask(row.id)"
              >
                取消
              </el-button>
              <el-tag v-if="row.parseStatus === 'SUCCESS' && row.scoreStatus === 'AI_SCORED'" type="success" size="small">
                已完成
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!listLoading && submissions.length === 0" description="暂无提交" :image-size="80" />
      </div>

      <el-empty v-else-if="!progress" description="请选择任务查看进度" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Refresh } from '@element-plus/icons-vue'
import { getTaskList, getBatchProgress, batchParse, batchScore, getSubmissions, startParse, startScore, getAsyncTasksByBizId, cancelAsyncTask } from '@/api/task'
import type { TrainingTask, BatchProgress, Submission, AsyncTask } from '@/types'
import { getParseStatusType, getParseStatusLabel, getScoreStatusType, getScoreStatusLabel } from '@/utils/status'
import { formatDate } from '@/utils/date'

const tasks = ref<TrainingTask[]>([])
const selectedTaskId = ref<number>()
const progress = ref<BatchProgress | null>(null)
const submissions = ref<Submission[]>([])
const listLoading = ref(false)
const parseLoading = ref(false)
const scoreLoading = ref(false)
const actionLoading = ref<Map<number, boolean>>(new Map())

// 轮询相关
const polling = ref<number | null>(null)
const isPolling = ref(false)

async function loadTasks() {
  try {
    const res = await getTaskList({ size: 100 })
    tasks.value = res.data.items
  } catch (e) {
    console.error('加载任务列表失败:', e)
  }
}

async function onTaskChange() {
  submissions.value = []
  await loadProgress()
}

async function loadProgress() {
  if (!selectedTaskId.value) {
    progress.value = null
    submissions.value = []
    return
  }
  listLoading.value = true
  try {
    const [progressRes, submissionsRes] = await Promise.all([
      getBatchProgress(selectedTaskId.value),
      getSubmissions({ taskId: selectedTaskId.value, size: 100 }),
    ])
    progress.value = progressRes.data
    submissions.value = submissionsRes.data.items || []
  } catch (e) {
    console.error('加载进度失败:', e)
    ElMessage.error('加载进度失败')
  } finally {
    listLoading.value = false
    if (hasRunningTask()) {
      startPolling()
    } else {
      stopPolling()
    }
  }
}

function refreshData() {
  loadProgress()
}

function hasRunningTask(): boolean {
  if (!progress.value) return false
  return progress.value.running > 0
}

function startPolling() {
  if (polling.value !== null) return
  isPolling.value = true
  polling.value = window.setInterval(async () => {
    await loadProgress()
  }, 3000)
}

function stopPolling() {
  if (polling.value !== null) {
    window.clearInterval(polling.value)
    polling.value = null
    isPolling.value = false
  }
}

async function retryParse(submissionId: number) {
  actionLoading.value.set(submissionId, true)
  try {
    await startParse(submissionId)
    ElMessage.success('解析任务已重新提交')
    await loadProgress()
  } catch (e) {
    console.error('重试解析失败:', e)
    ElMessage.error('重试解析失败')
  } finally {
    actionLoading.value.set(submissionId, false)
  }
}

async function retryScore(submissionId: number) {
  actionLoading.value.set(submissionId, true)
  try {
    await startScore(submissionId)
    ElMessage.success('评分任务已重新提交')
    await loadProgress()
  } catch (e) {
    console.error('重试评分失败:', e)
    ElMessage.error('重试评分失败')
  } finally {
    actionLoading.value.set(submissionId, false)
  }
}

async function cancelTask(submissionId: number) {
  actionLoading.value.set(submissionId, true)
  try {
    const asyncTasks = await getAsyncTasksByBizId(submissionId)
    const runningTask = asyncTasks.data?.find(
      (t: AsyncTask) => t.status === 'PENDING' || t.status === 'RUNNING'
    )
    if (!runningTask) {
      ElMessage.warning('未找到可取消的异步任务')
      return
    }
    await cancelAsyncTask(runningTask.id)
    ElMessage.success('任务已取消')
    await loadProgress()
  } catch (e) {
    console.error('取消任务失败:', e)
    ElMessage.error('取消任务失败')
  } finally {
    actionLoading.value.set(submissionId, false)
  }
}

async function handleBatchParse() {
  try {
    await ElMessageBox.confirm('确定对当前任务的所有提交执行批量解析？', '确认批量解析', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  parseLoading.value = true
  try {
    await batchParse(selectedTaskId.value!)
    ElMessage.success('批量解析任务已提交')
    await loadProgress()
  } catch (e) {
    console.error('批量解析失败:', e)
    ElMessage.error('批量解析失败')
  } finally {
    parseLoading.value = false
  }
}

async function handleBatchScore() {
  try {
    await ElMessageBox.confirm('确定对当前任务的所有提交执行批量评分？', '确认批量评分', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  scoreLoading.value = true
  try {
    await batchScore(selectedTaskId.value!)
    ElMessage.success('批量评分任务已提交')
    await loadProgress()
  } catch (e) {
    console.error('批量评分失败:', e)
    ElMessage.error('批量评分失败')
  } finally {
    scoreLoading.value = false
  }
}

onMounted(loadTasks)
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
  align-items: center;
  gap: 8px;
}

.polling-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
