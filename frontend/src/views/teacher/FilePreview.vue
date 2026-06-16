<template>
  <div class="file-preview">
    <el-page-header @back="$router.back()" title="返回" content="文件预览" />
    <el-card style="margin-top: 16px" v-loading="loading">
      <el-empty v-if="!loading && files.length === 0" description="暂无可预览的文件" />

      <el-tabs v-if="files.length > 0" v-model="activeTab">
        <el-tab-pane
          v-for="file in files"
          :key="file.id"
          :label="file.originalName"
          :name="String(file.id)"
        >
          <div class="preview-area">
            <!-- PDF 预览 -->
            <iframe v-if="isPdf(file) && blobUrls[file.id]" :src="blobUrls[file.id]" class="preview-iframe" />
            <!-- Office(doc/docx/xls/xlsx) 后端转 PDF 后用 iframe 预览 -->
            <iframe v-else-if="isOfficeConvertible(file) && blobUrls[file.id] && blobUrls[file.id] !== '__NO_PREVIEW__'" :src="blobUrls[file.id]" class="preview-iframe" />
            <!-- 图片预览 -->
            <el-image v-else-if="isImage(file) && blobUrls[file.id]" :src="blobUrls[file.id]" fit="contain" class="preview-image" />
            <!-- 其他文件 -->
            <div v-else class="no-preview">
              <el-icon :size="64"><Document /></el-icon>
              <p>该文件类型暂不支持在线预览</p>
              <el-button type="primary" @click="handleDownload(file.id)">下载文件</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { getFileList, getFilePreview } from '@/api/task'
import { downloadFile as downloadFileApi } from '@/api/system'
import type { FileInfo } from '@/types'

const route = useRoute()
const loading = ref(false)
const files = ref<FileInfo[]>([])
const activeTab = ref('')
const blobUrls = reactive<Record<number, string>>({})

const submissionId = computed(() => Number(route.params.id) || 0)

function isPdf(file: FileInfo) {
  return file.fileType === 'PDF' || file.originalName.endsWith('.pdf')
}

// 后端 FileController 会把 doc/docx/xls/xlsx 转成 PDF 返回（Content-Type: application/pdf），
// 因此这些 Office 类型也走 iframe 预览，而非显示"暂不支持"。
function isOfficeConvertible(file: FileInfo) {
  return ['DOC', 'DOCX', 'XLS', 'XLSX'].includes(file.fileType)
}

function isImage(file: FileInfo) {
  return ['JPG', 'JPEG', 'PNG'].includes(file.fileType)
}

async function loadBlob(file: FileInfo) {
  try {
    const res = await getFilePreview(file.id)
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
    // 后端对 Office 文件转 PDF：若转换失败会回退返回原二进制（非 PDF），
    // 此时 blob.type 不是 application/pdf，标记为不可预览，避免 iframe 空白。
    if (isOfficeConvertible(file) && blob.type && !blob.type.includes('pdf')) {
      blobUrls[file.id] = '__NO_PREVIEW__'
      return
    }
    blobUrls[file.id] = URL.createObjectURL(blob)
  } catch {
    ElMessage.error('文件预览加载失败')
  }
}

async function handleDownload(fileId: number) {
  try {
    await downloadFileApi(fileId)
  } catch {
    ElMessage.error('下载失败')
  }
}

async function loadFiles() {
  if (!submissionId.value) return
  loading.value = true
  try {
    const res = await getFileList(submissionId.value)
    files.value = res.data
    if (files.value.length > 0) {
      activeTab.value = String(files.value[0].id)
      for (const file of files.value) {
        await loadBlob(file)
      }
    }
  } catch {
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadFiles)

onUnmounted(() => {
  Object.values(blobUrls).forEach(url => {
    if (url && url !== '__NO_PREVIEW__') URL.revokeObjectURL(url)
  })
})
</script>

<style lang="scss" scoped>
.preview-area {
  min-height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;

  .preview-iframe {
    width: 100%;
    height: 700px;
    border: none;
  }

  .preview-image {
    max-width: 100%;
    max-height: 700px;
  }

  .no-preview {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    color: #909399;
  }
}
</style>
