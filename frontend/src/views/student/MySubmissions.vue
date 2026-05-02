<template>
  <div class="my-submissions">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的提交记录</span>
        </div>
      </template>

      <el-table :data="submissions" stripe v-loading="loading">
        <el-table-column prop="taskTitle" label="任务名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="版本" width="80" align="center">
          <template #default="{ row }">V{{ row.version }}</template>
        </el-table-column>
        <el-table-column label="解析状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getParseStatusType(row.parseStatus)" size="small">{{ getParseStatusLabel(row.parseStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="核查状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getCheckStatusType(row.checkStatus)" size="small">{{ getCheckStatusLabel(row.checkStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getScoreStatusType(row.scoreStatus)" size="small">{{ getScoreStatusLabel(row.scoreStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/student/tasks/${row.taskId}`)">任务详情</el-button>
            <el-button v-if="row.scoreStatus === 'PUBLISHED'" type="success" link @click="$router.push(`/student/result/${row.id}`)">查看成绩</el-button>
            <el-button v-else-if="row.scoreStatus !== 'NOT_SCORED'" type="warning" link @click="$router.push(`/student/result/${row.id}`)">查看进度</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && submissions.length === 0" description="暂无提交记录，快去完成任务吧" />

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @change="loadSubmissions"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getSubmissions } from '@/api/task'
import { getParseStatusType, getParseStatusLabel, getCheckStatusType, getCheckStatusLabel, getScoreStatusType, getScoreStatusLabel } from '@/utils/status'
import { formatDate } from '@/utils/date'
import { getUserInfo } from '@/utils/auth'
import type { Submission } from '@/types'

const loading = ref(false)
const submissions = ref<Submission[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })

async function loadSubmissions() {
  loading.value = true
  try {
    const info = getUserInfo()
    if (!info?.id) return
    
    const res = await getSubmissions({ 
      page: pagination.page, 
      size: pagination.size, 
      studentId: info.id 
    })
    submissions.value = res.data.items
    pagination.total = res.data.total
  } catch (e) {
    console.error('加载提交记录失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadSubmissions)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
