CREATE DATABASE IF NOT EXISTS bisai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE bisai;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '登录用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '加密密码',
    `role` VARCHAR(32) NOT NULL DEFAULT 'STUDENT' COMMENT '角色: STUDENT/TEACHER/ADMIN',
    `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
    `class_id` BIGINT DEFAULT NULL COMMENT '学生所属班级ID',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    `must_change_password` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否首次登录必须修改密码',
    `last_password_change_at` DATETIME DEFAULT NULL COMMENT '最近密码修改时间',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最近登录时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 班级表
CREATE TABLE IF NOT EXISTS `class` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    `name` VARCHAR(128) NOT NULL COMMENT '班级名称',
    `grade` VARCHAR(32) DEFAULT NULL COMMENT '年级',
    `major` VARCHAR(128) DEFAULT NULL COMMENT '专业',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 课程表
CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程ID',
    `name` VARCHAR(128) NOT NULL COMMENT '课程名称',
    `teacher_id` BIGINT NOT NULL COMMENT '任课教师ID',
    `class_id` BIGINT NOT NULL COMMENT '授课班级ID',
    `description` TEXT DEFAULT NULL COMMENT '课程说明',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_class_id` (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 评价模板表
CREATE TABLE IF NOT EXISTS `evaluation_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `name` VARCHAR(128) NOT NULL COMMENT '模板名称',
    `description` TEXT DEFAULT NULL COMMENT '模板说明',
    `total_score` DECIMAL(6,2) NOT NULL DEFAULT 100.00 COMMENT '模板总分',
    `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价模板表';

-- 评价指标表
CREATE TABLE IF NOT EXISTS `indicator` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '指标ID',
    `template_id` BIGINT NOT NULL COMMENT '所属模板ID',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父级指标ID',
    `name` VARCHAR(128) NOT NULL COMMENT '指标名称',
    `weight` DECIMAL(6,2) NOT NULL COMMENT '权重',
    `max_score` DECIMAL(6,2) NOT NULL COMMENT '满分',
    `score_rule` TEXT DEFAULT NULL COMMENT '评分规则',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价指标表';

-- 实训任务表
CREATE TABLE IF NOT EXISTS `training_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `course_id` BIGINT NOT NULL COMMENT '所属课程ID',
    `template_id` BIGINT NOT NULL COMMENT '绑定评价模板ID',
    `title` VARCHAR(128) NOT NULL COMMENT '任务名称',
    `requirements` TEXT NOT NULL COMMENT '实训要求',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '截止时间',
    `allow_resubmit` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许重新提交',
    `allowed_file_types` VARCHAR(512) DEFAULT NULL COMMENT '允许文件类型',
    `max_file_size` BIGINT DEFAULT 209715200 COMMENT '最大文件大小',
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/CLOSED/ARCHIVED',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实训任务表';

-- 成果提交表
CREATE TABLE IF NOT EXISTS `submission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交ID',
    `task_id` BIGINT NOT NULL COMMENT '实训任务ID',
    `student_id` BIGINT NOT NULL COMMENT '学生ID',
    `submit_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `version` INT NOT NULL DEFAULT 1 COMMENT '提交版本号',
    `parse_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态',
    `check_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_CHECKED' COMMENT '核查状态',
    `score_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_SCORED' COMMENT '评分状态',
    `total_score` DECIMAL(6,2) DEFAULT NULL COMMENT '教师确认后的最终总分',
    `auto_total_score` DECIMAL(6,2) DEFAULT NULL COMMENT '系统建议总分',
    `teacher_comment` TEXT DEFAULT NULL COMMENT '教师评语',
    `parse_summary` TEXT DEFAULT NULL COMMENT 'AI解析摘要',
    `parse_topics` TEXT DEFAULT NULL COMMENT 'AI解析主题JSON',
    `parse_completeness` VARCHAR(32) DEFAULT NULL COMMENT '完整度评估',
    `parse_quality` VARCHAR(32) DEFAULT NULL COMMENT '质量评估',
    `parse_suggestions` TEXT DEFAULT NULL COMMENT '改进建议JSON',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_task_student` (`task_id`, `student_id`),
    KEY `idx_student_id` (`student_id`),
    KEY `idx_parse_status` (`parse_status`),
    KEY `idx_score_status` (`score_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成果提交表';

CREATE TABLE IF NOT EXISTS `parse_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '解析结果ID',
    `submission_id` BIGINT DEFAULT NULL COMMENT '提交ID',
    `knowledge_document_id` BIGINT DEFAULT NULL COMMENT '知识库文档ID',
    `file_id` BIGINT DEFAULT NULL COMMENT '文件ID',
    `parser_type` VARCHAR(64) NOT NULL COMMENT '解析方式',
    `content` LONGTEXT DEFAULT NULL COMMENT '解析文本',
    `summary` TEXT DEFAULT NULL COMMENT '摘要',
    `main_topics` TEXT DEFAULT NULL COMMENT '主题JSON',
    `completeness` VARCHAR(32) DEFAULT NULL COMMENT '完整度',
    `quality` VARCHAR(32) DEFAULT NULL COMMENT '质量',
    `suggestions` TEXT DEFAULT NULL COMMENT '建议JSON',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_submission_id` (`submission_id`),
    KEY `idx_knowledge_document_id` (`knowledge_document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析结果表';

-- 文件表
CREATE TABLE IF NOT EXISTS `file` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `submission_id` BIGINT DEFAULT NULL COMMENT '提交ID',
    `knowledge_document_id` BIGINT DEFAULT NULL COMMENT '知识库文档ID',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_path` VARCHAR(512) NOT NULL COMMENT '文件存储路径',
    `file_type` VARCHAR(32) NOT NULL COMMENT '文件类型',
    `file_size` BIGINT NOT NULL COMMENT '文件大小',
    `file_hash` VARCHAR(128) NOT NULL COMMENT '文件哈希',
    `version` INT NOT NULL DEFAULT 1 COMMENT '文件版本',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_submission_id` (`submission_id`),
    KEY `idx_knowledge_document_id` (`knowledge_document_id`),
    KEY `idx_file_hash` (`file_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- 核查结果表
CREATE TABLE IF NOT EXISTS `check_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '核查结果ID',
    `submission_id` BIGINT NOT NULL COMMENT '提交ID',
    `check_type` VARCHAR(64) NOT NULL COMMENT '核查类型',
    `check_item` VARCHAR(128) NOT NULL COMMENT '核查项',
    `result` VARCHAR(32) NOT NULL COMMENT '结果: COMPLETED/PARTIAL/NOT_COMPLETED',
    `description` TEXT DEFAULT NULL COMMENT '说明',
    `evidence` TEXT DEFAULT NULL COMMENT '证据片段',
    `suggestion` TEXT DEFAULT NULL COMMENT '修改建议',
    `risk_level` VARCHAR(16) DEFAULT 'LOW' COMMENT '风险等级',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_submission_id` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核查结果表';

-- 评分结果表
CREATE TABLE IF NOT EXISTS `score_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评分结果ID',
    `submission_id` BIGINT NOT NULL COMMENT '提交ID',
    `indicator_id` BIGINT NOT NULL COMMENT '指标ID',
    `auto_score` DECIMAL(6,2) DEFAULT NULL COMMENT '系统建议分',
    `teacher_score` DECIMAL(6,2) DEFAULT NULL COMMENT '教师确认分',
    `final_score` DECIMAL(6,2) DEFAULT NULL COMMENT '最终得分',
    `reason` TEXT DEFAULT NULL COMMENT '评分理由',
    `evidence` TEXT DEFAULT NULL COMMENT '评分证据',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_submission_id` (`submission_id`),
    KEY `idx_indicator_id` (`indicator_id`),
    UNIQUE KEY `uk_submission_indicator` (`submission_id`, `indicator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分结果表';

-- 知识库表
CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
    `name` VARCHAR(128) NOT NULL COMMENT '知识库名称',
    `course_id` BIGINT DEFAULT NULL COMMENT '适用课程ID',
    `description` TEXT DEFAULT NULL COMMENT '说明',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

-- 知识库文档表
CREATE TABLE IF NOT EXISTS `knowledge_document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '知识库ID',
    `file_id` BIGINT DEFAULT NULL COMMENT '关联文件ID',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `parse_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态',
    `vector_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '向量化状态',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_knowledge_base_id` (`knowledge_base_id`),
    KEY `idx_file_id` (`file_id`),
    KEY `idx_parse_vector_status` (`parse_status`, `vector_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

CREATE TABLE IF NOT EXISTS `document_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '切片ID',
    `knowledge_document_id` BIGINT NOT NULL COMMENT '知识库文档ID',
    `chunk_index` INT NOT NULL COMMENT '切片序号',
    `content` TEXT NOT NULL COMMENT '切片内容',
    `token_count` INT NOT NULL DEFAULT 0 COMMENT '估算token数',
    `embedding` LONGTEXT DEFAULT NULL COMMENT '向量JSON',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_knowledge_document_id` (`knowledge_document_id`),
    UNIQUE KEY `uk_document_chunk_index` (`knowledge_document_id`, `chunk_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档切片表';

CREATE TABLE IF NOT EXISTS `ai_call_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'AI调用ID',
    `model` VARCHAR(128) NOT NULL COMMENT '模型名称',
    `call_type` VARCHAR(32) NOT NULL COMMENT '调用类型',
    `input_tokens` INT NOT NULL DEFAULT 0 COMMENT '输入token',
    `output_tokens` INT NOT NULL DEFAULT 0 COMMENT '输出token',
    `total_tokens` INT NOT NULL DEFAULT 0 COMMENT '总token',
    `success` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id` BIGINT NOT NULL COMMENT '接收用户ID',
    `type` VARCHAR(32) NOT NULL COMMENT '消息类型',
    `title` VARCHAR(255) NOT NULL COMMENT '消息标题',
    `content` TEXT DEFAULT NULL COMMENT '消息内容',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_is_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '操作人用户名',
    `action` VARCHAR(128) NOT NULL COMMENT '操作类型',
    `description` TEXT DEFAULT NULL COMMENT '操作描述',
    `ip` VARCHAR(64) DEFAULT NULL COMMENT 'IP地址',
    `request_path` VARCHAR(512) DEFAULT NULL COMMENT '请求路径',
    `request_method` VARCHAR(16) DEFAULT NULL COMMENT '请求方法',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `config_key` VARCHAR(128) NOT NULL COMMENT '配置键',
    `config_value` TEXT DEFAULT NULL COMMENT '配置值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '说明',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 异步任务表
CREATE TABLE IF NOT EXISTS `async_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `task_type` VARCHAR(32) NOT NULL COMMENT '任务类型: PARSE/CHECK/SCORE/EXPORT',
    `biz_id` BIGINT NOT NULL COMMENT '业务ID(提交ID)',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED/RETRYING',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retry` INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `next_run_at` DATETIME DEFAULT NULL COMMENT '下次执行时间',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_biz_id` (`biz_id`),
    KEY `idx_status_next_run_at` (`status`, `next_run_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务表';

-- 评分校准表
CREATE TABLE IF NOT EXISTS `score_calibration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '校准记录ID',
    `task_id` BIGINT NOT NULL COMMENT '实训任务ID',
    `submission_id` BIGINT NOT NULL COMMENT '校准样本提交ID',
    `indicator_id` BIGINT NOT NULL COMMENT '指标ID',
    `calibration_score` DECIMAL(6,2) NOT NULL COMMENT '校准分数',
    `calibration_reason` TEXT DEFAULT NULL COMMENT '校准理由',
    `typical_advantages` TEXT DEFAULT NULL COMMENT '典型优点',
    `typical_problems` TEXT DEFAULT NULL COMMENT '典型问题',
    `deduction_basis` TEXT DEFAULT NULL COMMENT '扣分依据',
    `confirmed_by` BIGINT DEFAULT NULL COMMENT '确认人ID',
    `confirmed_at` DATETIME DEFAULT NULL COMMENT '确认时间',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_submission_id` (`submission_id`),
    KEY `idx_indicator_id` (`indicator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评分校准表';

-- 成绩修正表
CREATE TABLE IF NOT EXISTS `score_correction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '修正记录ID',
    `submission_id` BIGINT NOT NULL COMMENT '提交ID',
    `indicator_id` BIGINT DEFAULT NULL COMMENT '指标ID，为空表示修正总分',
    `original_score` DECIMAL(6,2) NOT NULL COMMENT '原始分数',
    `new_score` DECIMAL(6,2) NOT NULL COMMENT '修正后分数',
    `reason` TEXT NOT NULL COMMENT '修正原因',
    `corrected_by` BIGINT NOT NULL COMMENT '修正人ID',
    `corrected_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修正时间',
    `deleted` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_submission_id` (`submission_id`),
    KEY `idx_indicator_id` (`indicator_id`),
    KEY `idx_corrected_by` (`corrected_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩修正表';

-- 初始化管理员账号 (密码: admin123，BCrypt加密)
INSERT INTO `user` (`username`, `password`, `role`, `real_name`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'ADMIN', '系统管理员', 'ENABLED');

-- ============================================================
-- 增量迁移 SQL（已有数据库执行以下语句完成升级）
-- ============================================================

-- DB-42: 补充缺失索引
ALTER TABLE `file` ADD INDEX `idx_knowledge_document_id` (`knowledge_document_id`);
ALTER TABLE `file` ADD INDEX `idx_file_hash` (`file_hash`);
ALTER TABLE `score_correction` ADD INDEX `idx_indicator_id` (`indicator_id`);
ALTER TABLE `score_correction` ADD INDEX `idx_corrected_by` (`corrected_by`);
ALTER TABLE `score_calibration` ADD INDEX `idx_indicator_id` (`indicator_id`);
ALTER TABLE `class` ADD UNIQUE INDEX `uk_name` (`name`);

-- P2-25: 核心业务表添加逻辑删除字段
ALTER TABLE `parse_result` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除' AFTER `suggestions`;
ALTER TABLE `check_result` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除' AFTER `risk_level`;
ALTER TABLE `score_result` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除' AFTER `evidence`;

-- P2-26: 补全 updated_at 字段
ALTER TABLE `parse_result` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `deleted`;
ALTER TABLE `check_result` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `deleted`;

-- P3-35: 分数精度提升 DECIMAL(5,2) → DECIMAL(6,2)
ALTER TABLE `evaluation_template` MODIFY `total_score` DECIMAL(6,2) NOT NULL DEFAULT 100.00;
ALTER TABLE `indicator` MODIFY `weight` DECIMAL(6,2) NOT NULL, MODIFY `max_score` DECIMAL(6,2) NOT NULL;
ALTER TABLE `submission` MODIFY `total_score` DECIMAL(6,2) DEFAULT NULL, MODIFY `auto_total_score` DECIMAL(6,2) DEFAULT NULL;
ALTER TABLE `score_result` MODIFY `auto_score` DECIMAL(6,2) DEFAULT NULL, MODIFY `teacher_score` DECIMAL(6,2) DEFAULT NULL, MODIFY `final_score` DECIMAL(6,2) DEFAULT NULL;
ALTER TABLE `score_calibration` MODIFY `calibration_score` DECIMAL(6,2) NOT NULL;
ALTER TABLE `score_correction` MODIFY `original_score` DECIMAL(6,2) NOT NULL, MODIFY `new_score` DECIMAL(6,2) NOT NULL;

-- DB-44: parse_result 添加 CHECK 约束（确保至少一个外键不为 NULL）
ALTER TABLE `parse_result` ADD CONSTRAINT `chk_parse_result_ref` CHECK (
    submission_id IS NOT NULL OR knowledge_document_id IS NOT NULL OR file_id IS NOT NULL
);
