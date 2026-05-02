# AGENTS.md

## 快速启动
```bash
# 后端 (端口 8080)
cd backend && mvn spring-boot:run

# 前端 (端口 3000，代理 /api → 8080)
cd frontend && npm run dev
```
**首次运行前**: 确保 MySQL 8.0 已启动，数据库 `bisai` 已创建，执行 `backend/src/main/resources/schema.sql`。

## 关键配置
- **数据库**: `root` / `${DB_PASSWORD:123456}`
- **JWT Secret**: `${JWT_SECRET:bisai-smart-evaluation-system-jwt-secret-key-2024}`
- **AI API Key**: `${AI_API_KEY:ms-cb4b0861-40d9-4697-86ac-b8764e1cdbd1}`
- 默认值保留在 `application.yml` 中，无需配置环境变量即可运行。

## 架构要点
- **三角色**: 学生/教师/管理员。路由守卫在 `frontend/src/router/guards.ts`，后端用 `@PreAuthorize` 控制。
- **核心流程**: 任务创建 → 学生上传 → AI解析 → AI核查 → AI评分 → 教师复核 → 成绩发布。
- **异步任务**: `AsyncTaskService` 轮询队列（5s），处理 PARSE/CHECK/SCORE 任务。
- **RAG知识库**: `KnowledgeService` 处理文档向量化，`KnowledgeRetrievalService` 做检索增强。
- **URL 路由**: `AsyncTaskController` 在 `/api/async-tasks`，不是 `/api/tasks`（避免与 `TaskController` 冲突）。

## 开发注意事项
- **热部署**: 启用了 `spring-boot-devtools`。修改 Java 代码后自动重启，但有时缓存会导致 Bean 注入失败，遇到时执行 `mvn clean` 再重启。
- **逻辑删除**: MyBatis-Plus 全局配置 `deleted` 字段，所有核心表已添加该字段。
- **文件上传**: 最大 200MB，存储在 `./data/files/`（已被 .gitignore 忽略）。
- **MyBatis 日志**: 使用 `Slf4jImpl` 而非 `StdOutImpl`，避免定时任务刷屏。
- **前端工具**: 状态标签统一用 `@/utils/status`，日期用 `@/utils/date`，禁止在组件内硬编码。

## 验证流程
```bash
# 后端编译检查
cd backend && mvn compile

# 前端类型检查
cd frontend && npx vue-tsc --noEmit

# 前端生产构建
cd frontend && npm run build
```

## 测试账号
管理员: `admin` / `admin123`
