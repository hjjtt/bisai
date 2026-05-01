import type { RouteRecordRaw } from 'vue-router'

// 学生路由
export const studentRoutes: RouteRecordRaw[] = [
  {
    path: '/student',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true, roles: ['STUDENT'] },
    children: [
      {
        path: '',
        name: 'StudentHome',
        component: () => import('@/views/student/Home.vue'),
        meta: { title: '学生首页', icon: 'HomeFilled' },
      },
      {
        path: 'tasks',
        name: 'StudentTaskList',
        component: () => import('@/views/student/TaskList.vue'),
        meta: { title: '实训任务', icon: 'Document' },
      },
      {
        path: 'tasks/:id',
        name: 'StudentTaskDetail',
        component: () => import('@/views/student/TaskDetail.vue'),
        meta: { title: '任务详情', hidden: true },
      },
      {
        path: 'submit/:taskId',
        name: 'StudentSubmit',
        component: () => import('@/views/student/Submit.vue'),
        meta: { title: '成果上传', hidden: true },
      },
      {
        path: 'result/:submissionId',
        name: 'StudentResult',
        component: () => import('@/views/student/Result.vue'),
        meta: { title: '评价结果', hidden: true },
      },
    ],
  },
]

// 教师路由
export const teacherRoutes: RouteRecordRaw[] = [
  {
    path: '/teacher',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true, roles: ['TEACHER'] },
    children: [
      {
        path: '',
        name: 'TeacherHome',
        component: () => import('@/views/teacher/Home.vue'),
        meta: { title: '教师首页', icon: 'HomeFilled' },
      },
      {
        path: 'tasks',
        name: 'TeacherTaskManage',
        component: () => import('@/views/teacher/TaskManage.vue'),
        meta: { title: '任务管理', icon: 'Document' },
      },
      {
        path: 'tasks/create',
        name: 'TeacherTaskCreate',
        component: () => import('@/views/teacher/TaskEdit.vue'),
        meta: { title: '创建任务', hidden: true },
      },
      {
        path: 'tasks/:id/edit',
        name: 'TeacherTaskEdit',
        component: () => import('@/views/teacher/TaskEdit.vue'),
        meta: { title: '编辑任务', hidden: true },
      },
      {
        path: 'submissions',
        name: 'TeacherSubmissions',
        component: () => import('@/views/teacher/Submissions.vue'),
        meta: { title: '提交管理', icon: 'FolderOpened' },
      },
      {
        path: 'submissions/:id/preview',
        name: 'TeacherFilePreview',
        component: () => import('@/views/teacher/FilePreview.vue'),
        meta: { title: '文件预览', hidden: true },
      },
      {
        path: 'submissions/:id/check',
        name: 'TeacherCheckDetail',
        component: () => import('@/views/teacher/CheckDetail.vue'),
        meta: { title: '核查详情', hidden: true },
      },
      {
        path: 'submissions/:id/score',
        name: 'TeacherScoreReview',
        component: () => import('@/views/teacher/ScoreReview.vue'),
        meta: { title: '评分复核', icon: 'EditPen' },
      },
      {
        path: 'reports',
        name: 'TeacherReports',
        component: () => import('@/views/teacher/Reports.vue'),
        meta: { title: '报表中心', icon: 'DataAnalysis' },
      },
      {
        path: 'batch',
        name: 'TeacherBatchProgress',
        component: () => import('@/views/teacher/BatchProgress.vue'),
        meta: { title: '批量任务', icon: 'Loading' },
      },
      {
        path: 'knowledge',
        name: 'TeacherKnowledge',
        component: () => import('@/views/teacher/Knowledge.vue'),
        meta: { title: '知识库管理', icon: 'Collection' },
      },
    ],
  },
]

// 管理员路由
export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN'] },
    children: [
      {
        path: '',
        name: 'AdminHome',
        component: () => import('@/views/admin/Home.vue'),
        meta: { title: '管理员首页', icon: 'HomeFilled' },
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
      {
        path: 'classes',
        name: 'AdminClasses',
        component: () => import('@/views/admin/Classes.vue'),
        meta: { title: '班级课程', icon: 'School' },
      },
      {
        path: 'knowledge',
        name: 'AdminKnowledge',
        component: () => import('@/views/admin/Knowledge.vue'),
        meta: { title: '知识库管理', icon: 'Collection' },
      },
      {
        path: 'model-config',
        name: 'AdminModelConfig',
        component: () => import('@/views/admin/ModelConfig.vue'),
        meta: { title: '模型配置', icon: 'Setting' },
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/Logs.vue'),
        meta: { title: '系统日志', icon: 'Tickets' },
      },
      {
        path: 'batch',
        name: 'AdminBatchProgress',
        component: () => import('@/views/teacher/BatchProgress.vue'),
        meta: { title: '批量任务', icon: 'Loading' },
      },
      {
        path: 'reports',
        name: 'AdminReports',
        component: () => import('@/views/teacher/Reports.vue'),
        meta: { title: '报表中心', icon: 'DataAnalysis' },
      },
    ],
  },
]
