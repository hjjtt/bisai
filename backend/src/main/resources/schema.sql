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

 Date: 03/05/2026 01:58:36
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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI调用日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_call_log
-- ----------------------------
INSERT INTO `ai_call_log` VALUES (1, 'Qwen/Qwen3.5-35B-A3B', 'CHAT', 24, 233, 257, 1, NULL, '2026-05-03 00:55:16');

-- ----------------------------
-- Table structure for async_task
-- ----------------------------
DROP TABLE IF EXISTS `async_task`;
CREATE TABLE `async_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务类型: PARSE/CHECK/SCORE/EXPORT',
  `biz_id` bigint NOT NULL COMMENT '业务ID(提交ID)',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED/RETRYING',
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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '异步任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of async_task
-- ----------------------------

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鏍告煡缁撴灉琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of check_result
-- ----------------------------

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鏂囦欢琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of file
-- ----------------------------

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐭ヨ瘑搴撹〃' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_base
-- ----------------------------

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐭ヨ瘑搴撴枃妗ｈ〃' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of knowledge_document
-- ----------------------------

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '娑堟伅琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of message
-- ----------------------------

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
) ENGINE = InnoDB AUTO_INCREMENT = 300 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鎿嶄綔鏃ュ織琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of operation_log
-- ----------------------------
INSERT INTO `operation_log` VALUES (1, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-04-29 13:03:47');
INSERT INTO `operation_log` VALUES (2, 2, 'teacher', 'GET /api/knowledge', '用户teacher执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-04-29 13:03:49');
INSERT INTO `operation_log` VALUES (3, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:03:50');
INSERT INTO `operation_log` VALUES (4, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:03:51');
INSERT INTO `operation_log` VALUES (5, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-04-29 13:03:53');
INSERT INTO `operation_log` VALUES (6, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:03:53');
INSERT INTO `operation_log` VALUES (7, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:03:53');
INSERT INTO `operation_log` VALUES (8, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-04-29 13:03:55');
INSERT INTO `operation_log` VALUES (9, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:03:57');
INSERT INTO `operation_log` VALUES (10, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-04-29 13:03:58');
INSERT INTO `operation_log` VALUES (11, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:03:58');
INSERT INTO `operation_log` VALUES (12, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:03:59');
INSERT INTO `operation_log` VALUES (13, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:04:00');
INSERT INTO `operation_log` VALUES (14, 2, 'teacher', 'GET /api/knowledge', '用户teacher执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-04-29 13:04:00');
INSERT INTO `operation_log` VALUES (15, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:04:01');
INSERT INTO `operation_log` VALUES (16, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-04-29 13:04:01');
INSERT INTO `operation_log` VALUES (17, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:04:01');
INSERT INTO `operation_log` VALUES (18, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:04:02');
INSERT INTO `operation_log` VALUES (19, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-04-29 13:04:03');
INSERT INTO `operation_log` VALUES (20, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:04:04');
INSERT INTO `operation_log` VALUES (21, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-04-29 13:07:27');
INSERT INTO `operation_log` VALUES (22, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:07:27');
INSERT INTO `operation_log` VALUES (23, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:07:28');
INSERT INTO `operation_log` VALUES (24, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:07:29');
INSERT INTO `operation_log` VALUES (25, 2, 'teacher', 'GET /api/knowledge', '用户teacher执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-04-29 13:07:29');
INSERT INTO `operation_log` VALUES (26, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-04-29 13:07:30');
INSERT INTO `operation_log` VALUES (27, NULL, '匿名用户', 'POST /api/auth/login', '用户匿名用户执行了AuthController.login操作', '127.0.0.1', '/api/auth/login', 'POST', '2026-04-29 13:07:40');
INSERT INTO `operation_log` VALUES (28, 3, 'student', 'GET /api/dashboard/student', '用户student执行了DashboardController.studentStats操作', '127.0.0.1', '/api/dashboard/student', 'GET', '2026-04-29 13:07:40');
INSERT INTO `operation_log` VALUES (29, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-04-29 13:07:43');
INSERT INTO `operation_log` VALUES (30, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-04-29 13:07:43');
INSERT INTO `operation_log` VALUES (31, 3, 'student', 'GET /api/tasks/1', '用户student执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-04-29 13:07:45');
INSERT INTO `operation_log` VALUES (32, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-04-29 13:07:45');
INSERT INTO `operation_log` VALUES (33, 3, 'student', 'GET /api/tasks/1', '用户student执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-04-29 13:41:34');
INSERT INTO `operation_log` VALUES (34, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-04-29 13:41:34');
INSERT INTO `operation_log` VALUES (35, NULL, '匿名用户', 'POST /api/auth/login', '用户匿名用户执行了AuthController.login操作', '127.0.0.1', '/api/auth/login', 'POST', '2026-05-03 00:53:50');
INSERT INTO `operation_log` VALUES (36, 1, 'admin', 'GET /api/dashboard/admin', '用户admin执行了DashboardController.adminStats操作', '127.0.0.1', '/api/dashboard/admin', 'GET', '2026-05-03 00:53:51');
INSERT INTO `operation_log` VALUES (37, 1, 'admin', 'GET /api/dashboard/admin', '用户admin执行了DashboardController.adminStats操作', '127.0.0.1', '/api/dashboard/admin', 'GET', '2026-05-03 00:53:55');
INSERT INTO `operation_log` VALUES (38, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 00:54:13');
INSERT INTO `operation_log` VALUES (39, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 00:54:13');
INSERT INTO `operation_log` VALUES (40, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 00:54:14');
INSERT INTO `operation_log` VALUES (41, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 00:54:14');
INSERT INTO `operation_log` VALUES (42, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 00:54:14');
INSERT INTO `operation_log` VALUES (43, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 00:54:14');
INSERT INTO `operation_log` VALUES (44, 1, 'admin', 'GET /api/knowledge', '用户admin执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 00:54:14');
INSERT INTO `operation_log` VALUES (45, 1, 'admin', 'GET /api/system/config', '用户admin执行了SystemController.getConfig操作', '127.0.0.1', '/api/system/config', 'GET', '2026-05-03 00:54:15');
INSERT INTO `operation_log` VALUES (46, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 00:54:16');
INSERT INTO `operation_log` VALUES (47, 1, 'admin', 'GET /api/tasks', '用户admin执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 00:54:17');
INSERT INTO `operation_log` VALUES (48, 1, 'admin', 'GET /api/system/config', '用户admin执行了SystemController.getConfig操作', '127.0.0.1', '/api/system/config', 'GET', '2026-05-03 00:54:17');
INSERT INTO `operation_log` VALUES (49, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 00:54:18');
INSERT INTO `operation_log` VALUES (50, 1, 'admin', 'GET /api/logs/model-call', '用户admin执行了LogController.modelCallLogs操作', '127.0.0.1', '/api/logs/model-call', 'GET', '2026-05-03 00:54:23');
INSERT INTO `operation_log` VALUES (51, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 00:54:23');
INSERT INTO `operation_log` VALUES (52, 1, 'admin', 'GET /api/knowledge', '用户admin执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 00:54:24');
INSERT INTO `operation_log` VALUES (53, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 00:54:24');
INSERT INTO `operation_log` VALUES (54, 1, 'admin', 'GET /api/system/config', '用户admin执行了SystemController.getConfig操作', '127.0.0.1', '/api/system/config', 'GET', '2026-05-03 00:54:25');
INSERT INTO `operation_log` VALUES (55, 1, 'admin', 'POST /api/system/test-model', '用户admin执行了SystemController.testModel操作', '127.0.0.1', '/api/system/test-model', 'POST', '2026-05-03 00:55:16');
INSERT INTO `operation_log` VALUES (56, 1, 'admin', 'PUT /api/system/config', '用户admin执行了SystemController.updateConfig操作', '127.0.0.1', '/api/system/config', 'PUT', '2026-05-03 00:55:22');
INSERT INTO `operation_log` VALUES (57, 1, 'admin', 'GET /api/dashboard/admin', '用户admin执行了DashboardController.adminStats操作', '127.0.0.1', '/api/dashboard/admin', 'GET', '2026-05-03 00:55:31');
INSERT INTO `operation_log` VALUES (58, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 00:55:34');
INSERT INTO `operation_log` VALUES (59, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 00:55:34');
INSERT INTO `operation_log` VALUES (60, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 00:55:36');
INSERT INTO `operation_log` VALUES (61, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 00:55:36');
INSERT INTO `operation_log` VALUES (62, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 00:55:36');
INSERT INTO `operation_log` VALUES (63, 1, 'admin', 'GET /api/knowledge', '用户admin执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 00:55:37');
INSERT INTO `operation_log` VALUES (64, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 00:55:37');
INSERT INTO `operation_log` VALUES (65, 1, 'admin', 'GET /api/system/config', '用户admin执行了SystemController.getConfig操作', '127.0.0.1', '/api/system/config', 'GET', '2026-05-03 00:55:37');
INSERT INTO `operation_log` VALUES (66, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 00:55:38');
INSERT INTO `operation_log` VALUES (67, 1, 'admin', 'GET /api/tasks', '用户admin执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 00:55:38');
INSERT INTO `operation_log` VALUES (68, NULL, '匿名用户', 'POST /api/auth/login', '用户匿名用户执行了AuthController.login操作', '127.0.0.1', '/api/auth/login', 'POST', '2026-05-03 00:55:42');
INSERT INTO `operation_log` VALUES (69, 1, 'admin', 'GET /api/dashboard/admin', '用户admin执行了DashboardController.adminStats操作', '127.0.0.1', '/api/dashboard/admin', 'GET', '2026-05-03 00:55:42');
INSERT INTO `operation_log` VALUES (70, 1, 'admin', 'GET /api/knowledge', '用户admin执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 00:55:43');
INSERT INTO `operation_log` VALUES (71, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 00:55:43');
INSERT INTO `operation_log` VALUES (72, 1, 'admin', 'GET /api/system/config', '用户admin执行了SystemController.getConfig操作', '127.0.0.1', '/api/system/config', 'GET', '2026-05-03 00:55:43');
INSERT INTO `operation_log` VALUES (73, NULL, '匿名用户', 'POST /api/auth/login', '用户匿名用户执行了AuthController.login操作', '127.0.0.1', '/api/auth/login', 'POST', '2026-05-03 00:55:51');
INSERT INTO `operation_log` VALUES (74, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-05-03 00:55:52');
INSERT INTO `operation_log` VALUES (75, 2, 'teacher', 'POST /api/submissions/0/score', '用户teacher执行了SubmissionController.triggerScore操作', '127.0.0.1', '/api/submissions/0/score', 'POST', '2026-05-03 00:55:56');
INSERT INTO `operation_log` VALUES (76, 2, 'teacher', 'POST /api/submissions/0/score', '用户teacher执行了SubmissionController.triggerScore操作', '127.0.0.1', '/api/submissions/0/score', 'POST', '2026-05-03 00:56:11');
INSERT INTO `operation_log` VALUES (77, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 00:56:22');
INSERT INTO `operation_log` VALUES (78, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 00:56:24');
INSERT INTO `operation_log` VALUES (79, 2, 'teacher', 'GET /api/knowledge', '用户teacher执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 00:56:24');
INSERT INTO `operation_log` VALUES (80, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 00:56:25');
INSERT INTO `operation_log` VALUES (81, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 00:56:25');
INSERT INTO `operation_log` VALUES (82, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 00:56:26');
INSERT INTO `operation_log` VALUES (83, 2, 'teacher', 'GET /api/courses', '用户teacher执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 00:56:28');
INSERT INTO `operation_log` VALUES (84, 2, 'teacher', 'GET /api/templates', '用户teacher执行了EvaluationTemplateController.list操作', '127.0.0.1', '/api/templates', 'GET', '2026-05-03 00:56:28');
INSERT INTO `operation_log` VALUES (85, 2, 'teacher', 'GET /api/courses', '用户teacher执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 00:58:04');
INSERT INTO `operation_log` VALUES (86, 2, 'teacher', 'GET /api/templates', '用户teacher执行了EvaluationTemplateController.list操作', '127.0.0.1', '/api/templates', 'GET', '2026-05-03 00:58:04');
INSERT INTO `operation_log` VALUES (87, 2, 'teacher', 'GET /api/courses', '用户teacher执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 00:58:20');
INSERT INTO `operation_log` VALUES (88, 2, 'teacher', 'GET /api/templates', '用户teacher执行了EvaluationTemplateController.list操作', '127.0.0.1', '/api/templates', 'GET', '2026-05-03 00:58:20');
INSERT INTO `operation_log` VALUES (89, 2, 'teacher', 'GET /api/courses', '用户teacher执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:01:02');
INSERT INTO `operation_log` VALUES (90, 2, 'teacher', 'GET /api/templates', '用户teacher执行了EvaluationTemplateController.list操作', '127.0.0.1', '/api/templates', 'GET', '2026-05-03 01:01:02');
INSERT INTO `operation_log` VALUES (91, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-05-03 01:16:18');
INSERT INTO `operation_log` VALUES (92, NULL, '匿名用户', 'POST /api/auth/login', '用户匿名用户执行了AuthController.login操作', '127.0.0.1', '/api/auth/login', 'POST', '2026-05-03 01:16:21');
INSERT INTO `operation_log` VALUES (93, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-05-03 01:16:21');
INSERT INTO `operation_log` VALUES (94, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:16:23');
INSERT INTO `operation_log` VALUES (95, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:16:24');
INSERT INTO `operation_log` VALUES (96, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:16:24');
INSERT INTO `operation_log` VALUES (97, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:17:58');
INSERT INTO `operation_log` VALUES (98, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:17:59');
INSERT INTO `operation_log` VALUES (99, 2, 'teacher', 'GET /api/knowledge', '用户teacher执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 01:18:00');
INSERT INTO `operation_log` VALUES (100, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:02');
INSERT INTO `operation_log` VALUES (101, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:18:02');
INSERT INTO `operation_log` VALUES (102, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-05-03 01:18:03');
INSERT INTO `operation_log` VALUES (103, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:04');
INSERT INTO `operation_log` VALUES (104, 2, 'teacher', 'GET /api/tasks/1', '用户teacher执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-05-03 01:18:06');
INSERT INTO `operation_log` VALUES (105, 2, 'teacher', 'GET /api/courses', '用户teacher执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:18:06');
INSERT INTO `operation_log` VALUES (106, 2, 'teacher', 'GET /api/templates', '用户teacher执行了EvaluationTemplateController.list操作', '127.0.0.1', '/api/templates', 'GET', '2026-05-03 01:18:06');
INSERT INTO `operation_log` VALUES (107, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:09');
INSERT INTO `operation_log` VALUES (108, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:10');
INSERT INTO `operation_log` VALUES (109, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:18:10');
INSERT INTO `operation_log` VALUES (110, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:13');
INSERT INTO `operation_log` VALUES (111, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:14');
INSERT INTO `operation_log` VALUES (112, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:15');
INSERT INTO `operation_log` VALUES (113, 2, 'teacher', 'GET /api/knowledge', '用户teacher执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 01:18:15');
INSERT INTO `operation_log` VALUES (114, NULL, '匿名用户', 'POST /api/auth/login', '用户匿名用户执行了AuthController.login操作', '127.0.0.1', '/api/auth/login', 'POST', '2026-05-03 01:18:27');
INSERT INTO `operation_log` VALUES (115, 3, 'student', 'GET /api/dashboard/student', '用户student执行了DashboardController.studentStats操作', '127.0.0.1', '/api/dashboard/student', 'GET', '2026-05-03 01:18:27');
INSERT INTO `operation_log` VALUES (116, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:18:31');
INSERT INTO `operation_log` VALUES (117, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:31');
INSERT INTO `operation_log` VALUES (118, 3, 'student', 'GET /api/dashboard/student', '用户student执行了DashboardController.studentStats操作', '127.0.0.1', '/api/dashboard/student', 'GET', '2026-05-03 01:18:32');
INSERT INTO `operation_log` VALUES (119, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:34');
INSERT INTO `operation_log` VALUES (120, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:18:34');
INSERT INTO `operation_log` VALUES (121, 3, 'student', 'GET /api/dashboard/student', '用户student执行了DashboardController.studentStats操作', '127.0.0.1', '/api/dashboard/student', 'GET', '2026-05-03 01:18:35');
INSERT INTO `operation_log` VALUES (122, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:18:37');
INSERT INTO `operation_log` VALUES (123, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:37');
INSERT INTO `operation_log` VALUES (124, 3, 'student', 'GET /api/tasks/1', '用户student执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-05-03 01:18:39');
INSERT INTO `operation_log` VALUES (125, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:18:39');
INSERT INTO `operation_log` VALUES (126, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:18:43');
INSERT INTO `operation_log` VALUES (127, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:18:43');
INSERT INTO `operation_log` VALUES (128, 3, 'student', 'GET /api/tasks/1', '用户student执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-05-03 01:19:10');
INSERT INTO `operation_log` VALUES (129, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:19:10');
INSERT INTO `operation_log` VALUES (130, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:20:58');
INSERT INTO `operation_log` VALUES (131, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:20:58');
INSERT INTO `operation_log` VALUES (132, 3, 'student', 'GET /api/tasks/1', '用户student执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-05-03 01:21:02');
INSERT INTO `operation_log` VALUES (133, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:21:02');
INSERT INTO `operation_log` VALUES (134, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:21:05');
INSERT INTO `operation_log` VALUES (135, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:21:05');
INSERT INTO `operation_log` VALUES (136, 3, 'student', 'GET /api/dashboard/student', '用户student执行了DashboardController.studentStats操作', '127.0.0.1', '/api/dashboard/student', 'GET', '2026-05-03 01:21:05');
INSERT INTO `operation_log` VALUES (137, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:21:07');
INSERT INTO `operation_log` VALUES (138, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:21:07');
INSERT INTO `operation_log` VALUES (139, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:25:38');
INSERT INTO `operation_log` VALUES (140, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:25:38');
INSERT INTO `operation_log` VALUES (141, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:28:01');
INSERT INTO `operation_log` VALUES (142, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:28:01');
INSERT INTO `operation_log` VALUES (143, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:28:02');
INSERT INTO `operation_log` VALUES (144, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:28:06');
INSERT INTO `operation_log` VALUES (145, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:28:06');
INSERT INTO `operation_log` VALUES (146, 3, 'student', 'GET /api/tasks/1', '用户student执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-05-03 01:28:08');
INSERT INTO `operation_log` VALUES (147, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:28:08');
INSERT INTO `operation_log` VALUES (148, 3, 'student', 'GET /api/courses', '用户student执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:28:09');
INSERT INTO `operation_log` VALUES (149, 3, 'student', 'GET /api/tasks', '用户student执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:28:09');
INSERT INTO `operation_log` VALUES (150, 3, 'student', 'GET /api/tasks/1', '用户student执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-05-03 01:28:10');
INSERT INTO `operation_log` VALUES (151, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:28:10');
INSERT INTO `operation_log` VALUES (152, 3, 'student', 'GET /api/tasks/1', '用户student执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-05-03 01:28:12');
INSERT INTO `operation_log` VALUES (153, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:28:12');
INSERT INTO `operation_log` VALUES (154, 3, 'student', 'GET /api/tasks/1', '用户student执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-05-03 01:28:17');
INSERT INTO `operation_log` VALUES (155, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:28:17');
INSERT INTO `operation_log` VALUES (156, 3, 'student', 'GET /api/tasks/1', '用户student执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-05-03 01:28:28');
INSERT INTO `operation_log` VALUES (157, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:28:28');
INSERT INTO `operation_log` VALUES (158, 3, 'student', 'GET /api/submissions', '用户student执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:28:29');
INSERT INTO `operation_log` VALUES (159, 3, 'student', 'GET /api/dashboard/student', '用户student执行了DashboardController.studentStats操作', '127.0.0.1', '/api/dashboard/student', 'GET', '2026-05-03 01:28:31');
INSERT INTO `operation_log` VALUES (160, NULL, '匿名用户', 'POST /api/auth/login', '用户匿名用户执行了AuthController.login操作', '127.0.0.1', '/api/auth/login', 'POST', '2026-05-03 01:28:35');
INSERT INTO `operation_log` VALUES (161, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-05-03 01:28:36');
INSERT INTO `operation_log` VALUES (162, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:28:37');
INSERT INTO `operation_log` VALUES (163, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:28:37');
INSERT INTO `operation_log` VALUES (164, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:28:37');
INSERT INTO `operation_log` VALUES (165, 2, 'teacher', 'GET /api/courses', '用户teacher执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:28:39');
INSERT INTO `operation_log` VALUES (166, 2, 'teacher', 'GET /api/templates', '用户teacher执行了EvaluationTemplateController.list操作', '127.0.0.1', '/api/templates', 'GET', '2026-05-03 01:28:39');
INSERT INTO `operation_log` VALUES (167, 2, 'teacher', 'GET /api/tasks/1', '用户teacher执行了TaskController.get操作', '127.0.0.1', '/api/tasks/1', 'GET', '2026-05-03 01:28:39');
INSERT INTO `operation_log` VALUES (168, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:28:41');
INSERT INTO `operation_log` VALUES (169, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:28:43');
INSERT INTO `operation_log` VALUES (170, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:28:43');
INSERT INTO `operation_log` VALUES (171, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:28:45');
INSERT INTO `operation_log` VALUES (172, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:28:46');
INSERT INTO `operation_log` VALUES (173, 2, 'teacher', 'GET /api/knowledge', '用户teacher执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 01:28:47');
INSERT INTO `operation_log` VALUES (174, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-05-03 01:28:48');
INSERT INTO `operation_log` VALUES (175, NULL, '匿名用户', 'POST /api/auth/login', '用户匿名用户执行了AuthController.login操作', '127.0.0.1', '/api/auth/login', 'POST', '2026-05-03 01:28:52');
INSERT INTO `operation_log` VALUES (176, 1, 'admin', 'GET /api/dashboard/admin', '用户admin执行了DashboardController.adminStats操作', '127.0.0.1', '/api/dashboard/admin', 'GET', '2026-05-03 01:28:52');
INSERT INTO `operation_log` VALUES (177, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 01:28:56');
INSERT INTO `operation_log` VALUES (178, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 01:28:56');
INSERT INTO `operation_log` VALUES (179, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 01:28:59');
INSERT INTO `operation_log` VALUES (180, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:28:59');
INSERT INTO `operation_log` VALUES (181, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 01:28:59');
INSERT INTO `operation_log` VALUES (182, 1, 'admin', 'GET /api/knowledge', '用户admin执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 01:29:00');
INSERT INTO `operation_log` VALUES (183, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:29:00');
INSERT INTO `operation_log` VALUES (184, 1, 'admin', 'GET /api/system/config', '用户admin执行了SystemController.getConfig操作', '127.0.0.1', '/api/system/config', 'GET', '2026-05-03 01:29:00');
INSERT INTO `operation_log` VALUES (185, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 01:29:01');
INSERT INTO `operation_log` VALUES (186, 1, 'admin', 'GET /api/tasks', '用户admin执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:29:02');
INSERT INTO `operation_log` VALUES (187, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:33:22');
INSERT INTO `operation_log` VALUES (188, 1, 'admin', 'GET /api/tasks', '用户admin执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:33:22');
INSERT INTO `operation_log` VALUES (189, 1, 'admin', 'GET /api/dashboard/admin', '用户admin执行了DashboardController.adminStats操作', '127.0.0.1', '/api/dashboard/admin', 'GET', '2026-05-03 01:33:24');
INSERT INTO `operation_log` VALUES (190, 1, 'admin', 'GET /api/messages', '用户admin执行了MessageController.list操作', '127.0.0.1', '/api/messages', 'GET', '2026-05-03 01:33:24');
INSERT INTO `operation_log` VALUES (191, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 01:33:27');
INSERT INTO `operation_log` VALUES (192, 1, 'admin', 'GET /api/messages', '用户admin执行了MessageController.list操作', '127.0.0.1', '/api/messages', 'GET', '2026-05-03 01:33:29');
INSERT INTO `operation_log` VALUES (193, 1, 'admin', 'GET /api/tasks', '用户admin执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:33:32');
INSERT INTO `operation_log` VALUES (194, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 01:33:33');
INSERT INTO `operation_log` VALUES (195, 1, 'admin', 'GET /api/system/config', '用户admin执行了SystemController.getConfig操作', '127.0.0.1', '/api/system/config', 'GET', '2026-05-03 01:33:33');
INSERT INTO `operation_log` VALUES (196, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 01:33:34');
INSERT INTO `operation_log` VALUES (197, 1, 'admin', 'GET /api/logs/model-call', '用户admin执行了LogController.modelCallLogs操作', '127.0.0.1', '/api/logs/model-call', 'GET', '2026-05-03 01:33:35');
INSERT INTO `operation_log` VALUES (198, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:33:52');
INSERT INTO `operation_log` VALUES (199, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:34:22');
INSERT INTO `operation_log` VALUES (200, 1, 'admin', 'GET /api/messages', '用户admin执行了MessageController.list操作', '127.0.0.1', '/api/messages', 'GET', '2026-05-03 01:34:28');
INSERT INTO `operation_log` VALUES (201, 1, 'admin', 'PUT /api/messages/read-all', '用户admin执行了MessageController.markAllRead操作', '127.0.0.1', '/api/messages/read-all', 'PUT', '2026-05-03 01:34:30');
INSERT INTO `operation_log` VALUES (202, 1, 'admin', 'GET /api/messages', '用户admin执行了MessageController.list操作', '127.0.0.1', '/api/messages', 'GET', '2026-05-03 01:34:36');
INSERT INTO `operation_log` VALUES (203, 1, 'admin', 'GET /api/dashboard/admin', '用户admin执行了DashboardController.adminStats操作', '127.0.0.1', '/api/dashboard/admin', 'GET', '2026-05-03 01:34:41');
INSERT INTO `operation_log` VALUES (204, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:34:52');
INSERT INTO `operation_log` VALUES (205, 1, 'admin', 'GET /api/messages', '用户admin执行了MessageController.list操作', '127.0.0.1', '/api/messages', 'GET', '2026-05-03 01:34:53');
INSERT INTO `operation_log` VALUES (206, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 01:34:54');
INSERT INTO `operation_log` VALUES (207, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:35:22');
INSERT INTO `operation_log` VALUES (208, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:35:52');
INSERT INTO `operation_log` VALUES (209, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:45:06');
INSERT INTO `operation_log` VALUES (210, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:46:06');
INSERT INTO `operation_log` VALUES (211, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:46:36');
INSERT INTO `operation_log` VALUES (212, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:47:06');
INSERT INTO `operation_log` VALUES (213, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:47:36');
INSERT INTO `operation_log` VALUES (214, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:48:43');
INSERT INTO `operation_log` VALUES (215, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:49:31');
INSERT INTO `operation_log` VALUES (216, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:49:33');
INSERT INTO `operation_log` VALUES (217, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 01:49:33');
INSERT INTO `operation_log` VALUES (218, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:49:35');
INSERT INTO `operation_log` VALUES (219, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 01:49:35');
INSERT INTO `operation_log` VALUES (220, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:50:05');
INSERT INTO `operation_log` VALUES (221, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:50:35');
INSERT INTO `operation_log` VALUES (222, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:51:19');
INSERT INTO `operation_log` VALUES (223, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 01:51:20');
INSERT INTO `operation_log` VALUES (224, 1, 'admin', 'GET /api/logs/model-call', '用户admin执行了LogController.modelCallLogs操作', '127.0.0.1', '/api/logs/model-call', 'GET', '2026-05-03 01:51:21');
INSERT INTO `operation_log` VALUES (225, 1, 'admin', 'GET /api/tasks', '用户admin执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:51:27');
INSERT INTO `operation_log` VALUES (226, 1, 'admin', 'GET /api/system/config', '用户admin执行了SystemController.getConfig操作', '127.0.0.1', '/api/system/config', 'GET', '2026-05-03 01:51:28');
INSERT INTO `operation_log` VALUES (227, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:51:29');
INSERT INTO `operation_log` VALUES (228, 1, 'admin', 'GET /api/knowledge', '用户admin执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 01:51:29');
INSERT INTO `operation_log` VALUES (229, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:51:30');
INSERT INTO `operation_log` VALUES (230, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 01:51:30');
INSERT INTO `operation_log` VALUES (231, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 01:51:30');
INSERT INTO `operation_log` VALUES (232, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 01:51:31');
INSERT INTO `operation_log` VALUES (233, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 01:51:31');
INSERT INTO `operation_log` VALUES (234, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:51:31');
INSERT INTO `operation_log` VALUES (235, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 01:51:31');
INSERT INTO `operation_log` VALUES (236, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 01:51:31');
INSERT INTO `operation_log` VALUES (237, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 01:51:40');
INSERT INTO `operation_log` VALUES (238, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 01:51:40');
INSERT INTO `operation_log` VALUES (239, 1, 'admin', 'GET /api/dashboard/admin', '用户admin执行了DashboardController.adminStats操作', '127.0.0.1', '/api/dashboard/admin', 'GET', '2026-05-03 01:51:42');
INSERT INTO `operation_log` VALUES (240, 1, 'admin', 'GET /api/system/config', '用户admin执行了SystemController.getConfig操作', '127.0.0.1', '/api/system/config', 'GET', '2026-05-03 01:51:43');
INSERT INTO `operation_log` VALUES (241, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 01:51:44');
INSERT INTO `operation_log` VALUES (242, 1, 'admin', 'GET /api/tasks', '用户admin执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:51:45');
INSERT INTO `operation_log` VALUES (243, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:51:49');
INSERT INTO `operation_log` VALUES (244, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:52:19');
INSERT INTO `operation_log` VALUES (245, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:52:50');
INSERT INTO `operation_log` VALUES (246, 1, 'admin', 'GET /api/messages/unread-count', '用户admin执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:53:10');
INSERT INTO `operation_log` VALUES (247, 1, 'admin', 'GET /api/tasks', '用户admin执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:53:10');
INSERT INTO `operation_log` VALUES (248, 1, 'admin', 'GET /api/logs/operation', '用户admin执行了LogController.operationLogs操作', '127.0.0.1', '/api/logs/operation', 'GET', '2026-05-03 01:53:12');
INSERT INTO `operation_log` VALUES (249, 1, 'admin', 'GET /api/system/config', '用户admin执行了SystemController.getConfig操作', '127.0.0.1', '/api/system/config', 'GET', '2026-05-03 01:53:12');
INSERT INTO `operation_log` VALUES (250, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:53:13');
INSERT INTO `operation_log` VALUES (251, 1, 'admin', 'GET /api/knowledge', '用户admin执行了KnowledgeController.list操作', '127.0.0.1', '/api/knowledge', 'GET', '2026-05-03 01:53:13');
INSERT INTO `operation_log` VALUES (252, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 01:53:14');
INSERT INTO `operation_log` VALUES (253, 1, 'admin', 'GET /api/courses', '用户admin执行了CourseController.list操作', '127.0.0.1', '/api/courses', 'GET', '2026-05-03 01:53:14');
INSERT INTO `operation_log` VALUES (254, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 01:53:14');
INSERT INTO `operation_log` VALUES (255, 1, 'admin', 'GET /api/classes', '用户admin执行了ClassController.list操作', '127.0.0.1', '/api/classes', 'GET', '2026-05-03 01:53:14');
INSERT INTO `operation_log` VALUES (256, 1, 'admin', 'GET /api/users', '用户admin执行了UserController.list操作', '127.0.0.1', '/api/users', 'GET', '2026-05-03 01:53:14');
INSERT INTO `operation_log` VALUES (257, 1, 'admin', 'GET /api/dashboard/admin', '用户admin执行了DashboardController.adminStats操作', '127.0.0.1', '/api/dashboard/admin', 'GET', '2026-05-03 01:53:15');
INSERT INTO `operation_log` VALUES (258, 1, 'admin', 'GET /api/tasks', '用户admin执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:53:16');
INSERT INTO `operation_log` VALUES (259, 1, 'admin', 'GET /api/dashboard/admin', '用户admin执行了DashboardController.adminStats操作', '127.0.0.1', '/api/dashboard/admin', 'GET', '2026-05-03 01:53:17');
INSERT INTO `operation_log` VALUES (260, NULL, '匿名用户', 'POST /api/auth/login', '用户匿名用户执行了AuthController.login操作', '127.0.0.1', '/api/auth/login', 'POST', '2026-05-03 01:53:21');
INSERT INTO `operation_log` VALUES (261, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:53:21');
INSERT INTO `operation_log` VALUES (262, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-05-03 01:53:21');
INSERT INTO `operation_log` VALUES (263, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:53:22');
INSERT INTO `operation_log` VALUES (264, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:53:22');
INSERT INTO `operation_log` VALUES (265, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:53:23');
INSERT INTO `operation_log` VALUES (266, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:53:40');
INSERT INTO `operation_log` VALUES (267, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:53:51');
INSERT INTO `operation_log` VALUES (268, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:54:10');
INSERT INTO `operation_log` VALUES (269, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:54:21');
INSERT INTO `operation_log` VALUES (270, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:54:40');
INSERT INTO `operation_log` VALUES (271, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:54:51');
INSERT INTO `operation_log` VALUES (272, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:54:58');
INSERT INTO `operation_log` VALUES (273, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:54:58');
INSERT INTO `operation_log` VALUES (274, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:54:58');
INSERT INTO `operation_log` VALUES (275, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:55:08');
INSERT INTO `operation_log` VALUES (276, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:55:08');
INSERT INTO `operation_log` VALUES (277, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:55:08');
INSERT INTO `operation_log` VALUES (278, NULL, '匿名用户', 'POST /api/auth/login', '用户匿名用户执行了AuthController.login操作', '127.0.0.1', '/api/auth/login', 'POST', '2026-05-03 01:55:13');
INSERT INTO `operation_log` VALUES (279, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:55:13');
INSERT INTO `operation_log` VALUES (280, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-05-03 01:55:13');
INSERT INTO `operation_log` VALUES (281, 2, 'teacher', 'GET /api/messages', '用户teacher执行了MessageController.list操作', '127.0.0.1', '/api/messages', 'GET', '2026-05-03 01:55:29');
INSERT INTO `operation_log` VALUES (282, 2, 'teacher', 'GET /api/messages', '用户teacher执行了MessageController.list操作', '127.0.0.1', '/api/messages', 'GET', '2026-05-03 01:55:32');
INSERT INTO `operation_log` VALUES (283, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:55:36');
INSERT INTO `operation_log` VALUES (284, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:55:37');
INSERT INTO `operation_log` VALUES (285, 2, 'teacher', 'GET /api/submissions', '用户teacher执行了SubmissionController.list操作', '127.0.0.1', '/api/submissions', 'GET', '2026-05-03 01:55:37');
INSERT INTO `operation_log` VALUES (286, 2, 'teacher', 'GET /api/tasks', '用户teacher执行了TaskController.list操作', '127.0.0.1', '/api/tasks', 'GET', '2026-05-03 01:55:37');
INSERT INTO `operation_log` VALUES (287, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:55:38');
INSERT INTO `operation_log` VALUES (288, 2, 'teacher', 'GET /api/dashboard/teacher', '用户teacher执行了DashboardController.teacherStats操作', '127.0.0.1', '/api/dashboard/teacher', 'GET', '2026-05-03 01:55:38');
INSERT INTO `operation_log` VALUES (289, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:55:43');
INSERT INTO `operation_log` VALUES (290, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:56:09');
INSERT INTO `operation_log` VALUES (291, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:56:13');
INSERT INTO `operation_log` VALUES (292, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:56:39');
INSERT INTO `operation_log` VALUES (293, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:56:43');
INSERT INTO `operation_log` VALUES (294, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:57:09');
INSERT INTO `operation_log` VALUES (295, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:57:13');
INSERT INTO `operation_log` VALUES (296, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:57:39');
INSERT INTO `operation_log` VALUES (297, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:57:43');
INSERT INTO `operation_log` VALUES (298, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:58:09');
INSERT INTO `operation_log` VALUES (299, 2, 'teacher', 'GET /api/messages/unread-count', '用户teacher执行了MessageController.unreadCount操作', '127.0.0.1', '/api/messages/unread-count', 'GET', '2026-05-03 01:58:13');

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '解析结果表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of parse_result
-- ----------------------------

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '璇勫垎缁撴灉琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of score_result
-- ----------------------------

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
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鎴愭灉鎻愪氦琛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of submission
-- ----------------------------

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
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$KwSxLKxk8N/cW5qHiFQRueyRNxdmJcBc0XwMKf0ZhRjCs9h4hjuL6', 'ADMIN', 'admin', NULL, 'ENABLED', 0, NULL, '2026-05-03 01:28:52', 0, '2026-04-28 17:48:05', '2026-04-28 17:51:00');
INSERT INTO `user` VALUES (2, 'teacher', '$2a$10$KwSxLKxk8N/cW5qHiFQRueyRNxdmJcBc0XwMKf0ZhRjCs9h4hjuL6', 'TEACHER', '张老师', NULL, 'ENABLED', 0, NULL, '2026-05-03 01:55:13', 0, '2026-04-28 17:48:49', '2026-04-28 17:51:00');
INSERT INTO `user` VALUES (3, 'student', '$2a$10$KwSxLKxk8N/cW5qHiFQRueyRNxdmJcBc0XwMKf0ZhRjCs9h4hjuL6', 'STUDENT', '李同学', 1, 'ENABLED', 0, NULL, '2026-05-03 01:18:27', 0, '2026-04-28 17:48:49', '2026-04-28 17:51:00');

SET FOREIGN_KEY_CHECKS = 1;
