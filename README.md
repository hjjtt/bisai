# 实训成果智能核查与评价系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.5-42b883?logo=vuedotjs)](https://vuejs.org/)
[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0-3178c6?logo=typescript)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-Educational-lightgrey)]()

基于 AI 的实训成果自动化核查与评分平台，支持文档智能解析、规范核查、AI 评分和教师复核全流程。

## 功能亮点

- **智能文档解析** — 支持 PDF/Word/Excel 等多种格式的自动解析与内容提取
- **AI 规范核查** — 基于任务要求从完整性、规范性、原创性等维度智能核查
- **AI 智能评分** — 结合知识库 RAG 检索结果，按评价指标自动生成评分与评语
- **教师复核校准** — 教师可逐项调整 AI 评分，支持评分校准与修正
- **可视化报表** — ECharts 图表展示班级成绩分布、指标达成度等统计数据
- **动态模型配置** — 管理后台可实时切换 AI 模型（支持 ModelScope 平台所有模型），无需重启
- **三角色权限体系** — 学生/教师/管理员严格的数据隔离与多层权限控制

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 3.4.3 · Spring Security (JWT) · MyBatis-Plus 3.5.9 |
| **AI** | Spring AI 1.0 · ModelScope (Qwen / DeepSeek / Kimi 等可切换) · RAG 检索增强 |
| **文档** | PDFBox 3.0 · POI 5.3 · docx4j 11.4 · iText 8.0 |
| **数据库** | MySQL 8.0 (utf8mb4) |
| **前端** | Vue 3.5 · TypeScript 5 · Vite 8 · Element Plus 2.13 · Pinia 3.0 · ECharts 6.0 |
| **工程化** | Maven · npm · Axios |

## 系统架构

```
┌──────────────────────────────────────────────────────┐
│                    前端 (Vue 3 :3000)                  │
│   学生端 · 教师端 · 管理端 · 登录                       │
│   Axios + Pinia + Vue Router + Element Plus           │
└──────────────────────┬───────────────────────────────┘
                       │ /api/* (Vite Proxy)
┌──────────────────────┴───────────────────────────────┐
│                后端 (Spring Boot :8080)                 │
│                                                        │
│  Controller (16 REST) → Service (业务逻辑) → Mapper     │
│                                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │ Security     │  │ AI 服务层     │  │ AsyncTask   │  │
│  │ JWT + CORS   │  │ ModelScope   │  │ Scheduler   │  │
│  │ @PreAuthorize│  │ RAG 检索     │  │ (5s 轮询)   │  │
│  └──────────────┘  └──────────────┘  └─────────────┘  │
│                       │                                │
│              ┌────────┴────────┐                       │
│              │    MySQL 8.0    │                       │
│              │  bisai (21 表)  │                       │
│              └─────────────────┘                       │
└──────────────────────────────────────────────────────┘
```

## 核心业务流程

```
任务创建 → 学生上传 → AI 解析 → AI 核查 → AI 评分 → 教师复核 → 成绩发布
  教师       学生      异步       异步       异步       教师       系统
```

1. **教师创建任务** — 设定任务要求、评价模板和截止时间
2. **学生上传成果** — 支持 doc/docx/pdf/xls/xlsx/jpg/png/zip 等格式
3. **AI 文档解析** — 异步提取文档内容（文本提取、图片 OCR）
4. **AI 智能核查** — 基于任务要求核查文档规范性与完整性
5. **AI 智能评分** — 结合知识库 RAG 检索结果，按指标生成评分与评语
6. **教师复核** — 查看 AI 评分，可校准调整各指标分数
7. **成绩发布** — 确认后学生可查看评价结果，支持导出 PDF/Word 报告

## 角色与权限

| 角色 | 能力 |
|------|------|
| **学生** | 查看任务 · 上传成果 · 查看提交记录 · 查看评价结果 · 消息中心 |
| **教师** | 任务管理 · 提交管理 · 文件预览 · 解析/核查/评分复核 · 评价模板 · 知识库 · 报表中心 · 批量操作 |
| **管理员** | 用户管理 · 班级课程 · 知识库 · AI 模型配置 · 系统日志 · 评分校准 · 评价模板 |

**权限控制层级**：URL 级 (Security) → 方法级 (@PreAuthorize) → 数据级 (Service 过滤) → 文件级 (submissionId 关联) → 路由级 (meta.roles) → 签名校验 (localStorage 防篡改)

## 项目结构

