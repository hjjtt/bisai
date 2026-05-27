<template>
  <div class="admin-home">
    <section class="hero-panel">
      <div class="hero-copy">
        <div class="eyebrow">
          <el-icon><Monitor /></el-icon>
          <span>管理员控制台</span>
        </div>
        <h2>系统运行与核查概览</h2>
        <p>集中查看用户、班级、核查任务和异常情况，确保 AI 核查流程稳定运行。</p>
        <div class="hero-actions">
          <el-button type="primary" :icon="User" @click="$router.push('/admin/users')">用户管理</el-button>
          <el-button :icon="School" @click="$router.push('/admin/classes')">班级课程</el-button>
          <el-button :icon="Setting" @click="$router.push('/admin/model-config')">模型配置</el-button>
        </div>
      </div>

      <div class="health-card">
        <div class="health-header">
          <span>服务健康度</span>
          <el-tag :type="healthType" effect="light">{{ healthLabel }}</el-tag>
        </div>
        <strong>{{ Math.max(0, 100 - serverLoad) }}%</strong>
        <p>综合服务器负载与接口调用频率，辅助判断当前系统压力。</p>
        <div class="health-metrics">
          <span>API {{ apiUsage }}%</span>
          <span>负载 {{ serverLoad }}%</span>
        </div>
      </div>
    </section>

    <section class="stat-grid">
      <article v-for="item in statCards" :key="item.title" class="stat-card" :class="item.tone">
        <div class="stat-main">
          <div class="stat-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div>
            <span>{{ item.title }}</span>
            <strong v-if="statsLoading"><el-icon class="is-loading"><Loading /></el-icon></strong>
            <strong v-else>{{ item.value.toLocaleString() }}</strong>
          </div>
        </div>
        <div class="stat-footer">
          <span class="trend" :class="item.trend >= 0 ? 'up' : 'down'">
            <el-icon><CaretTop v-if="item.trend >= 0" /><CaretBottom v-else /></el-icon>
            {{ item.trend >= 0 ? '+' : '' }}{{ item.trend }}%
          </span>
          <span>较上期</span>
        </div>
      </article>
    </section>

    <section class="content-grid">
      <div class="chart-panel" v-loading="statsLoading">
        <div class="section-header">
          <div>
            <h3>系统核查概览</h3>
            <p>提交、解析与评分完成趋势。</p>
          </div>
          <el-radio-group v-model="timeRange" size="small">
            <el-radio-button value="7d">近 7 天</el-radio-button>
            <el-radio-button value="30d">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="chartRef" class="chart-container"></div>
      </div>

      <aside class="status-panel">
        <div class="section-header compact">
          <div>
            <h3>服务运行状态</h3>
            <p>关键服务与资源占用。</p>
          </div>
        </div>

        <div class="status-list">
          <div class="status-item" v-for="status in systemStatus" :key="status.name">
            <div class="status-name">
              <el-badge is-dot :type="status.type" />
              <span>{{ status.name }}</span>
            </div>
            <el-tag :type="status.type" size="small" effect="light">{{ status.text }}</el-tag>
          </div>
        </div>
        <el-empty v-if="systemStatus.length === 0 && !statsLoading" description="暂无状态数据" :image-size="70" />

        <div class="usage-list">
          <div class="usage-item">
            <div class="usage-header">
              <span>API 调用频率</span>
              <strong>{{ apiUsage }}%</strong>
            </div>
            <el-progress :percentage="apiUsage" :show-text="false" />
          </div>
          <div class="usage-item">
            <div class="usage-header">
              <span>服务器负载</span>
              <strong>{{ serverLoad }}%</strong>
            </div>
            <el-progress :percentage="serverLoad" :show-text="false" :status="serverLoad < 50 ? 'success' : 'warning'" />
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted, markRaw } from 'vue'
import * as echarts from 'echarts/core'
import type { LinearGradientObject } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  CaretBottom,
  CaretTop,
  Document,
  Loading,
  Monitor,
  School,
  Setting,
  User,
  Warning,
} from '@element-plus/icons-vue'
import { getAdminStats } from '@/api/dashboard'
import type { SystemStatusItem, BaseStats } from '@/types'

