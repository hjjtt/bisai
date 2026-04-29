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
            <el-tooltip content="通知" placement="bottom">
              <el-badge is-dot class="notice-badge">
                <el-icon><Bell /></el-icon>
              </el-badge>
            </el-tooltip>
            <el-tooltip content="搜索" placement="bottom">
              <el-icon><Search /></el-icon>
            </el-tooltip>
          </div>
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <span class="welcome">欢迎您，</span>
              <span class="username">{{ userInfo?.realName || '管理员' }}</span>
              <el-avatar :size="32" icon="UserFilled" class="user-avatar" />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
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
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, Bell, Search, UserFilled, Monitor } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { studentRoutes, teacherRoutes, adminRoutes } from '@/router/guards'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarCollapsed = ref(false)
const userInfo = computed(() => userStore.userInfo)
// 修复：role 在 store 中是一个函数，需要执行
const userRole = computed(() => userStore.role())

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const currentRoute = computed(() => route.path)

const currentTitle = computed(() => {
  return (route.meta.title as string) || ''
})

const menuItems = computed(() => {
  const role = userRole.value
  let routes: any[] = []
  
  if (role === 'STUDENT') routes = studentRoutes[0].children || []
  else if (role === 'TEACHER') routes = teacherRoutes[0].children || []
  else if (role === 'ADMIN') routes = adminRoutes[0].children || []
  
  return routes.filter(r => !r.meta?.hidden).map(r => {
    // 基础路径处理
    const basePath = role === 'ADMIN' ? '/admin' : role === 'TEACHER' ? '/teacher' : '/student'
    const fullPath = r.path === '' ? basePath : `${basePath}/${r.path}`
    
    return {
      path: fullPath,
      title: r.meta?.title,
      icon: r.meta?.icon
    }
  })
})

const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
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
</style>