```
bisai/
├── backend/                          # Spring Boot 后端
│   └── src/main/java/com/bisai/
│       ├── config/                   # Security · AI · Async · Jackson · MyBatis · MVC
│       ├── controller/               # 16 个 REST 控制器
│       ├── service/                  # 21 个业务服务
│       │   ├── AiService             #   AI 解析/核查/评分核心
│       │   ├── ModelScopeClient      #   ModelScope API 调用封装
│       │   ├── KnowledgeService      #   知识库（文档→分块→向量化）
│       │   ├── KnowledgeRetrievalService  # RAG 两阶段检索
│       │   └── ...
│       ├── entity/  mapper/  dto/    # 数据层
│       ├── interceptor/              # JWT 认证过滤器
│       └── util/                     # JWT · JSON · 验证码
│
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── views/
│       │   ├── student/             # 6 个学生页面
│       │   ├── teacher/             # 14 个教师页面
│       │   └── admin/               # 6 个管理员页面
│       ├── api/                      # 9 个 API 模块
│       ├── store/                    # Pinia (user · app)
│       ├── router/                   # 三角色路由守卫
│       ├── utils/                    # Axios 封装 · 状态映射 · 日期工具
│       └── types/                    # TypeScript 类型定义
│
├── CLAUDE.md                         # 开发指南
└── README.md
```

## 快速开始

### 环境要求

- JDK 17+ · Node.js 18+ · MySQL 8.0+ · Maven 3.8+

### 1. 初始化数据库

```bash
mysql -u root -p
source backend/src/main/resources/schema.sql;
```

默认管理员账号：`admin` / `admin123`

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run    # 端口 8080
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev            # 端口 3000，自动代理 /api → localhost:8080
```

打开浏览器访问 `http://localhost:3000`。

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_PASSWORD` | 数据库密码 | `123456` |
| `JWT_SECRET` | JWT 签名密钥 | 内置默认值 |
| `AI_API_KEY` | ModelScope API Key | 内置开发用 Key |

### AI 模型配置

系统支持动态切换 AI 模型，在管理后台 **模型配置** 页面可实时修改：

- **聊天模型** — 默认 `Qwen/Qwen3.5-35B-A3B`，可切换为 ModelScope 平台任意模型
- **向量化模型** — `damo/nlp_corom_sentence-embedding_chinese-base`
- **参数调节** — 温度参数、最大 Token 数、超时时间等均可在线调整
- **连通性测试** — 修改后可即时测试模型是否可用

> 模型 ID 需使用 ModelScope 平台完整格式，如 `moonshotai/Kimi-K2.6:DashScope`

### 异步任务配置

- 轮询间隔：5 秒 · 最大重试：3 次（递增延迟）· 僵尸清理：10 分钟

## 关键模块

| 模块 | 说明 |
|------|------|
| [AiService](backend/src/main/java/com/bisai/service/AiService.java) | 文档解析 · 智能核查 · 智能评分 |
| [KnowledgeService](backend/src/main/java/com/bisai/service/KnowledgeService.java) | 文档上传 → 解析 → 1200 字符分块 → 向量化存储 |
| [KnowledgeRetrievalService](backend/src/main/java/com/bisai/service/KnowledgeRetrievalService.java) | 向量 Top-20 检索 → 可选 Rerank Top-5 精排 |
| [AsyncTaskService](backend/src/main/java/com/bisai/service/AsyncTaskService.java) | 数据库轮询调度，处理 PARSE/CHECK/SCORE/EXPORT 任务 |
| [ReportService](backend/src/main/java/com/bisai/service/ReportService.java) | 生成含图表的 PDF/Word 报告 |
| [SystemService](backend/src/main/java/com/bisai/service/SystemService.java) | 系统配置管理，修改后即时刷新 AiConfig |

## 数据库

核心数据表共 21 张，均启用逻辑删除（`deleted` 字段），ID 自增策略。

| 分类 | 表 |
|------|-----|
| 用户 | `user` · `class` · `course` |
| 任务 | `training_task` · `submission` · `file` |
| AI | `parse_result` · `check_result` · `score_result` · `score_calibration` · `score_correction` |
| 知识库 | `knowledge_base` · `knowledge_document` · `document_chunk` |
| 系统 | `async_task` · `evaluation_template` · `indicator` · `ai_call_log` · `message` · `system_config` · `operation_log` |

## API 规范

- 响应格式：`Result<T>` — `{ code: 0, message, data }`
- 分页格式：`PageQuery` (page, size) → `PageResult<T>` (items, total)
- 认证：`/api/auth/**` 无需 Token，其余接口需 `Authorization: Bearer <token>`

## 构建部署

```bash
# 后端打包
cd backend && mvn clean package -DskipTests
# → backend/target/backend-1.0.0.jar

# 前端构建
cd frontend && npm run build
# → frontend/dist/

# 部署
java -jar backend-1.0.0.jar          # 后端
# 前端 dist/ 部署到 Nginx，反向代理 /api → 后端
```

## 项目状态

| 模块 | 状态 |
|------|------|
| 后端开发 | ✅ 完成 |
| 前端开发 | ✅ 完成 |
| AI 集成 | ✅ 完成（支持动态模型切换） |
| 安全加固 | ✅ IDOR · 路径遍历 · Mass Assignment · XSS 防护 |
| 自动化测试 | 🚧 计划中 |
| CI/CD | 🚧 计划中 |

## 许可证

本项目仅供学习与实训使用。
