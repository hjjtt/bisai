<template>
  <div class="student-result">
    <el-page-header @back="$router.back()" title="返回" content="评价结果" />
    <el-card style="margin-top: 16px" v-loading="loading">
      <template v-if="scores.length > 0">
        <!-- 总分 -->
        <div class="score-summary">
          <span class="score-label">最终得分</span>
          <span class="score-value">{{ submission?.totalScore ?? '--' }}</span>
        </div>

        <!-- 各项得分 -->
        <el-table :data="scores" stripe style="margin-top: 20px">
          <el-table-column prop="indicatorName" label="评价指标" />
          <el-table-column prop="finalScore" label="得分" width="100" />
          <el-table-column prop="reason" label="评分理由" min-width="200" />
        </el-table>

        <!-- 教师评语 -->
        <el-divider />
        <div v-if="teacherComment">
          <h4>教师评语</h4>
          <p class="comment-text">{{ teacherComment }}</p>
        </div>

        <!-- 操作按钮 -->
        <div style="margin-top: 20px; text-align: right">
          <el-button type="primary" @click="downloadReport('PDF')">下载 PDF 报告</el-button>
          <el-button @click="downloadReport('WORD')">下载 Word 报告</el-button>
        </div>
      </template>

      <el-empty v-else description="暂无评价结果" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSubmission, getScoreResults } from '@/api/task'
import { exportStudentReport } from '@/api/report'
import type { Submission, ScoreResult } from '@/types'

const route = useRoute()
const loading = ref(false)
const submission = ref<Submission | null>(null)
const scores = ref<ScoreResult[]>([])
const teacherComment = ref('')

const submissionId = computed(() => Number(route.params.submissionId) || 0)

async function loadData() {
  if (!submissionId.value) return
  loading.value = true
  try {
    const [subRes, scoreRes] = await Promise.all([
      getSubmission(submissionId.value),
      getScoreResults(submissionId.value),
    ])
    submission.value = subRes.data
    scores.value = scoreRes.data
  } catch (e) {
    console.error('加载评价结果失败:', e)
    ElMessage.error('加载评价结果失败')
  } finally {
    loading.value = false
  }
}

async function downloadReport(format: 'PDF' | 'WORD') {
  try {
    const res = await exportStudentReport(submissionId.value, format)
    window.open(`/api/files/${res.data.fileId}/download`)
  } catch (e) {
    console.error('报告导出失败:', e)
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
  }
}

.comment-text {
  line-height: 1.8;
  color: #606266;
}
</style>
