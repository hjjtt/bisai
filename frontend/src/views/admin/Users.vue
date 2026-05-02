<template>
  <div class="user-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div class="header-actions">
            <el-input v-model="keyword" placeholder="搜索用户名/姓名" style="width: 200px" clearable @clear="loadUsers" @keyup.enter="loadUsers">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="roleFilter" placeholder="角色筛选" clearable @change="loadUsers" style="width: 120px">
              <el-option label="学生" value="STUDENT" />
              <el-option label="教师" value="TEACHER" />
              <el-option label="管理员" value="ADMIN" />
            </el-select>
            <el-button type="primary" @click="showDialog()">新增用户</el-button>
          </div>
        </div>
      </template>

      <el-table :data="users" stripe v-loading="loading">
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)">{{ getRoleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDialog(row)">编辑</el-button>
            <el-button :type="row.status === 'ENABLED' ? 'warning' : 'success'" link @click="toggleStatus(row)">
              {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
            </el-button>
            <el-button type="info" link @click="resetPwd(row)">重置密码</el-button>
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
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
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
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" v-if="userForm.role === 'STUDENT'" prop="classId">
          <el-select v-model="userForm.classId" placeholder="请选择班级" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
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
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getUserList, createUser, updateUser, resetPassword, toggleUserStatus } from '@/api/user'
import { getClassList } from '@/api/course'
import { getRoleLabel, getRoleType } from '@/utils/status'
import type { UserInfo, ClassInfo, UserRole } from '@/types'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const roleFilter = ref('')
const dialogVisible = ref(false)
const editingUser = ref<UserInfo | null>(null)
const users = ref<UserInfo[]>([])
const classes = ref<ClassInfo[]>([])
const formRef = ref<FormInstance>()
const pagination = reactive({ page: 1, size: 20, total: 0 })

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
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUserList({ page: pagination.page, size: pagination.size, keyword: keyword.value, role: roleFilter.value })
    users.value = res.data.items
    pagination.total = res.data.total
  } catch (e) {
    console.error('加载用户列表失败:', e)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

async function loadClasses() {
  try {
    const res = await getClassList({ size: 100 })
    classes.value = res.data.items
  } catch (e) {
    console.error('加载班级列表失败:', e)
  }
}

function showDialog(user?: UserInfo) {
  editingUser.value = user || null
  if (user) {
    Object.assign(userForm, { username: user.username, realName: user.realName, role: user.role, classId: user.classId, password: '' })
  } else {
    Object.assign(userForm, { username: '', realName: '', role: 'STUDENT', classId: undefined, password: '' })
  }
  dialogVisible.value = true
}

async function handleSaveUser() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingUser.value) {
      await updateUser(editingUser.value.id, userForm)
      ElMessage.success('更新成功')
    } else {
      await createUser(userForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (e) {
    console.error('保存用户失败:', e)
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(user: UserInfo) {
  try {
    const newStatus = user.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
    await toggleUserStatus(user.id, newStatus)
    ElMessage.success('操作成功')
    loadUsers()
  } catch (e) {
    console.error('切换用户状态失败:', e)
  }
}

async function resetPwd(user: UserInfo) {
  try {
    await ElMessageBox.confirm(`确定重置用户 ${user.realName} 的密码？`, '重置密码')
    await resetPassword(user.id)
    ElMessage.success('密码已重置')
  } catch {
    // 用户取消
  }
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

  .header-actions {
    display: flex;
    gap: 12px;
    align-items: center;
  }
}

:deep(.el-table) {
  margin-top: 8px;

  th.el-table__cell {
    background-color: #f8fafc;
    color: #64748b;
    font-weight: 600;
    font-size: 13px;
    padding: 12px 0;
  }

  td.el-table__cell {
    padding: 12px 0;
    color: #334155;
  }
}

.el-pagination {
  padding: 24px 0 8px;
  justify-content: flex-end;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #475569;
}
</style>
