# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

### Backend (Spring Boot 3 + Java 17)
```bash
cd backend
mvn compile                 # 编译
mvn spring-boot:run         # 启动 (端口 8080)
mvn test                    # 运行全部测试
mvn test -Dtest=ClassName   # 运行单个测试类
mvn test -Dtest=ClassName#methodName  # 运行单个测试方法
mvn clean package -DskipTests         # 生产构建 (target/backend-1.0.0.jar)
```

### Frontend (Vue 3 + Vite)
```bash
cd frontend
npm install                 # 安装依赖
npm run dev                 # 开发服务器 (端口 3000，代理 /api → localhost:8080)
npm run build               # 生产构建 (vue-tsc + vite build)
npx vue-tsc --noEmit        # 仅类型检查
```
- `@` 别名映射到 `src/`（`@/api/xxx` = `src/api/xxx`），配置在 `tsconfig.app.json` 的 `paths` 和 `vite.config.ts` 的 `resolve.alias`
- Vite 开发代理：`/api` → `http://localhost:8080`，无需在前端配置后端地址

### Database
MySQL 8.0，数据库名 `bisai`。Schema 在 `backend/src/main/resources/schema.sql`，包含增量迁移 SQL。默认管理员账号 `admin` / `admin123`。

**前置条件**：需先创建空数据库 `CREATE DATABASE bisai`，然后导入 `schema.sql`。应用启动时不会自动建表。

## Architecture

实训成果智能核查与评价系统，三角色（学生/教师/管理员）。

### 核心业务流程
任务创建 → 学生上传文件 → AI文档解析 → AI智能核查 → AI评分 → 教师复核 → 成绩发布

### 技术栈
- **后端**: Spring Boot 3.4.3 + Spring Security (JWT) + MyBatis-Plus 3.5.9 + Spring AI (ModelScope)
- **前端**: Vue 3 + TypeScript + Vite 8 + Element Plus + Pinia + ECharts + Axios
- **文档解析**: PDFBox 3.0.3, POI 5.3.0, docx4j 11.4.11, iText 8.0.4
- **AI**: ModelScope 平台，默认 Qwen3.5-35B-A3B，管理后台可动态切换任意模型，支持 RAG 知识库检索增强

### 后端包结构 (`com.bisai`)
- `controller/` — REST API（16个），使用 `@PreAuthorize` 做角色控制
- `service/` — 业务逻辑层，核心：`AiService`（解析/核查/评分）、`KnowledgeService`（知识库管理）、`ScoreService`（评分流程）、`ModelScopeClient`（AI调用封装）
- `entity/` — MyBatis-Plus 实体，核心业务表已启用 `@TableLogic` 逻辑删除
- `mapper/` — MyBatis-Plus Mapper 接口
- `config/` — `SecurityConfig`（CORS/JWT/权限）、`AsyncConfig`（AI任务线程池）、`JacksonConfig`（LocalDateTime 全局格式化）、`AiConfig`（AI模型参数，支持运行时刷新）
- 工具库：Hutool 5.8（通用工具）、EasyExcel 3.3（Excel 导入导出）、JFreeChart 1.5（可视化报表图表）、Lombok（实体类注解）

### 前端结构
- `src/router/guards.ts` — 路由守卫，三套路由：`studentRoutes`、`teacherRoutes`、`adminRoutes`
- `src/api/` — 按 domain 拆分的 API 模块，统一通过 `utils/request.ts` 发请求
- `src/store/` — Pinia stores：`user`（认证/角色）、`app`（UI状态）
- `src/utils/status.ts` — 状态标签映射（集中管理，禁止在组件内硬编码）
- `src/utils/date.ts` — 日期格式化工具

### 权限模型
- Spring Security URL 级：`/api/auth/**` 放行，其余需认证
- 方法级：Controller 使用 `@PreAuthorize("hasRole('ADMIN')")` 等
- 数据级：Service 层按 role 过滤查询（学生只看自己的数据，教师只看自己课程的数据）
- 文件访问：`FileController.hasFileAccess` 通过 submissionId 关联验证权限，前端用 axios blob 下载（非 `window.open`）
- 前端路由级：`meta.roles` 控制页面访问

### 系统配置管理
- `SystemService` 维护 `FRONTEND_TO_DB_KEY` / `DB_TO_FRONTEND_KEY` 映射表，解决前端字段名（如 `model`）与数据库 key（如 `ai.chat-model`）不一致的问题
- `updateConfig()` 保存到数据库后调用 `refreshAiConfig()` 刷新 `AiConfig` Bean，使模型参数变更立即生效，无需重启
- `JacksonConfig` 全局配置 `LocalDateTime` 序列化/反序列化格式为 `yyyy-MM-dd HH:mm:ss`，所有实体自动生效
- 前端日期选择器 `value-format="YYYY-MM-DD HH:mm:ss"` 与后端格式匹配

