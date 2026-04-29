<template>
  <div class="student-home">
    <!-- 欢迎区域 -->
    <header class="welcome-section">
      <div class="welcome-text">
        <h2>你好，{{ userInfo?.realName || '学生' }}</h2>
        <p class="subtitle">欢迎回到实训成果智能核查系统，开始今天的学习吧。</p>
      </div>
      <div class="date-display">
        <span class="day">{{ today.getDate() }}</span>
        <div class="month-year">
          <span class="month">{{ today.getMonth() + 1 }}月</span>
          <span class="year">{{ today.getFullYear() }}</span>
        </div>
      </div>
    </header>

    <el-row :gutter="20" class="stat-grid">
      <el-col :span="6" v-for="item in statCards" :key="item.title">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">{{ item.title }}</div>
          <div class="stat-content">
            <span class="stat-number">{{ item.value }}</span>
            <span class="stat-unit">项</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="section-container">
      <div class="section-header">
        <h3 class="title">近期实训任务</h3>
        <el-button type="primary" link @click="$router.push('/student/tasks')">查看全部</el-button>
      </div>
      
      <el-card shadow="never" class="task-card-wrapper">
        <el-table :data="recentTasks" style="width: 100%">
          <el-table-column prop="title" label="任务名称" min-width="200" />
          <el-table-column prop="courseName" label="所属课程" width="180" />
          <el-table-column prop="endTime" label="截止日期" width="180" />
          <el-table-column label="提交状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getSubmitStatusType(row.submitStatus)" size="small">
                {{ row.submitStatus }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="评价得分" width="100" align="right">
            <template #default="{ row }">
              <span class="score-text">{{ row.score || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="recentTasks.length === 0" class="empty-state">
          <el-empty description="暂无进行中的实训任务" :image-size="100" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { getStudentStats } from '@/api/dashboard'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const today = new Date()

const statCards = ref([
  { title: '进行中任务', value: 0 },
  { title: '已提交成果', value: 0 },
  { title: '待评价反馈', value: 0 },
  { title: '系统通知', value: 0 },
])

const recentTasks = ref<any[]>([])

function getSubmitStatusType(status: string) {
  const map: Record<string, string> = { '已提交': 'success', '未提交': 'info', '待提交': 'warning' }
  return map[status] || 'info'
}

async function loadStats() {
  try {
    const res = await getStudentStats()
    const d = res.data as any
    statCards.value[0].value = d.ongoingTasks
    statCards.value[1].value = d.submittedCount
    statCards.value[2].value = d.pendingFeedback
    statCards.value[3].value = d.unreadMessages
    recentTasks.value = d.recentTasks || []
  } catch { /* ignore */ }
}

onMounted(loadStats)
</script>

<style lang="scss" scoped>
.student-home {
  max-width: 1400px;
  margin: 0 auto;
}

.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e2e8f0;

  .welcome-text {
    h2 {
      font-size: 28px;
      color: #0f172a;
      margin-bottom: 8px;
    }
    .subtitle {
      color: #64748b;
      font-size: 14px;
    }
  }

  .date-display {
    display: flex;
    align-items: center;
    gap: 12px;
    color: #1e293b;
    
    .day {
      font-size: 40px;
      font-weight: 700;
      line-height: 1;
    }
    .month-year {
      display: flex;
      flex-direction: column;
      font-size: 12px;
      font-weight: 600;
      border-left: 2px solid #3b82f6;
      padding-left: 12px;
      color: #64748b;
    }
  }
}

.stat-grid {
  margin-bottom: 32px;
}

.stat-card {
  .stat-label {
    font-size: 13px;
    color: #64748b;
    margin-bottom: 12px;
  }
  .stat-content {
    display: flex;
    align-items: baseline;
    gap: 4px;
    .stat-number {
      font-size: 28px;
      font-weight: 700;
      color: #0f172a;
    }
    .stat-unit {
      font-size: 12px;
      color: #94a3b8;
    }
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  .title {
    font-size: 16px;
    font-weight: 600;
    color: #1e293b;
    position: relative;
    padding-left: 12px;
    &::before {
      content: "";
      position: absolute;
      left: 0;
      top: 4px;
      bottom: 4px;
      width: 4px;
      background: #3b82f6;
      border-radius: 2px;
    }
  }
}

.score-text {
  font-weight: 600;
  color: #3b82f6;
}

.empty-state {
  padding: 40px 0;
}
</style>
