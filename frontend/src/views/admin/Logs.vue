<template>
  <div class="system-logs">
    <el-card>
      <template #header><span>系统日志</span></template>

      <el-tabs v-model="activeTab">
        <!-- 操作日志 -->
        <el-tab-pane label="操作日志" name="operation">
          <el-table :data="operationLogs" stripe v-loading="loading">
            <el-table-column prop="username" label="操作人" width="100" />
            <el-table-column prop="action" label="操作类型" width="150" />
            <el-table-column prop="description" label="操作描述" min-width="200" />
            <el-table-column prop="ip" label="IP 地址" width="140" />
            <el-table-column label="操作时间" width="170">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 模型调用日志 -->
        <el-tab-pane label="模型调用日志" name="model">
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
        </el-tab-pane>
      </el-tabs>

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
import { ref, reactive, watch, onMounted } from 'vue'
import { getOperationLogs, getModelCallLogs } from '@/api/system'
import { formatDate } from '@/utils/date'
import type { PageResponse } from '@/types'

interface OperationLog {
  username: string
  action: string
  description: string
  ip: string
  createdAt: string
}

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

const activeTab = ref('operation')
const loading = ref(false)
const operationLogs = ref<OperationLog[]>([])
const modelLogs = ref<ModelCallLog[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })

async function loadLogs() {
  loading.value = true
  try {
    if (activeTab.value === 'operation') {
      const res = await getOperationLogs({ page: pagination.page, size: pagination.size })
      const data = res.data as PageResponse<OperationLog>
      operationLogs.value = data.items || []
      pagination.total = data.total || 0
    } else {
      const res = await getModelCallLogs({ page: pagination.page, size: pagination.size })
      const data = res.data as PageResponse<ModelCallLog>
      modelLogs.value = data.items || []
      pagination.total = data.total || 0
    }
  } catch (e) {
    console.error('加载日志失败:', e)
  } finally {
    loading.value = false
  }
}

watch(activeTab, () => {
  pagination.page = 1
  loadLogs()
})

onMounted(loadLogs)
</script>
