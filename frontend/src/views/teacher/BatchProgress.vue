<template>
  <div class="batch-progress">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>批量任务进度</span>
          <el-select v-model="selectedTaskId" placeholder="选择任务" @change="loadProgress" style="width: 200px">
            <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
          </el-select>
        </div>
      </template>

      <el-row :gutter="20" v-if="progress">
        <el-col :span="6">
          <el-statistic title="总数" :value="progress.total" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="成功" :value="progress.success" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="失败" :value="progress.failed" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="执行中" :value="progress.running" />
        </el-col>
      </el-row>

      <el-progress
        v-if="progress"
        :percentage="Math.round(((progress.success + progress.failed) / progress.total) * 100)"
        style="margin-top: 20px"
      />

      <el-empty v-else description="请选择任务查看进度" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTaskList, getBatchProgress } from '@/api/task'
import type { TrainingTask, BatchProgress } from '@/types'

const tasks = ref<TrainingTask[]>([])
const selectedTaskId = ref<number>()
const progress = ref<BatchProgress | null>(null)

async function loadTasks() {
  try {
    const res = await getTaskList({ size: 100 })
    tasks.value = res.data.items
  } catch (e) {
    console.error('加载任务列表失败:', e)
  }
}

async function loadProgress() {
  if (!selectedTaskId.value) return
  try {
    const res = await getBatchProgress(selectedTaskId.value)
    progress.value = res.data
  } catch (e) {
    console.error('加载进度失败:', e)
    ElMessage.error('加载进度失败')
  }
}

onMounted(loadTasks)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
