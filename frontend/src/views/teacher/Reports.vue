<template>
  <div class="reports">
    <el-card>
      <template #header><span>报表中心</span></template>

      <el-tabs v-model="activeTab">
        <!-- 个人报告 -->
        <el-tab-pane label="个人评价报告" name="student">
          <el-form :inline="true" style="margin-bottom: 16px">
            <el-form-item label="选择任务">
              <el-select v-model="studentReport.taskId" placeholder="请选择任务" clearable style="width: 200px" @change="handleTaskChange">
                <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="选择学生">
              <el-select v-model="studentReport.submissionId" placeholder="请选择学生" :disabled="!studentReport.taskId || submissionList.length === 0" style="width: 200px">
                <el-option v-for="s in submissionList" :key="s.id" :label="s.studentName || ('学生' + s.studentId)" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="doExportStudentReport('PDF')" :loading="exporting" :disabled="!studentReport.submissionId">导出 PDF</el-button>
              <el-button @click="doExportStudentReport('WORD')" :loading="exporting" :disabled="!studentReport.submissionId">导出 Word</el-button>
            </el-form-item>
          </el-form>

          <!-- 提交列表表格 -->
          <el-table :data="submissionList" stripe v-loading="loadingSubmissions" style="margin-top: 16px">
            <el-table-column prop="studentName" label="学生姓名" width="120" />
            <el-table-column prop="version" label="版本" width="80" />
            <el-table-column prop="submitTime" label="提交时间" width="180">
              <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
            </el-table-column>
            <el-table-column label="解析状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getParseStatusType(row.parseStatus)" size="small">{{ getParseStatusLabel(row.parseStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="核查状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getCheckStatusType(row.checkStatus)" size="small">{{ getCheckStatusLabel(row.checkStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="评分状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getScoreStatusType(row.scoreStatus)" size="small">{{ getScoreStatusLabel(row.scoreStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="totalScore" label="总分" width="80" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="selectStudent(row.id)">选择</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 班级报表 -->
        <el-tab-pane label="班级统计报表" name="class">
          <el-form :inline="true" style="margin-bottom: 16px">
            <el-form-item label="选择任务">
              <el-select v-model="classReport.taskId" placeholder="请选择任务" style="width: 200px">
                <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="doExportClassReport('EXCEL')" :loading="exporting">导出 Excel</el-button>
              <el-button @click="doExportClassReport('PDF')" :loading="exporting">导出 PDF</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getTaskList, getSubmissions } from '@/api/task'
import { exportStudentReport as exportStudentReportApi, exportClassReport as exportClassReportApi } from '@/api/report'
import { getParseStatusType, getParseStatusLabel, getCheckStatusType, getCheckStatusLabel, getScoreStatusType, getScoreStatusLabel } from '@/utils/status'
import { formatDate } from '@/utils/date'
import type { TrainingTask, Submission } from '@/types'

const activeTab = ref('student')
const exporting = ref(false)
const loadingSubmissions = ref(false)
const tasks = ref<TrainingTask[]>([])
const submissionList = ref<Submission[]>([])

const studentReport = reactive({ taskId: undefined as number | undefined, submissionId: undefined as number | undefined })
const classReport = reactive({ taskId: undefined as number | undefined })

async function loadTasks() {
  try {
    const res = await getTaskList({ size: 100 })
    tasks.value = res.data.items
    console.log('加载任务列表成功:', tasks.value.length, '个任务')
    console.log('任务数据详情:', JSON.stringify(tasks.value))
    if (tasks.value.length === 0) {
      ElMessage.warning('暂无任务数据，请先创建实训任务')
    }
  } catch (e: any) {
    console.error('加载任务列表失败:', e)
    ElMessage.error('加载任务列表失败: ' + (e.message || '未知错误'))
  }
}

async function loadSubmissions() {
  if (!studentReport.taskId) {
    submissionList.value = []
    return
  }
  loadingSubmissions.value = true
  try {
    console.log('加载任务', studentReport.taskId, '下的提交列表')
    const res = await getSubmissions({ taskId: studentReport.taskId, page: 1, size: 100 })
    submissionList.value = res.data.items
    console.log('加载提交列表成功:', submissionList.value.length, '个提交')
    if (submissionList.value.length === 0) {
      ElMessage.info('该任务下暂无学生提交记录')
    }
  } catch (e: any) {
    console.error('加载提交列表失败:', e)
    ElMessage.error('加载学生列表失败: ' + (e.message || '未知错误'))
    submissionList.value = []
  } finally {
    loadingSubmissions.value = false
  }
}

function handleTaskChange() {
  studentReport.submissionId = undefined
  loadSubmissions()
}

function selectStudent(id: number) {
  studentReport.submissionId = id
}

async function doExportStudentReport(format: 'PDF' | 'WORD') {
  if (!studentReport.submissionId) { ElMessage.warning('请选择学生'); return }
  exporting.value = true
  try {
    const res = await exportStudentReportApi(studentReport.submissionId, format)
    window.open(`/api/files/${res.data.fileId}/download`)
  } catch (e) {
    console.error('导出报告失败:', e)
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

async function doExportClassReport(format: 'PDF' | 'EXCEL') {
  if (!classReport.taskId) { ElMessage.warning('请选择任务'); return }
  exporting.value = true
  try {
    const res = await exportClassReportApi(classReport.taskId, format)
    window.open(`/api/files/${res.data.fileId}/download`)
  } catch (e) {
    console.error('导出报表失败:', e)
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(loadTasks)
</script>
