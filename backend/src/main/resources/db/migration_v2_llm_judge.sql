-- ============================================================
-- LLM-as-a-Judge 评分改造 — 数据库迁移 V2
-- 执行方式: 在 bisai 数据库中手动执行本文件
-- ============================================================

USE bisai;

-- ----------------------------
-- 1. 扩展 score_result 表 — 多轮采样 / 覆盖度 / 交叉模型 / 冗长修正
-- ----------------------------
ALTER TABLE `score_result`
  ADD COLUMN `sample_scores` TEXT COMMENT '多轮采样分数JSON数组, 如 [85,82,88]' AFTER `evidence`,
  ADD COLUMN `coverage_details` TEXT COMMENT '结构化覆盖度分析JSON' AFTER `sample_scores`,
  ADD COLUMN `cross_model_score` DECIMAL(6,2) DEFAULT NULL COMMENT '交叉模型(备用模型)评分' AFTER `coverage_details`,
  ADD COLUMN `cross_model_divergence` DECIMAL(6,2) DEFAULT NULL COMMENT '主模型与交叉模型偏差(绝对值)' AFTER `cross_model_score`,
  ADD COLUMN `word_count` INT DEFAULT NULL COMMENT '提交内容字数(冗长偏差修正用)' AFTER `cross_model_divergence`,
  ADD COLUMN `verbosity_factor` DECIMAL(5,4) DEFAULT NULL COMMENT '冗长偏差修正系数' AFTER `word_count`;

-- ----------------------------
-- 2. Pairwise 比较结果表
-- ----------------------------
DROP TABLE IF EXISTS `score_pairwise`;
CREATE TABLE `score_pairwise` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `submission_a_id` BIGINT NOT NULL COMMENT '提交A的ID',
  `submission_b_id` BIGINT NOT NULL COMMENT '提交B的ID',
  `winner` VARCHAR(16) COMMENT '比较结果: A/B/TIE',
  `reasoning` TEXT COMMENT 'AI比较理由',
  `model` VARCHAR(128) COMMENT '使用的模型',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_task_id` (`task_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Pairwise比较结果表';

-- ----------------------------
-- 3. 评分一致性统计快照表
-- ----------------------------
DROP TABLE IF EXISTS `score_consistency_snapshot`;
CREATE TABLE `score_consistency_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT DEFAULT NULL COMMENT '任务ID, NULL表示全局统计',
  `total_evaluated` INT NOT NULL DEFAULT 0 COMMENT '总评估数',
  `total_teacher_confirmed` INT NOT NULL DEFAULT 0 COMMENT '教师已确认数',
  `pearson_correlation` DECIMAL(6,4) DEFAULT NULL COMMENT 'Pearson相关系数',
  `spearman_correlation` DECIMAL(6,4) DEFAULT NULL COMMENT 'Spearman相关系数',
  `rmse` DECIMAL(8,4) DEFAULT NULL COMMENT '均方根误差',
  `mae` DECIMAL(8,4) DEFAULT NULL COMMENT '平均绝对误差',
  `avg_divergence` DECIMAL(8,4) DEFAULT NULL COMMENT 'AI与教师平均偏差',
  `cross_model_agreement` DECIMAL(6,4) DEFAULT NULL COMMENT '多模型一致率',
  `snapshot_date` DATE NOT NULL COMMENT '快照日期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_task_date` (`task_id`, `snapshot_date`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评分一致性统计快照';