### 异步任务
`AsyncTaskService` 用 `@Scheduled(fixedDelay=5000)` 数据库轮询（非消息队列），处理 PARSE/CHECK/SCORE 任务，最多重试 3 次（递增延迟），僵尸任务 10 分钟自动清理。`TaskService` 管理批量操作并发控制。

### 知识库 RAG
`KnowledgeService` 处理文档上传→解析→分块(1200字符)→向量化。`KnowledgeRetrievalService` 两阶段检索（向量 Top-20 + 可选 Rerank Top-5），为 AI 评分提供上下文。

### 核心状态机

提交(Submission) 三条并行状态流：
- **parseStatus**: `PENDING` → `PARSING` → `SUCCESS` / `FAILED`
- **checkStatus**: `NOT_CHECKED` → `CHECKING` → `SUCCESS` / `CHECK_FAILED`
- **scoreStatus**: `NOT_SCORED` → `SCORING` → `AI_SCORED` → `TEACHER_CONFIRMED` → `PUBLISHED`（或 `SCORE_FAILED` / `RETURNED`）

任务(TrainingTask) 状态：`DRAFT` → `PUBLISHED` → `CLOSED` / `ARCHIVED`

异步任务(AsyncTask) 状态：`PENDING` → `RUNNING` → `SUCCESS` / `FAILED`（失败可重试：`RETRYING` → `RUNNING`）

前端状态标签统一在 `utils/status.ts` 管理，禁止在组件中硬编码状态映射。

## Conventions
- 前端使用中文界面，代码注释中文，变量名英文
- 后端 API 响应统一用 `Result<T>` 包装
- 分页统一用 `PageQuery` → `PageResult<T>`
- MyBatis-Plus 全局逻辑删除字段 `deleted`，ID 策略 `AUTO`
- 敏感配置（密码、API Key、JWT Secret）使用环境变量但保留开发用默认值
- 前端文件下载/预览必须通过 axios 带 token，禁止 `window.open` 直接访问 API
- 热部署已禁用（devtools `restart.enabled: false`），修改 Java 代码后需手动重启
- MyBatis 日志使用 `Slf4jImpl`（非 `StdOutImpl`），避免定时任务刷屏

## Pitfalls

### 实体类命名
- 班级实体叫 `ClassEntity`，不是 `ClassInfo`，引用时注意区分
- 前端消息类型中文件实体是 `FileInfo`，不是 `FileEntity`

### MyBatis-Plus 空值陷阱
- `updateById()` 默认不更新 null 字段。需要清空字段时必须用 `UpdateWrapper.set("column", null)`

### LocalDateTime 序列化
- `JacksonConfig` 已全局配置 `yyyy-MM-dd HH:mm:ss` 格式，所有 `LocalDateTime` 字段自动生效
- 前端日期选择器必须使用 `value-format="YYYY-MM-dd HH:mm:ss"` 以匹配后端格式
- `spring.jackson.date-format` 只对 `java.util.Date` 生效，对 `LocalDateTime` 无效（由 `JacksonConfig` 处理）

### 系统配置 key 映射
- 前端表单字段名（如 `model`、`textModelApiUrl`）与数据库 key（如 `ai.chat-model`、`ai.api-url`）不同
- `SystemService` 中有双向映射表 `FRONTEND_TO_DB_KEY` / `DB_TO_FRONTEND_KEY`，新增配置项时必须同步更新

### TypeScript 6.0.3
- `tsconfig.app.json` 必须保留 `"ignoreDeprecations": "6.0"`，TS 6.0.3 的 `baseUrl` 已废弃，不加会报 TS5101
- `noUnusedLocals: true` + `noUnusedParameters: true` — 未使用的导入或变量会导致 `vue-tsc` 类型检查失败，进而导致 `npm run build` 失败

### 关键方法签名
- `ScoreService.saveTeacherScores()` 接收 4 个参数：`(Long submissionId, List<ScoreResult>, String comment, String expectedUpdatedAt)`，其中 `expectedUpdatedAt` 用于乐观锁
- `DashboardService.getAdminStats()` 接收 `int days` 参数控制图表天数范围

### 管理后台数据
- 仪表盘不接受硬编码 0 或假数据，所有统计必须来自数据库查询

### 课程权限
- `CourseService.listCourses` 按角色过滤：教师只看到自己的课程，管理员看到所有
- `TaskService.createTask` 通过 `isTeacherOwnerOfCourse` 验证教师只能在自己课程下创建任务
- `PermissionService` 提供 `isStudentOwnerOfSubmission` 验证学生只能访问自己的提交

### 文件路径
- `ReportService` 生成报告文件时使用 `toAbsolutePath().normalize()` 存储绝对路径
- `FileController` 对相对路径会拼接 `./data/files/` 基础路径，所以必须存绝对路径避免路径重复

## Agent skills

### Issue tracker

Issues tracked via GitHub Issues (`gh` CLI). See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout: `CONTEXT.md` + `docs/adr/` at repo root. See `docs/agents/domain.md`.
