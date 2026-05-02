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

      <div style="margin-top: 20px; text-align: right">
        <el-button @click="$router.back()">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="fileList.length === 0" @click="submitUpload">确认提交</el-button>
      </div>

      <!-- 历史版本 -->
      <el-divider v-if="submissions.length > 0">历史提交版本</el-divider>
      <el-table v-if="submissions.length > 0" :data="submissions" stripe size="small">
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="解析状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getParseStatusType(row.parseStatus)" size="small">{{ getParseStatusLabel(row.parseStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分状态" width="120">
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { UploadFile, UploadInstance, UploadRawFile } from 'element-plus'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getTask, getSubmissions, uploadFiles } from '@/api/task'
import { getParseStatusType, getParseStatusLabel, getScoreStatusType, getScoreStatusLabel } from '@/utils/status'
import { formatDate } from '@/utils/date'
import type { TrainingTask, Submission } from '@/types'

const route = useRoute()
const router = useRouter()
const uploadRef = ref<UploadInstance>()
const uploading = ref(false)
const uploadProgress = ref(0)
const task = ref<TrainingTask | null>(null)
const fileList = ref<UploadFile[]>([])
const selectedFiles = ref<File[]>([])
const submissions = ref<Submission[]>([])

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

  uploading.value = true
  uploadProgress.value = 0

  try {
    const formData = new FormData()
    selectedFiles.value.forEach(file => {
      formData.append('files', file)
    })

    await uploadFiles(taskId.value, formData)
    uploadProgress.value = 100
    ElMessage.success('上传成功')
    selectedFiles.value = []
    fileList.value = []
    router.push(`/student/tasks/${taskId.value}`)
  } catch (e) {
    console.error('上传失败:', e)
    ElMessage.error('上传失败，请重试')
  } finally {
    uploading.value = false
    uploadProgress.value = 0
  }
}

async function loadTask() {
  if (!taskId.value) return
  try {
    const res = await getTask(taskId.value)
    task.value = res.data
  } catch (e) {
    console.error('加载任务信息失败:', e)
    ElMessage.error('加载任务信息失败')
  }
}

async function loadSubmissions() {
  if (!taskId.value) return
  try {
    const res = await getSubmissions({ taskId: taskId.value, size: 5, sort: 'version', order: 'desc' })
    submissions.value = res.data.items
  } catch (e) {
    console.error('加载提交历史失败:', e)
  }
}

onMounted(() => {
  loadTask()
  loadSubmissions()
})
</script>
