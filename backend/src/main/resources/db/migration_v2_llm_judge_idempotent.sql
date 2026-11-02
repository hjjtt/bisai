-- ============================================================
-- 迁移 V2 幂等版 — LLM-as-a-Judge 结构补齐
--
-- 背景：migration_v2_llm_judge.sql 是 DDL 直改脚本，重复执行会在
-- ALTER TABLE 处报 Duplicate column，导致后续建表语句被跳过、
-- 而开发者以为已执行。本脚本改为可重复执行，并在结尾自检。
--
-- 执行：mysql bisai < migration_v2_llm_judge_idempotent.sql
-- ============================================================

-- ------------------------------------------------------------
-- 1. score_result 补 6 列（多轮采样 / 覆盖度 / 交叉模型 / 冗长修正）
--    MySQL 8 无 ADD COLUMN IF NOT EXISTS，用 information_schema 判存在。
-- ------------------------------------------------------------
SET @db := DATABASE();

SET @s := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'score_result' AND column_name = 'sample_scores') = 0,
  'ALTER TABLE `score_result` ADD COLUMN `sample_scores` TEXT NULL COMMENT ''多轮采样分数JSON数组, 如 [85,82,88]'' AFTER `evidence`',
  'DO 0');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'score_result' AND column_name = 'coverage_details') = 0,
  'ALTER TABLE `score_result` ADD COLUMN `coverage_details` TEXT NULL COMMENT ''结构化覆盖度分析JSON'' AFTER `sample_scores`',
  'DO 0');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'score_result' AND column_name = 'cross_model_score') = 0,
  'ALTER TABLE `score_result` ADD COLUMN `cross_model_score` DECIMAL(6,2) NULL COMMENT ''交叉模型(备用模型)评分'' AFTER `coverage_details`',
  'DO 0');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'score_result' AND column_name = 'cross_model_divergence') = 0,
  'ALTER TABLE `score_result` ADD COLUMN `cross_model_divergence` DECIMAL(6,2) NULL COMMENT ''主模型与交叉模型偏差(绝对值)'' AFTER `cross_model_score`',
  'DO 0');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'score_result' AND column_name = 'word_count') = 0,
  'ALTER TABLE `score_result` ADD COLUMN `word_count` INT NULL COMMENT ''提交内容字数(冗长偏差修正用)'' AFTER `cross_model_divergence`',
  'DO 0');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s := IF(
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = @db AND table_name = 'score_result' AND column_name = 'verbosity_factor') = 0,
  'ALTER TABLE `score_result` ADD COLUMN `verbosity_factor` DECIMAL(5,4) NULL COMMENT ''冗长偏差修正系数'' AFTER `word_count`',
  'DO 0');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. score_pairwise（Pairwise 比较结果）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `score_pairwise` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `submission_a_id` BIGINT NOT NULL COMMENT '提交A的ID',
  `submission_b_id` BIGINT NOT NULL COMMENT '提交B的ID',
  `winner` VARCHAR(16) NULL COMMENT '比较结果: A/B/TIE',
  `reasoning` TEXT NULL COMMENT 'AI比较理由',
  `model` VARCHAR(128) NULL COMMENT '使用的模型',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_task_id` (`task_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Pairwise比较结果表' ROW_FORMAT = Dynamic;

-- ------------------------------------------------------------
-- 3. score_consistency_snapshot（一致性统计快照）
--    注意 idx_task_date 覆盖 (task_id, snapshot_date)：
--    task_id 为 NULL 表示全局快照，MySQL 唯一索引允许多个 NULL，
--    因此全局快照的唯一性由应用侧的 saveSnapshot 先删后插保证。
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `score_consistency_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT NULL DEFAULT NULL COMMENT '任务ID, NULL表示全局统计',
  `total_evaluated` INT NOT NULL DEFAULT 0 COMMENT '总评估数',
  `total_teacher_confirmed` INT NOT NULL DEFAULT 0 COMMENT '教师已确认数',
  `pearson_correlation` DECIMAL(6,4) NULL DEFAULT NULL COMMENT 'Pearson相关系数',
  `spearman_correlation` DECIMAL(6,4) NULL DEFAULT NULL COMMENT 'Spearman相关系数',
  `rmse` DECIMAL(8,4) NULL DEFAULT NULL COMMENT '均方根误差',
  `mae` DECIMAL(8,4) NULL DEFAULT NULL COMMENT '平均绝对误差',
  `avg_divergence` DECIMAL(8,4) NULL DEFAULT NULL COMMENT 'AI与教师平均偏差',
  `cross_model_agreement` DECIMAL(6,4) NULL DEFAULT NULL COMMENT '多模型一致率',
  `snapshot_date` DATE NOT NULL COMMENT '快照日期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_task_date` (`task_id` ASC, `snapshot_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评分一致性统计快照' ROW_FORMAT = Dynamic;

-- ------------------------------------------------------------
-- 4. 自检：应输出 2 张表名 + 6 个列名
-- ------------------------------------------------------------
SELECT 'tables' AS kind, table_name AS name
  FROM information_schema.tables
 WHERE table_schema = @db AND table_name IN ('score_pairwise', 'score_consistency_snapshot')
UNION ALL
SELECT 'columns' AS kind, column_name AS name
  FROM information_schema.columns
 WHERE table_schema = @db AND table_name = 'score_result'
   AND column_name IN ('sample_scores', 'coverage_details', 'cross_model_score',
                       'cross_model_divergence', 'word_count', 'verbosity_factor')
 ORDER BY kind, name;
