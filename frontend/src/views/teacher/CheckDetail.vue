<template>
  <div class="check-detail">
    <el-page-header @back="$router.back()" title="返回" content="智能核查详情" />

    <!-- 提交信息摘要 -->
    <el-card v-if="submission" style="margin-top: 16px" body-style="padding: 15px">
      <el-descriptions title="基本信息" :column="3" border size="small">
        <el-descriptions-item label="学生姓名">{{ submission.studentName || '学生 #' + submission.studentId }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ submission.taskTitle || '任务 #' + submission.taskId }}</el-descriptions-item>
        <el-descriptions-item label="提交版本">V{{ submission.version }}</el-descriptions-item>
        <el-descriptions-item label="核查状态">
          <el-tag :type="getCheckStatusType(submission.checkStatus)" size="small">
            {{ getCheckStatusLabel(submission.checkStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="风险概览">
          <el-tag v-if="highRiskCount > 0" type="danger" size="small" style="margin-right: 5px">{{ highRiskCount }} 高风险</el-tag>
          <el-tag v-if="mediumRiskCount > 0" type="warning" size="small">{{ mediumRiskCount }} 中风险</el-tag>
          <el-tag v-if="highRiskCount === 0 && mediumRiskCount === 0" type="success" size="small">暂无显著风险</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 风险统计 -->
    <el-row :gutter="16" style="margin-top: 16px" v-if="checkResults.length > 0">
      <el-col :span="8">
        <el-card shadow="never" class="risk-card" body-style="padding: 10px">
          <div class="risk-stat">
            <span class="risk-number" style="color: #67c23a">{{ lowRiskCount }}</span>
            <span class="risk-label">低风险</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="risk-card" body-style="padding: 10px">
          <div class="risk-stat">
            <span class="risk-number" style="color: #e6a23c">{{ mediumRiskCount }}</span>
            <span class="risk-label">中风险</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="risk-card" body-style="padding: 10px">
          <div class="risk-stat">
            <span class="risk-number" style="color: #f56c6c">{{ highRiskCount }}</span>
            <span class="risk-label">高风险</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>详细核查项</span>
          <div>
            <el-button type="info" @click="$router.push(`/teacher/submissions/${submissionId}/preview`)">预览文件</el-button>
            <el-button type="primary" @click="handleRecheck" :loading="rechecking">
              {{ rechecking ? '正在核查...' : (submission?.checkStatus === 'SUCCESS' ? '重新核查' : '开始 AI 核查') }}
            </el-button>
          </div>
        </div>
      </template>

      <el-alert v-if="rechecking" title="AI 核查进行中，系统正在对比任务要求与提交内容，请耐心等待..." type="info" :closable="false" show-icon style="margin-bottom: 16px" />

      <template v-if="checkResults.length > 0">
        <el-table :data="checkResults" stripe>
          <el-table-column prop="checkType" label="核查维度" width="130" />
          <el-table-column prop="checkItem" label="检查项" width="160" />
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="getResultType(row.result)" size="small">{{ getResultLabel(row.result) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="风险等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getRiskType(row.riskLevel)" size="small" effect="dark">{{ getRiskLabel(row.riskLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="200" />
          <el-table-column prop="evidence" label="证据片段" min-width="180" />
          <el-table-column prop="suggestion" label="修改建议" min-width="180" />
        </el-table>
      </template>

      <el-empty v-else-if="submission?.checkStatus !== 'CHECKING'" description="暂无核查结果，请先触发 AI 核查" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCheckResults, startCheck, getSubmission } from '@/api/task'
import { getResultType, getResultLabel, getRiskType, getRiskLabel, getCheckStatusType, getCheckStatusLabel } from '@/utils/status'
import type { CheckResult, Submission } from '@/types'

const route = useRoute()
const loading = ref(false)
const rechecking = ref(false)
const submission = ref<Submission | null>(null)
const checkResults = ref<CheckResult[]>([])
const polling = ref<number | null>(null)

const submissionId = computed(() => Number(route.params.id) || 0)

const highRiskCount = computed(() => checkResults.value.filter(r => r.riskLevel === 'HIGH').length)
const mediumRiskCount = computed(() => checkResults.value.filter(r => r.riskLevel === 'MEDIUM').length)
const lowRiskCount = computed(() => checkResults.value.filter(r => r.riskLevel === 'LOW').length)

async function loadData() {
  if (!submissionId.value) return
  loading.value = true
  try {
    const [subRes, checkRes] = await Promise.all([
      getSubmission(submissionId.value),
      getCheckResults(submissionId.value)
    ])
    submission.value = subRes.data
    checkResults.value = checkRes.data
    if (submission.value.checkStatus === 'CHECKING') {
      startPolling()
    }
  } catch (e) {
    console.error('加载详情失败:', e)
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

async function handleRecheck() {
  rechecking.value = true
  try {
    await startCheck(submissionId.value)
    ElMessage.success('AI 核查任务已启动')
    startPolling()
  } catch (e) {
    console.error('触发核查失败:', e)
    ElMessage.error('触发核查失败')
    rechecking.value = false
  }
}

function startPolling() {
  if (polling.value !== null) return
  rechecking.value = true
  polling.value = window.setInterval(async () => {
    try {
      const res = await getSubmission(submissionId.value)
      submission.value = res.data
      if (res.data.checkStatus !== 'CHECKING') {
        stopPolling()
        rechecking.value = false
        const checkRes = await getCheckResults(submissionId.value)
        checkResults.value = checkRes.data
        if (res.data.checkStatus === 'SUCCESS') ElMessage.success('核查完成')
        if (res.data.checkStatus === 'CHECK_FAILED') ElMessage.error('核查失败')
      }
    } catch {
      stopPolling()
      rechecking.value = false
    }
  }, 3000)
}

function stopPolling() {
  if (polling.value !== null) {
    window.clearInterval(polling.value)
    polling.value = null
  }
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
.risk-card {
  text-align: center;
}
.risk-stat {
  padding: 8px 0;
}
.risk-number {
  font-size: 32px;
  font-weight: 700;
}
.risk-label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
