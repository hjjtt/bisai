<template>
  <div class="admin-knowledge">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="header-title">知识库资源管理</span>
          <div class="header-actions">
            <el-input v-model="searchKeyword" placeholder="搜索文档或课程..." style="width: 240px">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="showUploadDialog = true">
              <el-icon><Upload /></el-icon>上传资源
            </el-button>
          </div>
        </div>
      </template>

      <!-- 核心数据表格 -->
      <el-table :data="knowledgeList" stripe v-loading="loading" style="width: 100%">
        <el-table-column prop="name" label="资源名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="course" label="关联课程" width="150" />
        <el-table-column label="解析状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.parseStatus)" size="small" effect="plain">
              {{ row.parseStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="向量化" width="100" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.vectorized" color="#10b981"><CircleCheck /></el-icon>
            <el-icon v-else color="#94a3b8"><Loading /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="最后更新" width="180" color="#64748b" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link>查看</el-button>
            <el-button type="primary" link>重新解析</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && knowledgeList.length === 0" description="暂无知识库资源" />

      <div class="pagination-box">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          layout="total, prev, pager, next"
          :total="total"
          @change="loadData"
        />
      </div>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传实训知识资源" width="560px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="关联课程" required>
          <el-select v-model="uploadForm.courseId" placeholder="请选择课程" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源文件" required>
          <el-upload
            class="upload-demo"
            drag
            action="#"
            multiple
            :auto-upload="false"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处 或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF, Word, Markdown 格式，单文件不超过 20MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpload">开始上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, UploadFilled, CircleCheck, Loading } from '@element-plus/icons-vue'
import { getKnowledgeList, deleteKnowledge, type KnowledgeDocument } from '@/api/knowledge'
import { getCourseList } from '@/api/course'
import type { Course } from '@/types'

const loading = ref(false)
const showUploadDialog = ref(false)
const searchKeyword = ref('')
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const courses = ref<Course[]>([])

const uploadForm = ref({
  courseId: ''
})

const knowledgeList = ref<KnowledgeDocument[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getKnowledgeList({
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    knowledgeList.value = res.data.items
    total.value = res.data.total
  } catch {
    ElMessage.error('加载知识库列表失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = async (row: KnowledgeDocument) => {
  try {
    await ElMessageBox.confirm(`确定删除文档 "${row.name}" 吗？`, '提示', { type: 'warning' })
    await deleteKnowledge(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'SUCCESS':
    case '已完成': return 'success'
    case 'PROCESSING':
    case '解析中': return 'warning'
    case 'FAILED':
    case '失败': return 'danger'
    default: return 'info'
  }
}

const handleUpload = () => {
  ElMessage.success('资源上传任务已提交，系统正在后台解析中...')
  showUploadDialog.value = false
}

async function loadCourses() {
  try {
    const res = await getCourseList({ size: 100 })
    courses.value = res.data.items
  } catch {
    // 忽略
  }
}

onMounted(() => {
  loadData()
  loadCourses()
})
</script>

<style lang="scss" scoped>
.admin-knowledge {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-title {
    font-size: 16px;
    font-weight: 600;
    color: #1e293b;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

:deep(.el-table) {
  margin-top: 12px;
  
  th.el-table__cell {
    background-color: #f8fafc;
    color: #64748b;
    font-weight: 600;
  }
}

.pagination-box {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.el-icon--upload {
  font-size: 48px;
  color: #94a3b8;
  margin-bottom: 8px;
}
</style>
