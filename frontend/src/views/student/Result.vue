<template>
  <div class="student-result">
    <el-page-header @back="$router.back()" title="返回" content="评价结果" />
    <el-card style="margin-top: 16px" v-loading="loading">
      <template v-if="hasActualScores || submission?.autoTotalScore != null">
        <!-- AI 得分 -->
        <div class="score-summary">
          <span class="score-label">AI 智能评分</span>
          <span class="score-value score-ai">{{ submission?.autoTotalScore ?? '--' }}</span>
        </div>

        <!-- 最终得分（教师确认后） -->
        <div v-if="submission?.scoreStatus === 'TEACHER_CONFIRMED' || submission?.scoreStatus === 'PUBLISHED'" class="score-summary" style="margin-top: 12px">
          <span class="score-label">最终得分</span>
          <span class="score-value">{{ submission?.totalScore ?? '--' }}</span>
        </div>

        <!-- 各项得分 -->
        <el-table :data="scores" stripe style="margin-top: 20px">
          <el-table-column prop="indicatorName" label="评价指标" />
          <el-table-column prop="autoScore" label="AI评分" width="100" />
          <el-table-column v-if="showTeacherScore" prop="teacherScore" label="教师评分" width="100" />
          <el-table-column prop="reason" label="评分理由" min-width="200" />
        </el-table>
      </template>

      <!-- 退回/评语信息 -->
      <template v-if="teacherComment">
        <el-divider />
        <div :class="['comment-block', submission?.scoreStatus === 'RETURNED' ? 'returned' : '']">
          <h4>{{ submission?.scoreStatus === 'RETURNED' ? '退回原因' : '教师评语' }}</h4>
          <p class="comment-text">{{ teacherComment }}</p>
        </div>
      </template>

      <!-- 操作按钮（仅已发布时显示下载） -->
      <div v-if="submission?.scoreStatus === 'PUBLISHED'" style="margin-top: 20px; text-align: right">
        <el-button type="primary" @click="downloadReport('PDF')">下载 PDF 报告</el-button>
        <el-button @click="downloadReport('WORD')">下载 Word 报告</el-button>
      </div>

      <el-empty v-if="!hasActualScores && !teacherComment" description="暂无评价结果" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSubmission, getStudentScores } from '@/api/task'
import { exportStudentReport } from '@/api/report'
import { downloadFile } from '@/api/system'
import type { Submission, ScoreResult } from '@/types'

const route = useRoute()
const loading = ref(false)
const submission = ref<Submission | null>(null)
const scores = ref<ScoreResult[]>([])
const teacherComment = ref('')

const submissionId = computed(() => Number(route.params.submissionId) || 0)

// 是否有实际评分（至少一条记录有分数）
const hasActualScores = computed(() =>
  scores.value.some(s => s.finalScore != null || s.autoScore != null || s.teacherScore != null)
)

// 是否显示教师评分列
const showTeacherScore = computed(() =>
  submission.value?.scoreStatus === 'TEACHER_CONFIRMED' || submission.value?.scoreStatus === 'PUBLISHED'
)

async function loadData() {
  if (!submissionId.value) return
  loading.value = true
  try {
    const [subRes, scoreRes] = await Promise.all([
      getSubmission(submissionId.value),
      getStudentScores(submissionId.value),
    ])
    submission.value = subRes.data
    scores.value = scoreRes.data
    teacherComment.value = subRes.data?.teacherComment || ''
  } catch (e) {
    ElMessage.error('加载评价结果失败')
  } finally {
    loading.value = false
  }
}

async function downloadReport(format: 'PDF' | 'WORD') {
  try {
    const res = await exportStudentReport(submissionId.value, format)
    await downloadFile(res.data.fileId)
  } catch (e) {
    ElMessage.error('报告导出失败')
  }
}

onMounted(loadData)
</script>

<style lang="scss" scoped>
.score-summary {
  text-align: center;
  padding: 20px 0;

  .score-label {
    display: block;
    font-size: 14px;
    color: #909399;
  }

  .score-value {
    font-size: 48px;
    font-weight: bold;
    color: #409eff;

    &.score-ai {
      color: #6366f1;
      font-size: 40px;
    }
  }
}

.comment-block {
  &.returned {
    h4 { color: #e6a23c; }
    .comment-text {
      padding: 12px 16px;
      border-radius: 6px;
      background: #fdf6ec;
      border: 1px solid #faecd8;
    }
  }
}

.comment-text {
  line-height: 1.8;
  color: #606266;
}
</style>
