<template>
  <div class="task-edit">
    <el-page-header @back="$router.back()" title="返回" :content="isEdit ? '编辑任务' : '创建任务'" />
    <el-card style="margin-top: 16px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 700px">
        <el-form-item label="任务名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="所属课程" prop="courseId">
          <el-select v-model="form.courseId" placeholder="请选择课程" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评价模板" prop="templateId">
          <el-select v-model="form.templateId" placeholder="请选择评价模板" style="width: 100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="截止时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择截止时间" style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="实训要求" prop="requirements">
          <el-input v-model="form.requirements" type="textarea" :rows="6" placeholder="请输入实训要求" />
        </el-form-item>
        <el-form-item label="允许文件类型">
          <el-checkbox-group v-model="form.allowedFileTypes">
            <el-checkbox label="DOC" value="DOC" />
            <el-checkbox label="DOCX" value="DOCX" />
            <el-checkbox label="PDF" value="PDF" />
            <el-checkbox label="JPG" value="JPG" />
            <el-checkbox label="JPEG" value="JPEG" />
            <el-checkbox label="PNG" value="PNG" />
            <el-checkbox label="XLS" value="XLS" />
            <el-checkbox label="XLSX" value="XLSX" />
            <el-checkbox label="ZIP" value="ZIP" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="最大文件大小(MB)">
          <el-input-number v-model="form.maxFileSize" :min="1" :max="500" :step="10" />
        </el-form-item>
        <el-form-item label="允许重新提交">
          <el-switch v-model="form.allowResubmit" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { getTask, createTask, updateTask } from '@/api/task'
import { getCourseList, getTemplateList } from '@/api/course'
import type { FileType, Course, EvaluationTemplate } from '@/types'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const saving = ref(false)
const courses = ref<Course[]>([])
const templates = ref<EvaluationTemplate[]>([])

const taskId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : 0
})
const isEdit = computed(() => !!taskId.value)

const form = reactive({
  title: '',
  courseId: undefined as number | undefined,
  templateId: undefined as number | undefined,
  startTime: '' as string,
  endTime: '' as string,
  requirements: '',
  allowResubmit: true,
  allowedFileTypes: ['DOC', 'DOCX', 'PDF'] as FileType[],
  maxFileSize: 200,
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { max: 100, message: '任务名称不能超过100个字符', trigger: 'blur' },
  ],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  templateId: [{ required: true, message: '请选择评价模板', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择截止时间', trigger: 'change' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value && form.startTime && new Date(value) <= new Date(form.startTime)) {
          callback(new Error('截止时间必须晚于开始时间'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
  requirements: [{ required: true, message: '请输入实训要求', trigger: 'blur' }],
  allowedFileTypes: [
    {
      type: 'array',
      required: true,
      message: '请至少选择一种文件类型',
      trigger: 'change',
    },
  ],
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    // 转换数据格式以匹配后端
    const payload = {
      ...form,
      allowedFileTypes: Array.isArray(form.allowedFileTypes) ? form.allowedFileTypes.join(',') : form.allowedFileTypes,
      maxFileSize: form.maxFileSize ? form.maxFileSize * 1024 * 1024 : null,
    }
    if (isEdit.value) {
      await updateTask(taskId.value, payload)
      ElMessage.success('保存成功')
    } else {
      await createTask(payload)
      ElMessage.success('创建成功')
    }
    router.push('/teacher/tasks')
  } catch (e) {
    console.error('保存任务失败:', e)
    ElMessage.error(isEdit.value ? '保存失败' : '创建失败')
  } finally {
    saving.value = false
  }
}

async function loadOptions() {
  try {
    const [courseRes, templateRes] = await Promise.all([
      getCourseList({ size: 100 }),
      getTemplateList({ size: 100 }),
    ])
    courses.value = courseRes.data.items
    templates.value = templateRes.data.items
  } catch (e) {
    console.error('加载选项失败:', e)
    ElMessage.error('加载选项失败')
  }
}

async function loadTask() {
  if (!isEdit.value) return
  try {
    const res = await getTask(taskId.value)
    const data = res.data
    // 处理允许文件类型：后端返回逗号分隔字符串，前端需要数组
    if (typeof data.allowedFileTypes === 'string') {
      data.allowedFileTypes = data.allowedFileTypes.split(',').filter(Boolean)
    } else if (!Array.isArray(data.allowedFileTypes)) {
      data.allowedFileTypes = []
    }
    // 处理最大文件大小：后端返回字节，前端显示 MB
    if (data.maxFileSize) {
      data.maxFileSize = Math.floor(data.maxFileSize / 1024 / 1024)
    }
    Object.assign(form, data)
  } catch (e) {
    console.error('加载任务失败:', e)
    ElMessage.error('加载任务失败')
  }
}

onMounted(() => {
  loadOptions()
  loadTask()
})
</script>
