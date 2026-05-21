# 实训成果智能核查与评价系统

基于 AI 的实训成果自动化核查与评分平台，支持文档智能解析、规范核查、AI 评分和教师复核全流程。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.4.3 |
| 语言 | Java | 17 |
| 安全框架 | Spring Security + JWT | — |
| ORM | MyBatis-Plus | 3.5.9 |
| AI | Spring AI + ModelScope (Qwen3.5-35B-A3B) | 1.0.0 |
| 文档解析 | PDFBox / POI / docx4j / iText | 3.0.3 / 5.3.0 / 11.4.11 / 8.0.4 |
| 数据库 | MySQL | 8.0 |
| 前端框架 | Vue 3 + TypeScript | 3.5 |
| 构建工具 | Vite | 8 |
| UI 组件 | Element Plus | 2.13 |
| 状态管理 | Pinia | 3.0 |
| 图表 | ECharts | 6.0 |
| HTTP 客户端 | Axios | 1.15 |

## 系统架构

```
┌─────────────────────────────────────────────────────┐
│                    前端 (Vue 3)                       │
│  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐            │
│  │ 学生  │  │ 教师  │  │管理员│  │ 登录  │            │
│  └──┬───┘  └──┬───┘  └──┬───┘  └──┬───┘            │
│     └──────────┴─────────┴──────────┘                │
│              Axios + Pinia + Vue Router               │
│                  Vite Proxy :3000                     │
└────────────────────┬────────────────────────────────┘
                     │ /api/*
┌────────────────────┴────────────────────────────────┐
│                后端 (Spring Boot 8080)                 │
│  ┌────────────┐   ┌────────────┐   ┌─────────────┐  │
│  │ Controller │──▶│  Service   │──▶│   Mapper    │  │
│  │ (16 REST)  │   │ (业务逻辑) │   │ (MyBatis+)  │  │
│  └────────────┘   └─────┬──────┘   └──────┬──────┘  │
│                         │                   │         │
│  ┌──────────────────────┤                   │         │
│  │ SecurityConfig       │            ┌──────┴──────┐  │
│  │ JWT Filter + CORS    │            │   MySQL 8   │  │
│  └──────────────────────┘            │   bisai     │  │
│                                      └─────────────┘  │
│  ┌────────────┐   ┌────────────────────────────────┐  │
│  │ AsyncTask  │   │        AI 服务层                │  │
│  │ Scheduler  │   │ ModelScopeClient → Qwen3.5     │  │
│  │ (5s轮询)   │   │ KnowledgeService (RAG 检索)    │  │
│  └────────────┘   └────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

## 核心业务流程

```
任务创建 → 学生上传文件 → AI文档解析 → AI智能核查 → AI评分 → 教师复核 → 成绩发布
  教师         学生        系统(异步)     系统(异步)    系统(异步)    教师       系统
