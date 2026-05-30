<template>
  <div class="user-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="title-area">
            <span class="title">用户管理</span>
            <span class="subtitle">按角色查看与维护账户信息</span>
          </div>
          <div class="header-actions">
            <el-input
              v-model="keyword"
              placeholder="搜索用户名/姓名"
              style="width: 220px"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="showDialog()">新增用户</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeRole" class="role-tabs" @tab-change="handleRoleTabChange">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="学生" name="STUDENT" />
        <el-tab-pane label="教师" name="TEACHER" />
        <el-tab-pane label="管理员" name="ADMIN" />
      </el-tabs>

      <el-table :data="users" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" min-width="120" align="center" />
        <el-table-column prop="realName" label="姓名" min-width="120" align="center" />
        <el-table-column label="角色" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">{{ getRoleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="showClassColumn"
          label="班级"
          min-width="150"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ getClassDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="showCourseColumn"
          label="授课课程"
          min-width="220"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ getCourseDisplay(row) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button class="edit-btn" type="primary" plain :icon="EditPen" @click="showDialog(row)">
                编辑
              </el-button>
              <el-dropdown trigger="click" @command="(command) => handleMoreCommand(command, row)">
                <el-button class="more-btn" plain>
                  更多
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="toggle">
                      {{ row.status === 'ENABLED' ? '禁用账号' : '启用账号' }}
                    </el-dropdown-item>
                    <el-dropdown-item command="reset">重置密码</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && users.length === 0" description="暂无用户数据" />

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @change="loadUsers"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingUser ? '编辑用户' : '新增用户'" width="500px">
      <el-form ref="formRef" :model="userForm" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="!!editingUser" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option v-for="role in ROLE_OPTIONS" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" v-if="userForm.role === 'STUDENT'" prop="classId">
          <el-select v-model="userForm.classId" placeholder="请选择班级" style="width: 100%">
            <el-option v-for="item in classes" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!editingUser">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveUser">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, EditPen, Search } from '@element-plus/icons-vue'
import { createUser, getUserList, resetPassword, toggleUserStatus, updateUser } from '@/api/user'
import { getClassList } from '@/api/course'
import { getRoleLabel, getRoleType, ROLE_OPTIONS } from '@/utils/status'
import type { ClassInfo, UserInfo, UserRole } from '@/types'

type RoleTab = '' | UserRole
type MoreCommand = 'toggle' | 'reset'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const activeRole = ref<RoleTab>('')
const dialogVisible = ref(false)
const editingUser = ref<UserInfo | null>(null)
const users = ref<UserInfo[]>([])
const classes = ref<ClassInfo[]>([])
const formRef = ref<FormInstance>()
const pagination = reactive({ page: 1, size: 20, total: 0 })

const showClassColumn = computed(() => activeRole.value === '' || activeRole.value === 'STUDENT')
const showCourseColumn = computed(() => activeRole.value === '' || activeRole.value === 'TEACHER')

const userForm = reactive({
  username: '',
  realName: '',
  role: 'STUDENT' as UserRole,
  classId: undefined as number | undefined,
  password: '',
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  classId: [
    {
      validator: (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
        if (userForm.role === 'STUDENT' && !value) {
          callback(new Error('学生必须绑定班级'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

watch(
  () => userForm.role,
  (role) => {
    if (role !== 'STUDENT') {
      userForm.classId = undefined
    }
  },
)

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUserList({
      page: pagination.page,
      size: pagination.size,
      keyword: keyword.value,
      role: activeRole.value,
    })
    users.value = res.data.items
    pagination.total = res.data.total
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

async function loadClasses() {
  try {
    const res = await getClassList({ size: 100 })
    classes.value = res.data.items
  } catch (error) {
    ElMessage.error('加载班级列表失败')
  }
}

function handleSearch() {
  pagination.page = 1
  loadUsers()
}

function handleRoleTabChange() {
  pagination.page = 1
  loadUsers()
}

function showDialog(user?: UserInfo) {
  editingUser.value = user || null
  if (user) {
    Object.assign(userForm, {
      username: user.username,
      realName: user.realName,
      role: user.role,
      classId: user.classId,
      password: '',
    })
  } else {
    Object.assign(userForm, {
      username: '',
      realName: '',
      role: 'STUDENT',
      classId: undefined,
      password: '',
    })
  }
  dialogVisible.value = true
}

async function handleSaveUser() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      ...userForm,
      classId: userForm.role === 'STUDENT' ? userForm.classId : undefined,
    }
    if (editingUser.value) {
      await updateUser(editingUser.value.id, payload)
      ElMessage.success('更新成功')
    } else {
      await createUser(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(user: UserInfo) {
  try {
    const newStatus = user.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    await toggleUserStatus(user.id, newStatus)
    ElMessage.success('状态更新成功')
    loadUsers()
  } catch (error) {
    ElMessage.error('状态更新失败')
  }
}

async function resetPwd(user: UserInfo) {
  try {
    await ElMessageBox.confirm(`确定重置用户 ${user.realName} 的密码？`, '重置密码')
    await resetPassword(user.id)
    ElMessage.success('密码已重置')
  } catch (error) {
    // 用户取消
  }
}

function handleMoreCommand(command: string | number | object, user: UserInfo) {
  const typedCommand = command as MoreCommand
  if (typedCommand === 'toggle') {
    toggleStatus(user)
    return
  }
  if (typedCommand === 'reset') {
    resetPwd(user)
  }
}

function getClassDisplay(row: UserInfo) {
  if (row.role === 'STUDENT') {
    return row.className || '未分配班级'
  }
  return '-'
}

function getCourseDisplay(row: UserInfo) {
  if (row.role === 'TEACHER') {
    return row.teachingCourseNames || '未关联课程'
  }
  return '-'
}

onMounted(() => {
  loadUsers()
  loadClasses()
})
</script>

<style lang="scss" scoped>
.user-manage {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.title-area {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.subtitle {
  font-size: 13px;
  color: #64748b;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

:deep(.role-tabs .el-tabs__header) {
  margin: 4px 0 12px;
}

:deep(.role-tabs .el-tabs__item) {
  font-weight: 600;
}

:deep(.el-table) {
  th.el-table__cell {
    background-color: #f8fafc;
    color: #64748b;
    font-weight: 600;
    font-size: 13px;
  }

  td.el-table__cell {
    color: #334155;
  }
}

.table-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

:deep(.edit-btn),
:deep(.more-btn) {
  min-width: 72px;
  height: 32px;
  border-radius: 8px;
}

:deep(.more-btn) {
  color: #475569 !important;
  background-color: #f8fafc !important;
  border-color: #cbd5e1 !important;
}

:deep(.more-btn:hover) {
  color: #0f172a !important;
  background-color: #eef2f7 !important;
}

:deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
