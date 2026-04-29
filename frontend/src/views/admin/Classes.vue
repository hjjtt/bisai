<template>
  <div class="class-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>班级课程管理</span>
          <el-button type="primary" @click="showClassDialog()">新增班级</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 班级管理 -->
        <el-tab-pane label="班级管理" name="class">
          <el-table :data="classes" stripe v-loading="classLoading">
            <el-table-column prop="name" label="班级名称" width="180" />
            <el-table-column prop="grade" label="年级" width="100" />
            <el-table-column prop="major" label="专业" width="150" />
            <el-table-column label="学生数" width="80">
              <template #default="{ row }">{{ row.studentCount ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="showClassDialog(row)">编辑</el-button>
                <el-button type="info" link @click="viewStudents(row)">查看学生</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 课程管理 -->
        <el-tab-pane label="课程管理" name="course">
          <div style="margin-bottom: 12px">
            <el-button type="primary" @click="showCourseDialog()">新增课程</el-button>
          </div>
          <el-table :data="courses" stripe v-loading="courseLoading">
            <el-table-column prop="name" label="课程名称" min-width="180" />
            <el-table-column prop="teacherName" label="授课教师" width="120" />
            <el-table-column prop="className" label="授课班级" width="120" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'ENABLED' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="showCourseDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 班级对话框 -->
    <el-dialog v-model="classDialogVisible" :title="editingClass ? '编辑班级' : '新增班级'" width="450px">
      <el-form ref="classFormRef" :model="classForm" :rules="classRules" label-width="80px">
        <el-form-item label="班级名称"><el-input v-model="classForm.name" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="classForm.grade" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="classForm.major" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveClass">确认</el-button>
      </template>
    </el-dialog>

    <!-- 课程对话框 -->
    <el-dialog v-model="courseDialogVisible" :title="editingCourse ? '编辑课程' : '新增课程'" width="450px">
      <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="80px">
        <el-form-item label="课程名称"><el-input v-model="courseForm.name" /></el-form-item>
        <el-form-item label="授课教师">
          <el-select v-model="courseForm.teacherId" style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.realName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授课班级">
          <el-select v-model="courseForm.classId" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="courseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCourse">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { getClassList, createClass, updateClass, getCourseList, createCourse, updateCourse } from '@/api/course'
import { getUserList } from '@/api/user'
import type { ClassInfo, Course } from '@/types'

const activeTab = ref('class')
const classLoading = ref(false)
const courseLoading = ref(false)
const classes = ref<ClassInfo[]>([])
const courses = ref<Course[]>([])
const teachers = ref<any[]>([])

// 班级表单
const classDialogVisible = ref(false)
const editingClass = ref<ClassInfo | null>(null)
const classFormRef = ref<FormInstance>()
const classForm = reactive({ name: '', grade: '', major: '' })
const classRules: FormRules = {
  name: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
}

// 课程表单
const courseDialogVisible = ref(false)
const editingCourse = ref<Course | null>(null)
const courseFormRef = ref<FormInstance>()
const courseForm = reactive({ name: '', teacherId: undefined as number | undefined, classId: undefined as number | undefined })
const courseRules: FormRules = {
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请选择授课教师', trigger: 'change' }],
  classId: [{ required: true, message: '请选择授课班级', trigger: 'change' }],
}

function showClassDialog(item?: ClassInfo) {
  editingClass.value = item || null
  if (item) Object.assign(classForm, item)
  else Object.assign(classForm, { name: '', grade: '', major: '' })
  classDialogVisible.value = true
}

function showCourseDialog(item?: Course) {
  editingCourse.value = item || null
  if (item) Object.assign(courseForm, item)
  else Object.assign(courseForm, { name: '', teacherId: undefined, classId: undefined })
  courseDialogVisible.value = true
}

function viewStudents(row: ClassInfo) {
  ElMessage.info('查看班级学生列表')
}

async function loadClasses() {
  classLoading.value = true
  try {
    const res = await getClassList({ size: 100 })
    classes.value = res.data.items
  } catch {
    ElMessage.error('加载班级列表失败')
  } finally {
    classLoading.value = false
  }
}

async function loadCourses() {
  courseLoading.value = true
  try {
    const res = await getCourseList({ size: 100 })
    courses.value = res.data.items
  } catch {
    ElMessage.error('加载课程列表失败')
  } finally {
    courseLoading.value = false
  }
}

async function loadTeachers() {
  try {
    const res = await getUserList({ role: 'TEACHER', size: 100 })
    teachers.value = res.data.items
  } catch { /* ignore */ }
}

async function saveClass() {
  const valid = await classFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (editingClass.value) await updateClass(editingClass.value.id, classForm)
    else await createClass(classForm)
    ElMessage.success('保存成功')
    classDialogVisible.value = false
    loadClasses()
  } catch {
    ElMessage.error('保存失败')
  }
}

async function saveCourse() {
  const valid = await courseFormRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    if (editingCourse.value) await updateCourse(editingCourse.value.id, courseForm)
    else await createCourse(courseForm)
    ElMessage.success('保存成功')
    courseDialogVisible.value = false
    loadCourses()
  } catch {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadClasses()
  loadCourses()
  loadTeachers()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
