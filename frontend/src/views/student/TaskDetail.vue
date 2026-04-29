<template>
  <div class="task-detail">
    <el-page-header @back="$router.back()" title="返回" :content="task?.title || '任务详情'" />
    <el-card style="margin-top: 16px" v-loading="loading">
      <el-descriptions :column="2" border v-if="task">
        <el-descriptions-item label="任务名称">{{ task.title }}</el-descriptions-item>
        <el-descriptions-item label="所属课程">{{ task.courseName }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ task.startTime }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ task.endTime }}</el-descriptions-item>
        <el-descriptions-item label="允许重新提交">
          <el-tag :type="task.allowResubmit ? 'success' : 'danger'">{{ task.allowResubmit ? '是' : '否' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag>{{ task.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="允许文件类型" :span="2">
          <el-tag v-for="ft in (task.allowedFileTypes || [])" :key="ft" size="small" style="margin-right: 4px">{{ ft }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="实训要求" :span="2">
          <div class="requirements">{{ task.requirements }}</div>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 提交状态 -->
      <el-divider />
      <div v-if="mySubmission" class="submit-status">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="提交状态">已提交（版本 {{ mySubmission.version }}）</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ mySubmission.submitTime }}</el-descriptions-item>
          <el-descriptions-item label="评分状态">
            <el-tag :type="getScoreStatusType(mySubmission.scoreStatus)" size="small">{{ mySubmission.scoreStatus }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px; text-align: right">
          <el-button v-if="mySubmission.scoreStatus === 'PUBLISHED'" type="success" @click="$router.push(`/student/result/${mySubmission.id}`)">查看评价结果</el-button>
          <el-button v-if="task?.allowResubmit && task?.status === 'PUBLISHED'" type="warning" @click="$router.push(`/student/submit/${taskId}`)">重新提交</el-button>
        </div>
      </div>

      <div style="margin-top: 20px; text-align: right" v-if="task?.status === 'PUBLISHED' && !mySubmission">
        <el-button type="primary" @click="$router.push(`/student/submit/${taskId}`)">提交成果</el-button>
      </div>

      <el-empty v-if="!task && !loading" description="任务不存在或已关闭" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTask, getSubmissions } from '@/api/task'
import { getUserInfo } from '@/utils/auth'
import type { TrainingTask, Submission } from '@/types'

const route = useRoute()
const loading = ref(false)
const task = ref<TrainingTask | null>(null)
const mySubmission = ref<Submission | null>(null)

const taskId = computed(() => Number(route.params.id) || 0)

function getScoreStatusType(status: string) {
  const map: Record<string, string> = {
    NOT_SCORED: 'info', SCORING: 'warning', AI_SCORED: '', TEACHER_CONFIRMED: 'success',
    PUBLISHED: 'success', SCORE_FAILED: 'danger', RETURNED: 'warning',
  }
  return map[status] || 'info'
}

async function loadTask() {
  if (!taskId.value) return
  loading.value = true
  try {
    const res = await getTask(taskId.value)
    task.value = res.data
  } catch {
    ElMessage.error('加载任务失败')
  } finally {
    loading.value = false
  }
}

async function loadMySubmission() {
  if (!taskId.value) return
  try {
    const userInfo = getUserInfo()
    if (!userInfo?.id) return
    const res = await getSubmissions({ taskId: taskId.value, studentId: userInfo.id, size: 1 })
    const items = res.data.items
    if (items.length > 0) {
      mySubmission.value = items[0]
    }
  } catch {
    // 忽略：可能还没有提交过
  }
}

onMounted(() => {
  loadTask()
  loadMySubmission()
})
</script>

<style scoped>
.requirements {
  white-space: pre-wrap;
  line-height: 1.8;
}
.submit-status {
  margin-top: 8px;
}
</style>
