-- =============================================
-- schema_sync_migration.sql
-- 目的: 补齐后端实体/Mapper 已依赖、但当前库缺失的表与列
-- 适用库: wx_study_room
-- 执行:   mysql -u<user> -p <db_name> < schema_sync_migration.sql
-- 幂等: 只做 ADD COLUMN / CREATE TABLE IF NOT EXISTS, 不改动既有数据, 可重复执行
--
-- 背景(为什么必须补):
--   CardUserMapper.selectVoListByUserId 查询了
--     sc.party_size / sc.valid_start / sc.valid_end   (sr_card)
--     scu.remaining_days                              (sr_card_user)
--   这 4 列在库里不存在 => 该 SQL 直接 ERROR 1054
--   => 「我的卡券」列表接口报错, 永远看不到任何卡。
--   另外 sr_card_redeem(双人卡好友兑换码) 与 sr_promo(首页热门活动) 两张表缺失,
--   购买双人卡 / 首页活动接口会 500。
-- =============================================

SET @db_name := DATABASE();

-- ---------- 1. sr_card: party_size(套餐人数) ----------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='sr_card' AND COLUMN_NAME='party_size');
SET @ddl := IF(@c=0,
  'ALTER TABLE `sr_card` ADD COLUMN `party_size` int NULL DEFAULT 1 COMMENT ''套餐人数: 1=单人 2=双人(购买后额外生成1个好友兑换码)''',
  'SELECT ''sr_card.party_size exists, skip''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. sr_card: valid_start(固定区间开始日) ----------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='sr_card' AND COLUMN_NAME='valid_start');
SET @ddl := IF(@c=0,
  'ALTER TABLE `sr_card` ADD COLUMN `valid_start` date NULL DEFAULT NULL COMMENT ''固定使用起始日(与 valid_end 同时存在时为固定区间卡, 每天单独预约不连锁锁座)''',
  'SELECT ''sr_card.valid_start exists, skip''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 3. sr_card: valid_end(固定区间截止日, 含当天) ----------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='sr_card' AND COLUMN_NAME='valid_end');
SET @ddl := IF(@c=0,
  'ALTER TABLE `sr_card` ADD COLUMN `valid_end` date NULL DEFAULT NULL COMMENT ''固定使用截止日(含当天)''',
  'SELECT ''sr_card.valid_end exists, skip''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 4. sr_card_user: remaining_days(长期卡剩余可用天数) ----------
-- NULL = 未激活, 代码按 term 起算; 停卡(请假)时写入冻结后的剩余天数
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='sr_card_user' AND COLUMN_NAME='remaining_days');
SET @ddl := IF(@c=0,
  'ALTER TABLE `sr_card_user` ADD COLUMN `remaining_days` int NULL DEFAULT NULL COMMENT ''长期卡剩余可用天数(NULL=未激活,按term起算; 请假停卡时冻结剩余)''',
  'SELECT ''sr_card_user.remaining_days exists, skip''');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 5. sr_card_redeem(双人卡好友兑换码) ----------
CREATE TABLE IF NOT EXISTS `sr_card_redeem` (
  `id` bigint NOT NULL COMMENT '主键',
  `code` varchar(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '兑换码(8位)',
  `card_id` bigint DEFAULT NULL COMMENT '要发放的卡券ID',
  `card_title` varchar(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '卡券名称(冗余)',
  `term` int DEFAULT NULL COMMENT '兑换后有效期(天)',
  `party_size` int DEFAULT NULL COMMENT '套餐人数',
  `owner_user_id` bigint DEFAULT NULL COMMENT '购买者(生成码的人)',
  `owner_nickname` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '购买者昵称(冗余)',
  `order_id` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '购卡订单号',
  `status` int DEFAULT 0 COMMENT '状态: 0未使用 1已使用 2已作废',
  `used_by_user_id` bigint DEFAULT NULL COMMENT '兑换人',
  `used_by_nickname` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '兑换人昵称(冗余)',
  `used_time` datetime DEFAULT NULL COMMENT '兑换时间',
  `expire_time` datetime DEFAULT NULL COMMENT '兑换码有效期',
  `remark` varchar(500) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) CHARACTER SET utf8mb4 DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_owner_user_id` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='双人卡好友兑换码';

-- ---------- 6. sr_promo(首页热门活动) ----------
CREATE TABLE IF NOT EXISTS `sr_promo` (
  `id` bigint NOT NULL COMMENT '主键',
  `title` varchar(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '活动标题',
  `subtitle` varchar(255) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '副标题说明',
  `price` decimal(10,2) DEFAULT NULL COMMENT '现价(元)',
  `original_price` decimal(10,2) DEFAULT NULL COMMENT '原价(元)',
  `price_unit` varchar(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '价格单位(如 元/人)',
  `tag` varchar(32) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '角标(如 2人团)',
  `image_url` varchar(500) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '配图地址',
  `link_url` varchar(500) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '点击跳转路径',
  `sort` int DEFAULT 0 COMMENT '排序, 越小越前',
  `status` int DEFAULT 1 COMMENT '状态: 1上架 0下架',
  `remark` varchar(500) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) CHARACTER SET utf8mb4 DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页热门活动';
