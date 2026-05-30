# 实训成果智能评价系统修改说明

## 一、修改概览

本次修改围绕“学生提交成果后的 AI 自动处理链路”展开，目标是让系统流程更贴近流程图中的要求。

主要变化如下：

- 原有链路从 `PARSE -> CHECK -> SCORE` 扩展为 `PRECHECK -> PARSE -> CHECK -> SCORE`。
- 学生上传成果后不再立即调用大模型解析，而是先进入 5 分钟缓冲期。
- 缓冲期内如果学生再次提交新版本，会取消旧版本的待处理门禁任务，避免重复消耗 AI Token。
- 增加 AI 门禁预检，用于在正式解析、核查、评分前判断提交是否具备基础有效性。
- 教师端评分、发布等操作增加状态约束，避免跳过前置流程。
- 前端补充 AI 处理阶段展示和状态类型映射，让界面状态与后端流程保持一致。

本说明基于当前 Git 已跟踪文件的修改生成，未包含未跟踪目录 `shannon/`。

## 二、后端核心修改

### 1. `SubmissionService`

文件：`backend/src/main/java/com/bisai/service/SubmissionService.java`

主要修改：

- 新增 `AsyncTaskService` 依赖，用于在上传成功后创建异步任务。
- 学生上传成果后，不再直接创建 `PARSE` 任务。
- 改为创建延迟 300 秒的 `PRECHECK` 任务：

```java
asyncTaskService.createDelayedTask("PRECHECK", submission.getId(), 300);
```

- 增加缓冲期覆盖逻辑：
  - 查询同一学生、同一任务下的历史提交。
  - 如果历史提交仍存在 `PENDING` 或 `RETRYING` 状态的 `PRECHECK` 任务，则取消该任务。
  - 将旧提交的 `parseStatus`、`checkStatus`、`scoreStatus` 标记为 `CANCELLED`。

修改效果：

- 学生短时间重复上传时，只保留最新提交进入 AI 门禁。
- 避免旧版本成果继续排队处理，减少无效 AI 调用。

### 2. `AsyncTaskService`

文件：`backend/src/main/java/com/bisai/service/AsyncTaskService.java`

主要修改：

- 新增延迟任务创建方法 `createDelayedTask`。
- 新增任务去重方法 `createTaskIfAbsent`。
- 异步任务执行分支新增 `PRECHECK`：

```java
case "PRECHECK" -> aiService.doPrecheck(fresh.getBizId(), fresh.getId());
```

- 原本任务成功后直接在执行方法里更新提交状态，现在改为统一调用 `handleTaskSuccess` 推进流程。
- 新的成功推进链路：
  - `PRECHECK` 成功后进入 `PARSE`
  - `PARSE` 成功后进入 `CHECK`
  - `CHECK` 成功后进入 `SCORE`
  - `SCORE` 成功后进入 `AI_SCORED`
- 失败、取消、重试时增加 `PRECHECK` 对应状态处理。
- 取消任务时，`PRECHECK` 和 `PARSE` 都会将解析状态置为 `CANCELLED`。
- 手动重试失败任务时，会根据任务类型恢复对应的处理中状态。

修改效果：

- 异步任务链路由统一入口推进，减少各阶段状态更新分散的问题。
- 支持 5 分钟延迟执行和门禁预检。
- 避免同一个提交重复创建同类型异步任务。

### 3. `AiService`

文件：`backend/src/main/java/com/bisai/service/AiService.java`

主要修改：

- 新增 `UserMapper` 依赖，用于读取提交学生信息。
- 新增 `doPrecheck` 方法，作为 AI 门禁预检入口。
- 预检内容包括：
  - 文档中是否包含或高度疑似提及学生姓名。
  - 文档中是否包含学生学号或账号。
  - 报告内容是否与任务标题和任务要求相关。
  - 成果是否有实质性内容，避免空文档或纯模板进入后续流程。
- 预检会读取提交文件的前 2000 字符，并对图片文件调用多模态分析。
- AI 返回 JSON，核心字段包括：

```json
{
  "passed": true,
  "reason": "说明",
  "details": {
    "nameMatched": true,
    "studentIdMatched": true,
    "titleMatched": true,
    "contentValid": true
  }
}
```

- 新增 `handlePrecheckFail`，用于门禁失败后的自动处理：
  - 将 `scoreStatus` 设置为 `RETURNED`。
  - 将 `parseStatus` 设置为 `FAILED`。
  - 将退回原因写入 `teacherComment`。
  - 发送消息通知学生重新提交。
  - 更新异步任务进度为“门禁校验未通过，已执行自动打回”。

修改效果：

- 基础信息明显不匹配、内容无效或跑题的提交会在正式解析评分前被自动退回。
- 无效提交不会继续消耗后续解析、核查、评分资源。

### 4. `ScoreService`

文件：`backend/src/main/java/com/bisai/service/ScoreService.java`

主要修改：

- 手动触发解析、核查、评分时，使用 `createTaskIfAbsent` 防止重复创建任务。
- 手动核查增加前置条件：

```java
if (!"SUCCESS".equals(submission.getParseStatus())) {
    return Result.error("前置解析未完成或未通过，无法执行核查");
}
```

- 手动评分增加前置条件：

```java
if (!"SUCCESS".equals(submission.getCheckStatus())) {
    return Result.error("前置核查未完成或未通过，无法执行评分");
}
```

- 发布成绩增加前置条件：必须先完成教师复核，即 `scoreStatus = TEACHER_CONFIRMED`。

修改效果：

- 防止教师绕过解析、核查流程直接评分。
- 防止 AI 评分完成后未经教师复核直接发布成绩。
- 保证 `解析 -> 核查 -> 评分 -> 教师复核 -> 发布` 的流程顺序。

