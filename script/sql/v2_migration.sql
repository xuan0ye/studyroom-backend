-- =============================================
-- v2_migration.sql - 自习室系统 V2 数据库迁移脚本
-- @author P_Peaceful
-- @date 2022-05-10
-- =============================================

-- ----------------------------
-- 1. 座位预占/锁定表 (sr_seat_lock)
-- ----------------------------
DROP TABLE IF EXISTS `sr_seat_lock`;
CREATE TABLE `sr_seat_lock` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '会员ID',
  `seat_id` bigint(20) DEFAULT NULL COMMENT '座位ID',
  `room_id` bigint(20) DEFAULT NULL COMMENT '自习室ID',
  `lock_type` tinyint(1) DEFAULT '0' COMMENT '锁定类型: 0-定时锁定, 1-手动锁定, 2-自动续期',
  `start_date` date DEFAULT NULL COMMENT '锁定开始日期',
  `end_date` date DEFAULT NULL COMMENT '锁定结束日期',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态: 0-待激活, 1-激活, 2-已过期, 3-已取消',
  `auto_renew` tinyint(1) DEFAULT '0' COMMENT '自动续期: 0-否, 1-是',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_seat_id` (`seat_id`) USING BTREE,
  KEY `idx_room_id` (`room_id`) USING BTREE,
  KEY `idx_start_end` (`start_date`,`end_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='座位预占/锁定表';

-- ----------------------------
-- 2. 日卡消费记录表 (sr_day_card_consumption)
-- ----------------------------
DROP TABLE IF EXISTS `sr_day_card_consumption`;
CREATE TABLE `sr_day_card_consumption` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '会员ID',
  `card_user_id` bigint(20) DEFAULT NULL COMMENT '会员卡关联ID',
  `seat_id` bigint(20) DEFAULT NULL COMMENT '座位ID',
  `consumption_date` date DEFAULT NULL COMMENT '消费日期',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态: 0-待使用, 1-已使用, 2-已失效',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_card_user_id` (`card_user_id`) USING BTREE,
  KEY `idx_consumption_date` (`consumption_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日卡消费记录表';

-- ----------------------------
-- 3. 自习室配置表 (sr_room_config)
-- ----------------------------
DROP TABLE IF EXISTS `sr_room_config`;
CREATE TABLE `sr_room_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `room_id` bigint(20) DEFAULT NULL COMMENT '自习室ID',
  `config_key` varchar(100) DEFAULT NULL COMMENT '配置键',
  `config_value` varchar(500) DEFAULT NULL COMMENT '配置值',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_room_id` (`room_id`) USING BTREE,
  KEY `idx_config_key` (`config_key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自习室配置表';

-- ----------------------------
-- 4. ALTER TABLE sr_order 新增字段（如不存在）
-- ----------------------------
ALTER TABLE `sr_order`
    ADD COLUMN `order_type` tinyint(1) DEFAULT '0' COMMENT '订单类型: 0-按时, 1-日卡, 2-月卡' AFTER `total`,
    ADD COLUMN `card_user_id` bigint(20) DEFAULT NULL COMMENT '关联会员卡ID' AFTER `order_type`,
    ADD COLUMN `lock_id` bigint(20) DEFAULT NULL COMMENT '关联锁定ID' AFTER `card_user_id`;

-- ----------------------------
-- 5. ALTER TABLE sr_card 新增字段（如不存在）
-- ----------------------------
ALTER TABLE `sr_card`
    ADD COLUMN `card_type` tinyint(1) DEFAULT '0' COMMENT '卡片类型: 0-按时卡, 1-日卡, 2-月卡' AFTER `term`,
    ADD COLUMN `max_daily_hours` int(11) DEFAULT '8' COMMENT '每日最大可用时长(小时)' AFTER `card_type`;

-- ----------------------------
-- 6. 默认自习室配置数据
-- ----------------------------
INSERT INTO `sr_room_config` (`room_id`, `config_key`, `config_value`, `remark`, `del_flag`) VALUES
(1, 'open_time', '08:00', '自习室开门时间', '0'),
(1, 'close_time', '23:00', '自习室关门时间', '0'),
(1, 'max_hours_per_order', '12', '单次最大预约时长(小时)', '0'),
(1, 'min_hours_per_order', '1', '单次最小预约时长(小时)', '0'),
(1, 'auto_lock_minutes', '30', '到店自动锁定座位超时时间(分钟)', '0'),
(1, 'device_auto_off', '1', '是否自动关闭设备: 0-否, 1-是', '0');
