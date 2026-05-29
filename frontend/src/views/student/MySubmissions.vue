<template>
  <div class="my-submissions">
    <section class="page-hero">
      <div>
        <div class="eyebrow">
          <el-icon><Files /></el-icon>
          <span>我的提交</span>
        </div>
        <h2>跟踪成果处理进度</h2>
        <p>查看每次提交的解析、核查与评分状态，成绩发布后可进入详情页查看反馈。</p>
      </div>
      <div class="hero-stats">
        <div>
          <strong>{{ pagination.total }}</strong>
          <span>提交总数</span>
        </div>
      </div>
    </section>

    <section class="list-panel">
      <div class="toolbar">
        <div>
          <h3>提交记录</h3>
          <p>按最新提交顺序查看处理结果。</p>
        </div>
        <el-button type="primary" :icon="Document" @click="router.push('/student/tasks')">继续提交任务</el-button>
      </div>

      <el-table :data="submissions" v-loading="loading">
        <el-table-column label="任务名称" min-width="220">
          <template #default="{ row }">
            <div class="task-name">
              <strong>{{ row.taskTitle || '未命名任务' }}</strong>
              <span>{{ formatDate(row.submitTime) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="版本" width="90" align="center">
          <template #default="{ row }">
            <span class="version-pill">V{{ row.version }}</span>
          </template>
        </el-table-column>
        <el-table-column label="处理进度" min-width="300">
          <template #default="{ row }">
            <div class="status-chain">
              <el-tag :type="getParseStatusType(row.parseStatus)" effect="light">{{ getParseStatusLabel(row.parseStatus) }}</el-tag>
              <el-tag :type="getCheckStatusType(row.checkStatus)" effect="light">{{ getCheckStatusLabel(row.checkStatus) }}</el-tag>
              <el-tag :type="getScoreStatusType(row.scoreStatus)" effect="light">{{ getScoreStatusLabel(row.scoreStatus) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最终得分" width="110" align="right">
          <template #default="{ row }">
            <span class="score-text">{{ row.totalScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" :icon="Document" @click="router.push(`/student/tasks/${row.taskId}`)">任务</el-button>
            <el-button
              v-if="row.scoreStatus === 'PUBLISHED'"
              size="small"
              type="success"
              :icon="Medal"
              @click="router.push(`/student/result/${row.id}`)"
            >
              成绩
            </el-button>
            <el-button
              v-else-if="row.scoreStatus !== 'NOT_SCORED'"
              size="small"
              type="warning"
              :icon="Clock"
              @click="router.push(`/student/result/${row.id}`)"
            >
              进度
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && submissions.length === 0" description="暂无提交记录，快去完成任务吧" :image-size="96" />

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @change="loadSubmissions"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Clock, Document, Files, Medal } from '@element-plus/icons-vue'
import { getSubmissions } from '@/api/task'
import {
  getParseStatusType,
  getParseStatusLabel,
  getCheckStatusType,
  getCheckStatusLabel,
  getScoreStatusType,
  getScoreStatusLabel,
} from '@/utils/status'
import { formatDate } from '@/utils/date'
import { getUserInfo } from '@/utils/auth'
import type { Submission } from '@/types'

const router = useRouter()
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
  } catch {
    // 请求错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

onMounted(loadSubmissions)
</script>

<style lang="scss" scoped>
.my-submissions {
  max-width: 1280px;
  margin: 0 auto;
}

.page-hero,
.list-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
  padding: 28px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(242, 248, 255, 0.94)),
    url('@/assets/hero.png') right center / auto 100% no-repeat;

  .eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    color: #2563eb;
    font-size: 13px;
    font-weight: 600;
  }

  h2 {
    margin-bottom: 8px;
    color: #0f172a;
    font-size: 28px;
    font-weight: 700;
  }

  p {
    color: #52657a;
    font-size: 14px;
    line-height: 1.8;
  }
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, 96px);
  gap: 10px;

  div {
    display: flex;
    min-height: 92px;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 1px solid #dbeafe;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.84);
  }

  strong {
    color: #0f172a;
    font-size: 28px;
    line-height: 1;
  }

  span {
    margin-top: 8px;
    color: #64748b;
    font-size: 12px;
  }
}

.list-panel {
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid #eef2f7;

  h3 {
    margin-bottom: 6px;
    color: #0f172a;
    font-size: 18px;
    font-weight: 700;
  }

  p {
    color: #64748b;
    font-size: 13px;
  }
}

:deep(.el-table) {
  --el-table-border-color: #eef2f7;
  --el-table-header-bg-color: #ffffff;
  --el-table-row-hover-bg-color: #f8fbff;
}

:deep(.el-table__header th) {
  height: 54px;
  color: #6b7280;
  font-weight: 600;
}

:deep(.el-table__body td) {
  height: 68px;
}

.task-name {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;

  strong {
    overflow: hidden;
    color: #111827;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: #64748b;
    font-size: 12px;
  }
}

.version-pill {
  display: inline-flex;
  min-width: 42px;
  height: 26px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.status-chain {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.score-text {
  color: #2563eb;
  font-size: 16px;
  font-weight: 700;
}

.el-pagination {
  justify-content: flex-end;
  padding: 18px 24px 22px;
}

@media (max-width: 920px) {
  .page-hero,
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-hero {
    padding: 22px;
    background: #ffffff;
  }

  .hero-stats {
    width: 100%;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
