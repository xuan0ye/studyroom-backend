-- =============================================
-- douyin_migration.sql - 抖音团购核销功能建表脚本
-- @date 2026-09-12
-- 数据库: wx_study_room (MySQL 8.0+)
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 抖音配置表 (sr_douyin_config)
--    存储抖音开放平台对接参数, 管理后台可改
-- ----------------------------
DROP TABLE IF EXISTS `sr_douyin_config`;
CREATE TABLE `sr_douyin_config` (
  `config_key` varchar(64) NOT NULL COMMENT '配置键',
  `config_value` varchar(512) DEFAULT '' COMMENT '配置值',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抖音团购核销配置表' ROW_FORMAT = Dynamic;

-- 初始化默认配置项
INSERT INTO `sr_douyin_config` (`config_key`, `config_value`, `update_time`) VALUES
  ('enabled', '0', NOW()),
  ('client_key', '', NOW()),
  ('client_secret', '', NOW()),
  ('poi_id', '', NOW()),
  ('spi_verify_enabled', '0', NOW());

-- ----------------------------
-- 2. 抖音核销记录表 (sr_douyin_verify)
--    每次核销一条; 撤销/退款审核都操作此表
-- ----------------------------
DROP TABLE IF EXISTS `sr_douyin_verify`;
CREATE TABLE `sr_douyin_verify` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '核销用户ID',
  `poi_id` varchar(64) DEFAULT NULL COMMENT '抖音门店POI',
  `code` varchar(128) DEFAULT NULL COMMENT '券码(脱敏, 仅前后2位)',
  `code_hash` varchar(64) DEFAULT NULL COMMENT '券码SHA-256哈希(本地去重)',
  `encrypted_code` varchar(512) DEFAULT NULL COMMENT '抖音加密券码',
  `certificate_id` varchar(128) DEFAULT NULL COMMENT '抖音券ID',
  `verify_id` varchar(128) DEFAULT NULL COMMENT '抖音核销单号',
  `douyin_order_id` varchar(128) DEFAULT NULL COMMENT '抖音订单号',
  `product_id` varchar(128) DEFAULT NULL COMMENT '抖音商品SKU ID',
  `product_title` varchar(256) DEFAULT NULL COMMENT '抖音商品名称',
  `grant_type` varchar(20) DEFAULT 'card' COMMENT '发放类型: card-发卡 balance-充值余额',
  `balance_amount` int(11) DEFAULT NULL COMMENT '充值点数(1点=1元)',
  `balance_action` tinyint(4) DEFAULT '0' COMMENT '余额动作: 0-无 1-已充值 2-已扣回',
  `verify_time` datetime DEFAULT NULL COMMENT '核销时间',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态: 1-已核销 2-已撤销 3-异常',
  `cancel_time` datetime DEFAULT NULL COMMENT '撤销时间',
  `card_user_id` bigint(20) DEFAULT NULL COMMENT '本地卡用户ID(发卡类)',
  `card_id` bigint(20) DEFAULT NULL COMMENT '本地卡类型ID',
  `seat_id` bigint(20) DEFAULT NULL COMMENT '锁座座位ID',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `raw_prepare` text COMMENT 'prepare原始报文(截断4000)',
  `raw_verify` text COMMENT 'verify原始报文(截断4000)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_code_hash` (`code_hash`) USING BTREE,
  KEY `idx_certificate_id` (`certificate_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_product_id` (`product_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抖音团购核销记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- 3. 抖音商品-本地卡映射表 (sr_douyin_product_map)
--    后台配置: 抖音团购SKU -> 本地卡类型/充值余额
-- ----------------------------
DROP TABLE IF EXISTS `sr_douyin_product_map`;
CREATE TABLE `sr_douyin_product_map` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id` varchar(128) NOT NULL COMMENT '抖音商品SKU ID',
  `product_title` varchar(256) DEFAULT NULL COMMENT '抖音商品名称(备注用)',
  `card_id` bigint(20) DEFAULT NULL COMMENT '本地卡类型ID(grant_type=card时必填)',
  `grant_type` varchar(20) DEFAULT 'card' COMMENT '发放类型: card-发卡 balance-充值余额',
  `balance_amount` int(11) DEFAULT NULL COMMENT '固定充值点数(为空则按券面额/名称推断)',
  `once_per_user` tinyint(1) DEFAULT '0' COMMENT '每人限核销一次: 0-否 1-是',
  `lock_mode` varchar(20) DEFAULT 'day' COMMENT '锁座模式: day-当天',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态: 1-启用 0-停用',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_product_id` (`product_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '抖音商品-本地卡映射表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