```

1. **教师创建任务**：设定任务要求、评分标准和截止时间
2. **学生上传成果**：支持 doc/docx/pdf/xls/xlsx/jpg/png/zip 等格式
3. **AI 文档解析**：异步解析文档内容（文本提取、图片 OCR）
4. **AI 智能核查**：基于任务要求核查文档规范性与完整性
5. **AI 智能评分**：结合知识库 RAG 检索结果进行评分，生成详细评语
6. **教师复核**：教师查看 AI 评分结果，可进行校准和调整
7. **成绩发布**：最终确认后学生可查看评价结果

## 角色与权限

### 学生 (STUDENT)
- 查看实训任务列表和详情
- 上传实训成果文件
- 查看自己的提交记录
- 查看 AI 评价结果和评语
- 消息中心

### 教师 (TEACHER)
- 任务管理（创建/编辑/删除）
- 提交管理和文件预览
- 解析详情查看
- 成果核查（逐份/批量）
- 评分复核与校准
- 评价模板管理
- 知识库管理
- 报表中心（可视化统计图表）
- 批量任务进度跟踪
- 消息中心

### 管理员 (ADMIN)
- 用户管理（增删改查）
- 班级课程管理
- 知识库管理
- AI 模型配置
- 系统日志查看
- 评分校准
- 批量任务管理
- 评价模板管理
- 消息中心

### 权限控制层级
- **URL 级**：Spring Security 配置，`/api/auth/**` 放行，其余需 JWT 认证
- **方法级**：Controller 使用 `@PreAuthorize` 注解控制角色访问
- **数据级**：Service 层按角色过滤，学生只能查看自己的数据，教师只能查看所管理课程的数据
- **文件访问**：通过 `submissionId` 关联验证权限，前端使用 axios blob 下载（非 `window.open`）
- **前端路由级**：路由 `meta.roles` 控制页面级访问
- **前端签名校验**：localStorage 用户信息带签名防篡改

## 项目结构

```
bisai/
├── backend/                          # 后端 Spring Boot 项目
│   ├── pom.xml                       # Maven 依赖配置
│   └── src/main/
│       ├── java/com/bisai/
│       │   ├── config/               # 配置类（6个）
│       │   │   ├── SecurityConfig    # 安全、CORS、JWT
│       │   │   ├── AsyncConfig       # AI 任务线程池
│       │   │   ├── AiConfig          # AI 模型配置
│       │   │   ├── WebMvcConfig      # MVC 配置
│       │   │   ├── MybatisPlusConfig # MyBatis-Plus 配置
│       │   │   └── GlobalExceptionHandler # 全局异常处理
│       │   ├── controller/           # REST 控制器（16个）
│       │   │   ├── AuthController    # 登录/注册/验证码
│       │   │   ├── TaskController    # 任务管理
│       │   │   ├── SubmissionController # 提交管理
│       │   │   ├── FileController    # 文件上传/下载/预览
│       │   │   ├── KnowledgeController # 知识库管理
│       │   │   ├── ReportController  # 报表导出
│       │   │   ├── CalibrationController # 评分校准
│       │   │   ├── AsyncTaskController   # 异步任务管理
│       │   │   ├── UserController    # 用户管理
│       │   │   ├── CourseController  # 课程管理
│       │   │   ├── ClassController   # 班级管理
│       │   │   ├── DashboardController   # 仪表盘数据
│       │   │   ├── EvaluationTemplateController # 评价模板管理
│       │   │   ├── LogController     # 系统日志
│       │   │   ├── MessageController # 消息通知
│       │   │   └── SystemController  # 系统配置
│       │   ├── service/              # 业务逻辑层（21个）
│       │   │   ├── AiService         # AI 解析/核查/评分核心
│       │   │   ├── ModelScopeClient  # ModelScope API 调用封装
│       │   │   ├── KnowledgeService  # 知识库管理（文档→分块→向量化）
│       │   │   ├── KnowledgeRetrievalService # RAG 两阶段检索
│       │   │   ├── ScoreService      # 评分流程管理
│       │   │   ├── AsyncTaskService  # 异步任务调度（5s 轮询）
│       │   │   ├── TaskService       # 批量操作并发控制
│       │   │   ├── DocumentTextExtractor # 文档文本提取
│       │   │   ├── ReportService     # 报表生成
│       │   │   ├── AiUsageService    # AI 调用用量追踪
│       │   │   ├── AuthService       # 认证服务
│       │   │   ├── CaptchaService    # 验证码服务
│       │   │   ├── UserService       # 用户管理
│       │   │   ├── CourseService     # 课程管理
│       │   │   ├── ClassService      # 班级管理
│       │   │   ├── DashboardService  # 仪表盘数据
│       │   │   ├── MessageService    # 消息服务
│       │   │   ├── SystemService     # 系统配置
│       │   │   ├── CalibrationService    # 评分校准
│       │   │   ├── SubmissionService     # 提交管理
│       │   │   └── EvaluationTemplateService # 评价模板
│       │   ├── entity/               # MyBatis-Plus 实体
│       │   ├── mapper/               # Mapper 接口
│       │   ├── dto/                  # 数据传输对象
│       │   ├── common/               # 公共类（Result、PageResult）
│       │   ├── interceptor/          # JWT 认证过滤器
│       │   └── util/                 # 工具类（JWT、JSON、验证码）
│       └── resources/
│           ├── application.yml       # 应用配置
│           └── schema.sql            # 数据库建表脚本（21张表）
│
├── frontend/                         # 前端 Vue 3 项目
│   ├── package.json                  # NPM 依赖
│   ├── vite.config.ts                # Vite 配置（代理 /api → 8080）
│   └── src/
│       ├── main.ts                   # 入口文件
│       ├── App.vue                   # 根组件
│       ├── router/
│       │   ├── index.ts              # 路由定义 + 导航守卫
│       │   └── guards.ts             # 三角色路由配置
│       ├── layouts/
│       │   └── MainLayout.vue        # 主布局（侧边栏 + 顶栏）
│       ├── views/
│       │   ├── login/                # 登录页
│       │   │   └── Index.vue         # 登录表单
│       │   ├── student/              # 学生端页面（6个）
│       │   │   ├── Home.vue          # 首页仪表盘
│       │   │   ├── TaskList.vue      # 任务列表
│       │   │   ├── TaskDetail.vue    # 任务详情
│       │   │   ├── Submit.vue        # 成果上传
│       │   │   ├── MySubmissions.vue # 我的提交
│       │   │   └── Result.vue        # 评价结果
│       │   ├── teacher/              # 教师端页面（14个）
│       │   │   ├── Home.vue          # 首页仪表盘
│       │   │   ├── TaskManage.vue    # 任务管理
│       │   │   ├── TaskEdit.vue      # 任务编辑
│       │   │   ├── Submissions.vue   # 提交管理
│       │   │   ├── FilePreview.vue   # 文件预览
│       │   │   ├── ParseDetail.vue   # 解析详情
│       │   │   ├── CheckDetail.vue   # 核查详情
│       │   │   ├── CheckList.vue     # 成果核查列表
│       │   │   ├── ScoreReview.vue   # 评分复核
│       │   │   ├── Reports.vue       # 报表中心
│       │   │   ├── Knowledge.vue     # 知识库管理
│       │   │   ├── TemplateManage.vue # 评价模板管理
│       │   │   ├── Calibration.vue   # 评分校准
│       │   │   └── BatchProgress.vue # 批量任务进度
│       │   ├── admin/                # 管理员页面（6个）
│       │   │   ├── Home.vue          # 管理仪表盘
│       │   │   ├── Users.vue         # 用户管理
│       │   │   ├── Classes.vue       # 班级课程管理
│       │   │   ├── Knowledge.vue     # 知识库管理
│       │   │   ├── ModelConfig.vue   # 模型配置
│       │   │   └── Logs.vue          # 系统日志
│       │   └── common/
│       │       ├── NotFound.vue      # 404 页面
│       │       └── Messages.vue      # 消息中心
│       ├── api/                      # API 请求模块（9个）
│       │   ├── auth.ts               # 认证接口
│       │   ├── course.ts             # 课程接口
│       │   ├── dashboard.ts          # 仪表盘接口
│       │   ├── knowledge.ts          # 知识库接口
│       │   ├── message.ts            # 消息接口
│       │   ├── report.ts             # 报表接口
│       │   ├── system.ts             # 系统配置接口
│       │   ├── task.ts               # 任务接口
│       │   └── user.ts               # 用户接口
│       ├── store/                    # Pinia 状态管理
│       │   ├── index.ts              # Store 入口
│       │   ├── user.ts               # 用户认证/角色
│       │   └── app.ts                # UI 状态
│       ├── types/                    # TypeScript 类型定义（5个）
│       │   ├── index.ts              # 类型导出
│       │   ├── common.ts             # 通用类型
│       │   ├── course.ts             # 课程类型
│       │   ├── task.ts               # 任务类型
│       │   └── user.ts               # 用户类型
│       ├── utils/
│       │   ├── request.ts            # Axios 封装（拦截器）
│       │   ├── auth.ts               # Token 工具
│       │   ├── status.ts             # 状态标签映射
│       │   └── date.ts               # 日期格式化
│       ├── assets/                   # 静态资源
│       └── styles/
│           └── index.scss            # 全局样式
│
├── CLAUDE.md                         # 项目开发指南
└── README.md                         # 本文件
```

## 快速开始

### 环境要求

- **JDK** 17+
- **Node.js** 18+
- **MySQL** 8.0+
- **Maven** 3.8+（或使用项目自带 mvnw）

### 1. 初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行建表脚本（包含建库语句和数据初始化）
source backend/src/main/resources/schema.sql;
```

默认管理员账号：`admin` / `admin123`

### 2. 启动后端

```bash
cd backend

# 编译项目
mvn compile

# 启动服务（端口 8080）
mvn spring-boot:run
```

后端启动后访问 `http://localhost:8080`。

### 3. 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器（端口 3000）
npm run dev
```

前端开发服务器会将 `/api` 请求代理到后端 `localhost:8080`。

### 4. 访问系统

打开浏览器访问 `http://localhost:3000`。

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_PASSWORD` | 数据库密码 | `123456` |
| `JWT_SECRET` | JWT 签名密钥 | 内置默认值 |
| `AI_API_KEY` | ModelScope API Key | 内置开发用 Key |

### 后端配置 ([application.yml](backend/src/main/resources/application.yml))

- **数据库连接**：`spring.datasource.url`，默认 `localhost:3306/bisai`
- **文件上传**：最大 200MB，存储路径 `./data/files/`
- **JWT 过期时间**：24 小时（86400000ms）
- **AI 模型**：Qwen/Qwen3.5-35B-A3B（聊天）、damo/nlp_corom_sentence-embedding_chinese-base（向量化）
- **AI 限制**：每日 Token 上限 200,000，调用上限 1,000 次
- **异步任务**：5 秒轮询间隔，最大重试 3 次（递增延迟），僵尸任务 10 分钟自动清理

### 前端配置 ([vite.config.ts](frontend/vite.config.ts))

- **开发端口**：3000
- **API 代理**：`/api` → `http://localhost:8080`

## 关键模块详解

### AI 服务 ([AiService](backend/src/main/java/com/bisai/service/AiService.java))

系统通过 `ModelScopeClient` 调用 ModelScope 平台的 Qwen 大模型，实现三大 AI 功能：

- **文档解析 (PARSE)**：提取 PDF/Word/Excel 等文档的文本内容和结构化信息
- **智能核查 (CHECK)**：基于任务要求检查文档的规范性、完整性和合规性
- **智能评分 (SCORE)**：结合知识库 RAG 检索的上下文，按评分指标生成评分和详细评语

### 知识库 RAG ([KnowledgeService](backend/src/main/java/com/bisai/service/KnowledgeService.java))

基于检索增强生成（RAG）的知识库系统：

1. **文档处理**：上传文档 → 解析文本 → 按 1200 字符分块
2. **向量化**：使用中文句向量模型将文本块向量化存储
3. **两阶段检索**：`KnowledgeRetrievalService` 先进行向量 Top-20 检索，再可选 Rerank 精排 Top-5
4. **上下文注入**：将检索到的相关知识片段注入 AI 评分 Prompt，提升评分准确性

### 异步任务 ([AsyncTaskService](backend/src/main/java/com/bisai/service/AsyncTaskService.java))

基于数据库轮询的异步任务调度系统：

- 任务类型：`PARSE`（解析）、`CHECK`（核查）、`SCORE`（评分）、`EXPORT`（导出）
- 调度方式：`@Scheduled(fixedDelay=5000)` 定时轮询数据库
- 容错机制：最多重试 3 次，采用递增延迟策略
- 僵尸清理：运行超过 10 分钟的任务自动标记为失败

### 文件管理 ([FileController](backend/src/main/java/com/bisai/controller/FileController.java))

- 上传支持格式：doc/docx/pdf/jpg/jpeg/png/xls/xlsx/zip
- 文件版本管理：最多保留 5 个历史版本
- 安全访问：通过 `submissionId` 关联验证权限，前端使用 axios blob 下载（非 `window.open`）

## 数据库表结构

核心数据表（共 21 张）：

| 表名 | 说明 |
|------|------|
| `user` | 用户表（学生/教师/管理员） |
| `training_task` | 实训任务表 |
| `submission` | 学生提交表 |
| `file` | 文件表（支持版本管理） |
| `async_task` | 异步任务表 |
| `parse_result` | 解析结果表 |
| `check_result` | 核查结果表 |
| `score_result` | 评分结果表 |
| `knowledge_base` | 知识库表 |
| `knowledge_document` | 知识库文档表 |
| `document_chunk` | 文档分块表 |
| `evaluation_template` | 评价模板表 |
| `indicator` | 评分指标表 |
| `course` | 课程表 |
| `class` | 班级表 |
| `ai_call_log` | AI 调用日志表 |
| `message` | 消息通知表 |
| `score_calibration` | 评分校准表 |
| `score_correction` | 评分修正表 |
| `system_config` | 系统配置表 |
| `operation_log` | 操作日志表 |

所有核心业务表均已启用逻辑删除（`deleted` 字段），ID 策略为自增。

## API 规范

- 响应统一使用 `Result<T>` 包装，格式为 `{ code, message, data }`
- 分页统一使用 `PageQuery`（接收 page、size 参数）→ `PageResult<T>`
- 认证接口 `/api/auth/**` 无需 Token
- 其余接口需在请求头携带 `Authorization: Bearer <token>`

## 构建部署

### 生产构建

```bash
# 后端打包
cd backend
mvn clean package -DskipTests
# 生成 backend/target/backend-1.0.0.jar

# 前端构建
cd frontend
npm run build
# 生成 frontend/dist/ 目录
```

### 部署建议

- 后端：`java -jar backend-1.0.0.jar`，配合 systemd 或 Docker 管理
- 前端：将 `dist/` 部署到 Nginx，配置反向代理 `/api` 到后端
- 数据库：确保 MySQL `utf8mb4` 字符集，配置定期备份

## 项目状态

| 模块 | 状态 |
|------|------|
| 后端开发 | ✅ 完成 |
| 前端开发 | ✅ 完成 |
| AI 集成 | ✅ 完成 |
| 安全加固 | ✅ 已修复 IDOR、路径遍历、Mass Assignment、配置暴露等 |
| 自动化测试 | 🚧 暂无 |
| CI/CD | 🚧 暂无 |
| Docker 部署 | 🚧 暂无 |

## 许可证

本项目仅供学习与实训使用。
