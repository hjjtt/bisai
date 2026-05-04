<template>
  <div class="calibration">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>评分校准</span>
          <div class="header-actions">
            <el-select v-model="selectedTaskId" placeholder="选择任务" clearable style="width: 240px" @change="handleTaskChange">
              <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
            </el-select>
            <el-button type="primary" :disabled="!selectedTaskId" @click="showAddDialog">添加校准样本</el-button>
          </div>
        </div>
      </template>

      <el-table :data="calibrations" stripe v-loading="loading">
        <el-table-column prop="submissionId" label="提交ID" width="100" />
        <el-table-column prop="indicatorName" label="评价指标" min-width="160">
          <template #default="{ row }">
            {{ row.indicatorName || '指标 #' + row.indicatorId }}
          </template>
        </el-table-column>
        <el-table-column prop="calibrationScore" label="校准分数" width="100">
          <template #default="{ row }">
            <span class="calibration-score">{{ row.calibrationScore ?? '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="calibrationReason" label="校准理由" min-width="200">
          <template #default="{ row }">
            <el-text type="info" size="small" class="text-ellipsis">{{ row.calibrationReason || '--' }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="确认时间" width="170">
          <template #default="{ row }">
            {{ row.confirmedAt ? formatDate(row.confirmedAt) : '--' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showEditDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && calibrations.length === 0" :description="selectedTaskId ? '暂无校准样本，请点击「添加校准样本」' : '请先选择任务'" />
    </el-card>

    <!-- 添加/编辑校准样本对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑校准样本' : '添加校准样本'"
      width="720px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form :model="form" label-width="100px" v-loading="dialogLoading">
        <el-form-item label="选择提交" v-if="!editingId">
          <el-select v-model="form.submissionId" placeholder="选择学生提交" filterable style="width: 100%" @change="handleSubmissionChange">
            <el-option v-for="s in submissions" :key="s.id" :label="`${s.studentName || '学生 #' + s.studentId} (V${s.version})`" :value="s.id" />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">各指标校准</el-divider>

        <div v-for="(item, idx) in form.items" :key="idx" class="indicator-item">
          <el-card shadow="never" class="indicator-card">
            <template #header>
              <span class="indicator-name">{{ item.indicatorName || '指标 #' + item.indicatorId }}</span>
              <el-tag size="small" type="info">满分 {{ item.maxScore }}</el-tag>
            </template>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="校准分数" label-width="80px">
                  <el-input-number v-model="item.score" :min="0" :max="item.maxScore || 100" :precision="1" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="16">
                <el-form-item label="校准理由" label-width="80px">
                  <el-input v-model="item.reason" placeholder="说明校准依据" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="典型优势" label-width="80px">
                  <el-input v-model="item.advantages" type="textarea" :rows="2" placeholder="该提交的典型优势" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="典型问题" label-width="80px">
                  <el-input v-model="item.problems" type="textarea" :rows="2" placeholder="该提交的典型问题" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="扣分依据" label-width="80px">
              <el-input v-model="item.deductionBasis" type="textarea" :rows="2" placeholder="具体扣分依据" />
            </el-form-item>
          </el-card>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTaskList, getSubmissions, saveCalibration, getCalibrations } from '@/api/task'
import { getTemplate } from '@/api/course'
import { formatDate } from '@/utils/date'
import type { TrainingTask, Submission, Indicator } from '@/types'
import type { CalibrationSaveData, CalibrationItem } from '@/api/task'

const loading = ref(false)
const saving = ref(false)
const dialogLoading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const selectedTaskId = ref<number | undefined>(undefined)
const tasks = ref<TrainingTask[]>([])
const submissions = ref<Submission[]>([])
const calibrations = ref<unknown[]>([])
const indicators = ref<Indicator[]>([])

interface CalibrationForm {
  submissionId: number | undefined
  items: (CalibrationItem & { indicatorName: string; maxScore: number })[]
}

const form = reactive<CalibrationForm>({
  submissionId: undefined,
  items: [],
})

async function loadTasks() {
  try {
    const res = await getTaskList({ size: 100 })
    tasks.value = res.data.items
  } catch {
    console.error('加载任务列表失败')
  }
}

async function handleTaskChange() {
  if (!selectedTaskId.value) {
    calibrations.value = []
    return
  }
  await loadCalibrations()
  await loadSubmissions()
}

async function loadSubmissions() {
  if (!selectedTaskId.value) return
  try {
    const res = await getSubmissions({ taskId: selectedTaskId.value, size: 100 })
    submissions.value = res.data.items
  } catch {
    console.error('加载提交列表失败')
  }
}

async function loadCalibrations() {
  if (!selectedTaskId.value) return
  loading.value = true
  try {
    const res = await getCalibrations(selectedTaskId.value)
    calibrations.value = res.data
  } catch {
    ElMessage.error('加载校准样本失败')
  } finally {
    loading.value = false
  }
}

async function loadIndicators(taskId: number) {
  const task = tasks.value.find(t => t.id === taskId)
  if (!task?.templateId) {
    ElMessage.warning('该任务未关联评分模板')
    return false
  }
  try {
    const res = await getTemplate(task.templateId)
    indicators.value = res.data.indicators || []
    return true
  } catch {
    ElMessage.error('加载评价指标失败')
    return false
  }
}

function showAddDialog() {
  editingId.value = null
  form.submissionId = undefined
  dialogVisible.value = true
}

function showEditDialog(row: unknown) {
  const r = row as Record<string, unknown>
  editingId.value = Number(r.id)
  form.submissionId = Number(r.submissionId)
  form.items = [{
    indicatorId: Number(r.indicatorId),
    indicatorName: (r.indicatorName as string) || '',
    maxScore: 100,
    score: Number(r.calibrationScore) || 0,
    reason: (r.calibrationReason as string) || '',
    advantages: (r.typicalAdvantages as string) || '',
    problems: (r.typicalProblems as string) || '',
    deductionBasis: (r.deductionBasis as string) || '',
  }]
  dialogVisible.value = true
}

async function handleSubmissionChange() {
  if (!selectedTaskId.value || !form.submissionId) return
  dialogLoading.value = true
  try {
    const ok = await loadIndicators(selectedTaskId.value)
    if (!ok) return
    form.items = indicators.value.map(ind => ({
      indicatorId: ind.id,
      indicatorName: ind.name,
      maxScore: ind.maxScore,
      score: 0,
      reason: '',
      advantages: '',
      problems: '',
      deductionBasis: '',
    }))
  } finally {
    dialogLoading.value = false
  }
}

async function handleSave() {
  if (!selectedTaskId.value) return
  if (!form.submissionId && !editingId.value) {
    ElMessage.warning('请选择提交')
    return
  }
  if (form.items.length === 0) {
    ElMessage.warning('暂无可保存的指标')
    return
  }

  saving.value = true
  try {
    const data: CalibrationSaveData = {
      taskId: selectedTaskId.value,
      submissionId: form.submissionId!,
      items: form.items.map(item => ({
        indicatorId: item.indicatorId,
        score: item.score,
        reason: item.reason,
        advantages: item.advantages,
        problems: item.problems,
        deductionBasis: item.deductionBasis,
      })),
    }
    await saveCalibration(data)
    ElMessage.success(editingId.value ? '校准样本已更新' : '校准样本已保存')
    dialogVisible.value = false
    await loadCalibrations()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function resetForm() {
  form.submissionId = undefined
  form.items = []
  editingId.value = null
}

onMounted(() => {
  loadTasks()
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
  gap: 12px;
}
.calibration-score {
  font-weight: bold;
  color: #f56c6c;
}
.text-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.indicator-item + .indicator-item {
  margin-top: 12px;
}
.indicator-card {
  border: 1px solid var(--el-border-color-light);
}
.indicator-card :deep(.el-card__header) {
  padding: 10px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.indicator-name {
  font-weight: 600;
  font-size: 14px;
}
</style>
