<template>
  <div class="reports">
    <el-card>
      <template #header><span>报表中心</span></template>

      <el-tabs v-model="activeTab">
        <!-- 个人报告 -->
        <el-tab-pane label="个人评价报告" name="student">
          <el-form :inline="true" style="margin-bottom: 16px">
            <el-form-item label="选择任务">
              <el-select v-model="studentReport.taskId" placeholder="请选择任务" @change="loadSubmissions">
                <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="选择学生">
              <el-select v-model="studentReport.submissionId" placeholder="请选择学生">
                <el-option v-for="s in submissionList" :key="s.id" :label="s.studentName" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="doExportStudentReport('PDF')" :loading="exporting">导出 PDF</el-button>
              <el-button @click="doExportStudentReport('WORD')" :loading="exporting">导出 Word</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 班级报表 -->
        <el-tab-pane label="班级统计报表" name="class">
          <el-form :inline="true" style="margin-bottom: 16px">
            <el-form-item label="选择任务">
              <el-select v-model="classReport.taskId" placeholder="请选择任务">
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTaskList, getSubmissions } from '@/api/task'
import { exportStudentReport as exportStudentReportApi, exportClassReport as exportClassReportApi } from '@/api/report'
import type { TrainingTask, Submission } from '@/types'

const activeTab = ref('student')
const exporting = ref(false)
const tasks = ref<TrainingTask[]>([])
const submissionList = ref<Submission[]>([])

const studentReport = reactive({ taskId: undefined as number | undefined, submissionId: undefined as number | undefined })
const classReport = reactive({ taskId: undefined as number | undefined })

async function loadTasks() {
  try {
    const res = await getTaskList({ size: 100 })
    tasks.value = res.data.items
  } catch (e) {
    console.error('加载任务列表失败:', e)
  }
}

async function loadSubmissions() {
  if (!studentReport.taskId) return
  try {
    const res = await getSubmissions({ taskId: studentReport.taskId, size: 100 })
    submissionList.value = res.data.items
  } catch (e) {
    console.error('加载提交列表失败:', e)
  }
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
