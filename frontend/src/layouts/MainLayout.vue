<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside :width="sidebarCollapsed ? '76px' : '248px'" class="sidebar">
      <div class="logo">
        <div class="logo-mark">
          <el-icon><Monitor /></el-icon>
        </div>
        <div v-show="!sidebarCollapsed" class="logo-text">
          <h1>实训成果核查</h1>
          <span>BisAI Evaluation</span>
        </div>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="sidebarCollapsed"
        :router="true"
        background-color="#ffffff"
        text-color="#64748b"
        active-text-color="#0f172a"
        class="sidebar-menu"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-button class="collapse-btn" text circle @click="toggleSidebar">
            <el-icon>
              <Fold v-if="!sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <div class="route-meta">
            <strong>{{ currentTitle || '首页' }}</strong>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </div>
        <div class="header-right">
          <div class="header-actions">
            <el-popover
              trigger="click"
              placement="bottom-end"
              :width="360"
              @show="loadMessages"
            >
              <template #reference>
                <el-badge :value="appStore.unreadMessageCount" :hidden="appStore.unreadMessageCount === 0" class="notice-badge">
                  <el-button class="icon-btn" text circle>
                    <el-icon><Bell /></el-icon>
                  </el-button>
                </el-badge>
              </template>
              <div class="notification-panel">
                <div class="panel-header">
                  <span>消息通知</span>
                  <el-button type="primary" link size="small" @click="handleMarkAllRead">全部已读</el-button>
                </div>
                <el-scrollbar height="300px">
                  <div v-if="messages.length === 0" class="empty-state">暂无消息</div>
                  <div
                    v-for="msg in messages"
                    :key="msg.id"
                    class="message-item"
                    :class="{ 'is-read': msg.isRead }"
                    @click="handleReadMessage(msg)"
                  >
                    <div class="msg-title">{{ msg.title }}</div>
                    <div class="msg-content">{{ msg.content }}</div>
                    <div class="msg-time">{{ formatDate(msg.createdAt) }}</div>
                  </div>
                </el-scrollbar>
                <div class="panel-footer">
                  <el-button type="primary" link @click="$router.push(messagesRoute)">查看全部消息</el-button>
                </div>
              </div>
            </el-popover>
          </div>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <span class="welcome">欢迎您，</span>
              <span class="username">{{ userInfo?.realName || userInfo?.username || getRoleLabel(userStore.role || '') }}</span>
              <el-avatar :size="32" icon="UserFilled" class="user-avatar" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="changePasswordVisible" title="修改密码" width="400px">
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item label="当前密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changePasswordVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">确认</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Fold, Expand, Bell, Monitor } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { studentRoutes, teacherRoutes, adminRoutes } from '@/router/guards'
import type { RouteRecordRaw } from 'vue-router'
import { getMessages, markMessageRead, markAllMessagesRead, getUnreadCount } from '@/api/message'
import { formatDate } from '@/utils/date'
import { getRoleLabel } from '@/utils/status'
import { changePassword } from '@/api/auth'
import type { Message } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const sidebarCollapsed = computed({
  get: () => appStore.sidebarCollapsed,
  set: (val: boolean) => { appStore.sidebarCollapsed = val },
})
const userInfo = computed(() => userStore.userInfo)
const userRole = computed(() => userStore.role)

const messagesRoute = computed(() => {
  const role = userRole.value
  if (role === 'ADMIN') return '/admin/messages'
  if (role === 'TEACHER') return '/teacher/messages'
  return '/student/messages'
})

const toggleSidebar = () => {
  appStore.toggleSidebar()
}

const currentRoute = computed(() => route.path)

const currentTitle = computed(() => {
  return (route.meta.title as string) || ''
})

interface MenuItem {
  path: string
  title: string
  icon: string
}

const menuItems = computed((): MenuItem[] => {
  const role = userRole.value
  let routes: RouteRecordRaw[] = []

  if (role === 'STUDENT') routes = studentRoutes[0].children || []
  else if (role === 'TEACHER') routes = teacherRoutes[0].children || []
  else if (role === 'ADMIN') routes = adminRoutes[0].children || []

  return routes.filter(r => !r.meta?.hidden).map(r => {
    const basePath = role === 'ADMIN' ? '/admin' : role === 'TEACHER' ? '/teacher' : '/student'
    const fullPath = r.path === '' ? basePath : `${basePath}/${r.path}`

    return {
      path: fullPath,
      title: r.meta?.title as string,
      icon: r.meta?.icon as string,
    }
  })
})

const changePasswordVisible = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordLoading = ref(false)

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'changePassword') {
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    changePasswordVisible.value = true
  }
}

async function handleChangePassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  if (!oldPassword || !newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  passwordLoading.value = true
  try {
    await changePassword(oldPassword, newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    changePasswordVisible.value = false
    userStore.logout()
    router.push('/login')
  } catch {
    // 错误已被 request.ts 拦截器处理
  } finally {
    passwordLoading.value = false
  }
}

// 通知功能
const messages = ref<Message[]>([])

async function loadMessages() {
  try {
    const res = await getMessages({ page: 1, size: 10 })
    messages.value = res.data.items
  } catch {
    // 忽略
  }
}

async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    appStore.unreadMessageCount = res.data
  } catch {
    // 忽略
  }
}

async function handleReadMessage(msg: Message) {
  if (!msg.isRead) {
    try {
      await markMessageRead(msg.id)
      msg.isRead = true
      appStore.unreadMessageCount = Math.max(0, appStore.unreadMessageCount - 1)
    } catch {
      // 忽略
    }
  }
}

