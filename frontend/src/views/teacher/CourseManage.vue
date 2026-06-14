<template>
  <div class="course-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>课程管理</span>
          <el-button type="primary" @click="showDialog()">新增课程</el-button>
        </div>
      </template>

      <el-table :data="courses" stripe v-loading="loading">
        <el-table-column prop="name" label="课程名称" min-width="200" align="center" />
        <el-table-column v-if="isAdmin" prop="teacherName" label="授课教师" width="120" align="center" />
        <el-table-column prop="className" label="授课班级" width="150" align="center" />
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

      <el-empty v-if="!loading && courses.length === 0" description="暂无课程数据" />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑课程' : '新增课程'" width="450px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入课程名称" />
        </el-form-item>
        <!-- 仅管理员显示教师选择 -->
        <el-form-item v-if="isAdmin" label="授课教师" prop="teacherId">
          <el-select v-model="form.teacherId" placeholder="请选择教师" style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.realName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授课班级" prop="classId">
          <el-select v-model="form.classId" placeholder="请选择班级" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
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
import { ref, reactive, computed, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { getCourseList, createCourse, updateCourse, getClassList } from '@/api/course'
import { getUserList } from '@/api/user'
import { useUserStore } from '@/store'
import type { Course, ClassInfo, UserInfo } from '@/types'
import { getEnableStatusType, getEnableStatusLabel } from '@/utils/status'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

const loading = ref(false)
const courses = ref<Course[]>([])
const classes = ref<ClassInfo[]>([])
const teachers = ref<UserInfo[]>([])

// 表单
const dialogVisible = ref(false)
const editing = ref<Course | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  teacherId: undefined as number | undefined,
  classId: undefined as number | undefined,
})

const formRules = computed<FormRules>(() => ({
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  teacherId: isAdmin.value
    ? [{ required: true, message: '请选择授课教师', trigger: 'change' }]
    : [],
  classId: [{ required: true, message: '请选择授课班级', trigger: 'change' }],
}))

function showDialog(item?: Course) {
  editing.value = item || null
  if (item) {
    Object.assign(form, { name: item.name, teacherId: item.teacherId, classId: item.classId })
  } else {
    Object.assign(form, { name: '', teacherId: undefined, classId: undefined })
  }
  dialogVisible.value = true
}

async function loadCourses() {
  loading.value = true
  try {
    const res = await getCourseList({ size: 100 })
    courses.value = res.data.items
  } catch {
    ElMessage.error('加载课程列表失败')
  } finally {
    loading.value = false
  }
}

async function loadClasses() {
  try {
    const res = await getClassList({ size: 100 })
    classes.value = res.data.items
  } catch {
    // 静默失败
  }
}

async function loadTeachers() {
  if (!isAdmin.value) return
  try {
    const res = await getUserList({ role: 'TEACHER', size: 100 })
    teachers.value = res.data.items
  } catch {
    // 静默失败
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (editing.value) {
      await updateCourse(editing.value.id, form)
    } else {
      await createCourse(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadCourses()
  } catch {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadCourses()
  loadClasses()
  loadTeachers()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
