/*
 Navicat Premium Data Transfer

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80041 (8.0.41)
 Source Host           : localhost:3306
 Source Schema         : bisai

 Target Server Type    : MySQL
 Target Server Version : 80041 (8.0.41)
 File Encoding         : 65001

 Date: 03/05/2026 14:32:30
*/
CREATE DATABASE IF NOT EXISTS bisai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE bisai;


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_call_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_call_log`;
CREATE TABLE `ai_call_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI调用ID',
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型名称',
  `call_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用类型',
  `input_tokens` int NOT NULL DEFAULT 0 COMMENT '输入token',
  `output_tokens` int NOT NULL DEFAULT 0 COMMENT '输出token',
  `total_tokens` int NOT NULL DEFAULT 0 COMMENT '总token',
  `success` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI调用日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_call_log
-- ----------------------------
INSERT INTO `ai_call_log` VALUES (40, 'Qwen/Qwen3.5-35B-A3B', 'CHAT', 24, 213, 237, 1, NULL, '2026-05-03 13:24:32');
INSERT INTO `ai_call_log` VALUES (41, 'Qwen/Qwen3.5-35B-A3B', 'CHAT', 1781, 3420, 5201, 1, NULL, '2026-05-03 13:25:23');
INSERT INTO `ai_call_log` VALUES (42, 'Qwen/Qwen3.5-35B-A3B', 'CHAT', 1781, 3643, 5424, 1, NULL, '2026-05-03 13:26:03');
INSERT INTO `ai_call_log` VALUES (43, 'Qwen/Qwen3.5-35B-A3B', 'CHAT', 1781, 2886, 4667, 1, NULL, '2026-05-03 13:26:36');
INSERT INTO `ai_call_log` VALUES (44, 'Qwen/Qwen3.5-35B-A3B', 'CHAT', 1656, 4812, 6468, 1, NULL, '2026-05-03 13:29:40');
INSERT INTO `ai_call_log` VALUES (45, 'Qwen/Qwen3.5-35B-A3B', 'CHAT', 1781, 3951, 5732, 1, NULL, '2026-05-03 13:43:45');

-- ----------------------------
-- Table structure for async_task
-- ----------------------------
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型: PARSE/CHECK/SCORE/EXPORT',
  `biz_id` bigint NOT NULL COMMENT '业务ID(提交ID)',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED/RETRYING',
  `progress` int NOT NULL DEFAULT 0 COMMENT '进度百分比 0-100',
  `current_step` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前执行步骤描述',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `max_retry` int NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  `next_run_at` datetime NULL DEFAULT NULL COMMENT '下次执行时间',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_biz_id`(`biz_id` ASC) USING BTREE,
  INDEX `idx_next_run_at`(`next_run_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '异步任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of async_task
-- ----------------------------
INSERT INTO `async_task` VALUES (1, 'PARSE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:07:02', NULL, 0, '2026-05-03 11:07:01', '2026-05-03 11:07:01');
INSERT INTO `async_task` VALUES (2, 'SCORE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:07:15', NULL, 0, '2026-05-03 11:07:15', '2026-05-03 11:07:15');
INSERT INTO `async_task` VALUES (3, 'PARSE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:07:54', NULL, 0, '2026-05-03 11:07:53', '2026-05-03 11:07:53');
INSERT INTO `async_task` VALUES (4, 'PARSE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:13:28', NULL, 0, '2026-05-03 11:13:28', '2026-05-03 11:13:28');
INSERT INTO `async_task` VALUES (5, 'CHECK', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:14:08', NULL, 0, '2026-05-03 11:14:08', '2026-05-03 11:14:08');
INSERT INTO `async_task` VALUES (6, 'PARSE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:16:30', NULL, 0, '2026-05-03 11:16:30', '2026-05-03 11:16:30');
INSERT INTO `async_task` VALUES (7, 'PARSE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:17:38', NULL, 0, '2026-05-03 11:17:38', '2026-05-03 11:17:38');
INSERT INTO `async_task` VALUES (8, 'PARSE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:19:16', NULL, 0, '2026-05-03 11:19:15', '2026-05-03 11:19:15');
INSERT INTO `async_task` VALUES (9, 'PARSE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:19:42', NULL, 0, '2026-05-03 11:19:41', '2026-05-03 11:19:41');
INSERT INTO `async_task` VALUES (10, 'PARSE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:25:49', NULL, 0, '2026-05-03 11:25:48', '2026-05-03 11:25:48');
INSERT INTO `async_task` VALUES (11, 'PARSE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:31:08', NULL, 0, '2026-05-03 11:31:07', '2026-05-03 11:31:07');
INSERT INTO `async_task` VALUES (12, 'CHECK', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:34:19', NULL, 0, '2026-05-03 11:34:19', '2026-05-03 11:34:19');
INSERT INTO `async_task` VALUES (13, 'PARSE', 2, 'SUCCESS', 0, NULL, 0, 3, '2026-05-03 11:36:11', NULL, 0, '2026-05-03 11:36:11', '2026-05-03 11:36:11');
INSERT INTO `async_task` VALUES (14, 'PARSE', 2, 'SUCCESS', 0, '解析失败: AI 服务调用异常: AI 服务返回空响应，请重试', 0, 3, '2026-05-03 11:46:17', '解析失败: AI 服务调用异常: AI 服务返回空响应，请重试', 0, '2026-05-03 11:46:16', '2026-05-03 11:46:16');
INSERT INTO `async_task` VALUES (15, 'PARSE', 2, 'SUCCESS', 100, '解析完成', 0, 3, '2026-05-03 11:56:53', NULL, 0, '2026-05-03 11:56:52', '2026-05-03 11:56:52');
INSERT INTO `async_task` VALUES (16, 'PARSE', 2, 'FAILED', 5, '任务开始执行...', 3, 3, '2026-05-03 12:09:47', '智能解析失败: AI 服务调用异常: HTTP 400 - {\"error\":{\"message\":\"Model id : Qwen/Qwen2.5-72B-Instruct , has no provider supported\",\"request_id\":\"b0d877b3-47e9-411a-a3c6-1a3f066bbe2e\"}}', 0, '2026-05-03 12:08:11', '2026-05-03 12:08:11');
INSERT INTO `async_task` VALUES (17, 'PARSE', 2, 'SUCCESS', 100, '执行完成', 0, 3, '2026-05-03 12:29:38', NULL, 0, '2026-05-03 12:29:38', '2026-05-03 12:29:38');
INSERT INTO `async_task` VALUES (18, 'PARSE', 2, 'SUCCESS', 100, '执行完成', 0, 3, '2026-05-03 12:36:13', NULL, 0, '2026-05-03 12:36:13', '2026-05-03 12:36:13');
INSERT INTO `async_task` VALUES (19, 'PARSE', 2, 'SUCCESS', 100, '执行完成', 0, 3, '2026-05-03 12:47:09', NULL, 0, '2026-05-03 12:47:09', '2026-05-03 12:47:09');
INSERT INTO `async_task` VALUES (20, 'PARSE', 2, 'SUCCESS', 100, '执行完成', 0, 3, '2026-05-03 12:52:21', NULL, 0, '2026-05-03 12:52:21', '2026-05-03 12:52:21');
INSERT INTO `async_task` VALUES (21, 'SCORE', 2, 'SUCCESS', 100, '执行完成', 2, 3, '2026-05-03 12:54:35', '智能评分失败: AI 服务调用异常: AI 服务返回空响应，请重试', 0, '2026-05-03 12:52:55', '2026-05-03 12:52:55');
INSERT INTO `async_task` VALUES (22, 'CHECK', 2, 'SUCCESS', 100, '执行完成', 0, 3, '2026-05-03 13:24:48', NULL, 0, '2026-05-03 13:24:48', '2026-05-03 13:24:48');
INSERT INTO `async_task` VALUES (23, 'CHECK', 2, 'SUCCESS', 100, '执行完成', 0, 3, '2026-05-03 13:25:25', NULL, 0, '2026-05-03 13:25:24', '2026-05-03 13:25:24');
INSERT INTO `async_task` VALUES (24, 'CHECK', 2, 'SUCCESS', 100, '执行完成', 0, 3, '2026-05-03 13:25:49', NULL, 0, '2026-05-03 13:25:48', '2026-05-03 13:25:48');
INSERT INTO `async_task` VALUES (25, 'SCORE', 2, 'SUCCESS', 100, '执行完成', 0, 3, '2026-05-03 13:28:42', NULL, 0, '2026-05-03 13:28:41', '2026-05-03 13:28:41');
INSERT INTO `async_task` VALUES (26, 'CHECK', 2, 'SUCCESS', 100, '执行完成', 0, 3, '2026-05-03 13:43:04', NULL, 0, '2026-05-03 13:43:04', '2026-05-03 13:43:04');

-- ----------------------------
-- Table structure for check_result
-- ----------------------------
DROP TABLE IF EXISTS `check_result`;
CREATE TABLE `check_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏍告煡缁撴灉ID',
  `submission_id` bigint NOT NULL COMMENT '鎻愪氦ID',
  `check_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鏍告煡绫诲瀷',
  `check_item` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鏍告煡椤',
  `result` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '缁撴灉: COMPLETED/PARTIAL/NOT_COMPLETED',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '璇存槑',
  `evidence` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '璇佹嵁鐗囨?',
  `suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '淇?敼寤鸿?',
  `risk_level` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'LOW' COMMENT '椋庨櫓绛夌骇',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_submission_id`(`submission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鏍告煡缁撴灉琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of check_result
-- ----------------------------
INSERT INTO `check_result` VALUES (1, 2, '系统核查', 'AI核查任务', 'FAIL', 'AI核查失败: AI 服务调用异常: Cannot invoke \"org.springframework.ai.chat.model.Generation.getOutput()\" because the return value of \"org.springframework.ai.chat.model.ChatResponse.getResult()\" is null', NULL, '请检查模型配置、网络或重试核查任务。', 'HIGH', 1, '2026-05-03 11:34:20', '2026-05-03 13:25:23');
INSERT INTO `check_result` VALUES (2, 2, '内容完整性', '文档章节覆盖情况', 'FAIL', '提交内容缺少关键章节，代码实现部分未完结，且完全缺失测试与总结章节。', '文本在\'3.1.2 StudentService 业务类\'处截断（\'public Map<Strin...\'），且目录中的第四部分\'测试\'和第五部分\'总结与反思\'未出现在正文中。', '请补全代码实现部分，并补充完整的单元测试、集成测试描述及项目总结反思内容。', 'HIGH', 1, '2026-05-03 13:25:23', '2026-05-03 13:26:03');
INSERT INTO `check_result` VALUES (3, 2, '格式规范性', '文档结构与代码风格', 'PASS', '可见部分的文档结构清晰，层级分明；代码遵循 Java 命名规范，缩进整齐。', '目录结构完整，包名使用小写，类名大写，变量命名符合驼峰规范，注释清晰。', '保持现有格式标准，确保后续补全的内容也遵循相同的排版规范。', 'LOW', 1, '2026-05-03 13:25:23', '2026-05-03 13:26:03');
INSERT INTO `check_result` VALUES (4, 2, '原创性评估', '内容连贯性与逻辑一致性', 'PASS', '可见内容逻辑连贯，术语使用准确，未发现明显的复制粘贴痕迹或格式混乱现象。', '需求分析与设计文档对应良好，代码注释与实现逻辑一致，无突兀的无关内容。', '继续独立完成剩余内容的编写，避免直接复制网络模板导致逻辑断层。', 'LOW', 1, '2026-05-03 13:25:23', '2026-05-03 13:26:03');
INSERT INTO `check_result` VALUES (5, 2, '技术准确性', '技术实现与架构合理性', 'WARNING', '可见代码使用了正确的 Java OOP 特性（封装、泛型、Stream API），但无法验证完整逻辑，且 Service 层直接使用 HashMap 持久化与 DAO 层设计存在潜在冲突。', 'StudentService 中使用 \'private final Map<String, Student> studentMap = new HashMap<>();\' 进行内存存储，而设计要求包含数据库持久化，需确认是否已对接 DAO 层。', '检查 Service 层与 DAO 层的交互逻辑，确保数据最终写入数据库而非仅保存在内存中，并完善异常处理机制。', 'MEDIUM', 1, '2026-05-03 13:25:23', '2026-05-03 13:26:03');
INSERT INTO `check_result` VALUES (6, 2, '任务匹配度', '任务要求达成情况', 'FAIL', '未完成任务要求的\'分析、设计、代码、测试、总结\'全流程交付，缺失测试与总结环节。', '任务明确要求包含\'Test\'和\'Summary\'，但提交内容中这两部分完全缺失。', '严格按照任务书要求，补充系统测试用例截图及运行结果，并撰写个人实训总结与反思。', 'HIGH', 1, '2026-05-03 13:25:23', '2026-05-03 13:26:03');
INSERT INTO `check_result` VALUES (7, 2, '内容完整性', '实训报告章节覆盖情况', 'FAIL', '提交内容严重缺失，代码实现部分未完成，且缺少测试与总结章节。', '文本在\'3.1.2 StudentService 业务类\'处截断（\'public Map<Strin...\'），未包含 3.2、3.3 节，完全缺失第四部分\'测试\'和第五部分\'总结与反思\'。', '请补充完整的代码实现（含异常处理、关键算法）、单元测试与集成测试用例、以及项目总结与反思内容。', 'HIGH', 1, '2026-05-03 13:26:03', '2026-05-03 13:26:35');
INSERT INTO `check_result` VALUES (8, 2, '格式规范性', '文档结构与代码风格', 'WARNING', '目录结构清晰，但代码片段存在明显的复制截断痕迹，影响阅读与编译。', '代码部分出现 \'public Map<Strin...\' 等不完整语句，Getter/Setter 方法被标记为省略但未提供完整文件。', '确保提交的代码文件完整无缺，检查所有类的方法实现是否闭合，保持代码缩进规范。', 'MEDIUM', 1, '2026-05-03 13:26:03', '2026-05-03 13:26:35');
INSERT INTO `check_result` VALUES (9, 2, '原创性评估', '内容连贯性与抄袭风险', 'PASS', '可见内容逻辑连贯，术语使用一致，未发现明显的拼凑或混乱痕迹。', '需求分析、设计与代码部分的描述风格统一，类名与数据库表设计对应关系合理。', '建议保留当前写作风格，注意后续章节保持一致的叙述深度。', 'LOW', 1, '2026-05-03 13:26:03', '2026-05-03 13:26:35');
INSERT INTO `check_result` VALUES (10, 2, '技术准确性', 'OOP 设计与代码实现正确性', 'PASS', '已展示的代码片段符合 Java OOP 规范，实体类封装及 Service 层逻辑基本正确。', 'Student 类使用了 private 字段、getter/setter、重写 equals/hashCode；Service 层使用 HashMap 存储数据。', '需验证剩余代码中泛型使用是否正确（如 Map<String, Student>），并确认数据库连接代码的准确性。', 'LOW', 1, '2026-05-03 13:26:03', '2026-05-03 13:26:35');
INSERT INTO `check_result` VALUES (11, 2, '任务匹配度', '任务要求达成情况', 'WARNING', '虽然标题和目录匹配任务要求，但实际交付物缺少关键的测试与总结环节。', '任务明确要求包含\'analysis,design,code,test,summary\'，目前仅完成 analysis, design, partial code。', '必须补充完整的测试报告（含截图或日志）和项目总结，以满足任务验收标准。', 'HIGH', 1, '2026-05-03 13:26:03', '2026-05-03 13:26:35');
INSERT INTO `check_result` VALUES (12, 2, '内容完整性', '实训报告章节覆盖情况', 'FAIL', '报告缺少关键章节内容，代码实现严重不完整。', '目录中包含‘四、测试’和‘五、总结’，但正文中完全缺失；代码部分在 3.1.2 节中断，语句为\'public Map<Strin...\'。', '必须补充完整的代码实现（包括 Service 层剩余部分）、测试章节（含单元测试/集成测试）以及总结反思部分。', 'HIGH', 1, '2026-05-03 13:26:36', '2026-05-03 13:43:45');
INSERT INTO `check_result` VALUES (13, 2, '格式规范性', '文档结构与代码风格', 'WARNING', '文档整体结构清晰，但存在文件截断现象。', '可见的代码缩进符合 Java 规范，注释清晰，但文件末尾非正常结束。', '请检查原始文件是否完整上传，确保所有代码块无截断。', 'MEDIUM', 1, '2026-05-03 13:26:36', '2026-05-03 13:43:45');
INSERT INTO `check_result` VALUES (14, 2, '原创性评估', '内容连贯性与抄袭风险', 'PASS', '现有文本逻辑通顺，无明显复制粘贴痕迹。', '需求分析与设计文档的表述符合学术规范，代码注释与实现逻辑一致。', '保持当前的写作质量，确保后续补充内容风格一致。', 'LOW', 1, '2026-05-03 13:26:36', '2026-05-03 13:43:45');
INSERT INTO `check_result` VALUES (15, 2, '技术准确性', 'Java OOP 与设计模式应用', 'PASS', '可见部分技术实现准确，符合面向对象设计原则。', 'Student 类正确使用了封装、equals/hashCode/toString 重写；架构采用三层架构与 DAO 模式。', '在后续代码中继续保持对异常处理和泛型集合的正确使用。', 'LOW', 1, '2026-05-03 13:26:36', '2026-05-03 13:43:45');
INSERT INTO `check_result` VALUES (16, 2, '任务匹配度', '任务要求达成情况', 'WARNING', '大纲匹配任务要求，但实际交付内容不足。', '任务要求包含 analysis,design,code,test,summary，目前仅有部分 analysis/design/code。', '严格对照任务清单中的五个阶段进行自查，确保每个阶段都有实质性产出。', 'MEDIUM', 1, '2026-05-03 13:26:36', '2026-05-03 13:43:45');
INSERT INTO `check_result` VALUES (17, 2, '内容完整性', '报告章节与代码完整性', 'WARNING', '目录包含测试与总结章节，但正文中未提供具体内容；代码实现部分在 StudentService 类处截断，未完成核心业务逻辑编写。', '目录显示\'四、测试\'和\'五、总结\'，但文档末尾结束于\'public Map<Strin...\'；无单元测试及总结反思内容。', '补充完整的测试用例代码及结果截图，完善总结与反思部分，修复代码截断问题。', 'HIGH', 0, '2026-05-03 13:43:45', '2026-05-03 13:43:45');
INSERT INTO `check_result` VALUES (18, 2, '格式规范性', '文档排版与代码风格', 'WARNING', '整体文档结构清晰，但代码片段存在非正常截断，影响阅读和后续编译验证。', '代码第 3.1.2 节 \'public Map<Strin...\' 语句不完整。', '检查文件导出或复制过程，确保代码完整无缺失，保持统一的缩进规范。', 'LOW', 0, '2026-05-03 13:43:45', '2026-05-03 13:43:45');
INSERT INTO `check_result` VALUES (19, 2, '原创性评估', '内容独特性与抄袭风险', 'PASS', '未发现明显的直接复制粘贴痕迹，需求分析与设计思路符合常规教学项目标准。', '文本描述虽通用但结构连贯，无明显异常重复段落。', '建议在总结部分增加个人具体的学习心得与难点攻克记录以增强原创性。', 'LOW', 0, '2026-05-03 13:43:45', '2026-05-03 13:43:45');
INSERT INTO `check_result` VALUES (20, 2, '技术准确性', 'OOP 设计与技术实现', 'PASS', '可见的实体类封装、DAO 模式设计及数据库表结构符合 Java OOP 规范，技术选型合理。', 'Student 类正确使用 equals/hashCode，Service 层使用 Map 管理数据。', '确保后续补充的代码中正确实现事务处理及连接池管理。', 'LOW', 0, '2026-05-03 13:43:45', '2026-05-03 13:43:45');
INSERT INTO `check_result` VALUES (21, 2, '任务匹配度', '任务要求覆盖情况', 'WARNING', '任务要求包含分析、设计、代码、测试、总结，当前提交仅完成分析及部分设计，代码与测试总结缺失。', 'TOC 齐全，但实际内容缺失关键交付物（测试报告、完整代码）。', '按照任务清单逐一核对，补全剩余模块的开发与文档撰写。', 'MEDIUM', 0, '2026-05-03 13:43:45', '2026-05-03 13:43:45');

-- ----------------------------
-- Table structure for class
-- ----------------------------
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鐝?骇ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鐝?骇鍚嶇О',
  `grade` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '骞寸骇',
  `major` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '涓撲笟',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '鐘舵?',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐝?骇琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of class
-- ----------------------------
INSERT INTO `class` VALUES (1, '计算机2301班', '2023', '计算机科学与技术', 'ENABLED', 0, '2026-04-28 17:48:49', '2026-04-28 17:48:49');

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '璇剧▼ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '璇剧▼鍚嶇О',
  `teacher_id` bigint NOT NULL COMMENT '浠昏?鏁欏笀ID',
  `class_id` bigint NOT NULL COMMENT '鎺堣?鐝?骇ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '璇剧▼璇存槑',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '鐘舵?',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璇剧▼琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course
-- ----------------------------
INSERT INTO `course` VALUES (1, 'Java程序设计', 2, 1, NULL, 'ENABLED', 0, '2026-04-28 17:48:49', '2026-04-28 17:48:49');

-- ----------------------------
-- Table structure for document_chunk
-- ----------------------------
DROP TABLE IF EXISTS `document_chunk`;
CREATE TABLE `document_chunk`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '切片ID',
  `knowledge_document_id` bigint NOT NULL COMMENT '知识库文档ID',
  `chunk_index` int NOT NULL COMMENT '切片序号',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '切片内容',
  `token_count` int NOT NULL DEFAULT 0 COMMENT '估算token数',
  `embedding` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '向量JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_document_id`(`knowledge_document_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库文档切片表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of document_chunk
-- ----------------------------

-- ----------------------------
-- Table structure for evaluation_template
-- ----------------------------
DROP TABLE IF EXISTS `evaluation_template`;
CREATE TABLE `evaluation_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '妯℃澘ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '妯℃澘鍚嶇О',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '妯℃澘璇存槑',
  `total_score` decimal(6, 2) NOT NULL DEFAULT 100.00,
  `creator_id` bigint NOT NULL COMMENT '鍒涘缓浜篒D',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '鐘舵?',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璇勪环妯℃澘琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of evaluation_template
-- ----------------------------
INSERT INTO `evaluation_template` VALUES (1, '实训报告评价模板', '通用实训报告评价标准', 100.00, 2, 'ENABLED', 0, '2026-04-28 17:48:49', '2026-04-28 17:48:49');

-- ----------------------------
-- Table structure for file
-- ----------------------------
DROP TABLE IF EXISTS `file`;
CREATE TABLE `file`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏂囦欢ID',
  `submission_id` bigint NULL DEFAULT NULL COMMENT '鎻愪氦ID',
  `knowledge_document_id` bigint NULL DEFAULT NULL COMMENT '鐭ヨ瘑搴撴枃妗?D',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍘熷?鏂囦欢鍚',
  `file_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鏂囦欢瀛樺偍璺?緞',
  `file_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鏂囦欢绫诲瀷',
  `file_size` bigint NOT NULL COMMENT '鏂囦欢澶у皬',
  `file_hash` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鏂囦欢鍝堝笇',
  `version` int NOT NULL DEFAULT 1 COMMENT '鏂囦欢鐗堟湰',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_submission_id`(`submission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鏂囦欢琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file
-- ----------------------------
INSERT INTO `file` VALUES (1, 2, NULL, 'Java_OOP_Training_实训报告.docx', 'D:\\vis\\bisai\\data\\files\\submissions\\1\\3\\1\\bd03041f-7c93-40ac-9abf-f083750017de.docx', 'DOCX', 43731, 'ae7c8a64a27faaa351b0c2c60316ec83', 1, 0, '2026-05-03 11:06:25');
INSERT INTO `file` VALUES (2, NULL, 1, 'Java_OOP_实训指导书.docx', 'D:\\vis\\bisai\\backend\\data\\files\\knowledge\\60d580e7-7f42-49b2-893a-ada0d021fd86.docx', 'DOCX', 42879, '722ccaee0f2be2c9c87c0ef035fbfd1d', 1, 0, '2026-05-03 11:56:15');

-- ----------------------------
-- Table structure for indicator
-- ----------------------------
DROP TABLE IF EXISTS `indicator`;
CREATE TABLE `indicator`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鎸囨爣ID',
  `template_id` bigint NOT NULL COMMENT '鎵?睘妯℃澘ID',
  `parent_id` bigint NULL DEFAULT NULL COMMENT '鐖剁骇鎸囨爣ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鎸囨爣鍚嶇О',
  `weight` decimal(6, 2) NOT NULL,
  `max_score` decimal(6, 2) NOT NULL,
  `score_rule` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '璇勫垎瑙勫垯',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '鎺掑簭',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_template_id`(`template_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璇勪环鎸囨爣琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of indicator
-- ----------------------------
INSERT INTO `indicator` VALUES (1, 1, NULL, '需求分析', 20.00, 20.00, '需求描述完整、准确', 1, 0, '2026-04-28 17:48:49', '2026-04-28 17:48:49');
INSERT INTO `indicator` VALUES (2, 1, NULL, '系统设计', 20.00, 20.00, '设计方案合理', 2, 0, '2026-04-28 17:48:49', '2026-04-28 17:48:49');
INSERT INTO `indicator` VALUES (3, 1, NULL, '功能实现', 25.00, 25.00, '功能完整、代码规范', 3, 0, '2026-04-28 17:48:49', '2026-04-28 17:48:49');
INSERT INTO `indicator` VALUES (4, 1, NULL, '测试验证', 15.00, 15.00, '测试用例覆盖充分', 4, 0, '2026-04-28 17:48:49', '2026-04-28 17:48:49');
INSERT INTO `indicator` VALUES (5, 1, NULL, '文档表达', 10.00, 10.00, '文档清晰、格式规范', 5, 0, '2026-04-28 17:48:49', '2026-04-28 17:48:49');
INSERT INTO `indicator` VALUES (6, 1, NULL, '总结反思', 10.00, 10.00, '总结深刻、有反思', 6, 0, '2026-04-28 17:48:49', '2026-04-28 17:48:49');

-- ----------------------------
-- Table structure for knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鐭ヨ瘑搴揑D',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鐭ヨ瘑搴撳悕绉',
  `course_id` bigint NULL DEFAULT NULL COMMENT '閫傜敤璇剧▼ID',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '璇存槑',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '鐘舵?',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐭ヨ瘑搴撹〃' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_base
-- ----------------------------
INSERT INTO `knowledge_base` VALUES (1, '课程知识库-1', 1, NULL, 'ENABLED', 0, '2026-05-03 11:56:16', '2026-05-03 11:56:16');

-- ----------------------------
-- Table structure for knowledge_document
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_document`;
CREATE TABLE `knowledge_document`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏂囨。ID',
  `knowledge_base_id` bigint NOT NULL COMMENT '鐭ヨ瘑搴揑D',
  `file_id` bigint NULL DEFAULT NULL COMMENT '鍏宠仈鏂囦欢ID',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍘熷?鏂囦欢鍚',
  `parse_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '瑙ｆ瀽鐘舵?',
  `vector_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '鍚戦噺鍖栫姸鎬',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '鏄?惁鍚?敤',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_knowledge_base_id`(`knowledge_base_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐭ヨ瘑搴撴枃妗ｈ〃' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_document
-- ----------------------------
INSERT INTO `knowledge_document` VALUES (1, 1, 2, 'Java_OOP_实训指导书.docx', 'PENDING', 'PENDING', 1, 0, '2026-05-03 11:56:16', '2026-05-03 11:56:16');

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '娑堟伅ID',
  `user_id` bigint NOT NULL COMMENT '鎺ユ敹鐢ㄦ埛ID',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '娑堟伅绫诲瀷',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '娑堟伅鏍囬?',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '娑堟伅鍐呭?',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '鏄?惁宸茶?',
  `related_id` bigint NULL DEFAULT NULL COMMENT '鍏宠仈涓氬姟ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '娑堟伅琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of message
-- ----------------------------
INSERT INTO `message` VALUES (1, 2, 'SUBMISSION', '学生提交实训成果', '学生（ID:3）已提交任务「Java-OOP-Training」的成果文件（课程：Java程序设计，版本：1），请及时处理。', 1, 2, '2026-05-03 11:06:25');
INSERT INTO `message` VALUES (2, 2, 'AI_SCORE', '智能评分完成', '提交记录（ID:2）的智能评分已完成，请及时复核确认。', 1, 2, '2026-05-03 12:54:45');
INSERT INTO `message` VALUES (3, 2, 'AI_SCORE', '智能评分完成', '提交记录（ID:2）的智能评分已完成，请及时复核确认。', 1, 2, '2026-05-03 13:29:40');
INSERT INTO `message` VALUES (4, 3, 'SCORE_PUBLISHED', '实训成绩已发布', '您的实训任务（提交ID:2）成绩已发布，最终得分：0.00分，请及时查看。', 0, 2, '2026-05-03 13:42:59');

-- ----------------------------
-- Table structure for operation_log
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鏃ュ織ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '鎿嶄綔浜篒D',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鎿嶄綔浜虹敤鎴峰悕',
  `action` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鎿嶄綔绫诲瀷',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '鎿嶄綔鎻忚堪',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP鍦板潃',
  `request_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璇锋眰璺?緞',
  `request_method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璇锋眰鏂规硶',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1736 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鎿嶄綔鏃ュ織琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of operation_log
-- ----------------------------

-- ----------------------------
-- Table structure for parse_result
-- ----------------------------
DROP TABLE IF EXISTS `parse_result`;
CREATE TABLE `parse_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '解析结果ID',
  `submission_id` bigint NULL DEFAULT NULL COMMENT '提交ID',
  `knowledge_document_id` bigint NULL DEFAULT NULL COMMENT '知识库文档ID',
  `file_id` bigint NULL DEFAULT NULL COMMENT '文件ID',
  `parser_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '解析方式',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '解析文本',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '摘要',
  `main_topics` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '主题JSON',
  `completeness` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '完整度',
  `quality` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '质量',
  `suggestions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '建议JSON',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_submission_id`(`submission_id` ASC) USING BTREE,
  INDEX `idx_knowledge_document_id`(`knowledge_document_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '解析结果表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of parse_result
-- ----------------------------
INSERT INTO `parse_result` VALUES (1, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	CHECK (0-100)	成绩\n\n\n\n三、代码实现\n3.1 核心类实现\n3.1.1 Student 实体类\nStudent类作为系统核心实体，采用封装原则，所有字段设为private，通过getter/setter方法进行访问。重写了equals()、hashCode()和toString()方法。\npackage com.example.entity;\n\nimport java.util.Objects;\n\npublic class Student {\n    private String id;\n    private String name;\n    private String gender;\n    private int age;\n    private String major;\n    private String className;\n\n    public Student() {}\n\n    public Student(String id, String name, String gender,\n                   int age, String major, String className) {\n        this.id = id;\n        this.name = name;\n        this.gender = gender;\n        this.age = age;\n        this.major = major;\n        this.className = className;\n    }\n\n    // Getter & Setter 方法省略...\n\n    @Override\n    public boolean equals(Object o) {\n        if (this == o) return true;\n        if (o == null || getClass() != o.getClass())\n            return false;\n        Student student = (Student) o;\n        return Objects.equals(id, student.id);\n    }\n\n    @Override\n    public int hashCode() {\n        return Objects.hash(id);\n    }\n\n    @Override\n    public String toString() {\n        return String.format(\n            \"Student{id=\'%s\', name=\'%s\', major=\'%s\'}\",\n            id, name, major);\n    }\n}\n\n3.1.2 StudentService 业务类\nStudentService类封装了学生管理的核心业务逻辑，使用泛型集合进行数据管理，实现了CRUD操作和统计功能。\npackage com.example.service;\n\nimport com.example.entity.Student;\nimport java.util.*;\nimport java.util.stream.Collectors;\n\npublic class StudentService {\n    private final Map<String, Student> studentMap = new HashMap<>();\n\n    /**\n     * 添加学生 - 使用防重复检查\n     */\n    public boolean addStudent(Student student) {\n        Objects.requireNonNull(student, \"学生信息不能为空\");\n        if (studentMap.containsKey(student.getId())) {\n            throw new IllegalArgumentException(\n                \"学号 \" + student.getId() + \" 已存在\");\n        }\n        studentMap.put(student.getId(), student);\n        return true;\n    }\n\n    /**\n     * 按专业统计学生人数 - 使用Stream API\n     */\n    public Map<String, Long> countByMajor() {\n        return studentMap.values().stream()\n            .collect(Collectors.groupingBy(\n                Student::getMajor,\n                Collectors.counting()));\n    }\n\n    /**\n     * 分页查询 - 泛型方法\n     */\n    public List<Student> findByPage(int page, int size) {\n        return studentMap.values().stream()\n            .skip((long) (page - 1) * size)\n            .limit(size)\n            .collect(Collectors.toList());\n    }\n}\n3.2 关键算法\nGPA计算采用标准4.0算法，根据分数段映射到对应的绩点：\npublic static double calculateGPA(double score) {\n    if (score >= 90) return 4.0;\n    else if (score >= 85) return 3.7;\n    else if (score >= 82) return 3.3;\n    else if (score >= 78) return 3.0;\n    else if (score >= 75) return 2.7;\n    else if (score >= 72) return 2.3;\n    else if (score >= 68) return 2.0;\n    else if (score >= 64) return 1.5;\n    else if (score >= 60) return 1.0;\n    else return 0.0;\n}\n\npublic static double calculateWeightedGPA(\n        List<Grade> grades, List<Course> courses) {\n    double totalPoints = 0.0;\n    double totalCredits = 0.0;\n\n    Map<Integer, Course> courseMap = courses.stream()\n        .collect(Collectors.toMap(Course::getId, c -> c));\n\n    for (Grade grade : grades) {\n        Course course = courseMap.get(grade.getCourseId());\n        if (course != null) {\n            totalPoints += calculateGPA(grade.getScore())\n                         * course.getCredit();\n            totalCredits += course.getCredit();\n        }\n    }\n    return totalCredits > 0\n        ? totalPoints / totalCredits : 0.0;\n}\n3.3 异常处理\n系统设计了自定义异常类层次结构，提供精细化的错误处理：\n• BusinessException：业务逻辑异常基类\n• DuplicateStudentException：学号重复异常\n• InvalidScoreException：成绩无效异常（0-100范围外）\n• DataPersistenceException：数据持久化异常\n\n所有异常统一通过GlobalExceptionHandler进行捕获和处理，确保程序不会因未处理异常而崩溃，同时向用户提供友好的错误提示信息。\n\n\n四、测试\n4.1 单元测试\n使用JUnit 5框架编写单元测试，覆盖核心业务逻辑。测试类遵循AAA（Arrange-Act-Assert）模式。\nimport org.junit.jupiter.api.*;\nimport static org.junit.jupiter.api.Assertions.*;\n\nclass StudentServiceTest {\n    private StudentService service;\n\n    @BeforeEach\n    void setUp() {\n        service = new StudentService();\n    }\n\n    @Test\n    @DisplayName(\"添加学生 - 正常情况\")\n    void testAddStudent() {\n        Student stu = new Student(\n            \"2024001\", \"张三\", \"男\", 20,\n            \"计算机科学\", \"2024-1班\");\n        assertTrue(service.addStudent(stu));\n        assertEquals(1, service.getTotalCount());\n    }\n\n    @Test\n    @DisplayName(\"添加学生 - 学号重复应抛出异常\")\n    void testAddDuplicateStudent() {\n        Student s1 = new Student(\n            \"2024001\", \"张三\", \"男\", 20,\n            \"计算机科学\", \"2024-1班\");\n        service.addStudent(s1);\n\n        Student s2 = new Student(\n            \"2024001\", \"李四\", \"男\", 21,\n            \"软件工程\", \"2024-2班\");\n\n        assertThrows(IllegalArgumentException.class,\n            () -> service.addStudent(s2));\n    }\n\n    @ParameterizedTest\n    @ValueSource(doubles = {0, 60, 78.5, 100})\n    @DisplayName(\"GPA计算 - 边界值测试\")\n    void testGPACalculation(double score) {\n        double gpa = StatisticsUtil.calculateGPA(score);\n        assertTrue(gpa >= 0.0 && gpa <= 4.0);\n    }\n}\n4.2 集成测试\n对系统主要功能流程进行集成测试，验证各模块间的协作：\n• 学生信息完整流程：添加 → 查询 → 修改 → 查询 → 删除 → 查询，验证状态变化正确\n• 成绩录入与统计：录入多名学生成绩 → 计算班级平均分 → 计算GPA排名 → 验证统计结果\n• 数据持久化：添加数据 → 关闭系统 → 重新启动 → 验证数据完整恢复\n• 边界条件：空数据集查询、0分/100分成绩、超长姓名等极端情况的正确处理\n4.3 测试结果\n测试模块	用例数	通过	覆盖率\nStudent实体类	8	8	100%\nStudentService	15	15	95%\nGradeService	12	12	92%\nStatisticsUtil	10	10	100%\nStudentDAO	8	8	88%\n集成测试	6	6	-\n合计	59	59	93%\n\n\n所有59个测试用例全部通过，核心代码覆盖率达到93%。\n\n\n五、总结与反思\n5.1 项目成果\n本项目成功实现了一个基于Java面向对象技术的学生管理系统，具备以下特点：\n完整实现了学生信息管理、成绩管理、课程管理和统计分析四大核心功能模块。\n采用三层架构设计，各层职责清晰，耦合度低，便于维护和扩展。\n充分运用了面向对象的封装、继承、多态三大特性，代码结构清晰。\n使用泛型集合（List、Map）和Stream API处理数据，代码简洁高效。\n建立了完善的异常处理体系，系统健壮性良好。\n编写了59个测试用例，代码覆盖率达到93%，质量有保障。\n5.2 OOP设计原则的应用\n单一职责原则（SRP）：每个类只负责一项功能。例如Student类只封装数据，StudentService只处理业务逻辑，StudentDAO只负责数据存取。\n开闭原则（OCP）：通过接口和抽象类设计，系统对扩展开放、对修改关闭。新增数据存储方式只需实现DAO接口，无需修改业务层代码。\n里氏替换原则（LSP）：所有子类可以替换父类而不影响程序正确性。例如FileDAO和DatabaseDAO都可以作为BaseDAO使用。\n依赖倒置原则（DIP）：高层模块（Service）依赖抽象接口（DAO接口），而非具体实现类，通过依赖注入实现解耦。\n5.3 遇\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 11:19:44', '2026-05-03 11:19:44');
INSERT INTO `parse_result` VALUES (2, 2, NULL, NULL, 'AI', '【文件: Java_OOP_Training_实训报告.docx | 类型: DOCX | 大小: 43731字节】\n\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	CHECK (0-100)	成绩\n\n\n\n三、代码实现\n3.1 核心类实现\n3.1.1 Student 实体类\nStudent类作为系统核心实体，采用封装原则，所有字段设为private，通过getter/setter方法进行访问。重写了equals()、hashCode()和toString()方法。\npackage com.example.entity;\n\nimport java.util.Objects;\n\npublic class Student {\n    private String id;\n    private String name;\n    private String gender;\n    private int age;\n    private String major;\n    private String className;\n\n    public Student() {}\n\n    public Student(String id, String name, String gender,\n                   int age, String major, String className) {\n        this.id = id;\n        this.name = name;\n        this.gender = gender;\n        this.age = age;\n        this.major = major;\n        this.className = className;\n    }\n\n    // Getter & Setter 方法省略...\n\n    @Override\n    public boolean equals(Object o) {\n        if (this == o) return true;\n        if (o == null || getClass() != o.getClass())\n            return false;\n        Student student = (Student) o;\n        return Objects.equals(id, student.id);\n    }\n\n    @Override\n    public int hashCode() {\n        return Objects.hash(id);\n    }\n\n    @Override\n    public String toString() {\n        return String.format(\n            \"Student{id=\'%s\', name=\'%s\', major=\'%s\'}\",\n            id, name, major);\n    }\n}\n\n3.1.2 StudentService 业务类\nStudentService类封装了学生管理的核心业务逻辑，使用泛型集合进行数据管理，实现了CRUD操作和统计功能。\npackage com.example.service;\n\nimport com.example.entity.Student;\nimport java.util.*;\nimport java.util.stream.Collectors;\n\npublic class StudentService {\n    private final Map<String, Student> studentMap = new HashMap<>();\n\n    /**\n     * 添加学生 - 使用防重复检查\n     */\n    public boolean addStudent(Student student) {\n        Objects.requireNonNull(student, \"学生信息不能为空\");\n        if (studentMap.containsKey(student.getId())) {\n            throw new IllegalArgumentException(\n                \"学号 \" + student.getId() + \" 已存在\");\n        }\n        studentMap.put(student.getId(), student);\n        return true;\n    }\n\n    /**\n     * 按专业统计学生人数 - 使用Stream API\n     */\n    public Map<String, Long> countByMajor() {\n        return studentMap.values().stream()\n            .collect(Collectors.groupingBy(\n                Student::getMajor,\n                Collectors.counting()));\n    }\n\n    /**\n     * 分页查询 - 泛型方法\n     */\n    public List<Student> findByPage(int page, int size) {\n        return studentMap.values().stream()\n            .skip((long) (page - 1) * size)\n            .limit(size)\n            .collect(Collectors.toList());\n    }\n}\n3.2 关键算法\nGPA计算采用标准4.0算法，根据分数段映射到对应的绩点：\npublic static double calculateGPA(double score) {\n    if (score >= 90) return 4.0;\n    else if (score >= 85) return 3.7;\n    else if (score >= 82) return 3.3;\n    else if (score >= 78) return 3.0;\n    else if (score >= 75) return 2.7;\n    else if (score >= 72) return 2.3;\n    else if (score >= 68) return 2.0;\n    else if (score >= 64) return 1.5;\n    else if (score >= 60) return 1.0;\n    else return 0.0;\n}\n\npublic static double calculateWeightedGPA(\n        List<Grade> grades, List<Course> courses) {\n    double totalPoints = 0.0;\n    double totalCredits = 0.0;\n\n    Map<Integer, Course> courseMap = courses.stream()\n        .collect(Collectors.toMap(Course::getId, c -> c));\n\n    for (Grade grade : grades) {\n        Course course = courseMap.get(grade.getCourseId());\n        if (course != null) {\n            totalPoints += calculateGPA(grade.getScore())\n                         * course.getCredit();\n            totalCredits += course.getCredit();\n        }\n    }\n    return totalCredits > 0\n        ? totalPoints / totalCredits : 0.0;\n}\n3.3 异常处理\n系统设计了自定义异常类层次结构，提供精细化的错误处理：\n• BusinessException：业务逻辑异常基类\n• DuplicateStudentException：学号重复异常\n• InvalidScoreException：成绩无效异常（0-100范围外）\n• DataPersistenceException：数据持久化异常\n\n所有异常统一通过GlobalExceptionHandler进行捕获和处理，确保程序不会因未处理异常而崩溃，同时向用户提供友好的错误提示信息。\n\n\n四、测试\n4.1 单元测试\n使用JUnit 5框架编写单元测试，覆盖核心业务逻辑。测试类遵循AAA（Arrange-Act-Assert）模式。\nimport org.junit.jupiter.api.*;\nimport static org.junit.jupiter.api.Assertions.*;\n\nclass StudentServiceTest {\n    private StudentService service;\n\n    @BeforeEach\n    void setUp() {\n        service = new StudentService();\n    }\n\n    @Test\n    @DisplayName(\"添加学生 - 正常情况\")\n    void testAddStudent() {\n        Student stu = new Student(\n            \"2024001\", \"张三\", \"男\", 20,\n            \"计算机科学\", \"2024-1班\");\n        assertTrue(service.addStudent(stu));\n        assertEquals(1, service.getTotalCount());\n    }\n\n    @Test\n    @DisplayName(\"添加学生 - 学号重复应抛出异常\")\n    void testAddDuplicateStudent() {\n        Student s1 = new Student(\n            \"2024001\", \"张三\", \"男\", 20,\n            \"计算机科学\", \"2024-1班\");\n        service.addStudent(s1);\n\n        Student s2 = new Student(\n            \"2024001\", \"李四\", \"男\", 21,\n            \"软件工程\", \"2024-2班\");\n\n        assertThrows(IllegalArgumentException.class,\n            () -> service.addStudent(s2));\n    }\n\n    @ParameterizedTest\n    @ValueSource(doubles = {0, 60, 78.5, 100})\n    @DisplayName(\"GPA计算 - 边界值测试\")\n    void testGPACalculation(double score) {\n        double gpa = StatisticsUtil.calculateGPA(score);\n        assertTrue(gpa >= 0.0 && gpa <= 4.0);\n    }\n}\n4.2 集成测试\n对系统主要功能流程进行集成测试，验证各模块间的协作：\n• 学生信息完整流程：添加 → 查询 → 修改 → 查询 → 删除 → 查询，验证状态变化正确\n• 成绩录入与统计：录入多名学生成绩 → 计算班级平均分 → 计算GPA排名 → 验证统计结果\n• 数据持久化：添加数据 → 关闭系统 → 重新启动 → 验证数据完整恢复\n• 边界条件：空数据集查询、0分/100分成绩、超长姓名等极端情况的正确处理\n4.3 测试结果\n测试模块	用例数	通过	覆盖率\nStudent实体类	8	8	100%\nStudentService	15	15	95%\nGradeService	12	12	92%\nStatisticsUtil	10	10	100%\nStudentDAO	8	8	88%\n集成测试	6	6	-\n合计	59	59	93%\n\n\n所有59个测试用例全部通过，核心代码覆盖率达到93%。\n\n\n五、总结与反思\n5.1 项目成果\n本项目成功实现了一个基于Java面向对象技术的学生管理系统，具备以下特点：\n完整实现了学生信息管理、成绩管理、课程管理和统计分析四大核心功能模块。\n采用三层架构设计，各层职责清晰，耦合度低，便于维护和扩展。\n充分运用了面向对象的封装、继承、多态三大特性，代码结构清晰。\n使用泛型集合（List、Map）和Stream API处理数据，代码简洁高效。\n建立了完善的异常处理体系，系统健壮性良好。\n编写了59个测试用例，代码覆盖率达到93%，质量有保障。\n5.2 OOP设计原则的应用\n单一职责原则（SRP）：每个类只负责一项功能。例如Student类只封装数据，StudentService只处理业务逻辑，StudentDAO只负责数据存取。\n开闭原则（OCP）：通过接口和抽象类设计，系统对扩展开放、对修改关闭。新增数据存储方式只需实现DAO接口，无需修改业务层代码。\n里氏替换原则（LSP）：所有子类可以替换父类而不影响程序正确性。例如FileDAO和DatabaseDAO都可以作为BaseDAO使用。\n依赖倒置原则（DIP）：高层模块（Service）依赖抽象接口（DAO接口），而非具体实现类，通过依赖注入实现解耦。\n5.3 遇\n...(内容过长已截断)\n\n', '本报告详述了Java学生管理系统的开发过程。包含需求分析、三层架构与数据库设计。实现了学生、成绩等核心业务逻辑，运用Stream API及自定义异常。通过JUnit 5进行单元测试，覆盖率93%。总结了OOP原则应用。结构清晰，技术实现规范，但结尾部分有截断。', '[\"Java面向对象编程\",\"三层架构设计\",\"MySQL数据库设计\",\"JUnit单元测试\",\"SOLID设计原则\",\"Stream API\",\"异常处理机制\"]', 'MEDIUM', 'HIGH', '[\"补充第5.3节关于遇到困难及解决方案的完整描述\",\"建议增加系统界面截图或部署说明\",\"考虑添加用户认证与密码加密功能\",\"完善数据库连接池配置细节\"]', 0, '2026-05-03 11:20:01', '2026-05-03 11:20:00');
INSERT INTO `parse_result` VALUES (3, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	CHECK (0-100)	成绩\n\n\n\n三、代码实现\n3.1 核心类实现\n3.1.1 Student 实体类\nStudent类作为系统核心实体，采用封装原则，所有字段设为private，通过getter/setter方法进行访问。重写了equals()、hashCode()和toString()方法。\npackage com.example.entity;\n\nimport java.util.Objects;\n\npublic class Student {\n    private String id;\n    private String name;\n    private String gender;\n    private int age;\n    private String major;\n    private String className;\n\n    public Student() {}\n\n    public Student(String id, String name, String gender,\n                   int age, String major, String className) {\n        this.id = id;\n        this.name = name;\n        this.gender = gender;\n        this.age = age;\n        this.major = major;\n        this.className = className;\n    }\n\n    // Getter & Setter 方法省略...\n\n    @Override\n    public boolean equals(Object o) {\n        if (this == o) return true;\n        if (o == null || getClass() != o.getClass())\n            return false;\n        Student student = (Student) o;\n        return Objects.equals(id, student.id);\n    }\n\n    @Override\n    public int hashCode() {\n        return Objects.hash(id);\n    }\n\n    @Override\n    public String toString() {\n        return String.format(\n            \"Student{id=\'%s\', name=\'%s\', major=\'%s\'}\",\n            id, name, major);\n    }\n}\n\n3.1.2 StudentService 业务类\nStudentService类封装了学生管理的核心业务逻辑，使用泛型集合进行数据管理，实现了CRUD操作和统计功能。\npackage com.example.service;\n\nimport com.example.entity.Student;\nimport java.util.*;\nimport java.util.stream.Collectors;\n\npublic class StudentService {\n    private final Map<String, Student> studentMap = new HashMap<>();\n\n    /**\n     * 添加学生 - 使用防重复检查\n     */\n    public boolean addStudent(Student student) {\n        Objects.requireNonNull(student, \"学生信息不能为空\");\n        if (studentMap.containsKey(student.getId())) {\n            throw new IllegalArgumentException(\n                \"学号 \" + student.getId() + \" 已存在\");\n        }\n        studentMap.put(student.getId(), student);\n        return true;\n    }\n\n    /**\n     * 按专业统计学生人数 - 使用Stream API\n     */\n    public Map<Strin\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 11:25:51', '2026-05-03 11:25:50');
INSERT INTO `parse_result` VALUES (4, 2, NULL, NULL, 'AI', '【文件: Java_OOP_Training_实训报告.docx | 类型: DOCX | 大小: 43731字节】\n\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	CHECK (0-100)	成绩\n\n\n\n三、代码实现\n3.1 核心类实现\n3.1.1 Student 实体类\nStudent类作为系统核心实体，采用封装原则，所有字段设为private，通过getter/setter方法进行访问。重写了equals()、hashCode()和toString()方法。\npackage com.example.entity;\n\nimport java.util.Objects;\n\npublic class Student {\n    private String id;\n    private String name;\n    private String gender;\n    private int age;\n    private String major;\n    private String className;\n\n    public Student() {}\n\n    public Student(String id, String name, String gender,\n                   int age, String major, String className) {\n        this.id = id;\n        this.name = name;\n        this.gender = gender;\n        this.age = age;\n        this.major = major;\n        this.className = className;\n    }\n\n    // Getter & Setter 方法省略...\n\n    @Override\n    public boolean equals(Object o) {\n        if (this == o) return true;\n        if (o == null || getClass() != o.getClass())\n            return false;\n        Student student = (Student) o;\n        return Objects.equals(id, student.id);\n    }\n\n    @Override\n    public int hashCode() {\n        return Objects.hash(id);\n    }\n\n    @Override\n    public String toString() {\n        return String.format(\n            \"Student{id=\'%s\', name=\'%s\', major=\'%s\'}\",\n            id, name, major);\n    }\n}\n\n3.1.2 StudentService 业务类\nStudentService类封装了学生管理的核心业务逻辑，使用泛型集合进行数据管理，实现了CRUD操作和统计功能。\npackage com.example.service;\n\nimport com.example.entity.Student;\nimport java.util.*;\nimport java.util.stream.Collectors;\n\npublic class StudentService {\n    private final Map<String, Student> studentMap = new HashMap<>();\n\n    /**\n     * 添加学生 - 使用防重复检查\n     */\n    public boolean addStudent(Student student) {\n        Objects.requireNonNull(student, \"学生信息不能为空\");\n        if (studentMap.containsKey(student.getId())) {\n            throw new IllegalArgumentException(\n                \"学号 \" + student.getId() + \" 已存在\");\n        }\n        studentMap.put(student.getId(), student);\n        return true;\n    }\n\n    /**\n     * 按专业统计学生人数 - 使用Stream API\n     */\n    public Map<Strin\n...(内容过长已截断)\n\n', '本报告为计算机专业学生张三提交的Java面向对象实训成果，项目为“学生管理系统”。报告涵盖需求分析、系统设计（三层架构、类图、数据库）、代码实现及测试规划。核心涉及实体类封装、业务逻辑层设计、DAO模式及MySQL持久化。代码展示了Student与Service类的关键实现，体现了良好的OOP规范。', '[\"Java 面向对象编程\",\"系统架构设计\",\"数据库设计\",\"代码实现\",\"异常处理\"]', 'MEDIUM', 'HIGH', '[\"补充完整的单元测试与集成测试结果\",\"增加数据安全性验证（如密码加密）\",\"完善数据库连接资源管理\",\"补充总结与反思章节的具体内容\"]', 0, '2026-05-03 11:26:16', '2026-05-03 11:26:15');
INSERT INTO `parse_result` VALUES (5, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 11:31:09', '2026-05-03 11:31:09');
INSERT INTO `parse_result` VALUES (6, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 11:36:14', '2026-05-03 11:36:13');
INSERT INTO `parse_result` VALUES (7, 2, NULL, NULL, 'AI', '【文件: Java_OOP_Training_实训报告.docx | 类型: DOCX | 大小: 43731字节】\n\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)\n\n', '该实训报告基于Java面向对象编程技术，设计并实现了一个学生管理系统。系统采用三层架构（表示层、业务逻辑层、数据访问层），使用Java Swing构建GUI，MySQL数据库持久化数据。功能包括学生信息CRUD、成绩管理、课程管理、数据统计与分析。报告涵盖了需求分析、系统设计（类图、数据库设计）、代码实现、测试和总结反思。', '[\"面向对象设计\",\"三层架构\",\"MVC模式\",\"DAO模式\",\"Java Swing\",\"MySQL数据库\",\"CRUD操作\",\"异常处理\",\"单元测试\",\"数据持久化\"]', 'MEDIUM', 'MEDIUM', '[\"补充完整的代码实现和测试结果\",\"增加系统界面截图或运行效果展示\",\"完善异常处理的具体实现细节\",\"添加性能优化或扩展性讨论\",\"提供更详细的数据库设计说明（如索引、外键约束）\"]', 0, '2026-05-03 11:36:17', '2026-05-03 11:36:17');
INSERT INTO `parse_result` VALUES (8, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 11:46:20', '2026-05-03 11:46:20');
INSERT INTO `parse_result` VALUES (9, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 11:56:55', '2026-05-03 11:56:54');
INSERT INTO `parse_result` VALUES (10, 2, NULL, NULL, 'AI', '【文件: Java_OOP_Training_实训报告.docx | 类型: DOCX | 大小: 43731字节】\n\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)\n\n', '本实训报告基于Java面向对象编程，设计并实现了一个学生管理系统。系统采用三层架构（表示层、业务逻辑层、数据访问层），支持学生信息、成绩、课程的增删改查及统计分析。使用MySQL数据库持久化数据，并设计了实体类、DAO和业务服务类。报告包含需求分析、系统设计、类图、数据库设计，但代码实现和测试部分被截断，缺少具体实现细节。', '[\"面向对象设计\",\"三层架构\",\"学生管理系统\",\"CRUD操作\",\"数据库设计\",\"Java Swing GUI\",\"DAO模式\",\"MVC模式\",\"成绩统计与分析\",\"异常处理\"]', 'MEDIUM', 'MEDIUM', '[\"补充核心代码实现，如StudentService、GradeService的具体方法逻辑\",\"提供单元测试和集成测试的详细用例及结果\",\"添加异常处理的具体实现示例\",\"完善数据统计与分析算法的代码片段\",\"补充系统截图或界面展示以增强可视化\"]', 0, '2026-05-03 11:57:08', '2026-05-03 11:57:08');
INSERT INTO `parse_result` VALUES (11, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 12:08:16', '2026-05-03 12:08:15');
INSERT INTO `parse_result` VALUES (12, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 12:08:46', '2026-05-03 12:08:46');
INSERT INTO `parse_result` VALUES (13, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 12:09:52', '2026-05-03 12:09:51');
INSERT INTO `parse_result` VALUES (14, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 12:29:42', '2026-05-03 12:29:41');
INSERT INTO `parse_result` VALUES (15, 2, NULL, NULL, 'AI', '【文件: Java_OOP_Training_实训报告.docx | 类型: DOCX | 大小: 43731字节】\n\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)\n\n', '该实训报告详细描述了学生管理系统的面向对象设计与实现过程，采用三层架构（表示层、业务逻辑层、数据访问层）和MVC模式，使用Java Swing构建GUI，MySQL数据库持久化。系统支持学生、课程、成绩的CRUD操作及统计分析，包含类图、数据库设计、异常处理、单元测试等内容，结构完整，逻辑清晰。', '[\"面向对象设计\",\"三层架构\",\"MVC模式\",\"DAO模式\",\"Java Swing\",\"数据库设计（MySQL）\",\"CRUD操作\",\"成绩统计与分析\",\"异常处理\",\"单元测试与集成测试\"]', 'HIGH', 'HIGH', '[\"补充更多代码实现细节，如核心类的具体方法实现\",\"增加性能优化策略，如数据库索引、缓存机制\",\"完善测试用例，覆盖边界条件和异常场景\",\"添加用户界面截图或交互流程图以增强可读性\"]', 0, '2026-05-03 12:29:47', '2026-05-03 12:29:46');
INSERT INTO `parse_result` VALUES (16, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 12:36:18', '2026-05-03 12:36:18');
INSERT INTO `parse_result` VALUES (17, 2, NULL, NULL, 'AI', '【文件: Java_OOP_Training_实训报告.docx | 类型: DOCX | 大小: 43731字节】\n\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)\n\n', '该实训报告围绕学生管理系统的面向对象设计与实现展开，采用三层架构（Swing表示层、业务逻辑层、DAO数据访问层）和MVC模式。系统支持学生、课程、成绩的CRUD操作，以及成绩统计与分析功能，使用MySQL数据库持久化。报告包含需求分析、系统设计（类图、数据库设计）、代码实现、测试和总结，结构完整，技术细节详实。', '[\"面向对象设计\",\"三层架构\",\"MVC模式\",\"DAO模式\",\"Java Swing\",\"数据库设计（MySQL）\",\"CRUD操作\",\"成绩统计与分析\",\"异常处理\",\"单元测试与集成测试\"]', 'MEDIUM', 'HIGH', '[\"补充完整的代码实现部分（当前被截断）\",\"增加更多边界测试用例\",\"完善异常处理的具体实现描述\",\"添加系统部署或运行说明\"]', 0, '2026-05-03 12:36:29', '2026-05-03 12:36:29');
INSERT INTO `parse_result` VALUES (18, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 12:47:13', '2026-05-03 12:47:13');
INSERT INTO `parse_result` VALUES (19, 2, NULL, NULL, 'AI', '【文件: Java_OOP_Training_实训报告.docx | 类型: DOCX | 大小: 43731字节】\n\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)\n\n', '该实训报告基于Java面向对象编程，设计并实现了一个学生管理系统。系统采用三层架构（表示层、业务逻辑层、数据访问层），使用Java Swing构建GUI，MySQL数据库持久化数据。功能包括学生信息CRUD、成绩管理、课程管理及数据统计分析。报告包含需求分析、系统设计（类图、数据库设计）、代码实现、测试和总结，但代码部分被截断。', '[\"面向对象编程\",\"三层架构\",\"MVC模式\",\"DAO模式\",\"Java Swing\",\"MySQL数据库\",\"CRUD操作\",\"异常处理\",\"单元测试\",\"集成测试\",\"数据统计分析\"]', 'MEDIUM', 'MEDIUM', '[\"补充完整的代码实现和测试结果\",\"添加系统截图或界面展示\",\"增加性能分析和优化建议\",\"完善异常处理的具体实现细节\"]', 0, '2026-05-03 12:47:21', '2026-05-03 12:47:20');
INSERT INTO `parse_result` VALUES (20, 2, NULL, 1, 'POI-DOCX', '\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)', NULL, NULL, NULL, NULL, NULL, 0, '2026-05-03 12:52:25', '2026-05-03 12:52:24');
INSERT INTO `parse_result` VALUES (21, 2, NULL, NULL, 'AI', '【文件: Java_OOP_Training_实训报告.docx | 类型: DOCX | 大小: 43731字节】\n\n\n\n\n实训报告\n\n学生管理系统的面向对象设计与实现\n\n实训项目：Java-OOP-Training\n课程名称：Java面向对象程序设计\n学生姓名：张三\n学号：2024010001\n专业班级：计算机科学与技术 2024-1班\n指导教师：李老师\n提交日期：2026年05月03日\n\n\n目录\n一、需求分析\n二、系统设计\n  2.1 系统架构设计\n  2.2 类图设计\n  2.3 数据库设计\n三、代码实现\n  3.1 核心类实现\n  3.2 关键算法\n  3.3 异常处理\n四、测试\n  4.1 单元测试\n  4.2 集成测试\n  4.3 测试结果\n五、总结与反思\n\n\n一、需求分析\n1.1 项目背景\n随着高校教育信息化的不断推进，学生管理工作面临着数据量大、管理流程复杂、信息共享困难等挑战。传统的人工管理方式效率低下，容易出错，已无法满足现代教学管理的需求。本项目旨在运用Java面向对象编程技术，设计并实现一个功能完善、易于维护的学生管理系统。\n1.2 功能需求\n• 学生信息管理：支持学生信息的增删改查（CRUD），包括学号、姓名、性别、年龄、专业、班级等基本信息的维护。\n• 成绩管理：支持课程成绩的录入、修改、查询和统计，能够计算加权平均分、GPA等指标。\n• 课程管理：支持课程信息的维护，包括课程名称、学分、授课教师等信息的管理。\n• 数据统计与分析：提供按班级、专业、课程等维度的成绩统计分析功能，支持排名、及格率、优秀率等指标的计算。\n• 数据持久化：使用文件或数据库实现数据的持久化存储，确保系统重启后数据不丢失。\n1.3 非功能需求\n系统应具有良好的可扩展性，便于后续功能扩展。\n界面友好，操作简便，提供清晰的操作提示和错误信息。\n数据输入应进行有效性验证，防止非法数据入库。\n系统应具备基本的异常处理能力，保证程序健壮性。\n1.4 用例分析\n系统主要包含以下用例：\nUC-01：管理员登录系统\nUC-02：添加学生信息\nUC-03：修改学生信息\nUC-04：删除学生信息（支持批量删除）\nUC-05：按条件查询学生\nUC-06：录入课程成绩\nUC-07：查看成绩统计报表\nUC-08：导出数据报表\n\n\n二、系统设计\n2.1 系统架构设计\n系统采用经典的三层架构（Three-Tier Architecture）进行设计：\n表示层（Presentation Layer）：使用Java Swing构建图形用户界面，负责用户交互和信息展示。采用MVC模式将视图与业务逻辑分离。\n业务逻辑层（Business Logic Layer）：封装核心业务逻辑，包括学生管理服务（StudentService）、成绩管理服务（GradeService）和统计服务（StatisticsService）。\n数据访问层（Data Access Layer）：使用DAO模式封装数据操作，支持基于文件和数据库两种持久化方式的灵活切换。\n2.2 类图设计\n系统核心类设计如下：\n\n类名	职责	关键属性/方法\nStudent	学生实体类	id, name, gender, age, major, className / getInfo()\nCourse	课程实体类	id, name, credit, teacher / getCreditHours()\nGrade	成绩实体类	studentId, courseId, score, semester / getGPA()\nStudentDAO	学生数据访问	insert(), update(), delete(), findById(), findAll()\nStudentService	学生业务逻辑	addStudent(), updateStudent(), getStatistics()\nStatisticsUtil	统计工具类	calculateAvg(), calculateGPA(), getRanking()\n\n\n2.3 数据库设计\n系统使用MySQL数据库，主要数据表设计如下：\n\n学生表（student）\n字段名	类型	约束	说明\nid	VARCHAR(20)	PRIMARY KEY	学号\nname	VARCHAR(50)	NOT NULL	姓名\ngender	CHAR(1)		性别\nage	INT		年龄\nmajor	VARCHAR(100)		专业\nclass_name	VARCHAR(50)		班级\n\n\n成绩表（grade）\n字段名	类型	约束	说明\nid	INT	PRIMARY KEY AUTO_INCREMENT	成绩ID\nstudent_id	VARCHAR(20)	FOREIGN KEY	学号\ncourse_id	INT	FOREIGN KEY	课程ID\nscore	DECIMAL(5,2)	C\n...(内容过长已截断)\n\n', '该实训报告详细描述了学生管理系统的面向对象设计与实现，包括需求分析、系统设计（三层架构、类图、数据库设计）、代码实现、测试和总结。系统支持学生信息、成绩、课程管理及数据统计，使用Java Swing和MySQL，采用MVC和DAO模式。', '[\"面向对象设计\",\"三层架构\",\"MVC模式\",\"DAO模式\",\"Java Swing\",\"MySQL数据库\",\"CRUD操作\",\"异常处理\",\"单元测试\",\"集成测试\"]', 'HIGH', 'MEDIUM', '[\"补充具体代码实现和关键算法细节\",\"提供测试用例和测试结果数据\",\"添加系统界面截图或原型图\",\"完善异常处理的具体实现方式\",\"增加用户权限管理或安全机制\"]', 0, '2026-05-03 12:52:30', '2026-05-03 12:52:29');

-- ----------------------------
-- Table structure for score_calibration
-- ----------------------------
DROP TABLE IF EXISTS `score_calibration`;
CREATE TABLE `score_calibration`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '校准记录ID',
  `task_id` bigint NOT NULL COMMENT '实训任务ID',
  `submission_id` bigint NOT NULL COMMENT '校准样本提交ID',
  `indicator_id` bigint NOT NULL COMMENT '指标ID',
  `calibration_score` decimal(6, 2) NOT NULL,
  `calibration_reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '校准理由',
  `typical_advantages` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '典型优点',
  `typical_problems` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '典型问题',
  `deduction_basis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '扣分依据',
  `confirmed_by` bigint NULL DEFAULT NULL COMMENT '确认人ID',
  `confirmed_at` datetime NULL DEFAULT NULL COMMENT '确认时间',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_submission_id`(`submission_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评分校准表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of score_calibration
-- ----------------------------

-- ----------------------------
-- Table structure for score_correction
-- ----------------------------
DROP TABLE IF EXISTS `score_correction`;
CREATE TABLE `score_correction`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '修正记录ID',
  `submission_id` bigint NOT NULL COMMENT '提交ID',
  `indicator_id` bigint NULL DEFAULT NULL COMMENT '指标ID，为空表示修正总分',
  `original_score` decimal(6, 2) NOT NULL,
  `new_score` decimal(6, 2) NOT NULL,
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '修正原因',
  `corrected_by` bigint NOT NULL COMMENT '修正人ID',
  `corrected_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修正时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_submission_id`(`submission_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '成绩修正表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of score_correction
-- ----------------------------

-- ----------------------------
-- Table structure for score_result
-- ----------------------------
DROP TABLE IF EXISTS `score_result`;
CREATE TABLE `score_result`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '璇勫垎缁撴灉ID',
  `submission_id` bigint NOT NULL COMMENT '鎻愪氦ID',
  `indicator_id` bigint NOT NULL COMMENT '鎸囨爣ID',
  `auto_score` decimal(6, 2) NULL DEFAULT NULL,
  `teacher_score` decimal(6, 2) NULL DEFAULT NULL,
  `final_score` decimal(6, 2) NULL DEFAULT NULL,
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '璇勫垎鐞嗙敱',
  `evidence` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '璇勫垎璇佹嵁',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_submission_id`(`submission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璇勫垎缁撴灉琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of score_result
-- ----------------------------
INSERT INTO `score_result` VALUES (1, 2, 1, 17.00, NULL, NULL, '需求描述较为完整，涵盖了项目背景、功能需求、非功能需求和用例分析，但用例分析较简单，未明确系统用户角色。', '一、需求分析中列出了5项功能需求（学生信息管理、成绩管理、课程管理、数据统计与分析、数据持久化）和8个用例（UC-01至UC-08）。', 1, '2026-05-03 12:54:46', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (2, 2, 2, 15.00, NULL, NULL, '设计方案合理，采用三层架构和MVC、DAO模式，类图设计清晰，但数据库设计缺少课程表，且未提供架构图。', '二、系统设计中描述了三层架构，类图表格列出了Student、Course、Grade等核心类及其职责，数据库设计给出了student和grade表，但缺少course表定义。', 1, '2026-05-03 12:54:46', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (3, 2, 3, 11.00, NULL, NULL, '仅展示了Student实体类的部分代码，其他核心类（如Course、Grade、DAO、Service）及界面代码缺失，代码规范但不够完整。', '三、代码实现中仅提供了Student类的代码片段（有省略号），未展示其他类实现和关键算法。', 1, '2026-05-03 12:54:46', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (4, 2, 4, 0.00, NULL, NULL, '测试部分仅有标题，无具体测试用例、测试过程或结果，未进行任何测试验证。', '四、测试部分仅列出4.1单元测试、4.2集成测试、4.3测试结果标题，无实际内容。', 1, '2026-05-03 12:54:46', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (5, 2, 5, 8.00, NULL, NULL, '文档结构清晰，有目录，格式规范，使用了表格和代码块，但代码块不完整，部分内容缺失。', '文档包含目录、章节标题、表格和代码片段，整体排版规范。', 1, '2026-05-03 12:54:46', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (6, 2, 6, 0.00, NULL, NULL, '总结与反思部分仅有标题，无任何内容，未进行总结和反思。', '五、总结与反思标题下无正文内容。', 1, '2026-05-03 12:54:46', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (7, 2, 1, 18.00, NULL, NULL, '需求描述较为完整，涵盖了背景、功能需求、非功能需求及用例分析。功能点明确（如 CRUD、成绩统计），但部分非功能需求的量化指标不够具体。', '1.2 功能需求 • 学生信息管理：支持学生信息的增删改查（CRUD）... 1.3 非功能需求 系统应具有良好的可扩展性... 1.4 用例分析 UC-01：管理员登录系统', 0, '2026-05-03 13:29:40', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (8, 2, 2, 17.00, NULL, NULL, '设计方案合理，采用了标准的三层架构和 DAO 模式。类图和数据库设计清晰，表结构定义规范。但缺少具体的交互时序图或详细的数据流说明。', '2.1 系统架构设计 系统采用经典的三层架构（Three-Tier Architecture）进行设计... 2.3 数据库设计 学生表（student）字段名 id VARCHAR(20) PRIMARY KEY', 0, '2026-05-03 13:29:40', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (9, 2, 3, 16.00, NULL, NULL, '代码片段展示了良好的面向对象编程实践（封装、重写方法）。但由于提交内容截断，无法验证业务逻辑层、持久化层及 UI 层的完整实现情况。', '3.1.1 Student 实体类 ... private String id; ... @Override public boolean equals(Object o) { ... } @Ov...', 0, '2026-05-03 13:29:40', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (10, 2, 4, 5.00, NULL, NULL, '文档中仅列出了测试章节的标题和分类，未提供具体的测试用例、输入数据、预期结果及实际运行截图，无法评估测试覆盖度。', '四、测试 4.1 单元测试 4.2 集成测试 4.3 测试结果', 0, '2026-05-03 13:29:40', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (11, 2, 5, 9.00, NULL, NULL, '文档结构清晰，包含封面信息、目录及分级标题。表格使用规范，排版整洁，符合实训报告的基本格式要求。', '目录 一、需求分析 二、系统设计 ... 学生表（student）字段名 类型 约束 说明', 0, '2026-05-03 13:29:40', '2026-05-03 13:29:40');
INSERT INTO `score_result` VALUES (12, 2, 6, 2.00, NULL, NULL, '虽然设置了总结与反思章节，但提交内容中该部分为空，缺乏对项目难点、收获及改进措施的实质性反思内容。', '五、总结与反思', 0, '2026-05-03 13:29:40', '2026-05-03 13:29:40');

-- ----------------------------
-- Table structure for submission
-- ----------------------------
DROP TABLE IF EXISTS `submission`;
CREATE TABLE `submission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鎻愪氦ID',
  `task_id` bigint NOT NULL COMMENT '瀹炶?浠诲姟ID',
  `student_id` bigint NOT NULL COMMENT '瀛︾敓ID',
  `submit_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鎻愪氦鏃堕棿',
  `version` int NOT NULL DEFAULT 1 COMMENT '鎻愪氦鐗堟湰鍙',
  `parse_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '瑙ｆ瀽鐘舵?',
  `check_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NOT_CHECKED' COMMENT '核查状态',
  `score_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'NOT_SCORED' COMMENT '璇勫垎鐘舵?',
  `total_score` decimal(6, 2) NULL DEFAULT NULL,
  `auto_total_score` decimal(6, 2) NULL DEFAULT NULL,
  `parse_summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `parse_topics` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `parse_completeness` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `parse_quality` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `parse_suggestions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `teacher_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '鏁欏笀璇勮?',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_student`(`task_id` ASC, `student_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鎴愭灉鎻愪氦琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of submission
-- ----------------------------
INSERT INTO `submission` VALUES (2, 1, 3, '2026-05-03 11:06:26', 1, 'SUCCESS', 'SUCCESS', 'PUBLISHED', 0.00, 12.85, '该实训报告详细描述了学生管理系统的面向对象设计与实现，包括需求分析、系统设计（三层架构、类图、数据库设计）、代码实现、测试和总结。系统支持学生信息、成绩、课程管理及数据统计，使用Java Swing和MySQL，采用MVC和DAO模式。', '[\"面向对象设计\",\"三层架构\",\"MVC模式\",\"DAO模式\",\"Java Swing\",\"MySQL数据库\",\"CRUD操作\",\"异常处理\",\"单元测试\",\"集成测试\"]', 'HIGH', 'MEDIUM', '[\"补充具体代码实现和关键算法细节\",\"提供测试用例和测试结果数据\",\"添加系统界面截图或原型图\",\"完善异常处理的具体实现方式\",\"增加用户权限管理或安全机制\"]', '', 0, '2026-05-03 11:06:25', '2026-05-03 11:06:25');

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '閰嶇疆閿',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '閰嶇疆鍊',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '璇存槑',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '绯荤粺閰嶇疆琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_config
-- ----------------------------
INSERT INTO `system_config` VALUES (1, 'textModelApiUrl', 'https://api-inference.modelscope.cn/v1', NULL, '2026-05-03 00:55:21');
INSERT INTO `system_config` VALUES (2, 'textModelApiKey', 'ms-cb4b0861-40d9-4697-86ac-b8764e1cdbd1', NULL, '2026-05-03 00:55:21');
INSERT INTO `system_config` VALUES (3, 'model', 'Qwen/Qwen3.5-35B-A3B', NULL, '2026-05-03 00:55:21');
INSERT INTO `system_config` VALUES (4, 'timeout', '30000', NULL, '2026-05-03 00:55:21');
INSERT INTO `system_config` VALUES (5, 'temperature', '0.3', NULL, '2026-05-03 00:55:21');
INSERT INTO `system_config` VALUES (6, 'maxTokens', '4096', NULL, '2026-05-03 00:55:21');

-- ----------------------------
-- Table structure for training_task
-- ----------------------------
DROP TABLE IF EXISTS `training_task`;
CREATE TABLE `training_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浠诲姟ID',
  `course_id` bigint NOT NULL COMMENT '鎵?睘璇剧▼ID',
  `template_id` bigint NOT NULL COMMENT '缁戝畾璇勪环妯℃澘ID',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '浠诲姟鍚嶇О',
  `requirements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '瀹炶?瑕佹眰',
  `start_time` datetime NOT NULL COMMENT '寮??鏃堕棿',
  `end_time` datetime NOT NULL COMMENT '鎴??鏃堕棿',
  `allow_resubmit` tinyint(1) NOT NULL DEFAULT 1 COMMENT '鏄?惁鍏佽?閲嶆柊鎻愪氦',
  `allowed_file_types` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '鍏佽?鏂囦欢绫诲瀷',
  `max_file_size` bigint NULL DEFAULT 209715200 COMMENT '鏈?ぇ鏂囦欢澶у皬',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'DRAFT' COMMENT '鐘舵?: DRAFT/PUBLISHED/CLOSED/ARCHIVED',
  `deleted` tinyint NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '瀹炶?浠诲姟琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_task
-- ----------------------------
INSERT INTO `training_task` VALUES (1, 1, 1, 'Java-OOP-Training', 'Build student management system with analysis,design,code,test,summary', '2026-04-20 00:00:00', '2026-05-20 23:59:59', 1, NULL, 209715200, 'PUBLISHED', 0, '2026-04-28 17:52:15', '2026-04-28 17:52:15');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '鐢ㄦ埛ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鐧诲綍鐢ㄦ埛鍚',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鍔犲瘑瀵嗙爜',
  `role` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'STUDENT' COMMENT '瑙掕壊: STUDENT/TEACHER/ADMIN',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '鐪熷疄濮撳悕',
  `class_id` bigint NULL DEFAULT NULL COMMENT '瀛︾敓鎵?睘鐝?骇ID',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ENABLED' COMMENT '鐘舵?: ENABLED/DISABLED',
  `must_change_password` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否首次登录必须修改密码',
  `last_password_change_at` datetime NULL DEFAULT NULL COMMENT '最近密码修改时间',
  `last_login_at` datetime NULL DEFAULT NULL COMMENT '最近登录时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '閫昏緫鍒犻櫎',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐢ㄦ埛琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$KwSxLKxk8N/cW5qHiFQRueyRNxdmJcBc0XwMKf0ZhRjCs9h4hjuL6', 'ADMIN', 'admin', NULL, 'ENABLED', 0, NULL, '2026-05-03 12:56:32', 0, '2026-04-28 17:48:05', '2026-04-28 17:51:00');
INSERT INTO `user` VALUES (2, 'teacher', '$2a$10$KwSxLKxk8N/cW5qHiFQRueyRNxdmJcBc0XwMKf0ZhRjCs9h4hjuL6', 'TEACHER', '张老师', NULL, 'ENABLED', 0, NULL, '2026-05-03 13:24:42', 0, '2026-04-28 17:48:49', '2026-04-28 17:51:00');
INSERT INTO `user` VALUES (3, 'student', '$2a$10$KwSxLKxk8N/cW5qHiFQRueyRNxdmJcBc0XwMKf0ZhRjCs9h4hjuL6', 'STUDENT', '李同学', 1, 'ENABLED', 0, NULL, '2026-05-03 10:48:57', 0, '2026-04-28 17:48:49', '2026-04-28 17:51:00');

SET FOREIGN_KEY_CHECKS = 1;