echarts.use([LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const timeRange = ref('7d')
const chartRef = ref<HTMLElement>()
const statsLoading = ref(true)
let chartInstance: echarts.ECharts | null = null

const statCards = ref([
  { title: '用户总数', value: 0, trend: 0, icon: markRaw(User), tone: 'is-blue' },
  { title: '活跃班级', value: 0, trend: 0, icon: markRaw(School), tone: 'is-green' },
  { title: '核查任务', value: 0, trend: 0, icon: markRaw(Document), tone: 'is-amber' },
  { title: '系统异常', value: 0, trend: 0, icon: markRaw(Warning), tone: 'is-red' },
])

const systemStatus = ref<SystemStatusItem[]>([])
const apiUsage = ref(0)
const serverLoad = ref(0)

const healthType = computed(() => {
  if (serverLoad.value >= 80 || apiUsage.value >= 90) return 'danger'
  if (serverLoad.value >= 50 || apiUsage.value >= 70) return 'warning'
  return 'success'
})

const healthLabel = computed(() => {
  if (healthType.value === 'danger') return '高压力'
  if (healthType.value === 'warning') return '需关注'
  return '稳定'
})

async function loadStats() {
  statsLoading.value = true
  try {
    const days = timeRange.value === '30d' ? 30 : 7
    const res = await getAdminStats(days)
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
    apiUsage.value = d.apiUsage || 0
    serverLoad.value = d.serverLoad || 0
    initChart(d)
  } catch {
    // 请求错误已由拦截器处理
  } finally {
    statsLoading.value = false
  }
}

function initChart(data: BaseStats) {
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const option: echarts.EChartsCoreOption = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.96)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' },
      padding: [12, 16],
      extraCssText: 'box-shadow: 0 12px 30px rgba(15, 23, 42, 0.12);'
    },
    legend: {
      data: ['提交数', '解析成功', '评分完成'],
      top: 0,
      right: 0,
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#64748b', fontSize: 13 }
    },
    grid: { top: 44, left: '2%', right: '3%', bottom: '4%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.dates || [],
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#64748b', margin: 12 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#eef2f7', type: 'dashed' } },
      axisLabel: { color: '#64748b' }
    },
    series: [
      {
        name: '提交数',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: data.submissions || [],
        itemStyle: { color: '#2563eb' },
        areaStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: 'rgba(37, 99, 235, 0.25)' },
            { offset: 1, color: 'rgba(37, 99, 235, 0.03)' }
          ] } as LinearGradientObject
        }
      },
      {
        name: '解析成功',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: data.parsed || [],
        itemStyle: { color: '#059669' },
        areaStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: 'rgba(5, 150, 105, 0.22)' },
            { offset: 1, color: 'rgba(5, 150, 105, 0.03)' }
          ] } as LinearGradientObject
        }
      },
      {
        name: '评分完成',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: data.scored || [],
        itemStyle: { color: '#d97706' },
        areaStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
            { offset: 0, color: 'rgba(217, 119, 6, 0.2)' },
            { offset: 1, color: 'rgba(217, 119, 6, 0.03)' }
          ] } as LinearGradientObject
        }
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

watch(timeRange, () => loadStats())

onUnmounted(() => {
  chartInstance?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.admin-home {
  max-width: 1280px;
  margin: 0 auto;
}

.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
  margin-bottom: 24px;
}

.hero-copy,
.health-card,
.stat-card,
.chart-panel,
.status-panel {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.hero-copy {
  padding: 30px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(242, 248, 255, 0.94)),
    url('@/assets/hero.png') right center / auto 100% no-repeat;

  .eyebrow {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 14px;
    color: #2563eb;
    font-size: 13px;
    font-weight: 600;
  }

  h2 {
    margin-bottom: 10px;
    color: #0f172a;
    font-size: 30px;
    font-weight: 700;
  }

  p {
    max-width: 620px;
    color: #52657a;
    font-size: 15px;
    line-height: 1.8;
  }

  .hero-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 24px;
  }
}

.health-card {
  display: flex;
  min-height: 220px;
  flex-direction: column;
  justify-content: space-between;
  padding: 24px;
  border-color: #d7e6f7;

  .health-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    color: #64748b;
    font-size: 13px;
    font-weight: 600;
  }

  strong {
    color: #0f172a;
    font-size: 46px;
    font-weight: 800;
    line-height: 1;
  }

  p {
    color: #64748b;
    font-size: 13px;
    line-height: 1.7;
  }

  .health-metrics {
    display: flex;
    gap: 10px;

    span {
      padding: 6px 10px;
      border-radius: 8px;
      background: #f1f5f9;
      color: #475569;
      font-size: 12px;
      font-weight: 600;
    }
  }
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  min-height: 132px;
  padding: 20px;

  .stat-main {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
  }

  .stat-icon {
    display: flex;
    width: 44px;
    height: 44px;
    flex: 0 0 44px;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    font-size: 22px;
  }

  span {
    display: block;
    color: #64748b;
    font-size: 13px;
  }

  strong {
    display: block;
    margin-top: 7px;
    color: #0f172a;
    font-size: 28px;
    font-weight: 700;
    line-height: 1;
  }

  .stat-footer {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-top: 12px;
    border-top: 1px solid #eef2f7;
    color: #94a3b8;
    font-size: 12px;

    .trend {
      display: inline-flex;
      align-items: center;
      color: #dc2626;
      font-weight: 700;

      &.up {
        color: #059669;
      }
    }
  }

  &.is-blue .stat-icon {
    background: #eaf2ff;
    color: #2563eb;
  }

  &.is-green .stat-icon {
    background: #e8f7ef;
    color: #059669;
  }

  &.is-amber .stat-icon {
    background: #fff4df;
    color: #d97706;
  }

  &.is-red .stat-icon {
    background: #fff1f2;
    color: #dc2626;
  }
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 24px;
}

.chart-panel,
.status-panel {
  min-width: 0;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px 18px;
  border-bottom: 1px solid #eef2f7;

  &.compact {
    align-items: flex-start;
  }

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

.chart-container {
  width: 100%;
  height: 330px;
  padding: 14px 20px 20px;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 24px;
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  .status-name {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 8px;
    color: #475569;
    font-size: 14px;
  }
}

.usage-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 4px 24px 24px;
}

.usage-item {
  .usage-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    color: #64748b;
    font-size: 13px;

    strong {
      color: #0f172a;
    }
  }
}

@media (max-width: 1180px) {
  .hero-panel,
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .hero-copy {
    padding: 22px;
    background: #ffffff;

    h2 {
      font-size: 26px;
    }
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .section-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
