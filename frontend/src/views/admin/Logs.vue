<template>
  <div class="system-logs">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>模型调用日志</span>
          <el-button type="danger" size="small" @click="handleClearLogs">清空日志</el-button>
        </div>
      </template>

      <el-table :data="modelLogs" stripe v-loading="loading">
        <el-table-column prop="model" label="模型类型" width="150" />
        <el-table-column prop="callType" label="调用类型" width="100" />
        <el-table-column prop="inputTokens" label="输入 Token" width="100" />
        <el-table-column prop="outputTokens" label="输出 Token" width="100" />
        <el-table-column prop="totalTokens" label="总 Token" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="调用时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && modelLogs.length === 0" description="暂无日志数据" />

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @change="loadLogs"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModelCallLogs, clearModelCallLogs } from '@/api/system'
import { formatDate } from '@/utils/date'
import type { PageResponse } from '@/types'

interface ModelCallLog {
  model: string
  callType: string
  inputTokens: number
  outputTokens: number
  totalTokens: number
  success: boolean
  errorMessage?: string
  createdAt: string
}

const loading = ref(false)
const modelLogs = ref<ModelCallLog[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })

async function loadLogs() {
  loading.value = true
  try {
    const res = await getModelCallLogs({ page: pagination.page, size: pagination.size })
    const data = res.data as PageResponse<ModelCallLog>
    modelLogs.value = data.items || []
    pagination.total = data.total || 0
  } catch (e) {
    console.error('加载日志失败:', e)
  } finally {
    loading.value = false
  }
}

async function handleClearLogs() {
  try {
    await ElMessageBox.confirm('确定要清空所有模型调用日志吗？此操作不可恢复。', '清空确认', {
      confirmButtonText: '确认清空',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await clearModelCallLogs()
    ElMessage.success('日志已清空')
    loadLogs()
  } catch {
    // 用户取消
  }
}

onMounted(loadLogs)
</script>
