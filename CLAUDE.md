# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

### Backend (Spring Boot 3 + Java 20)
```bash
cd backend
mvn compile                 # 编译
mvn spring-boot:run         # 启动 (端口 8080)
mvn test                    # 运行测试
```

### Frontend (Vue 3 + Vite)
```bash
cd frontend
npm install                 # 安装依赖
npm run dev                 # 开发服务器 (端口 3000，代理 /api → localhost:8080)
npm run build               # 生产构建 (vue-tsc + vite build)
npx vue-tsc --noEmit        # 仅类型检查
```

### Database
MySQL 8.0，数据库名 `bisai`。Schema 在 `backend/src/main/resources/schema.sql`，包含增量迁移 SQL。默认管理员账号 `admin` / `admin123`。

## Architecture

实训成果智能核查与评价系统，三角色（学生/教师/管理员）。

### 核心业务流程
任务创建 → 学生上传文件 → AI文档解析 → AI智能核查 → AI评分 → 教师复核 → 成绩发布

### 技术栈
- **后端**: Spring Boot 3.3 + Spring Security (JWT) + MyBatis-Plus + Spring AI (ModelScope)
- **前端**: Vue 3 + TypeScript + Element Plus + Pinia + ECharts + Axios
- **AI**: 阿里 ModelScope 平台，Qwen3.5-35B-A3B 聊天模型 + 中文句向量模型，支持 RAG 知识库检索增强

### 后端包结构 (`com.bisai`)
- `controller/` — REST API，使用 `@PreAuthorize` 做角色控制
- `service/` — 业务逻辑层，核心：`AiService`（解析/核查/评分）、`KnowledgeService`（知识库管理）、`ScoreService`（评分流程）
- `entity/` — MyBatis-Plus 实体，核心业务表已启用 `@TableLogic` 逻辑删除
- `mapper/` — MyBatis-Plus Mapper 接口
- `config/` — `SecurityConfig`（CORS/JWT/权限）、`AsyncConfig`（AI任务线程池）

### 前端结构
- `src/router/guards.ts` — 路由守卫，三套路由：`studentRoutes`、`teacherRoutes`、`adminRoutes`
- `src/api/` — 按 domain 拆分的 API 模块，统一通过 `utils/request.ts` 发请求
- `src/store/` — Pinia stores：`user`（认证/角色）、`app`（UI状态）
- `src/utils/status.ts` — 状态标签映射（集中管理，避免重复）
- `src/utils/date.ts` — 日期格式化工具

### 权限模型
- Spring Security URL 级：`/api/auth/**` 放行，其余需认证
- 方法级：Controller 使用 `@PreAuthorize("hasRole('ADMIN')")` 等
- 数据级：Service 层按 role 过滤查询（学生只看自己的数据，教师只看自己课程的数据）
- 前端路由级：`meta.roles` 控制页面访问

### 异步任务
`AsyncTaskService` 管理后台 AI 处理队列（PARSE/CHECK/SCORE），定时轮询（5s）执行，支持重试。`TaskService` 管理批量操作并发控制。

### 知识库 RAG
`KnowledgeService` 处理文档上传→解析→分段→向量化。`KnowledgeRetrievalService` 实现向量检索 + 可选 Rerank 重排，为 AI 评分提供上下文。

## Conventions
- 前端使用中文界面，代码注释中文，变量名英文
- 后端 API 响应统一用 `Result<T>` 包装
- 分页统一用 `PageQuery` → `PageResult<T>`
- MyBatis-Plus 全局逻辑删除字段 `deleted`，ID 策略 `AUTO`
- 敏感配置（密码、API Key、JWT Secret）使用环境变量但保留开发用默认值
