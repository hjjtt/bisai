<template>
  <div class="messages">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>消息中心</span>
          <div class="header-actions">
            <el-statistic v-if="unreadCount > 0" :value="unreadCount" title="未读消息" class="unread-stat" />
            <el-button type="primary" @click="handleMarkAllRead" :disabled="unreadCount === 0">全部标记已读</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选区 -->
      <div class="filter-bar">
        <el-select v-model="filter.type" placeholder="按类型筛选" clearable style="width: 160px" @change="loadMessages">
          <el-option label="提交通知" value="SUBMIT" />
          <el-option label="重提通知" value="RESUBMIT" />
          <el-option label="评分完成" value="SCORE_COMPLETE" />
          <el-option label="成绩发布" value="SCORE_PUBLISH" />
          <el-option label="批量失败" value="BATCH_FAIL" />
          <el-option label="配额警告" value="QUOTA_WARNING" />
        </el-select>
        <el-select v-model="filter.isRead" placeholder="按已读状态筛选" clearable style="width: 160px" @change="loadMessages">
          <el-option label="未读" :value="false" />
          <el-option label="已读" :value="true" />
        </el-select>
      </div>

      <!-- 消息列表 -->
      <el-table :data="messages" stripe v-loading="loading" row-class-name="message-row">
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getMessageTypeType(row.type)" size="small">{{ getMessageTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'unread-title': !row.isRead }">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'unread-content': !row.isRead }">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRead ? 'info' : 'danger'" size="small">
              {{ row.isRead ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="!row.isRead" type="primary" link @click="handleMarkRead(row.id)">标记已读</el-button>
            <span v-else class="read-hint">已读</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && messages.length === 0" description="暂无消息" />

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadMessages"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getMessages, markMessageRead, markAllMessagesRead, getUnreadCount } from '@/api/message'
import { getMessageTypeType, getMessageTypeLabel } from '@/utils/status'
import { formatDate } from '@/utils/date'
import { useAppStore } from '@/store/app'
import type { Message } from '@/types'

const appStore = useAppStore()
const loading = ref(false)
const messages = ref<Message[]>([])
const unreadCount = computed(() => appStore.unreadMessageCount)
const pagination = reactive({ page: 1, size: 20, total: 0 })
const filter = reactive({
  type: undefined as string | undefined,
  isRead: undefined as boolean | undefined,
})

async function loadMessages() {
  loading.value = true
  try {
    const res = await getMessages({
      page: pagination.page,
      size: pagination.size,
      type: filter.type,
      isRead: filter.isRead,
    })
    messages.value = res.data.items
    pagination.total = res.data.total
  } catch (e) {
    console.error('加载消息列表失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    appStore.unreadMessageCount = res.data
  } catch (e) {
    console.error('加载未读数量失败:', e)
  }
}

async function handleMarkRead(id: number) {
  try {
    await markMessageRead(id)
    ElMessage.success('已标记为已读')
    const msg = messages.value.find(m => m.id === id)
    if (msg) msg.isRead = true
    appStore.unreadMessageCount = Math.max(0, appStore.unreadMessageCount - 1)
  } catch (e) {
    console.error('标记已读失败:', e)
  }
}

async function handleMarkAllRead() {
  try {
    await markAllMessagesRead()
    ElMessage.success('全部标记为已读')
    messages.value.forEach(m => m.isRead = true)
    appStore.unreadMessageCount = 0
  } catch (e) {
    console.error('全部标记已读失败:', e)
  }
}

onMounted(() => {
  loadMessages()
  loadUnreadCount()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.unread-stat :deep(.el-statistic__head) {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.unread-stat :deep(.el-statistic__number) {
  color: var(--el-color-danger);
  font-size: 20px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.unread-title {
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.unread-content {
  color: var(--el-text-color-regular);
}

.read-hint {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

:deep(.el-table__row.unread-row) {
  background-color: var(--el-color-danger-light-9);
}
</style>