### 5. `TaskService`

文件：`backend/src/main/java/com/bisai/service/TaskService.java`

主要修改：

- 批量解析、批量核查、批量评分创建异步任务时，改用 `createTaskIfAbsent`。

修改效果：

- 批量操作时避免重复创建同类型异步任务。
- 与单个提交的任务去重逻辑保持一致。

## 三、前端修改

### 1. 学生上传页 `Submit.vue`

文件：`frontend/src/views/student/Submit.vue`

主要修改：

- 原本上传后只展示单个异步任务进度，现在改为按阶段展示。
- 新增 AI 处理步骤条：
  - 解析
  - 核查
  - 评分
- 新增 `aiTasks` 保存当前提交关联的所有异步任务。
- 新增当前任务识别逻辑：
  - 优先显示 `RUNNING`、`RETRYING`、`PENDING` 的任务。
  - 如果没有进行中的任务，则按 `SCORE -> CHECK -> PARSE` 回退展示最新阶段。
- 当 `SCORE` 任务成功时，显示“AI 处理完成，等待教师复核”。
- 如果任一任务失败或取消，停止轮询并显示失败原因。

修改效果：

- 学生可以看到 AI 处理处于哪个阶段。
- 不再误以为只有解析一个阶段。
- 处理失败时能更清楚地看到失败发生在哪个阶段。

### 2. 教师评分页 `ScoreReview.vue`

文件：`frontend/src/views/teacher/ScoreReview.vue`

主要修改：

- 提交信息区域新增“评分状态”展示。
- AI 智能评分按钮增加可用条件：
  - 仅在 `NOT_SCORED`、`SCORE_FAILED`、`CANCELLED` 状态下可触发。
- `SCORING` 状态下禁用暂存、发布、退回操作。
- `PUBLISHED` 状态下隐藏普通保存、发布、退回操作，只保留成绩修正入口。
- 成绩修正按钮只在成绩已发布后显示。

修改效果：

- 教师端操作与后端状态约束保持一致。
- 避免评分中重复触发 AI 评分。
- 避免已发布成绩继续走普通保存流程。

### 3. 类型与状态映射

相关文件：

- `frontend/src/types/common.ts`
- `frontend/src/types/task.ts`
- `frontend/src/utils/status.ts`

主要修改：

- `ParseStatus` 增加 `CANCELLED`。
- `CheckStatus` 增加 `CANCELLED`。
- `ScoreStatus` 增加 `CANCELLED`。
- 新增 `AsyncTaskStatus` 类型：

```ts
export type AsyncTaskStatus = 'PENDING' | 'RUNNING' | 'RETRYING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'
```

- `AsyncTask.status` 从普通 `string` 改为 `AsyncTaskStatus`。
- 评分状态文案和颜色映射补充 `CANCELLED`。

修改效果：

- 前端类型定义与后端新增状态保持一致。
- 取消状态可以被正确显示为“已取消”。

## 四、系统配置与项目说明文档修改

### 1. `frontend/src/api/system.ts`

主要修改：

- `SystemConfigMap` 的索引类型从只允许字符串，改为允许字符串或数字：

```ts
[key: string]: string | number | undefined
```

- `testModelConnection` 参数允许传入 `model`：

```ts
export function testModelConnection(data: { apiUrl: string; apiKey: string; model?: string })
```

修改效果：

- 管理后台模型配置中 `timeout`、`temperature`、`maxTokens` 等数字字段可以通过类型检查。
- 模型连通性测试可以带上当前选择的模型名称。

### 2. `CLAUDE.md`

主要修改：

- 更新 AI 配置说明：默认模型为 `Qwen3.5-35B-A3B`，管理后台支持动态切换模型。
- 补充后端配置模块说明：
  - `JacksonConfig`
  - `AiConfig`
- 补充系统配置管理说明：
  - 前端字段名与数据库配置 key 存在映射关系。
  - `SystemService` 维护双向映射。
  - 配置更新后会刷新 `AiConfig`，无需重启。
- 补充 `LocalDateTime` 序列化约定：
  - 后端统一使用 `yyyy-MM-dd HH:mm:ss`。
  - 前端日期选择器需要使用匹配的 `value-format`。
- 补充课程权限说明：
  - 教师只能查看和管理自己的课程。
  - 教师只能在自己的课程下创建任务。
  - 学生只能访问自己的提交。
- 补充文件路径说明：
  - 报告生成使用绝对路径。
  - 文件访问时避免相对路径重复拼接。

修改效果：

- 项目说明文档更贴合当前实现。
- 后续开发时更容易避免系统配置、时间格式、权限和文件路径相关问题。

## 五、验证结果

本次修改后已执行以下验证：

```bash
cd backend && mvn compile
```

结果：通过。

```bash
cd frontend && npx vue-tsc --noEmit
```

结果：通过。

```bash
cd frontend && npm run build
```

结果：通过。

构建时仅存在 Vite chunk 体积警告，不影响构建成功。

## 六、本次涉及的已跟踪文件

本说明基于以下 Git 已跟踪文件的修改生成：

- `CLAUDE.md`
- `backend/src/main/java/com/bisai/service/AiService.java`
- `backend/src/main/java/com/bisai/service/AsyncTaskService.java`
- `backend/src/main/java/com/bisai/service/ScoreService.java`
- `backend/src/main/java/com/bisai/service/SubmissionService.java`
- `backend/src/main/java/com/bisai/service/TaskService.java`
- `frontend/src/api/system.ts`
- `frontend/src/types/common.ts`
- `frontend/src/types/task.ts`
- `frontend/src/utils/status.ts`
- `frontend/src/views/student/Submit.vue`
- `frontend/src/views/teacher/ScoreReview.vue`

未跟踪目录 `shannon/` 未纳入本说明。

