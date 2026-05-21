# 实训成果智能核查与评价系统 - 前端

基于 Vue 3 + TypeScript + Vite 的三角色（学生/教师/管理员）前端应用。

## 技术栈

- **框架**：Vue 3.5 (`<script setup>` + Composition API)
- **语言**：TypeScript 6.0
- **构建**：Vite 8
- **UI**：Element Plus 2.13
- **状态管理**：Pinia 3.0
- **路由**：Vue Router 4.6
- **图表**：ECharts 6.0（按需导入）
- **HTTP**：Axios 1.15

## 开发

```bash
npm install       # 安装依赖
npm run dev       # 开发服务器（端口 3000，代理 /api → localhost:8080）
npm run build     # 生产构建（vue-tsc 类型检查 + vite build）
```

## 目录结构

```
src/
├── api/          # API 请求模块（按 domain 拆分）
├── views/        # 页面组件
│   ├── login/    #   登录页
│   ├── student/  #   学生端（6个页面）
│   ├── teacher/  #   教师端（14个页面）
│   ├── admin/    #   管理员端（6个页面）
│   └── common/   #   公共页面（消息中心、404）
├── layouts/      # 布局组件（MainLayout）
├── router/       # 路由配置 + 导航守卫
├── store/        # Pinia 状态管理（user、app）
├── types/        # TypeScript 类型定义
├── utils/        # 工具函数（request、auth、status、date）
└── styles/       # 全局样式
```

## 关键约定

- 所有 API 请求通过 `utils/request.ts` 统一拦截（JWT 注入、401/403 自动跳转登录）
- 文件下载必须通过 axios blob，禁止 `window.open`
- 状态标签统一在 `utils/status.ts` 管理，禁止组件内硬编码
- 三套路由（student/teacher/admin）通过 `meta.roles` 控制访问
- localStorage 用户信息带签名校验，防篡改
