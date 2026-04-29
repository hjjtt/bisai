import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken, getUserRole } from '@/utils/auth'
import { studentRoutes, teacherRoutes, adminRoutes } from './guards'

NProgress.configure({ showSpinner: false })

// 公共路由
const publicRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Index.vue'),
    meta: { title: '登录', guest: true },
  },
  {
    path: '/',
    redirect: () => {
      const role = getUserRole()
      if (role === 'STUDENT') return '/student'
      if (role === 'TEACHER') return '/teacher'
      if (role === 'ADMIN') return '/admin'
      return '/login'
    },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/common/NotFound.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes: [...publicRoutes, ...studentRoutes, ...teacherRoutes, ...adminRoutes],
})

// 路由守卫
const whiteList = ['/login']

router.beforeEach((to, _from, next) => {
  NProgress.start()
  document.title = (to.meta.title as string) || '实训成果智能核查与评价系统'

  const token = getToken()

  if (whiteList.includes(to.path)) {
    if (token) {
      const role = getUserRole()
      next(getHomePath(role))
    } else {
      next()
    }
  } else {
    if (!token) {
      next(`/login?redirect=${to.path}`)
    } else {
      const requiredRoles = to.meta.roles as string[] | undefined
      if (requiredRoles) {
        const role = getUserRole()
        if (!role || !requiredRoles.includes(role)) {
          next(getHomePath(role))
        } else {
          next()
        }
      } else {
        next()
      }
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

function getHomePath(role: string | null): string {
  if (role === 'STUDENT') return '/student'
  if (role === 'TEACHER') return '/teacher'
  if (role === 'ADMIN') return '/admin'
  return '/login'
}

export default router