async function handleMarkAllRead() {
  try {
    await markAllMessagesRead()
    messages.value.forEach(m => m.isRead = true)
    appStore.unreadMessageCount = 0
  } catch {
    // 忽略
  }
}

let timer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  loadUnreadCount()
  // 每 30 秒刷新一次未读数量
  timer = setInterval(loadUnreadCount, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
  background: #f6f8fb;
}

.sidebar {
  background: #ffffff;
  border-right: 1px solid #e5edf5;
  transition: width 0.25s ease;
  overflow: hidden;
  z-index: 100;

  .logo {
    height: 72px;
    display: flex;
    align-items: center;
    padding: 0 18px;
    gap: 12px;
    border-bottom: 1px solid #edf2f7;

    .logo-mark {
      width: 40px;
      height: 40px;
      flex: 0 0 40px;
      background: #edf7ff;
      border: 1px solid #d9e9f8;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #2563eb;
      font-size: 20px;
    }

    .logo-text {
      min-width: 0;

      h1 {
        font-size: 16px;
        color: #0f172a;
        font-weight: 700;
        line-height: 1.2;
        white-space: nowrap;
      }

      span {
        display: block;
        margin-top: 4px;
        color: #8a98aa;
        font-size: 11px;
        letter-spacing: 0;
        white-space: nowrap;
      }
    }
  }

  .sidebar-menu {
    border-right: none;
    padding: 14px 12px;
    background: transparent !important;

    :deep(.el-menu-item) {
      height: 44px;
      line-height: 44px;
      margin: 4px 0;
      border-radius: 8px;
      color: #64748b;
      font-weight: 500;

      .el-icon {
        color: #8a98aa;
      }

      &:hover {
        color: #0f172a !important;
        background-color: #f5f8fb !important;

        .el-icon {
          color: #2563eb;
        }
      }

      &.is-active {
        color: #0f172a !important;
        background-color: #eaf4ff !important;
        box-shadow: inset 3px 0 0 #2563eb;

        .el-icon {
          color: #2563eb;
        }
      }
    }

    :deep(.el-menu--collapse) {
      width: auto;
    }

    :deep(.el-menu-tooltip__trigger) {
      justify-content: center;
      padding: 0;
    }

    :deep(.el-menu-item [class^='el-icon']) {
      margin-right: 10px;
    }

    &.el-menu--collapse {
      padding-right: 10px;
      padding-left: 10px;

      :deep(.el-menu-item) {
        justify-content: center;
      }

      :deep(.el-menu-item [class^='el-icon']) {
        margin-right: 0;
      }
    }
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.92);
  padding: 0 28px;
  height: 72px !important;
  border-bottom: 1px solid #e5edf5;
  backdrop-filter: blur(12px);
  z-index: 99;

  .header-left {
    display: flex;
    align-items: center;
    gap: 14px;

    .collapse-btn {
      width: 36px;
      height: 36px;
      color: #64748b;

      &:hover {
        background: #f1f5f9;
        color: #3b82f6;
      }
    }

    .route-meta {
      display: flex;
      min-width: 0;
      flex-direction: column;
      gap: 5px;

      strong {
        color: #0f172a;
        font-size: 16px;
        font-weight: 700;
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 18px;

    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      color: #64748b;

      .icon-btn {
        width: 36px;
        height: 36px;
        color: #64748b;
        font-size: 18px;
      }

      .icon-btn:hover {
        background: #f1f5f9;
        color: #3b82f6;
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      min-height: 40px;
      padding: 4px 6px 4px 12px;
      border: 1px solid transparent;
      border-radius: 8px;

      &:hover {
        border-color: #e2e8f0;
        background: #f8fafc;
      }

      .welcome {
        font-size: 13px;
        color: #94a3b8;
      }

      .username {
        font-size: 14px;
        color: #1e293b;
        font-weight: 600;
        margin-right: 4px;
      }

      .user-avatar {
        background: #e9f2ff;
        color: #2563eb;
      }
    }
  }
}

.main-content {
  background:
    linear-gradient(180deg, #f6f8fb 0%, #f8fafc 42%, #f6f8fb 100%);
  padding: 28px;
  overflow-y: auto;
}

.notification-panel {
  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #f1f5f9;
    font-weight: 600;
    color: #1e293b;
  }

  .empty-state {
    padding: 40px 0;
    text-align: center;
    color: #94a3b8;
    font-size: 14px;
  }

  .message-item {
    padding: 12px 0;
    border-bottom: 1px solid #f8fafc;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: #f8fafc;
    }

    &.is-read {
      opacity: 0.6;
    }

    .msg-title {
      font-size: 14px;
      font-weight: 500;
      color: #1e293b;
      margin-bottom: 4px;
    }

    .msg-content {
      font-size: 12px;
      color: #64748b;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .msg-time {
      font-size: 11px;
      color: #94a3b8;
      margin-top: 4px;
    }
  }

  .panel-footer {
    padding: 12px 0;
    text-align: center;
    border-top: 1px solid #f1f5f9;
  }
}

@media (max-width: 900px) {
  .header {
    padding: 0 16px;

    .header-left .route-meta {
      .el-breadcrumb {
        display: none;
      }
    }

    .header-right .user-info .welcome {
      display: none;
    }
  }

  .main-content {
    padding: 16px;
  }
}
</style>
