-- =============================================
-- leave_quota_migration.sql
-- 长期卡(月卡/季卡)请假停卡: 每张卡默认 3 次, 后台可单独追加
-- 执行方式: mysql -u<user> -p <db_name> < leave_quota_migration.sql
--           注意: 必须带库名(脚本依赖 DATABASE()), 否则报 No database selected
-- 可重复执行(列已存在时自动跳过 ALTER), 执行前后均不丢数据
-- =============================================

SET @db_name := DATABASE();

-- 0) 前提列: leave_count(已请假次数)
--    历史库曾手工加过该列, 但基线脚本 wx_study_room-5-16.sql 里没有;
--    这里先兜底补齐, 避免后面 AFTER `leave_count` 失败。
SET @cnt_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'sr_card_user'
      AND COLUMN_NAME = 'leave_count'
);
SET @ddl := IF(
    @cnt_exists = 0,
    'ALTER TABLE `sr_card_user` ADD COLUMN `leave_count` int NOT NULL DEFAULT 0 COMMENT ''已请假次数(该卡累计请假停卡次数)''',
    'SELECT ''leave_count already exists, skip'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1) 主列: leave_quota(请假额度, 默认 3 次)
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'sr_card_user'
      AND COLUMN_NAME = 'leave_quota'
);
SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE `sr_card_user` ADD COLUMN `leave_quota` int NOT NULL DEFAULT 3 COMMENT ''请假额度(该卡总共可请假的次数, 默认3, 后台可追加)'' AFTER `leave_count`',
    'SELECT ''leave_quota already exists, skip'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 历史数据补默认额度 3(保留后台已单独追加过的值, 只补 NULL)
UPDATE `sr_card_user` SET `leave_quota` = 3 WHERE `leave_quota` IS NULL;

-- 3) 已请假次数为空补 0(避免额度计算出现 NULL)
UPDATE `sr_card_user` SET `leave_count` = 0 WHERE `leave_count` IS NULL;
