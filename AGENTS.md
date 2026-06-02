# AGENTS.md

## Quick start
```bash
# 后端 (端口 8080) — 需要 MySQL 8.0 + 数据库 bisai 已初始化
cd backend && mvn spring-boot:run

# 前端 (端口 3000，代理 /api → localhost:8080)
cd frontend && npm run dev
```
**首次运行前**: MySQL 8.0 已启动，数据库 `bisai` 已创建，执行 `backend/src/main/resources/schema.sql`。

## Verification
```bash
cd backend && mvn compile            # 后端编译
cd frontend && npx vue-tsc --noEmit  # 前端类型检查（无 lint 脚本）
cd frontend && npm run build         # 前端生产构建 (vue-tsc + vite build)
```
**无测试套件**：前后端均无真实测试。`backend/src/test/` 中只有一个反射工具类（非 `@Test`），前端无 `*.test.*` / `*.spec.*`。不要假设可以 `mvn test` 或 `npm test`。

## Architecture

Spring Boot 3.4.3 + Spring AI 1.0.0 + Vue 3 + Vite 8 单体应用。三角色（学生/教师/管理员）。

**核心流程**: 任务创建 → 学生上传 → AI解析 → AI核查 → AI评分 → 教师复核 → 成绩发布。

### 后端 (`com.bisai`)
- 16 个 Controller，`@PreAuthorize` 角色控制。`AsyncTaskController` 在 `/api/async-tasks`（非 `/api/tasks`）
- 核心 Service: `AiService`(解析/核查/评分)、`ModelScopeClient`(AI调用+fallback)、`KnowledgeService`(RAG)、`AsyncTaskService`(@Scheduled 5s 轮询，非消息队列)
- 异步任务: 最多重试 3 次（递增延迟），僵尸任务 10 分钟清理
- RAG: 文档→分块(1200字符)→向量化；`KnowledgeRetrievalService` 向量 Top-20 + 可选 Rerank Top-5
- AI 调用通过 Spring AI OpenAI 兼容接口连 ModelScope，日限 50 万 token / 2000 次

### AI 模型与 fallback
- 主模型和备用模型可在管理后台动态切换（`system_config` 表），无需重启
- `ModelScopeClient` 自动构建 fallback 链：主模型 → `ai.fallback-model` 逗号分隔的模型列表
- 429 限流时标记模型耗尽并自动 fallback 到下一个模型
- **思考模式处理**：Qwen 系列模型，`doChat` 自动在 system prompt 前追加 `/no_think` 禁用思考；`stripThinkingTags()` 兜底剥离 `<think...</think` 标签。修改 AI 调用逻辑时注意这两个机制

### 前端 (`frontend/src`)
- 三套独立路由: `studentRoutes`/`teacherRoutes`/`adminRoutes`，守卫在 `router/guards.ts`
- API 统一通过 `utils/request.ts` (Axios)，按 domain 拆分在 `api/`
- Pinia stores: `user`(认证/角色)、`app`(UI状态)
- `@` 别名映射到 `src/`（`tsconfig.app.json` + `vite.config.ts`）

### `shannon/` 目录
独立 git 仓库（`@keygraph/shannon` 渗透测试工具），不属于本项目，修改时忽略。

## Key config
- **数据库**: `root` / `${DB_PASSWORD:123456}`，`localhost:3306/bisai`
- **JWT**: `${JWT_SECRET:bisai-smart-evaluation-system-jwt-secret-key-2024}`，过期 24h
- **AI**: `${AI_API_KEY:YOUR_MODELSCOPE_API_KEY}`，默认 Chat=`Qwen/Qwen3.5-35B-A3B`（yml 覆盖 Java 默认 `stepfun-ai/Step-3.7-Flash`），Embedding=`damo/nlp_corom_sentence-embedding_chinese-base`
- **Rerank**: 当前未启用（`ai.rerank-model` 为空）
- 默认值在 `application.yml`，无需环境变量即可运行
- 管理后台可通过 `SystemService.updateConfig()` 热更新 AI 配置，立即生效

## Conventions
- API 响应统一 `Result<T>`，分页 `PageQuery` → `PageResult<T>`
- 前端状态标签统一用 `@/utils/status`，日期用 `@/utils/date`，**禁止组件内硬编码**
- MyBatis-Plus 全局逻辑删除 `deleted`，ID 策略 `AUTO`
- 前端文件下载/预览必须通过 axios blob 带 token，**禁止 `window.open`**
- 前端中文界面，变量名英文，注释中文
- `JsonUtil` / `JacksonConfig` 已注册 `JavaTimeModule`，反序列化含 `LocalDateTime` 不报错
- `spring.jackson.date-format` 只对 `java.util.Date` 生效，`LocalDateTime` 由 `JacksonConfig` 处理
- 前端日期选择器必须用 `value-format="YYYY-MM-DD HH:mm:ss"` 匹配后端格式

## Gotchas

### 无测试
前后端均无测试。不要写 `mvn test` 或 `npm test` 验证，用 `mvn compile` 和 `vue-tsc --noEmit` 代替。

### 实体类命名
- 班级实体叫 `ClassEntity`，不是 `ClassInfo`
- 前端消息类型中文件是 `FileInfo`，不是 `FileEntity`

### MyBatis-Plus 空值
- `updateById()` 不更新 null 字段。清空字段必须用 `UpdateWrapper.set("column", null)`

### TypeScript ~6.0.2
- `tsconfig.app.json` 必须保留 `"ignoreDeprecations": "6.0"`，否则报 TS5101
- `noUnusedLocals: true` + `noUnusedParameters: true` — 未使用的导入/变量会导致 `vue-tsc` 和 `npm run build` 失败

### 热部署
- devtools **已禁用**（pom.xml 注释，yml `restart.enabled: false`）。改 Java 代码需手动重启。

### 系统配置 key 映射
- 前端表单字段名（如 `model`、`textModelApiUrl`）与数据库 key（如 `ai.chat-model`、`ai.api-url`）不同
- `SystemService` 中有双向映射表 `FRONTEND_TO_DB_KEY` / `DB_TO_FRONTEND_KEY`，新增配置项时必须同步更新

### 关键方法签名
- `ScoreService.saveTeacherScores(Long submissionId, List<ScoreResult>, String comment, String expectedUpdatedAt)` — `expectedUpdatedAt` 用于乐观锁
- `DashboardService.getAdminStats(int days)` — 仪表盘不接受硬编码假数据

### 文件路径
- `ReportService` 生成报告时用 `toAbsolutePath().normalize()` 存绝对路径
- `FileController` 对相对路径会拼接 `./data/files/`，所以必须存绝对路径避免路径重复

### 其他
- MyBatis 日志用 `Slf4jImpl`（非 `StdOutImpl`），避免定时任务刷屏
- 文件上传最大 200MB，存 `./data/files/`
- 仪表盘统计必须来自数据库查询，禁止硬编码 0 或假数据
- `PermissionService` 提供数据级权限校验：`isStudentOwnerOfSubmission`、`isTeacherOwnerOfCourse`

## Test account
管理员: `admin` / `admin123`

## Agent skills
- Issue tracker: GitHub Issues (`gh` CLI) → `docs/agents/issue-tracker.md`
- Triage labels: `needs-triage` → `needs-info` → `ready-for-agent`/`ready-for-human` → `wontfix` → `docs/agents/triage-labels.md`
- Domain: `CONTEXT.md` + `docs/adr/` → `docs/agents/domain.md`
