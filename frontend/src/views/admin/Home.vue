<template>
  <div class="admin-home">
    <!-- 统计卡片区 -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in statCards" :key="item.title">
        <el-card shadow="never" class="stat-card">
          <div class="card-body">
            <div class="stat-info">
              <div class="stat-label">{{ item.title }}</div>
              <div class="stat-value" v-if="statsLoading">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
              <div class="stat-value" v-else>{{ item.value.toLocaleString() }}</div>
            </div>
            <div class="stat-icon" :style="{ background: item.color + '15', color: item.color }">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
          </div>
          <div class="card-footer">
            <span class="trend" :class="item.trend >= 0 ? 'up' : 'down'">
              <el-icon><CaretTop v-if="item.trend >= 0" /><CaretBottom v-else /></el-icon>
              {{ item.trend >= 0 ? '+' : '' }}{{ item.trend }}%
            </span>
            <span class="footer-text">较上期统计</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表与列表区 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :span="16">
        <el-card shadow="never" v-loading="statsLoading">
          <template #header>
            <div class="card-header">
              <span class="title">系统核查概览</span>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button value="7d">近7天</el-radio-button>
                <el-radio-button value="30d">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="chartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="status-card">
          <template #header>
            <div class="card-header">
              <span class="title">服务运行状态</span>
            </div>
          </template>
          <div class="status-list">
            <div class="status-item" v-for="status in systemStatus" :key="status.name">
              <div class="status-name">
                <el-badge is-dot :type="status.type" />
                <span>{{ status.name }}</span>
              </div>
              <el-tag :type="status.type" size="small" effect="plain">{{ status.text }}</el-tag>
            </div>
          </div>
          <el-empty v-if="systemStatus.length === 0 && !statsLoading" description="暂无状态数据" :image-size="60" />
          <div class="usage-stats">
            <div class="usage-item">
              <div class="usage-header">
                <span>API 调用频率 (每日)</span>
                <span>{{ apiUsage }}%</span>
              </div>
              <el-progress :percentage="apiUsage" :show-text="false" />
            </div>
            <div class="usage-item">
              <div class="usage-header">
                <span>服务器负载</span>
                <span>{{ serverLoad }}%</span>
              </div>
              <el-progress :percentage="serverLoad" :show-text="false" :status="serverLoad < 50 ? 'success' : 'warning'" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近日志 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="title">最近操作日志</span>
              <el-button type="primary" link @click="$router.push('/admin/logs')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="logs" style="width: 100%" size="small" v-loading="statsLoading">
            <el-table-column prop="time" label="操作时间" width="180" />
            <el-table-column prop="user" label="操作员" width="150" />
            <el-table-column prop="type" label="操作类型" width="150">
              <template #default="scope">
                <el-tag size="small">{{ scope.row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="详情" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === '失败' ? 'danger' : 'success'" size="small" effect="dark">
                  {{ row.status || '成功' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!statsLoading && logs.length === 0" description="暂无操作日志" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { Loading } from '@element-plus/icons-vue'
import { getAdminStats } from '@/api/dashboard'
import type { SystemStatusItem, LogEntry, BaseStats } from '@/types'

const timeRange = ref('7d')
const chartRef = ref<HTMLElement>()
const statsLoading = ref(true)
let chartInstance: echarts.ECharts | null = null

const statCards = ref([
  { title: '用户总数', value: 0, trend: 0, icon: 'User', color: '#3b82f6' },
  { title: '活跃班级', value: 0, trend: 0, icon: 'School', color: '#10b981' },
  { title: '核查任务', value: 0, trend: 0, icon: 'Document', color: '#f59e0b' },
  { title: '系统异常', value: 0, trend: 0, icon: 'Warning', color: '#ef4444' },
])

const systemStatus = ref<SystemStatusItem[]>([])
const logs = ref<LogEntry[]>([])
const apiUsage = ref(0)
const serverLoad = ref(0)

async function loadStats() {
  statsLoading.value = true
  try {
    const res = await getAdminStats()
    const d = res.data as BaseStats
    statCards.value[0].value = d.userCount || 0
    statCards.value[0].trend = d.userTrend || 0
    statCards.value[1].value = d.classCount || 0
    statCards.value[1].trend = d.classTrend || 0
    statCards.value[2].value = d.taskCount || 0
    statCards.value[2].trend = d.taskTrend || 0
    statCards.value[3].value = d.todayError || 0
    statCards.value[3].trend = d.errorTrend || 0
    systemStatus.value = d.systemStatus || []
    logs.value = d.recentLogs || []
    apiUsage.value = d.apiUsage || 0
    serverLoad.value = d.serverLoad || 0
    initChart(d)
  } catch (e) {
    console.error('加载管理统计失败:', e)
  } finally {
    statsLoading.value = false
  }
}

function initChart(data: BaseStats) {
  if (!chartRef.value) return

  chartInstance = echarts.init(chartRef.value)

  const option: echarts.EChartsOption = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['提交数', '解析成功', '评分完成'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.dates || [],
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '提交数',
        type: 'line',
        smooth: true,
        data: data.submissions || [],
        itemStyle: { color: '#3b82f6' },
      },
      {
        name: '解析成功',
        type: 'line',
        smooth: true,
        data: data.parsed || [],
        itemStyle: { color: '#10b981' },
      },
      {
        name: '评分完成',
        type: 'line',
        smooth: true,
        data: data.scored || [],
        itemStyle: { color: '#f59e0b' },
      },
    ],
  }

  chartInstance.setOption(option)
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  chartInstance?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.admin-home {
  max-width: 1400px;
  margin: 0 auto;
}

.mt-20 {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  .title {
    font-size: 15px;
    font-weight: 600;
    color: #1e293b;
  }
}

.stat-card {
  .card-body {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .stat-label {
      font-size: 13px;
      color: #64748b;
      margin-bottom: 4px;
    }
    .stat-value {
      font-size: 24px;
      font-weight: 700;
      color: #0f172a;
    }
    .stat-icon {
      width: 44px;
      height: 44px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
    }
  }

  .card-footer {
    border-top: 1px solid #f1f5f9;
    padding-top: 12px;
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 8px;

    .trend {
      font-weight: 600;
      display: flex;
      align-items: center;
      &.up { color: #10b981; }
    }
    .footer-text {
      color: #94a3b8;
    }
  }
}

.chart-container {
  height: 300px;
  width: 100%;
}

.status-card {
  .status-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 24px;

    .status-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      .status-name {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: #475569;
      }
    }
  }

  .usage-stats {
    display: flex;
    flex-direction: column;
    gap: 16px;
    .usage-item {
      .usage-header {
        display: flex;
        justify-content: space-between;
        font-size: 12px;
        color: #64748b;
        margin-bottom: 6px;
      }
    }
  }
}
</style>
