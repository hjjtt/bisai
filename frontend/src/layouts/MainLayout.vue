<template>
  <el-container class="main-layout">
    <!-- 侧边栏：经典企业级深色 -->
    <el-aside :width="sidebarCollapsed ? '64px' : '240px'" class="sidebar">
      <div class="logo">
        <div class="logo-circle">
          <el-icon><Monitor /></el-icon>
        </div>
        <h1 v-show="!sidebarCollapsed">实训成果核查</h1>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="sidebarCollapsed"
        :router="true"
        background-color="#1e293b"
        text-color="#94a3b8"
        active-text-color="#ffffff"
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
      <!-- 顶栏：白色简约投影 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleSidebar">
            <Fold v-if="!sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
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
                  <el-icon :size="20" style="cursor: pointer"><Bell /></el-icon>
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
                  <el-button type="primary" link @click="$router.push('/admin/logs')">查看全部消息</el-button>
                </div>
              </div>
            </el-popover>
          </div>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <span class="welcome">欢迎您，</span>
              <span class="username">{{ userInfo?.realName || '管理员' }}</span>
              <el-avatar :size="32" icon="UserFilled" class="user-avatar" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
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
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, Bell, Monitor } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { studentRoutes, teacherRoutes, adminRoutes } from '@/router/guards'
import type { RouteRecordRaw } from 'vue-router'
import { getMessages, markMessageRead, markAllMessagesRead, getUnreadCount } from '@/api/report'
import { formatDate } from '@/utils/date'
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

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
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
}

.sidebar {
  background-color: #1e293b;
  transition: width 0.3s;
  overflow: hidden;
  z-index: 100;

  .logo {
    height: 64px;
    display: flex;
    align-items: center;
    padding: 0 16px;
    gap: 12px;
    background-color: #0f172a;

    .logo-circle {
      width: 32px;
      height: 32px;
      background: #3b82f6;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 18px;
    }

    h1 {
      font-size: 16px;
      color: #fff;
      font-weight: 600;
      white-space: nowrap;
    }
  }

  .sidebar-menu {
    border-right: none;
    padding-top: 12px;

    :deep(.el-menu-item) {
      height: 50px;
      line-height: 50px;

      &:hover {
        color: #fff !important;
        background-color: rgba(255, 255, 255, 0.05) !important;
      }

      &.is-active {
        background-color: #3b82f6 !important;
        color: #fff !important;
      }
    }
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  padding: 0 24px;
  height: 64px !important;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 99;

  .header-left {
    display: flex;
    align-items: center;
    gap: 20px;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: #64748b;
      &:hover {
        color: #3b82f6;
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 24px;

    .header-actions {
      display: flex;
      align-items: center;
      gap: 16px;
      color: #64748b;
      font-size: 18px;
      cursor: pointer;

      .el-icon:hover {
        color: #3b82f6;
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      .welcome {
        font-size: 13px;
        color: #94a3b8;
      }

      .username {
        font-size: 14px;
        color: #1e293b;
        font-weight: 500;
        margin-right: 4px;
      }

      .user-avatar {
        background: #e2e8f0;
        color: #64748b;
      }
    }
  }
}

.main-content {
  background-color: #f1f5f9;
  padding: 24px;
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
</style>
