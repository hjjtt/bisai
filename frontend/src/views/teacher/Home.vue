<template>
  <div class="teacher-home">
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in statCards" :key="item.title">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-info">
              <span class="stat-value">{{ item.value }}</span>
              <span class="stat-title">{{ item.title }}</span>
            </div>
            <el-icon :size="40" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待复核列表 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>待复核成果</span>
      </template>
      <el-table :data="pendingReviews" stripe>
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="title" label="任务名称" />
        <el-table-column prop="submitTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/teacher/submissions/${row.id}/score`)">
              去复核
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 高风险成果 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span style="color: #f56c6c">高风险成果</span>
      </template>
      <el-table :data="highRiskSubmissions" stripe>
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="title" label="任务名称" />
        <el-table-column prop="riskReason" label="风险说明" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/teacher/submissions/${row.id}/check`)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTeacherStats } from '@/api/dashboard'

const statCards = ref([
  { title: '待评价', value: 0, icon: 'EditPen', color: '#e6a23c' },
  { title: '待复核', value: 0, icon: 'Document', color: '#409eff' },
  { title: '高风险成果', value: 0, icon: 'Warning', color: '#f56c6c' },
  { title: '已完成', value: 0, icon: 'CircleCheck', color: '#67c23a' },
])

const pendingReviews = ref<any[]>([])
const highRiskSubmissions = ref<any[]>([])

async function loadStats() {
  try {
    const res = await getTeacherStats()
    const d = res.data as any
    statCards.value[0].value = d.pendingScore
    statCards.value[1].value = d.pendingReview
    statCards.value[2].value = d.highRisk
    statCards.value[3].value = d.completed
    pendingReviews.value = d.pendingReviews || []
    highRiskSubmissions.value = d.highRiskSubmissions || []
  } catch { /* ignore */ }
}

onMounted(loadStats)
</script>

<style lang="scss" scoped>
.stat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .stat-info {
    display: flex;
    flex-direction: column;

    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
    }

    .stat-title {
      font-size: 14px;
      color: #909399;
      margin-top: 4px;
    }
  }
}
</style>
