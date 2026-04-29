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
            <iframe v-if="isPdf(file)" :src="getPreviewUrl(file.id)" class="preview-iframe" />
            <!-- 图片预览 -->
            <el-image v-else-if="isImage(file)" :src="getPreviewUrl(file.id)" fit="contain" class="preview-image" />
            <!-- 其他文件 -->
            <div v-else class="no-preview">
              <el-icon :size="64"><Document /></el-icon>
              <p>该文件类型暂不支持在线预览</p>
              <el-button type="primary" @click="downloadFile(file.id)">下载文件</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { getFileList } from '@/api/task'
import type { FileInfo } from '@/types'

const route = useRoute()
const loading = ref(false)
const files = ref<FileInfo[]>([])
const activeTab = ref('')

const submissionId = computed(() => Number(route.params.id) || 0)

function getPreviewUrl(fileId: number) {
  return `/api/files/${fileId}/preview`
}

function isPdf(file: FileInfo) {
  return file.fileType === 'PDF' || file.originalName.endsWith('.pdf')
}

function isImage(file: FileInfo) {
  return ['JPG', 'JPEG', 'PNG'].includes(file.fileType)
}

function downloadFile(fileId: number) {
  window.open(`/api/files/${fileId}/download`)
}

async function loadFiles() {
  if (!submissionId.value) return
  loading.value = true
  try {
    const res = await getFileList(submissionId.value)
    files.value = res.data
    if (files.value.length > 0) {
      activeTab.value = String(files.value[0].id)
    }
  } catch {
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadFiles)
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
