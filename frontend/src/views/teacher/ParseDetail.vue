<template>
  <div class="parse-detail">
    <el-page-header @back="$router.back()" title="返回" content="智能解析详情" />

    <!-- 提交信息摘要 -->
    <el-card v-if="submission" style="margin-top: 16px" body-style="padding: 15px">
      <el-descriptions title="基本信息" :column="3" border size="small">
        <el-descriptions-item label="学生姓名">{{ submission.studentName || '学生 #' + submission.studentId }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ submission.taskTitle || '任务 #' + submission.taskId }}</el-descriptions-item>
        <el-descriptions-item label="提交版本">V{{ submission.version }}</el-descriptions-item>
        <el-descriptions-item label="解析状态">
          <el-tag :type="getParseStatusType(submission.parseStatus)" size="small">
            {{ getParseStatusLabel(submission.parseStatus) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="20" style="margin-top: 16px">
      <!-- 左侧：解析结果 -->
      <el-col :span="16">
        <el-card v-loading="loading">
          <template #header>
            <div class="card-header">
              <span>AI 解析报告</span>
              <el-button type="primary" :loading="parsing" @click="handleReparse">
                {{ parsing ? '正在解析...' : (submission?.parseStatus === 'SUCCESS' ? '重新解析' : '开始 AI 解析') }}
              </el-button>
            </div>
          </template>

          <div v-if="submission?.parseStatus === 'SUCCESS'" class="parse-content">
            <div class="info-section">
              <h4>内容摘要</h4>
              <p class="summary-text">{{ submission.parseSummary }}</p>
            </div>

            <div class="info-section">
              <h4>核心知识点</h4>
              <div class="tag-group">
                <el-tag v-for="topic in parseTopics" :key="topic" class="topic-tag">{{ topic }}</el-tag>
              </div>
            </div>

            <el-row :gutter="20">
              <el-col :span="12">
                <div class="info-section">
                  <h4>完整度评估</h4>
                  <el-tag :type="getCompletenessType(submission.parseCompleteness)">
                    {{ submission.parseCompleteness }}
                  </el-tag>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="info-section">
                  <h4>质量初评</h4>
                  <el-tag :type="getQualityType(submission.parseQuality)">
                    {{ submission.parseQuality }}
                  </el-tag>
                </div>
              </el-col>
            </el-row>

            <div class="info-section">
              <h4>改进建议</h4>
              <ul>
                <li v-for="suggestion in parseSuggestions" :key="suggestion" class="suggestion-item">
                  {{ suggestion }}
                </li>
              </ul>
            </div>
          </div>

          <el-empty v-else-if="submission?.parseStatus !== 'PARSING'" description="暂无解析结果，请点击上方按钮开始解析" />
          <div v-else class="parsing-state">
            <el-skeleton :rows="5" animated />
            <p>AI 正在深入阅读文档，请稍候...</p>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：文件列表 -->
      <el-col :span="8">
        <el-card title="提交文件列表">
          <template #header>
            <span>附件信息</span>
          </template>
          <el-table :data="files" size="small" style="width: 100%">
            <el-table-column prop="originalName" label="文件名" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="primary" link @click="$router.push(`/teacher/submissions/${submissionId}/preview`)">预览</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSubmission, startParse, getFileList } from '@/api/task'
import { getParseStatusType, getParseStatusLabel } from '@/utils/status'
import { formatDate } from '@/utils/date'
import type { Submission, FileEntity } from '@/types'

const route = useRoute()
const loading = ref(false)
const parsing = ref(false)
const submission = ref<Submission | null>(null)
const files = ref<FileEntity[]>([])
const submissionId = computed(() => Number(route.params.id) || 0)
const polling = ref<number | null>(null)

const parseTopics = computed(() => {
  try {
    return JSON.parse(submission.value?.parseTopics || '[]')
  } catch {
    return []
  }
})

const parseSuggestions = computed(() => {
  try {
    return JSON.parse(submission.value?.parseSuggestions || '[]')
  } catch {
    return []
  }
})

async function loadData() {
  if (!submissionId.value) return
  loading.value = true
  try {
    const [subRes, fileRes] = await Promise.all([
      getSubmission(submissionId.value),
      getFileList(submissionId.value)
    ])
    submission.value = subRes.data
    files.value = fileRes.data
    if (submission.value.parseStatus === 'PARSING') {
      startPolling()
    }
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

async function handleReparse() {
  parsing.value = true
  try {
    await startParse(submissionId.value)
    ElMessage.success('解析任务已启动')
    startPolling()
  } catch {
    ElMessage.error('触发解析失败')
    parsing.value = false
  }
}

function startPolling() {
  if (polling.value !== null) return
  parsing.value = true
  polling.value = window.setInterval(async () => {
    try {
      const res = await getSubmission(submissionId.value)
      submission.value = res.data
      if (res.data.parseStatus !== 'PARSING') {
        stopPolling()
        parsing.value = false
        if (res.data.parseStatus === 'SUCCESS') ElMessage.success('解析完成')
        if (res.data.parseStatus === 'FAILED') ElMessage.error('解析失败')
      }
    } catch {
      stopPolling()
      parsing.value = false
    }
  }, 3000)
}

function stopPolling() {
  if (polling.value !== null) {
    window.clearInterval(polling.value)
    polling.value = null
  }
}

function getCompletenessType(val?: string) {
  if (val === 'HIGH') return 'success'
  if (val === 'MEDIUM') return 'warning'
  return 'danger'
}

function getQualityType(val?: string) {
  if (val === 'HIGH') return 'success'
  if (val === 'MEDIUM') return 'warning'
  return 'danger'
}

onMounted(loadData)
onBeforeUnmount(stopPolling)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.info-section {
  margin-bottom: 24px;
}
.info-section h4 {
  margin: 0 0 12px 0;
  color: #303133;
  border-left: 4px solid #409eff;
  padding-left: 10px;
}
.summary-text {
  line-height: 1.8;
  color: #606266;
  background: #f8f9fb;
  padding: 15px;
  border-radius: 4px;
}
.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.suggestion-item {
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
}
.parsing-state {
  padding: 40px 0;
  text-align: center;
  color: #909399;
}
</style>
