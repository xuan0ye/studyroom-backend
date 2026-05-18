-- ===================================================
-- 米家IoT设备集成 - 数据库迁移脚本
-- 用于 StudyRoom 自习室小程序
-- ===================================================

-- 1. 创建座位-设备映射表
DROP TABLE IF EXISTS `sr_seat_device`;
CREATE TABLE `sr_seat_device` (
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `seat_id` bigint NOT NULL COMMENT '关联座位ID',
  `device_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT 'PLUG' COMMENT '设备类型: LIGHT-灯光, PLUG-插座, AC-空调',
  `device_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '设备名称',
  `protocol` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT 'MQTT' COMMENT '通信协议: MQTT, HTTP(HA_API), MIOT_API',
  `device_uid` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '设备唯一标识(HA entity_id / miot did / mqtt client_id)',
  `mqtt_topic` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'MQTT消息主题',
  `on_payload` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '{"power":"on"}' COMMENT '开指令payload',
  `off_payload` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT '{"power":"off"}' COMMENT '关指令payload',
  `status` tinyint DEFAULT '0' COMMENT '设备状态: 0-离线, 1-在线, 2-已开, 3-已关',
  `auto_control` tinyint DEFAULT '1' COMMENT '是否启用自动控制: 0-否, 1-是',
  `room_id` bigint DEFAULT NULL COMMENT '关联房间ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`device_id`),
  KEY `idx_seat_id` (`seat_id`),
  KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='座位IoT设备映射表';

-- 2. 插入示例数据(根据实际设备修改)
-- 示例: 为座位1绑定一个智能插座，通过Home Assistant控制
-- INSERT INTO sr_seat_device VALUES (
--   1, 1, 'PLUG', '座位1-智能插座', 'HTTP', 'switch.seat_1_plug', NULL,
--   '{"power":"on"}', '{"power":"off"}', 0, 1, 1,
--   NOW(), NOW(), 'admin', 'admin'
-- );
