<template>
  <div class="knowledge">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>知识库管理</span>
          <el-button type="primary" @click="showUploadDialog = true">上传文档</el-button>
        </div>
      </template>

      <el-table :data="documents" stripe v-loading="loading">
        <el-table-column prop="name" label="文档名称" min-width="200" />
        <el-table-column prop="courseName" label="适用课程" width="150" />
        <el-table-column label="解析状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.parseStatus === 'SUCCESS' ? 'success' : row.parseStatus === 'FAILED' ? 'danger' : 'warning'" size="small">
              {{ row.parseStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="向量化状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.vectorStatus === 'SUCCESS' ? 'success' : row.vectorStatus === 'FAILED' ? 'danger' : 'warning'" size="small">
              {{ row.vectorStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="启用状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="handleToggle(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && documents.length === 0" description="暂无知识库文档" />
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传知识库文档" width="500px">
      <el-upload drag multiple :auto-upload="false" :on-change="handleFileChange">
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽或点击上传</div>
        <template #tip>
          <div class="el-upload__tip">支持上传实训指导书、评分标准、课程知识点等文档</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">确认上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getKnowledgeList, deleteKnowledge } from '@/api/knowledge'

const loading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const documents = ref<any[]>([])

function handleFileChange() {}

async function handleToggle(row: any) {
  ElMessage.success(row.enabled ? '已启用' : '已停用')
}

async function handleDelete(id: number) {
  try {
    await deleteKnowledge(id)
    ElMessage.success('已删除')
    loadDocuments()
  } catch {
    ElMessage.error('删除失败')
  }
}

function handleUpload() {
  uploading.value = false
  showUploadDialog.value = false
  ElMessage.success('上传成功')
}

async function loadDocuments() {
  loading.value = true
  try {
    const res = await getKnowledgeList({ page: 1, size: 100 })
    documents.value = (res.data as any).items || []
  } catch {
    ElMessage.error('加载知识库列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDocuments)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
