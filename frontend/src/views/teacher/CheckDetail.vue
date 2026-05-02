<template>
  <div class="check-detail">
    <el-page-header @back="$router.back()" title="返回" content="智能核查详情" />

    <!-- 风险概览 -->
    <el-row :gutter="16" style="margin-top: 16px" v-if="checkResults.length > 0">
      <el-col :span="8">
        <el-card shadow="never" class="risk-card">
          <div class="risk-stat">
            <span class="risk-number" style="color: #67c23a">{{ lowRiskCount }}</span>
            <span class="risk-label">低风险</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="risk-card">
          <div class="risk-stat">
            <span class="risk-number" style="color: #e6a23c">{{ mediumRiskCount }}</span>
            <span class="risk-label">中风险</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="risk-card">
          <div class="risk-stat">
            <span class="risk-number" style="color: #f56c6c">{{ highRiskCount }}</span>
            <span class="risk-label">高风险</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>核查结果详情</span>
          <el-button type="primary" @click="handleRecheck" :loading="rechecking">重新核查</el-button>
        </div>
      </template>

      <template v-if="checkResults.length > 0">
        <el-table :data="checkResults" stripe>
          <el-table-column prop="checkType" label="核查维度" width="130" />
          <el-table-column prop="checkItem" label="检查项" width="160" />
          <el-table-column label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="getResultType(row.result)" size="small">{{ getResultLabel(row.result) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="风险等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getRiskType(row.riskLevel)" size="small" effect="dark">{{ getRiskLabel(row.riskLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="200" />
          <el-table-column prop="evidence" label="证据片段" min-width="180" />
          <el-table-column prop="suggestion" label="修改建议" min-width="180" />
        </el-table>
      </template>

      <el-empty v-if="!loading && checkResults.length === 0" description="暂无核查结果，请先触发 AI 核查" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCheckResults, startCheck } from '@/api/task'
import { getResultType, getResultLabel, getRiskType, getRiskLabel } from '@/utils/status'
import type { CheckResult } from '@/types'

const route = useRoute()
const loading = ref(false)
const rechecking = ref(false)
const checkResults = ref<CheckResult[]>([])

const submissionId = computed(() => Number(route.params.id) || 0)

const highRiskCount = computed(() => checkResults.value.filter(r => r.riskLevel === 'HIGH').length)
const mediumRiskCount = computed(() => checkResults.value.filter(r => r.riskLevel === 'MEDIUM').length)
const lowRiskCount = computed(() => checkResults.value.filter(r => r.riskLevel === 'LOW').length)

async function loadData() {
  if (!submissionId.value) return
  loading.value = true
  try {
    const res = await getCheckResults(submissionId.value)
    checkResults.value = res.data
  } catch (e) {
    console.error('加载核查结果失败:', e)
    ElMessage.error('加载核查结果失败')
  } finally {
    loading.value = false
  }
}

async function handleRecheck() {
  rechecking.value = true
  try {
    await startCheck(submissionId.value)
    ElMessage.success('AI 重新核查完成')
    loadData()
  } catch (e) {
    console.error('AI 核查失败:', e)
    ElMessage.error('AI 核查失败')
  } finally {
    rechecking.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.risk-card {
  text-align: center;
}
.risk-stat {
  padding: 8px 0;
}
.risk-number {
  font-size: 32px;
  font-weight: 700;
}
.risk-label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
