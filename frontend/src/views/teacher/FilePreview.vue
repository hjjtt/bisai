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
            <!-- docx: mammoth 前端转 HTML / doc: 后端 WordToHtmlConverter 转 HTML -->
            <div v-else-if="(isDocx(file) || isDocOld(file)) && docHtmls[file.id]" class="doc-html-content" v-html="docHtmls[file.id]" />
            <!-- Excel(xls/xlsx) 后端转 PDF 后用 iframe 预览 -->
            <iframe v-else-if="isExcel(file) && blobUrls[file.id] && blobUrls[file.id] !== '__NO_PREVIEW__'" :src="blobUrls[file.id]" class="preview-iframe" />
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
import { getFileList, getFilePreview, getFileRaw } from '@/api/task'
import { downloadFile as downloadFileApi } from '@/api/system'
import type { FileInfo } from '@/types'
import mammoth from 'mammoth'

const route = useRoute()
const loading = ref(false)
const files = ref<FileInfo[]>([])
const activeTab = ref('')
const blobUrls = reactive<Record<number, string>>({})
const docHtmls = reactive<Record<number, string>>({})

const submissionId = computed(() => Number(route.params.id) || 0)

function isPdf(file: FileInfo) {
  return file.fileType === 'PDF' || file.originalName.endsWith('.pdf')
}

function isDocx(file: FileInfo) {
  return file.fileType === 'DOCX' || file.originalName.endsWith('.docx')
}

function isDocOld(file: FileInfo) {
  return file.fileType === 'DOC' || file.originalName.endsWith('.doc')
}

function isExcel(file: FileInfo) {
  return ['XLS', 'XLSX'].includes(file.fileType)
}

function isImage(file: FileInfo) {
  return ['JPG', 'JPEG', 'PNG'].includes(file.fileType)
}

async function loadBlob(file: FileInfo) {
  try {
    // docx: 走下载接口拿原始文件，mammoth 前端解析（保留格式）
    if (isDocx(file)) {
      try {
        const res = await getFileRaw(file.id)
        const blob = res.data instanceof Blob ? res.data : new Blob([res.data])
        const arrayBuffer = await blob.arrayBuffer()
        const result = await mammoth.convertToHtml({ arrayBuffer })
        docHtmls[file.id] = result.value || '<p style="color:#909399">文档内容为空</p>'
      } catch (e) {
        console.error('mammoth 解析失败:', e)
        docHtmls[file.id] = '<p style="color:#F56C6C">文档解析失败，请下载后查看</p>'
      }
      return
    }

    // doc(旧格式): 后端 WordToHtmlConverter 转 HTML，text 接收
    if (isDocOld(file)) {
      try {
        const res = await getFilePreview(file.id)
        // 后端返回 HTML 文本
        const html = typeof res.data === 'string' ? res.data : await (res.data as Blob).text()
        docHtmls[file.id] = html || '<p style="color:#909399">文档内容为空</p>'
      } catch (e) {
        console.error('doc HTML 预览失败:', e)
        docHtmls[file.id] = '<p style="color:#F56C6C">文档解析失败，请下载后查看</p>'
      }
      return
    }

    // Excel/PDF/图片: 走预览接口
    const res = await getFilePreview(file.id)
    const blob = res.data instanceof Blob ? res.data : new Blob([res.data])

    if (isExcel(file) && blob.type && !blob.type.includes('pdf')) {
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

  .doc-html-content {
    width: 100%;
    max-height: 750px;
    overflow-y: auto;
    padding: 24px 32px;
    line-height: 1.8;
    font-size: 14px;
    color: #303133;
    background: #fff;

    :deep(table) {
      border-collapse: collapse;
      width: 100%;
      margin: 12px 0;

      th, td {
        border: 1px solid #dcdfe6;
        padding: 8px 12px;
        text-align: left;
      }

      th {
        background: #f5f7fa;
        font-weight: 600;
      }
    }

    :deep(img) {
      max-width: 100%;
      height: auto;
    }

    :deep(h1), :deep(h2), :deep(h3) {
      margin: 16px 0 8px;
      color: #1e293b;
    }

    :deep(p) {
      margin: 6px 0;
    }

    :deep(ul), :deep(ol) {
      padding-left: 24px;
    }

    :deep(pre), :deep(code) {
      background: #f5f7fa;
      border-radius: 4px;
      padding: 2px 6px;
      font-family: 'Consolas', 'Monaco', monospace;
    }

    :deep(blockquote) {
      border-left: 4px solid #409eff;
      padding-left: 12px;
      margin: 12px 0;
      color: #606266;
    }
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
