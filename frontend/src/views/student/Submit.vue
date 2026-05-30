<template>
  <div class="student-submit">
    <el-page-header @back="$router.back()" title="返回" content="成果上传" />
    <el-card style="margin-top: 16px">
      <el-alert v-if="task" :title="`当前任务：${task.title}`" type="info" :closable="false" style="margin-bottom: 20px" />

      <el-upload
        ref="uploadRef"
        :accept="acceptTypes"
        :limit="10"
        :on-exceed="handleExceed"
        :before-upload="beforeUpload"
        :file-list="fileList"
        :auto-upload="false"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        multiple
        drag
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            支持 Word(.doc/.docx)、PDF(.pdf)、图片(.jpg/.png)、Excel(.xls/.xlsx)、压缩包(.zip)，单文件不超过 200MB
          </div>
        </template>
      </el-upload>

      <!-- 上传进度 -->
      <el-progress v-if="uploading" :percentage="uploadProgress" :stroke-width="20" :text-inside="true" style="margin-top: 16px" />

      <!-- AI 处理进度 -->
      <el-card v-if="aiTasks.length > 0" style="margin-top: 16px" shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span>AI 处理进度</span>
            <el-tag :type="getAsyncTaskStatusType(aiTaskStatus)">{{ getAsyncTaskStatusLabel(aiTaskStatus) }}</el-tag>
          </div>
        </template>
        <el-steps :active="activeAiStep" finish-status="success" process-status="process" simple style="margin-bottom: 16px">
          <el-step title="解析" :status="getStepStatus('PARSE')" />
          <el-step title="核查" :status="getStepStatus('CHECK')" />
          <el-step title="评分" :status="getStepStatus('SCORE')" />
        </el-steps>
        <el-progress :percentage="aiTaskProgress" :status="aiTaskProgress === 100 ? 'success' : undefined" :stroke-width="16" />
        <p style="margin-top: 8px; color: #666; font-size: 14px">{{ aiTaskCurrentStep }}</p>
      </el-card>

      <div style="margin-top: 20px; text-align: right">
        <el-button @click="$router.back()">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="selectedFiles.length === 0" @click="submitUpload">确认提交</el-button>
      </div>

      <!-- 历史版本 -->
      <el-divider v-if="submissions.length > 0">历史提交版本</el-divider>
      <el-table v-if="submissions.length > 0" :data="submissions" stripe size="small">
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column label="提交时间" min-width="180" align="center">
          <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="解析状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getParseStatusType(row.parseStatus)" size="small">{{ getParseStatusLabel(row.parseStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getScoreStatusType(row.scoreStatus)" size="small">{{ getScoreStatusLabel(row.scoreStatus) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!uploading && submissions.length === 0" description="暂无历史提交" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import type { UploadFile, UploadRawFile } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getTask, getSubmissions, uploadFiles, getAsyncTasksByBizId } from '@/api/task'
import { getParseStatusType, getParseStatusLabel, getScoreStatusType, getScoreStatusLabel, getAsyncTaskStatusType, getAsyncTaskStatusLabel } from '@/utils/status'
import { formatDate } from '@/utils/date'
import type { AsyncTask, TrainingTask, Submission } from '@/types'

const route = useRoute()
const uploading = ref(false)
const uploadProgress = ref(0)
const task = ref<TrainingTask | null>(null)
const fileList = ref<UploadFile[]>([])
const selectedFiles = ref<File[]>([])
const submissions = ref<Submission[]>([])

// AI 任务进度
const aiTaskProgress = ref<number>(0)
const aiTaskCurrentStep = ref<string>('')
const aiTaskStatus = ref<string>('')
const aiTasks = ref<AsyncTask[]>([])
let progressTimer: ReturnType<typeof setInterval> | null = null

const taskId = computed(() => Number(route.params.taskId) || 0)
const acceptTypes = '.doc,.docx,.pdf,.jpg,.jpeg,.png,.xls,.xlsx,.zip'

const beforeUpload = (file: UploadRawFile) => {
  const maxSize = 200 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 200MB')
    return false
  }
  return true
}

function handleExceed() {
  ElMessage.warning('最多上传 10 个文件')
}

function handleFileChange(file: UploadFile) {
  if (file.raw && beforeUpload(file.raw)) {
    selectedFiles.value.push(file.raw)
  }
}

function handleFileRemove(file: UploadFile) {
  const idx = selectedFiles.value.findIndex(f => f.name === file.name)
  if (idx > -1) selectedFiles.value.splice(idx, 1)
}

async function submitUpload() {
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  // 纯图片提交提醒
  const IMAGE_EXTS = new Set(['jpg', 'jpeg', 'png', 'bmp', 'webp'])
  const allImages = selectedFiles.value.every(f => {
    const ext = f.name.split('.').pop()?.toLowerCase() ?? ''
    return IMAGE_EXTS.has(ext)
  })
  if (allImages) {
    try {
      await ElMessageBox.confirm(
        '您上传的全部是图片文件，AI 解析效果可能不佳。建议上传 Word/PDF 等文档文件，图片可作为补充材料一起提交。',
        '提交提醒',
        { confirmButtonText: '仍然提交', cancelButtonText: '返回修改', type: 'warning' }
      )
    } catch {
      return
    }
  }

  uploading.value = true
  uploadProgress.value = 0

  try {
    const formData = new FormData()
    selectedFiles.value.forEach(file => {
      formData.append('files', file)
    })

    await uploadFiles(taskId.value, formData)
    uploadProgress.value = 100
    ElMessage.success('上传成功，AI 正在处理...')
    selectedFiles.value = []
    fileList.value = []

    // 开始轮询 AI 任务进度
    startProgressPolling()
  } catch (e) {
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

// 轮询 AI 任务进度
function startProgressPolling() {
  if (progressTimer) clearInterval(progressTimer)

  progressTimer = setInterval(async () => {
    try {
      // 获取最新的提交记录
      const res = await getSubmissions({ taskId: taskId.value, size: 1, sort: 'version', order: 'desc' })
      if (res.data.items.length === 0) return

      const latestSubmission = res.data.items[0]
      if (!latestSubmission) return

      // 查询该提交的异步任务
      const tasksRes = await getAsyncTasksByBizId(latestSubmission.id)

      if (tasksRes.data.length === 0) return
      aiTasks.value = tasksRes.data

      const currentTask = getCurrentAiTask(tasksRes.data)
      if (!currentTask) return

      aiTaskProgress.value = currentTask.progress || 0
      aiTaskCurrentStep.value = `${getTaskTypeLabel(currentTask.taskType)}：${currentTask.currentStep || getAsyncTaskStatusLabel(currentTask.status)}`
      aiTaskStatus.value = currentTask.status || ''

      const failedTask = tasksRes.data.find(task => task.status === 'FAILED' || task.status === 'CANCELLED')
      const scoreTask = findLatestTask(tasksRes.data, 'SCORE')
      // 如果链路完成或失败，停止轮询
      if ((scoreTask && scoreTask.status === 'SUCCESS') || failedTask) {
        clearInterval(progressTimer!)
        progressTimer = null
        if (scoreTask && scoreTask.status === 'SUCCESS') {
          aiTaskProgress.value = 100
          aiTaskStatus.value = 'SUCCESS'
          aiTaskCurrentStep.value = '评分：AI 处理完成，等待教师复核'
          ElMessage.success('AI 处理完成！')
        } else {
          aiTaskStatus.value = failedTask?.status || 'FAILED'
          aiTaskCurrentStep.value = `${getTaskTypeLabel(failedTask?.taskType || '')}：${failedTask?.currentStep || failedTask?.errorMessage || '处理失败'}`
          ElMessage.error('AI 处理失败: ' + (failedTask?.currentStep || failedTask?.errorMessage || '请联系教师处理'))
        }
        // 刷新提交列表
        loadSubmissions()
      }
    } catch (e) {
    }
  }, 2000) // 每 2 秒查询一次
}

function findLatestTask(tasks: AsyncTask[], taskType: string) {
  return tasks.find(task => task.taskType === taskType)
}

function getCurrentAiTask(tasks: AsyncTask[]) {
  return tasks.find(task => ['RUNNING', 'RETRYING', 'PENDING'].includes(task.status))
    || findLatestTask(tasks, 'SCORE')
    || findLatestTask(tasks, 'CHECK')
    || findLatestTask(tasks, 'PARSE')
}

function getTaskTypeLabel(taskType: string) {
  const map: Record<string, string> = { PARSE: '解析', CHECK: '核查', SCORE: '评分' }
  return map[taskType] || '处理'
}

const activeAiStep = computed(() => {
  const current = getCurrentAiTask(aiTasks.value)
  if (!current) return 0
  const map: Record<string, number> = { PARSE: 0, CHECK: 1, SCORE: 2 }
  return map[current.taskType] ?? 0
})

function getStepStatus(taskType: string) {
  const task = findLatestTask(aiTasks.value, taskType)
  if (!task) return 'wait'
  if (task.status === 'SUCCESS') return 'success'
  if (task.status === 'FAILED' || task.status === 'CANCELLED') return 'error'
  return 'process'
}

onUnmounted(() => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
})

async function loadTask() {
  if (!taskId.value) return
  try {
    const res = await getTask(taskId.value)
    task.value = res.data
  } catch (e) {
    ElMessage.error('加载任务信息失败')
  }
}

async function loadSubmissions() {
  if (!taskId.value) return
  try {
    const res = await getSubmissions({ taskId: taskId.value, size: 5, sort: 'version', order: 'desc' })
    submissions.value = res.data.items
  } catch (e) {
  }
}

onMounted(() => {
  loadTask()
  loadSubmissions()
})
</script>
