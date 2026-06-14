<template>
  <div class="class-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>班级管理</span>
          <el-button type="primary" @click="showDialog()">新增班级</el-button>
        </div>
      </template>

      <el-table :data="classes" stripe v-loading="loading">
        <el-table-column prop="name" label="班级名称" min-width="180" align="center" />
        <el-table-column prop="grade" label="年级" width="120" align="center" />
        <el-table-column prop="major" label="专业" min-width="150" align="center" />
        <el-table-column label="学生数" width="100" align="center">
          <template #default="{ row }">{{ row.studentCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getEnableStatusType(row.status)" size="small">
              {{ getEnableStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain size="small" @click="showDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && classes.length === 0" description="暂无班级数据" />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑班级' : '新增班级'" width="450px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="班级名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入班级名称" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="form.grade" placeholder="如：2023级" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="form.major" placeholder="如：软件工程" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { getClassList, createClass, updateClass } from '@/api/course'
import type { ClassInfo } from '@/types'
import { getEnableStatusType, getEnableStatusLabel } from '@/utils/status'

const loading = ref(false)
const classes = ref<ClassInfo[]>([])

// 表单
const dialogVisible = ref(false)
const editing = ref<ClassInfo | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({ name: '', grade: '', major: '' })
const rules: FormRules = {
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
}

function showDialog(item?: ClassInfo) {
  editing.value = item || null
  if (item) {
    Object.assign(form, { name: item.name, grade: item.grade || '', major: item.major || '' })
  } else {
    Object.assign(form, { name: '', grade: '', major: '' })
  }
  dialogVisible.value = true
}

async function loadClasses() {
  loading.value = true
  try {
    const res = await getClassList({ size: 100 })
    classes.value = res.data.items
  } catch {
    ElMessage.error('加载班级列表失败')
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (editing.value) {
      await updateClass(editing.value.id, form)
    } else {
      await createClass(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadClasses()
  } catch {
    ElMessage.error('保存失败')
  }
}

onMounted(loadClasses)
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
