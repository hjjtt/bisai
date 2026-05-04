<template>
  <div class="score-review">
    <el-page-header @back="$router.back()" title="返回" content="评分复核" />

    <!-- 提交信息摘要 -->
    <el-card v-if="submission" style="margin-top: 16px" body-style="padding: 15px">
      <el-descriptions title="提交信息" :column="3" border size="small">
        <el-descriptions-item label="学生姓名">{{ submission.studentName || '学生 #' + submission.studentId }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ submission.taskTitle || '任务 #' + submission.taskId }}</el-descriptions-item>
        <el-descriptions-item label="提交版本">V{{ submission.version }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatDate(submission.submitTime) }}</el-descriptions-item>
        <el-descriptions-item label="解析状态">
          <el-tag :type="getParseStatusType(submission.parseStatus)" size="small">
            {{ getParseStatusLabel(submission.parseStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总分">
          <span style="font-weight: bold; color: #f56c6c">{{ submission.totalScore ?? '--' }}</span> 分
        </el-descriptions-item>
      </el-descriptions>
      
      <div v-if="submission.parseSummary" style="margin-top: 12px">
        <h4 style="margin: 0 0 8px 0; font-size: 14px">内容摘要 (AI 解析)</h4>
        <el-text type="info" size="small" style="line-height: 1.6">{{ submission.parseSummary }}</el-text>
      </div>
    </el-card>

    <el-row :gutter="20" style="margin-top: 16px">
      <!-- 左侧：评分表格 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>评分详情</span>
              <div>
                <el-button type="primary" :loading="aiScoring" @click="handleAiScore">
                  AI 智能评分
                </el-button>
                <el-button type="info" @click="$router.push(`/teacher/submissions/${submissionId}/preview`)">
                  预览原始文件
                </el-button>
              </div>
            </div>
          </template>

          <el-table :data="scores" stripe v-loading="loading">
            <el-table-column prop="indicatorName" label="评价指标" width="180">
              <template #default="{ row }">
                {{ row.indicatorName || '指标 #' + row.indicatorId }}
              </template>
            </el-table-column>
            <el-table-column prop="autoScore" label="AI建议分" width="100">
              <template #default="{ row }">
                <span class="auto-score">{{ row.autoScore ?? '--' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="教师评分" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.teacherScore" :min="0" :max="row.maxScore || 100" :precision="1" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="评分理由" min-width="200">
              <template #default="{ row }">
                <el-text type="info" size="small">{{ row.reason || '--' }}</el-text>
              </template>
            </el-table-column>
            <el-table-column prop="evidence" label="证据" min-width="150">
              <template #default="{ row }">
                <el-text type="warning" size="small">{{ row.evidence || '--' }}</el-text>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="!loading && scores.length === 0" description="暂无评分数据，请先点击 AI 智能评分" />
        </el-card>
      </el-col>

      <!-- 右侧：操作面板 -->
      <el-col :span="8">
        <el-card>
          <h4>教师评语</h4>
          <el-input v-model="comment" type="textarea" :rows="4" placeholder="请输入评语" style="margin-top: 12px" />

          <el-divider />
          <el-button type="warning" :loading="objectiveLoading" @click="handleViewObjective" style="width: 100%">
            查看客观评分
          </el-button>
          <el-button type="primary" @click="showCorrectDialog = true" style="width: 100%; margin-top: 8px">
            修正成绩
          </el-button>
          <el-divider />
          <div class="score-actions">
            <el-button type="success" :loading="saving" @click="handlePublish" style="width: 100%">
              确认并发布成绩
            </el-button>
            <el-button :loading="saving" @click="handleSave" style="width: 100%; margin-top: 8px">
              暂存评分
            </el-button>
            <el-button type="danger" @click="handleReturn" style="width: 100%; margin-top: 8px">
              退回学生
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 客观评分弹窗 -->
    <el-dialog v-model="showObjectiveDialog" title="客观评分详情" width="500px">
      <el-descriptions v-if="objectiveScore" :column="1" border>
        <el-descriptions-item v-for="(score, key) in objectiveScore.scores" :key="key" :label="key">
          {{ score }} 分
        </el-descriptions-item>
        <el-descriptions-item label="总分">
          <span style="font-weight: bold; color: #f56c6c; font-size: 18px">{{ objectiveScore.total }}</span> 分
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无客观评分数据" />
    </el-dialog>

    <!-- 成绩修正弹窗 -->
    <el-dialog v-model="showCorrectDialog" title="修正成绩" width="500px">
      <el-form :model="correctForm" label-width="100px">
        <el-form-item label="指标">
          <el-select v-model="correctForm.indicatorId" placeholder="选择指标（可选）" clearable style="width: 100%">
            <el-option
              v-for="s in scores"
              :key="s.indicatorId"
              :label="s.indicatorName || '指标 #' + s.indicatorId"
              :value="s.indicatorId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="新分数">
          <el-input-number v-model="correctForm.newScore" :min="0" :max="100" :precision="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="修正原因">
          <el-input v-model="correctForm.reason" type="textarea" :rows="3" placeholder="请输入修正原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCorrectDialog = false">取消</el-button>
        <el-button type="primary" :loading="correcting" @click="handleCorrect">确认修正</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getScoreResults, saveTeacherScores, publishScore, returnSubmission, startScore, getSubmission, getObjectiveScore, correctScore } from '@/api/task'
import { getParseStatusType, getParseStatusLabel } from '@/utils/status'
import { formatDate } from '@/utils/date'
import type { ScoreResult, Submission } from '@/types'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const aiScoring = ref(false)
const comment = ref('')
const submission = ref<Submission | null>(null)
const scores = ref<(ScoreResult & { maxScore?: number })[]>([])
const submissionId = computed(() => Number(route.params.id) || 0)
const polling = ref<number | null>(null)

// 客观评分
const showObjectiveDialog = ref(false)
const objectiveLoading = ref(false)
const objectiveScore = ref<{ scores: Record<string, number>; total: number } | null>(null)

// 成绩修正
const showCorrectDialog = ref(false)
const correcting = ref(false)
const correctForm = ref<{ indicatorId?: number; newScore: number; reason: string }>({
  indicatorId: undefined,
  newScore: 0,
  reason: '',
})

async function loadSubmission() {
  if (!submissionId.value) return
  try {
    const res = await getSubmission(submissionId.value)
    submission.value = res.data
    comment.value = res.data.teacherComment || ''
  } catch {
    ElMessage.error('加载提交信息失败')
  }
}

async function loadScores() {
  if (!submissionId.value) return
  loading.value = true
  try {
    const res = await getScoreResults(submissionId.value)
    scores.value = res.data
  } catch {
    ElMessage.error('加载评分数据失败')
  } finally {
    loading.value = false
  }
}

async function handleAiScore() {
  aiScoring.value = true
  try {
    await startScore(submissionId.value)
    ElMessage.success('AI 评分任务已启动')
    startScorePolling()
  } catch {
    ElMessage.error('AI 评分失败，请检查是否已关联评分模板')
  } finally {
    if (polling.value === null) aiScoring.value = false
  }
}

function startScorePolling() {
  if (polling.value !== null) return
  polling.value = window.setInterval(async () => {
    const res = await getSubmission(submissionId.value)
    submission.value = res.data // 轮询时顺便更新 submission 信息（如总分、状态）
    if (res.data.scoreStatus !== 'SCORING') {
      stopScorePolling()
      aiScoring.value = false
      await loadScores()
      if (res.data.scoreStatus === 'AI_SCORED') ElMessage.success('AI 评分完成')
      if (res.data.scoreStatus === 'SCORE_FAILED') ElMessage.error('AI 评分失败')
    }
  }, 3000)
}

function stopScorePolling() {
  if (polling.value !== null) {
    window.clearInterval(polling.value)
    polling.value = null
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveTeacherScores(submissionId.value, { scores: scores.value, comment: comment.value })
    ElMessage.success('保存成功')
    await loadSubmission() // 重新加载以获取最新总分
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  try {
    await ElMessageBox.confirm('确认发布成绩？发布后学生可查看评价结果。', '确认发布')
  } catch { return }
  saving.value = true
  try {
    await saveTeacherScores(submissionId.value, { scores: scores.value, comment: comment.value })
    await publishScore(submissionId.value)
    ElMessage.success('成绩已发布')
    router.back()
  } catch {
    ElMessage.error('发布失败')
  } finally {
    saving.value = false
  }
}

async function handleReturn() {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入退回原因', '退回提交', {
      inputPattern: /.+/,
      inputErrorMessage: '请输入退回原因',
    })
    await returnSubmission(submissionId.value, reason)
    ElMessage.success('已退回')
    router.back()
  } catch {
    // 用户取消
  }
}

// 查看客观评分
async function handleViewObjective() {
  objectiveLoading.value = true
  try {
    const res = await getObjectiveScore(submissionId.value)
    objectiveScore.value = res.data
    showObjectiveDialog.value = true
  } catch {
    ElMessage.error('加载客观评分失败')
  } finally {
    objectiveLoading.value = false
  }
}

// 修正成绩
async function handleCorrect() {
  if (!correctForm.value.reason.trim()) {
    ElMessage.warning('请输入修正原因')
    return
  }
  correcting.value = true
  try {
    await correctScore(submissionId.value, correctForm.value)
    ElMessage.success('成绩修正成功')
    showCorrectDialog.value = false
    // 重新加载评分和提交信息
    await loadScores()
    await loadSubmission()
  } catch {
    ElMessage.error('成绩修正失败')
  } finally {
    correcting.value = false
  }
}

onMounted(() => {
  loadSubmission()
  loadScores()
})
onBeforeUnmount(stopScorePolling)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.auto-score {
  color: #409eff;
  font-weight: bold;
}
</style>
