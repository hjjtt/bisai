<template>
  <div class="knowledge">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实训参考资料</span>
          <el-button type="primary" @click="showUploadDialog = true">上传文档</el-button>
        </div>
      </template>

      <el-table :data="documents" stripe v-loading="loading">
        <el-table-column prop="name" label="资料名称" min-width="200" align="center" />
        <el-table-column prop="taskName" label="关联实训任务" min-width="180" align="center" show-overflow-tooltip />
        <el-table-column prop="courseName" label="所属课程" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column label="解析状态" min-width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getKnowledgeStatusType(row.parseStatus)" size="small">{{ getKnowledgeStatusLabel(row.parseStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="向量化状态" min-width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.vectorStatus === 'SUCCESS' ? 'success' : row.vectorStatus === 'FAILED' ? 'danger' : 'warning'" size="small">
              {{ row.vectorStatus === 'SUCCESS' ? '已完成' : row.vectorStatus === 'FAILED' ? '失败' : (row.vectorStatus === 'PROCESSING' || row.vectorStatus === 'PENDING' ? '处理中' : row.vectorStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用状态" min-width="100" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="handleToggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && documents.length === 0" description="暂无参考资料" />
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传参考资料" width="500px">
      <el-form label-position="top" class="upload-form">
        <el-form-item label="关联实训任务" required>
          <el-select v-model="uploadTaskId" filterable placeholder="请选择要服务的实训任务" style="width: 100%">
            <el-option
              v-for="task in tasks"
              :key="task.id"
              :label="`${task.title}${task.courseName ? ' / ' + task.courseName : ''}`"
              :value="task.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="参考资料文件" required>
          <el-upload drag multiple :auto-upload="false" :on-change="handleFileChange">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽或点击上传</div>
            <template #tip>
              <div class="el-upload__tip">支持上传实训指导书、评分标准、课程知识点等文档</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">确认上传</el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑参考资料" width="500px">
      <el-form label-position="top" class="upload-form">
        <el-form-item label="文档名称" required>
          <el-input v-model="editForm.name" placeholder="请输入文档名称" />
        </el-form-item>
        <el-form-item label="关联实训任务" required>
          <el-select v-model="editForm.taskId" filterable placeholder="请选择要服务的实训任务" style="width: 100%">
            <el-option
              v-for="task in tasks"
              :key="task.id"
              :label="`${task.title}${task.courseName ? ' / ' + task.courseName : ''}`"
              :value="task.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="editing" @click="handleEditSubmit">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getKnowledgeList, deleteKnowledge, uploadKnowledge, toggleKnowledgeStatus, updateKnowledge, type KnowledgeDocument } from '@/api/knowledge'
import { getTaskList } from '@/api/task'
import { getKnowledgeStatusType, getKnowledgeStatusLabel } from '@/utils/status'
import type { TrainingTask } from '@/types'

const loading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const documents = ref<KnowledgeDocument[]>([])
const selectedFiles = ref<File[]>([])
const tasks = ref<TrainingTask[]>([])
const uploadTaskId = ref<number | undefined>()
const polling = ref<number | null>(null)

const showEditDialog = ref(false)
const editing = ref(false)
const editForm = ref({
  id: 0,
  name: '',
  taskId: undefined as number | undefined
})

function handleFileChange(_file: unknown, fileList: { raw: File }[]) {
  selectedFiles.value = fileList.map(item => item.raw).filter(Boolean)
}

async function handleToggle(row: KnowledgeDocument) {
  try {
    await toggleKnowledgeStatus(row.id, row.enabled)
    ElMessage.success(row.enabled ? '已启用' : '已停用')
  } catch (e) {
    row.enabled = !row.enabled
    ElMessage.error('状态更新失败')
  }
}

async function handleDelete(id: number) {
  try {
    await deleteKnowledge(id)
    ElMessage.success('已删除')
    loadDocuments()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function openEditDialog(row: KnowledgeDocument) {
  editForm.value = {
    id: row.id,
    name: row.name,
    taskId: row.taskId
  }
  showEditDialog.value = true
}

async function handleEditSubmit() {
  if (!editForm.value.name) {
    ElMessage.warning('请输入文档名称')
    return
  }
  if (!editForm.value.taskId) {
    ElMessage.warning('请选择关联实训任务')
    return
  }
  editing.value = true
  try {
    await updateKnowledge(editForm.value.id, {
      name: editForm.value.name,
      taskId: editForm.value.taskId
    })
    ElMessage.success('修改成功')
    showEditDialog.value = false
    loadDocuments()
  } catch (e) {
    ElMessage.error('修改失败')
  } finally {
    editing.value = false
  }
}

async function handleUpload() {
  if (!uploadTaskId.value) {
    ElMessage.warning('请选择关联实训任务')
    return
  }
  if (selectedFiles.value.length === 0) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    for (const file of selectedFiles.value) {
      await uploadKnowledge(file, { taskId: uploadTaskId.value })
    }
    selectedFiles.value = []
    uploadTaskId.value = undefined
    showUploadDialog.value = false
    ElMessage.success('上传成功，正在解析和向量化')
    await loadDocuments()
    startPolling()
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

async function loadTasks() {
  try {
    const res = await getTaskList({ page: 1, size: 100 })
    tasks.value = res.data.items || []
  } catch {
    ElMessage.error('加载实训任务失败')
  }
}

async function loadDocuments() {
  loading.value = true
  try {
    const res = await getKnowledgeList({ page: 1, size: 100 })
    documents.value = res.data.items || []
  } catch (e) {
    ElMessage.error('加载参考资料列表失败')
  } finally {
    loading.value = false
  }
}

function hasRunningDocument() {
  return documents.value.some(item => ['PENDING', 'PARSING', 'VECTORIZING'].includes(item.parseStatus) || ['PENDING', 'VECTORIZING'].includes(item.vectorStatus))
}

function startPolling() {
  if (polling.value !== null) return
  polling.value = window.setInterval(async () => {
    await loadDocuments()
    if (!hasRunningDocument()) stopPolling()
  }, 3000)
}

function stopPolling() {
  if (polling.value !== null) {
    window.clearInterval(polling.value)
    polling.value = null
  }
}

onMounted(() => {
  loadTasks()
  loadDocuments().then(() => {
    if (hasRunningDocument()) startPolling()
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
</style>
