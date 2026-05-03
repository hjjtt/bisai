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

function isImage(file: FileInfo) {
  return ['JPG', 'JPEG', 'PNG'].includes(file.fileType)
}

async function loadBlob(fileId: number) {
  try {
    const res = await getFilePreview(fileId)
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
    blobUrls[fileId] = URL.createObjectURL(blob)
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
        await loadBlob(file.id)
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
  Object.values(blobUrls).forEach(url => URL.revokeObjectURL(url))
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
