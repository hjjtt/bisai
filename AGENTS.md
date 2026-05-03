# AGENTS.md

## 快速启动
```bash
# 后端 (端口 8080)
cd backend && mvn spring-boot:run

# 前端 (端口 3000，代理 /api → localhost:8080)
cd frontend && npm run dev
```
**首次运行前**: 确保 MySQL 8.0 已启动，数据库 `bisai` 已创建，执行 `backend/src/main/resources/schema.sql`。

## 关键配置
- **数据库**: `root` / `${DB_PASSWORD:123456}`
- **JWT Secret**: `${JWT_SECRET:bisai-smart-evaluation-system-jwt-secret-key-2024}`，过期 24h
- **AI API Key**: `${AI_API_KEY:ms-cb4b0861-40d9-4697-86ac-b8764e1cdbd1}`
- **AI 模型**: Chat=`Qwen/Qwen3.5-35B-A3B`，Embedding=`damo/nlp_corom_sentence-embedding_chinese-base`
- **Rerank**: 当前未启用（`ai.rerank-model` 为空）
- 默认值保留在 `application.yml` 中，无需配置环境变量即可运行。

## 技术栈
- **后端**: Spring Boot 3.4.3 + Java 17 + Spring Security (JWT) + MyBatis-Plus 3.5.9 + Spring AI 1.0.0 (ModelScope)
- **前端**: Vue 3 + TypeScript + Vite 8 + Element Plus + Pinia + ECharts + Axios
- **文档解析**: PDFBox 3.0.3, POI 5.3.0, docx4j 11.4.11, iText 8.0.4
- **工具**: Hutool 5.8.34, EasyExcel 3.3.4, JFreeChart 1.5.5

## 架构要点
- **三角色**: 学生/教师/管理员。路由守卫在 `frontend/src/router/guards.ts`（三套独立路由），后端用 `@PreAuthorize` 控制。
- **核心流程**: 任务创建 → 学生上传 → AI解析 → AI核查 → AI评分 → 教师复核 → 成绩发布。
- **异步任务**: `AsyncTaskService` 用 `@Scheduled(fixedDelay=5000)` 数据库轮询（非消息队列），处理 PARSE/CHECK/SCORE 任务，最多重试 3 次（递增延迟 30s×retryCount），僵尸任务 10 分钟自动清理。
- **RAG知识库**: `KnowledgeService` 处理文档→分块(1200字符)→向量化；`KnowledgeRetrievalService` 两阶段检索（向量 Top-20 + 可选 Rerank Top-5）。
- **AI 调用**: 通过 Spring AI OpenAI 兼容接口连接 ModelScope，`AiUsageService` 追踪 token 用量（日限额 20 万 token / 1000 次调用）。
- **URL 路由**: `AsyncTaskController` 在 `/api/async-tasks`，不是 `/api/tasks`（避免与 `TaskController` 冲突）。
- **后端共 17 个 Controller**，覆盖 auth/tasks/submissions/knowledge/files/reports/users/courses/classes/templates/dashboard/calibration/system/messages/logs。

## 开发注意事项
- **热部署**: devtools **已禁用**（pom.xml 中注释，yml 中 `restart.enabled: false`）。修改 Java 代码后需手动重启。
- **逻辑删除**: MyBatis-Plus 全局配置 `deleted` 字段，所有核心表已添加该字段。
- **文件上传**: 最大 200MB，存储在 `./data/files/`（已被 .gitignore 忽略）。
- **MyBatis 日志**: 使用 `Slf4jImpl` 而非 `StdOutImpl`，避免定时任务刷屏。
- **前端工具**: 状态标签统一用 `@/utils/status`，日期用 `@/utils/date`，禁止在组件内硬编码。
- **教师账号修复**: `fix_teacher.sql` 包含教师密码重置 SQL。
- **前端无 lint 脚本**: `package.json` 只有 `dev`/`build`/`preview`，无 lint 命令。

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

---

## 防幻觉规则（CRITICAL）

### 1. 文件状态同步
- **每次编辑前**，必须使用 `Read` 工具确认文件最新内容。
- **禁止**基于记忆或假设修改代码。如果 `oldString` 匹配失败，立即停止并重新读取文件。
- **禁止**连续 3 次以上对同一文件执行相同操作。如果失败，必须向用户报告。

### 2. 死循环检测
- 如果连续 2 次工具调用返回相同错误或无变化，**立即停止**并询问用户。
- **禁止**在不确定文件内容时盲目重试 `edit` 操作。
- 当用户说"继续"时，先确认上一步是否真正完成，不要盲目推进。

### 3. 上下文锚定
- 修改代码时，必须引用具体的行号或代码片段作为锚点。
- **示例**："找到第 15 行的 `<el-select>`，修改其 `label` 属性。"
- 如果无法定位，先使用 `Grep` 或 `Read` 搜索，不要猜测。

### 4. 工具调用纪律
- `edit` 失败后，**必须**检查错误信息（如 `oldString not found`），不要忽略。
- 如果工具返回红色报错，向用户说明原因，不要静默重试。
- 每次编辑后，简要说明修改了什么、在哪里、预期效果。

### 5. 用户打断响应
- 当用户指出"你卡住了"或"重复了"，**立即停止**当前操作。
- 重新读取相关文件，确认状态后再继续。
- 不要辩解，直接修正。
