/*
 Navicat Premium Data Transfer

 Source Server         : MySQL8
 Source Server Type    : MySQL
 Source Server Version : 80019
 Source Host           : localhost:3306
 Source Schema         : wx_study_room

 Target Server Type    : MySQL
 Target Server Version : 80019
 File Encoding         : 65001

 Date: 16/05/2022 21:56:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table`  (
  `table_id` bigint(0) NOT NULL COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成业务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table
-- ----------------------------
INSERT INTO `gen_table` VALUES (1523310808388988930, 'sr_room', '自习室表', NULL, NULL, 'Room', 'crud', 'com.ruoyi.studyroom', 'store', 'room', '自习室', 'P_Peaceful', '0', '/', '{\"treeCode\":null,\"treeName\":null,\"treeParentCode\":null,\"parentMenuId\":\"1523299779399487489\"}', '', '2022-05-08 21:36:42', '', '2022-05-08 22:40:23', NULL);
INSERT INTO `gen_table` VALUES (1523312153158328321, 'sr_card', '优惠卡表', NULL, NULL, 'Card', 'crud', 'com.ruoyi.studyroom', 'store', 'card', '优惠卡', 'P_Peaceful', '0', '/', '{\"treeCode\":null,\"treeName\":null,\"treeParentCode\":null,\"parentMenuId\":\"1523303590499291138\"}', '', '2022-05-08 21:47:57', '', '2022-05-08 23:10:46', NULL);
INSERT INTO `gen_table` VALUES (1523314114679119874, 'sr_seat', '座位表', NULL, NULL, 'Seat', 'crud', 'com.ruoyi.studyroom', 'store', 'seat', '座位', 'P_Peaceful', '0', '/', '{\"treeCode\":null,\"treeName\":null,\"treeParentCode\":null,\"parentMenuId\":\"1523301438703898626\"}', '', '2022-05-08 21:36:42', '', '2022-05-08 22:51:02', NULL);
INSERT INTO `gen_table` VALUES (1523314804528881665, 'sr_balance', '余额表\r\n', NULL, NULL, 'Balance', 'crud', 'com.ruoyi.studyroom', 'studyroom', 'balance', '余额\r\n', 'P_Peaceful', '0', '/', NULL, '', '2022-05-08 21:36:42', '', '2022-05-08 21:36:42', NULL);
INSERT INTO `gen_table` VALUES (1523314804595990534, 'sr_card_user', '优惠卡-会员关联表', NULL, NULL, 'CardUser', 'crud', 'com.ruoyi.studyroom', 'studyroom', 'cardUser', '优惠卡-会员关联', 'P_Peaceful', '0', '/', NULL, '', '2022-05-08 21:36:42', '', '2022-05-08 21:36:42', NULL);
INSERT INTO `gen_table` VALUES (1523314804730208258, 'sr_order', '订单表', NULL, NULL, 'Order', 'crud', 'com.ruoyi.studyroom', 'order', 'order', '订单', 'P_Peaceful', '0', '/', '{\"treeCode\":null,\"treeName\":null,\"treeParentCode\":null,\"parentMenuId\":\"1523306205261959169\"}', '', '2022-05-08 21:36:42', '', '2022-05-08 23:05:54', NULL);
INSERT INTO `gen_table` VALUES (1523314804860231681, 'sr_order_recharge', '充值订单表', NULL, NULL, 'OrderRecharge', 'crud', 'com.ruoyi.studyroom', 'order', 'recharge', '充值订单', 'P_Peaceful', '0', '/', '{\"treeCode\":null,\"treeName\":null,\"treeParentCode\":null,\"parentMenuId\":\"1523306536213516290\"}', '', '2022-05-08 21:36:42', '', '2022-05-08 23:06:31', NULL);
INSERT INTO `gen_table` VALUES (1523314804923146250, 'sr_record', '学习记录表', NULL, NULL, 'Record', 'crud', 'com.ruoyi.studyroom', 'studyroom', 'record', '学习记录', 'P_Peaceful', '0', '/', NULL, '', '2022-05-08 21:36:42', '', '2022-05-08 21:36:42', NULL);
INSERT INTO `gen_table` VALUES (1523314805053169666, 'sr_room_seat', '自习室-座位关联表', NULL, NULL, 'RoomSeat', 'crud', 'com.ruoyi.studyroom', 'studyroom', 'roomSeat', '自习室-座位关联', 'P_Peaceful', '0', '/', NULL, '', '2022-05-08 21:36:42', '', '2022-05-08 21:36:42', NULL);
INSERT INTO `gen_table` VALUES (1523314805178998786, 'sr_seat_record', '座位-记录表', NULL, NULL, 'SeatRecord', 'crud', 'com.ruoyi.studyroom', 'studyroom', 'seatRecord', '座位-记录', 'P_Peaceful', '0', '/', NULL, '', '2022-05-08 21:36:42', '', '2022-05-08 21:36:42', NULL);
INSERT INTO `gen_table` VALUES (1523314805246107650, 'sr_swiper', '轮播图表', NULL, NULL, 'Swiper', 'crud', 'com.ruoyi.studyroom', 'store', 'swiper', '轮播图', 'P_Peaceful', '0', '/', '{\"treeCode\":null,\"treeName\":null,\"treeParentCode\":null,\"parentMenuId\":\"1523309159708725249\"}', '', '2022-05-08 21:36:42', '', '2022-05-08 23:10:04', NULL);
INSERT INTO `gen_table` VALUES (1523314805376131074, 'sr_user', '会员表', NULL, NULL, 'User', 'crud', 'com.ruoyi.studyroom', 'member', 'user', '会员', 'P_Peaceful', '0', '/', '{\"treeCode\":null,\"treeName\":null,\"treeParentCode\":null,\"parentMenuId\":\"1523305725727182849\"}', '', '2022-05-08 21:36:42', '', '2022-05-10 01:20:33', NULL);
INSERT INTO `gen_table` VALUES (1524041494745858049, 'sr_order_card', '优惠卡订单表', NULL, NULL, 'OrderCard', 'crud', 'com.ruoyi.studyroom', 'order', 'orderCard', '购卡管理', 'P_Peaceful', '0', '/', '{\"treeCode\":null,\"treeName\":null,\"treeParentCode\":null,\"parentMenuId\":\"1523304703357194242\"}', '', '2022-05-10 22:52:53', '', '2022-05-10 23:02:18', NULL);

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column`  (
  `column_id` bigint(0) NOT NULL COMMENT '编号',
  `table_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典类型',
  `sort` int(0) NULL DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '代码生成业务表字段' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------
INSERT INTO `gen_table_column` VALUES (1523310808519012354, '1523310808388988930', 'room_id', '自习室ID', 'bigint', 'Long', 'roomId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:36:13', '', '2022-05-08 22:40:24');
INSERT INTO `gen_table_column` VALUES (1523310808519012355, '1523310808388988930', 'room_name', '自习室名字', 'varchar(255)', 'String', 'roomName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 2, '', '2022-05-08 22:36:13', '', '2022-05-08 22:40:24');
INSERT INTO `gen_table_column` VALUES (1523310808519012356, '1523310808388988930', 'room_addr', '自习室地址', 'varchar(255)', 'String', 'roomAddr', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, '', '2022-05-08 22:36:13', '', '2022-05-08 22:40:24');
INSERT INTO `gen_table_column` VALUES (1523310808519012357, '1523310808388988930', 'phone', '自习室联系电话', 'varchar(255)', 'String', 'phone', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, '', '2022-05-08 22:36:13', '', '2022-05-08 22:40:24');
INSERT INTO `gen_table_column` VALUES (1523310808519012358, '1523310808388988930', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 5, '', '2022-05-08 22:36:13', '', '2022-05-08 22:40:24');
INSERT INTO `gen_table_column` VALUES (1523310808586121217, '1523310808388988930', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 6, '', '2022-05-08 22:36:13', '', '2022-05-08 22:40:25');
INSERT INTO `gen_table_column` VALUES (1523310808586121218, '1523310808388988930', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 7, '', '2022-05-08 22:36:13', '', '2022-05-08 22:40:25');
INSERT INTO `gen_table_column` VALUES (1523310808586121219, '1523310808388988930', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 8, '', '2022-05-08 22:36:13', '', '2022-05-08 22:40:25');
INSERT INTO `gen_table_column` VALUES (1523312153288351746, '1523312153158328321', 'id', '主键ID', 'bigint', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:47');
INSERT INTO `gen_table_column` VALUES (1523312153355460610, '1523312153158328321', 'card_title', '优惠卡', 'varchar(255)', 'String', 'cardTitle', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:47');
INSERT INTO `gen_table_column` VALUES (1523312153355460611, '1523312153158328321', 'price', '价格', 'decimal(10,2)', 'BigDecimal', 'price', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:47');
INSERT INTO `gen_table_column` VALUES (1523312153355460612, '1523312153158328321', 'usable', '可用区域', 'tinyint(1)', 'Integer', 'usable', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', 'area_status', 4, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:47');
INSERT INTO `gen_table_column` VALUES (1523312153355460613, '1523312153158328321', 'frequency', '次数(天)', 'int', 'Long', 'frequency', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:47');
INSERT INTO `gen_table_column` VALUES (1523312153355460614, '1523312153158328321', 'term', '有效期(天)', 'int', 'Long', 'term', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:48');
INSERT INTO `gen_table_column` VALUES (1523312153355460615, '1523312153158328321', 'status', '状态', 'tinyint(1)', 'Integer', 'status', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', 'common_status', 7, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:48');
INSERT INTO `gen_table_column` VALUES (1523312153355460616, '1523312153158328321', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:48');
INSERT INTO `gen_table_column` VALUES (1523312153355460617, '1523312153158328321', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 9, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:48');
INSERT INTO `gen_table_column` VALUES (1523312153355460618, '1523312153158328321', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:48');
INSERT INTO `gen_table_column` VALUES (1523312153355460619, '1523312153158328321', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, '', '2022-05-08 22:41:34', '', '2022-05-08 23:10:48');
INSERT INTO `gen_table_column` VALUES (1523314114804948993, '1523314114679119874', 'seat_id', '座位ID', 'bigint', 'Long', 'seatId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:49:21', '', '2022-05-08 22:51:02');
INSERT INTO `gen_table_column` VALUES (1523314114804948994, '1523314114679119874', 'seat_num', '座位号', 'int', 'Long', 'seatNum', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-08 22:49:21', '', '2022-05-08 22:51:02');
INSERT INTO `gen_table_column` VALUES (1523314114804948995, '1523314114679119874', 'seat_area', '座位区域', 'tinyint(1)', 'Integer', 'seatArea', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', 'area_status', 3, '', '2022-05-08 22:49:21', '', '2022-05-08 22:51:02');
INSERT INTO `gen_table_column` VALUES (1523314114804948996, '1523314114679119874', 'area_name', '区域名字', 'varchar(255)', 'String', 'areaName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 4, '', '2022-05-08 22:49:21', '', '2022-05-08 22:51:02');
INSERT INTO `gen_table_column` VALUES (1523314114804948997, '1523314114679119874', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 5, '', '2022-05-08 22:49:21', '', '2022-05-08 22:51:03');
INSERT INTO `gen_table_column` VALUES (1523314114804948998, '1523314114679119874', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 6, '', '2022-05-08 22:49:21', '', '2022-05-08 22:51:03');
INSERT INTO `gen_table_column` VALUES (1523314114804948999, '1523314114679119874', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 7, '', '2022-05-08 22:49:21', '', '2022-05-08 22:51:03');
INSERT INTO `gen_table_column` VALUES (1523314114804949000, '1523314114679119874', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 8, '', '2022-05-08 22:49:21', '', '2022-05-08 22:51:03');
INSERT INTO `gen_table_column` VALUES (1523314804528881666, '1523314804528881665', 'id', '主键ID', 'bigint', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804528881667, '1523314804528881665', 'user_id', '会员ID', 'bigint', 'Long', 'userId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804595990529, '1523314804528881665', 'balance', '余额', 'decimal(10,0)', 'Long', 'balance', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804595990530, '1523314804528881665', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 4, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804595990531, '1523314804528881665', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 5, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804595990532, '1523314804528881665', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 6, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804595990533, '1523314804528881665', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 7, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804663099393, '1523314804595990534', 'id', '主键ID', 'bigint', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804663099394, '1523314804595990534', 'user_id', '会员ID', 'bigint', 'Long', 'userId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804663099395, '1523314804595990534', 'card_id', '卡片ID', 'bigint', 'Long', 'cardId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804663099396, '1523314804595990534', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 4, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804663099397, '1523314804595990534', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 5, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804663099398, '1523314804595990534', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 6, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804663099399, '1523314804595990534', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 7, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804730208259, '1523314804730208258', 'id', '主键', 'bigint', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:52:06', '', '2022-05-08 23:05:55');
INSERT INTO `gen_table_column` VALUES (1523314804730208260, '1523314804730208258', 'order_id', '订单编号', 'bigint', 'Long', 'orderId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-08 22:52:06', '', '2022-05-08 23:05:55');
INSERT INTO `gen_table_column` VALUES (1523314804730208261, '1523314804730208258', 'user_id', '会员ID', 'bigint', 'Long', 'userId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, '', '2022-05-08 22:52:06', '', '2022-05-08 23:05:55');
INSERT INTO `gen_table_column` VALUES (1523314804730208262, '1523314804730208258', 'description', '商品描述', 'varchar(255)', 'String', 'description', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, '', '2022-05-08 22:52:06', '', '2022-05-08 23:05:56');
INSERT INTO `gen_table_column` VALUES (1523314804797317122, '1523314804730208258', 'start_time', '开始时间', 'datetime', 'Date', 'startTime', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'datetime', '', 7, '', '2022-05-08 22:52:06', '', '2022-05-08 23:05:56');
INSERT INTO `gen_table_column` VALUES (1523314804797317123, '1523314804730208258', 'end_time', '结束时间', 'datetime', 'Date', 'endTime', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'datetime', '', 8, '', '2022-05-08 22:52:06', '', '2022-05-08 23:05:56');
INSERT INTO `gen_table_column` VALUES (1523314804797317124, '1523314804730208258', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, '', '2022-05-08 22:52:06', '', '2022-05-08 23:05:56');
INSERT INTO `gen_table_column` VALUES (1523314804797317125, '1523314804730208258', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, '', '2022-05-08 22:52:06', '', '2022-05-08 23:05:56');
INSERT INTO `gen_table_column` VALUES (1523314804797317126, '1523314804730208258', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 12, '', '2022-05-08 22:52:06', '', '2022-05-08 23:05:57');
INSERT INTO `gen_table_column` VALUES (1523314804797317127, '1523314804730208258', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 13, '', '2022-05-08 22:52:06', '', '2022-05-08 23:05:57');
INSERT INTO `gen_table_column` VALUES (1523314804860231682, '1523314804860231681', 'id', '主键', 'bigint', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:52:06', '', '2022-05-08 23:06:31');
INSERT INTO `gen_table_column` VALUES (1523314804860231683, '1523314804860231681', 'order_id', '订单编号', 'bigint', 'Long', 'orderId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-08 22:52:06', '', '2022-05-08 23:06:31');
INSERT INTO `gen_table_column` VALUES (1523314804923146242, '1523314804860231681', 'user_id', '会员ID', 'bigint', 'Long', 'userId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, '', '2022-05-08 22:52:06', '', '2022-05-08 23:06:31');
INSERT INTO `gen_table_column` VALUES (1523314804923146243, '1523314804860231681', 'description', '商品描述', 'varchar(255)', 'String', 'description', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, '', '2022-05-08 22:52:06', '', '2022-05-08 23:06:31');
INSERT INTO `gen_table_column` VALUES (1523314804923146244, '1523314804860231681', 'amount_total', '支付金额', 'decimal(10,0)', 'Long', 'amountTotal', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, '', '2022-05-08 22:52:06', '', '2022-05-08 23:06:32');
INSERT INTO `gen_table_column` VALUES (1523314804923146245, '1523314804860231681', 'recharge_total', '充值额度', 'decimal(10,0)', 'Long', 'rechargeTotal', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, '', '2022-05-08 22:52:06', '', '2022-05-08 23:06:32');
INSERT INTO `gen_table_column` VALUES (1523314804923146246, '1523314804860231681', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, '', '2022-05-08 22:52:06', '', '2022-05-08 23:06:32');
INSERT INTO `gen_table_column` VALUES (1523314804923146247, '1523314804860231681', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 9, '', '2022-05-08 22:52:06', '', '2022-05-08 23:06:32');
INSERT INTO `gen_table_column` VALUES (1523314804923146248, '1523314804860231681', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, '', '2022-05-08 22:52:06', '', '2022-05-08 23:06:33');
INSERT INTO `gen_table_column` VALUES (1523314804923146249, '1523314804860231681', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, '', '2022-05-08 22:52:06', '', '2022-05-08 23:06:33');
INSERT INTO `gen_table_column` VALUES (1523314804990255105, '1523314804923146250', 'id', '主键ID', 'bigint', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804990255106, '1523314804923146250', 'user_id', '会员ID', 'bigint', 'Long', 'userId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804990255107, '1523314804923146250', 'hours', '小时数', 'double', 'Long', 'hours', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804990255108, '1523314804923146250', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 4, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804990255109, '1523314804923146250', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 5, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804990255110, '1523314804923146250', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 6, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314804990255111, '1523314804923146250', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 7, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314805116084225, '1523314805053169666', 'id', '主键ID', 'bigint', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314805116084226, '1523314805053169666', 'room_id', '自习室ID', 'bigint', 'Long', 'roomId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314805116084227, '1523314805053169666', 'seat_id', '座位ID', 'bigint', 'Long', 'seatId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314805116084228, '1523314805053169666', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 4, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314805116084229, '1523314805053169666', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 5, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314805116084230, '1523314805053169666', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 6, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314805116084231, '1523314805053169666', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 7, '', '2022-05-08 22:52:06', '', '2022-05-08 22:52:06');
INSERT INTO `gen_table_column` VALUES (1523314805178998787, '1523314805178998786', 'id', '主键ID', 'bigint', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:52:06', '', '2022-05-08 23:00:44');
INSERT INTO `gen_table_column` VALUES (1523314805178998788, '1523314805178998786', 'room_id', '自习室ID', 'bigint', 'Long', 'roomId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-08 22:52:06', '', '2022-05-08 23:00:45');
INSERT INTO `gen_table_column` VALUES (1523314805178998789, '1523314805178998786', 'seat_id', '座位ID', 'bigint', 'Long', 'seatId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, '', '2022-05-08 22:52:06', '', '2022-05-08 23:00:45');
INSERT INTO `gen_table_column` VALUES (1523314805178998790, '1523314805178998786', 'start_time', '开始时间', 'datetime', 'Date', 'startTime', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'datetime', '', 5, '', '2022-05-08 22:52:06', '', '2022-05-08 23:00:45');
INSERT INTO `gen_table_column` VALUES (1523314805178998791, '1523314805178998786', 'ent_time', '结束时间', 'datetime', 'Date', 'entTime', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'datetime', '', 6, '', '2022-05-08 22:52:06', '', '2022-05-08 23:00:45');
INSERT INTO `gen_table_column` VALUES (1523314805178998792, '1523314805178998786', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 7, '', '2022-05-08 22:52:06', '', '2022-05-08 23:00:46');
INSERT INTO `gen_table_column` VALUES (1523314805178998793, '1523314805178998786', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 8, '', '2022-05-08 22:52:06', '', '2022-05-08 23:00:46');
INSERT INTO `gen_table_column` VALUES (1523314805178998794, '1523314805178998786', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 9, '', '2022-05-08 22:52:06', '', '2022-05-08 23:00:46');
INSERT INTO `gen_table_column` VALUES (1523314805178998795, '1523314805178998786', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 10, '', '2022-05-08 22:52:06', '', '2022-05-08 23:00:46');
INSERT INTO `gen_table_column` VALUES (1523314805246107651, '1523314805246107650', 'oss_id', '对象存储主键', 'bigint', 'Long', 'ossId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:04');
INSERT INTO `gen_table_column` VALUES (1523314805246107652, '1523314805246107650', 'file_name', '文件名', 'varchar(255)', 'String', 'fileName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 2, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:04');
INSERT INTO `gen_table_column` VALUES (1523314805246107653, '1523314805246107650', 'original_name', '原名', 'varchar(255)', 'String', 'originalName', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 3, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:04');
INSERT INTO `gen_table_column` VALUES (1523314805246107654, '1523314805246107650', 'file_suffix', '文件后缀名', 'varchar(10)', 'String', 'fileSuffix', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:04');
INSERT INTO `gen_table_column` VALUES (1523314805313216513, '1523314805246107650', 'url', 'URL地址', 'varchar(500)', 'String', 'url', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'textarea', '', 5, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:05');
INSERT INTO `gen_table_column` VALUES (1523314805313216514, '1523314805246107650', 'status', '状态', 'tinyint(1)', 'Integer', 'status', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', 'common_status', 6, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:05');
INSERT INTO `gen_table_column` VALUES (1523314805313216515, '1523314805246107650', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 7, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:05');
INSERT INTO `gen_table_column` VALUES (1523314805313216516, '1523314805246107650', 'create_by', '上传人', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:05');
INSERT INTO `gen_table_column` VALUES (1523314805313216517, '1523314805246107650', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 9, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:05');
INSERT INTO `gen_table_column` VALUES (1523314805313216518, '1523314805246107650', 'update_by', '更新人', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:05');
INSERT INTO `gen_table_column` VALUES (1523314805313216519, '1523314805246107650', 'service', '服务商', 'varchar(10)', 'String', 'service', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 11, '', '2022-05-08 22:52:06', '', '2022-05-08 23:10:06');
INSERT INTO `gen_table_column` VALUES (1523314805376131075, '1523314805376131074', 'user_id', '会员ID', 'bigint', 'Long', 'userId', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-08 22:52:06', '', '2022-05-10 01:20:33');
INSERT INTO `gen_table_column` VALUES (1523314805376131076, '1523314805376131074', 'openid', '微信openid', 'varchar(255)', 'String', 'openid', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-08 22:52:06', '', '2022-05-10 01:20:33');
INSERT INTO `gen_table_column` VALUES (1523314805376131077, '1523314805376131074', 'nickname', '微信昵称', 'varchar(255)', 'String', 'nickname', '0', '0', '1', '1', '1', '1', '1', 'LIKE', 'input', '', 3, '', '2022-05-08 22:52:06', '', '2022-05-10 01:20:34');
INSERT INTO `gen_table_column` VALUES (1523314805376131078, '1523314805376131074', 'sex', '性别', 'tinyint(1)', 'Integer', 'sex', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'select', 'sys_user_sex', 5, '', '2022-05-08 22:52:06', '', '2022-05-10 01:20:34');
INSERT INTO `gen_table_column` VALUES (1523314805376131079, '1523314805376131074', 'phone', '手机号', 'varchar(255)', 'String', 'phone', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, '', '2022-05-08 22:52:06', '', '2022-05-10 01:20:34');
INSERT INTO `gen_table_column` VALUES (1523314805376131080, '1523314805376131074', 'status', '状态', 'tinyint(1)', 'Integer', 'status', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', 'common_status', 7, '', '2022-05-08 22:52:06', '', '2022-05-10 01:20:34');
INSERT INTO `gen_table_column` VALUES (1523314805376131081, '1523314805376131074', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, '', '2022-05-08 22:52:06', '', '2022-05-10 01:20:34');
INSERT INTO `gen_table_column` VALUES (1523314805376131082, '1523314805376131074', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, '1', '1', '1', '1', 'EQ', 'datetime', '', 9, '', '2022-05-08 22:52:06', '', '2022-05-10 01:20:34');
INSERT INTO `gen_table_column` VALUES (1523314805376131083, '1523314805376131074', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, '', '2022-05-08 22:52:06', '', '2022-05-10 01:20:34');
INSERT INTO `gen_table_column` VALUES (1523314805376131084, '1523314805376131074', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, '', '2022-05-08 22:52:06', '', '2022-05-10 01:20:34');
INSERT INTO `gen_table_column` VALUES (1523315480151564289, '1523314804730208258', 'pay_status', '付款状态', 'tinyint(1)', 'Integer', 'payStatus', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', 'pay_status', 9, '', '2022-05-08 22:54:47', '', '2022-05-08 23:05:56');
INSERT INTO `gen_table_column` VALUES (1523316219141791745, '1523314804860231681', 'pay_status', '付款状态', 'tinyint(1)', 'Integer', 'payStatus', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', 'pay_status', 7, '', '2022-05-08 22:57:43', '', '2022-05-08 23:06:32');
INSERT INTO `gen_table_column` VALUES (1523316982131826689, '1523314805178998786', 'user_id', '会员ID', 'bigint', 'Long', 'userId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, '', '2022-05-08 23:00:45', '', '2022-05-08 23:00:45');
INSERT INTO `gen_table_column` VALUES (1523318078636138497, '1523314804730208258', 'room_id', '自习室ID', 'bigint', 'Long', 'roomId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, '', '2022-05-08 23:05:06', '', '2022-05-08 23:05:55');
INSERT INTO `gen_table_column` VALUES (1523318079437250562, '1523314804730208258', 'seat_id', '座位ID', 'bigint', 'Long', 'seatId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, '', '2022-05-08 23:05:07', '', '2022-05-08 23:05:55');
INSERT INTO `gen_table_column` VALUES (1523354849344331777, '1523314805376131074', 'avatar_url', '微信头像', 'varchar(255)', 'String', 'avatarUrl', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'imageUpload', '', 4, '', '2022-05-09 01:31:13', '', '2022-05-10 01:20:34');
INSERT INTO `gen_table_column` VALUES (1524041495001710594, '1524041494745858049', 'id', '主键ID', 'bigint', 'Long', 'id', '1', '0', '1', NULL, '1', '1', NULL, 'EQ', 'input', '', 1, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');
INSERT INTO `gen_table_column` VALUES (1524041495001710595, '1524041494745858049', 'user_id', '用户ID', 'bigint', 'Long', 'userId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 2, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');
INSERT INTO `gen_table_column` VALUES (1524041495064625153, '1524041494745858049', 'card_id', '优惠卡ID', 'bigint', 'Long', 'cardId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 3, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');
INSERT INTO `gen_table_column` VALUES (1524041495064625154, '1524041494745858049', 'order_id', '订单编号', 'varchar(0)', 'String', 'orderId', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 4, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');
INSERT INTO `gen_table_column` VALUES (1524041495064625155, '1524041494745858049', 'description', '描述', 'varchar(255)', 'String', 'description', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 5, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');
INSERT INTO `gen_table_column` VALUES (1524041495064625156, '1524041494745858049', 'amount_total', '账单金额', 'decimal(10,0)', 'BigDecimal', 'amountTotal', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'input', '', 6, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');
INSERT INTO `gen_table_column` VALUES (1524041495064625157, '1524041494745858049', 'pay_status', '付款状态', 'tinyint(1)', 'Integer', 'payStatus', '0', '0', '1', '1', '1', '1', '1', 'EQ', 'radio', 'pay_status', 7, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');
INSERT INTO `gen_table_column` VALUES (1524041495064625158, '1524041494745858049', 'create_by', '创建者', 'varchar(64)', 'String', 'createBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 8, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');
INSERT INTO `gen_table_column` VALUES (1524041495064625159, '1524041494745858049', 'create_time', '创建时间', 'datetime', 'Date', 'createTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 9, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');
INSERT INTO `gen_table_column` VALUES (1524041495127539714, '1524041494745858049', 'update_by', '更新者', 'varchar(64)', 'String', 'updateBy', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'input', '', 10, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');
INSERT INTO `gen_table_column` VALUES (1524041495127539715, '1524041494745858049', 'update_time', '更新时间', 'datetime', 'Date', 'updateTime', '0', '0', NULL, NULL, NULL, NULL, NULL, 'EQ', 'datetime', '', 11, '', '2022-05-10 22:59:42', '', '2022-05-10 23:02:19');

-- ----------------------------
-- Table structure for sr_balance
-- ----------------------------
DROP TABLE IF EXISTS `sr_balance`;
CREATE TABLE `sr_balance`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `user_id` bigint(0) NOT NULL COMMENT '会员ID',
  `balance` decimal(10, 0) NULL DEFAULT NULL COMMENT '余额',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '余额表\r\n' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_balance
-- ----------------------------
INSERT INTO `sr_balance` VALUES (1524407108815663105, 1524407108731777026, 5156, NULL, '2022-05-11 23:12:32', 'P_Peaceful', '2022-05-15 21:31:07');
INSERT INTO `sr_balance` VALUES (1525579827405955073, 1525579827343040514, 0, NULL, '2022-05-15 04:52:29', NULL, '2022-05-15 04:52:29');

-- ----------------------------
-- Table structure for sr_card
-- ----------------------------
DROP TABLE IF EXISTS `sr_card`;
CREATE TABLE `sr_card`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `card_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '优惠卡标题',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '价格',
  `usable` tinyint(1) NULL DEFAULT NULL COMMENT '可用区域 0经济区 1舒适区',
  `frequency` int(0) NULL DEFAULT NULL COMMENT '次数(天)',
  `term` int(0) NULL DEFAULT NULL COMMENT '有效期(天)',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态 0停用 1正常',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '优惠卡表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_card
-- ----------------------------
INSERT INTO `sr_card` VALUES (1523353949733232642, '全天体验卡', 25.00, 1, 1, 1, 1, '', '2022-05-09 01:27:39', '', '2022-05-16 18:06:03');
INSERT INTO `sr_card` VALUES (1526141885650362370, '考试冲刺卡', 300.00, 0, 30, 90, 1, '', '2022-05-16 18:05:55', '', '2022-05-16 18:12:38');

-- ----------------------------
-- Table structure for sr_card_user
-- ----------------------------
DROP TABLE IF EXISTS `sr_card_user`;
CREATE TABLE `sr_card_user`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `user_id` bigint(0) NOT NULL COMMENT '会员ID',
  `card_id` bigint(0) NULL DEFAULT NULL COMMENT '卡片ID',
  `expiry_time` datetime(0) NULL DEFAULT NULL COMMENT '过期时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '优惠卡-会员关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_card_user
-- ----------------------------
INSERT INTO `sr_card_user` VALUES (1, 1524407108731777026, 1523353949733232642, '2022-05-16 19:39:27', '', NULL, '', NULL);
INSERT INTO `sr_card_user` VALUES (1526152715687288833, 1524407108731777026, 1523353949733232642, '2022-05-17 19:39:30', NULL, '2022-05-16 18:48:57', NULL, '2022-05-16 18:48:57');
INSERT INTO `sr_card_user` VALUES (1526164806146043905, 1524407108731777026, 1526141885650362370, '2022-08-14 19:36:59', NULL, '2022-05-16 19:36:59', NULL, '2022-05-16 19:36:59');

-- ----------------------------
-- Table structure for sr_order
-- ----------------------------
DROP TABLE IF EXISTS `sr_order`;
CREATE TABLE `sr_order`  (
  `id` bigint(0) NOT NULL COMMENT '主键',
  `order_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单编号',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '会员ID',
  `room_id` bigint(0) NULL DEFAULT NULL COMMENT '自习室ID',
  `seat_id` bigint(0) NULL DEFAULT NULL COMMENT '座位ID',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品描述',
  `start_time` datetime(0) NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime(0) NULL DEFAULT NULL COMMENT '结束时间',
  `hours` double NULL DEFAULT NULL COMMENT '小时数',
  `total` int(0) NULL DEFAULT NULL COMMENT '扣款余额',
  `pay_method` tinyint(1) NULL DEFAULT NULL COMMENT '支付方式 0 余额 1优惠卡',
  `pay_status` tinyint(1) NULL DEFAULT NULL COMMENT '付款状态',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_order
-- ----------------------------
INSERT INTO `sr_order` VALUES (1525522219206807553, '202205151525522217618509824', 1524407108731777026, 1523324098582265857, 1523607047911284738, '预定座位W区经济区-4', '2022-05-15 08:30:00', '2022-05-15 12:30:00', 4, 20, 0, 4, 'P_Peaceful', '2022-05-15 01:03:35', 'P_Peaceful', '2022-05-15 05:41:41');
INSERT INTO `sr_order` VALUES (1525530960941649921, '202205151525530959655342080', 1524407108731777026, 1523324098582265857, 1523607047772872706, '预定座位W区经济区-2', '2022-05-15 08:30:00', '2022-05-15 09:30:00', 1, 5, 0, 3, 'P_Peaceful', '2022-05-15 01:38:19', 'P_Peaceful', '2022-05-15 01:38:19');
INSERT INTO `sr_order` VALUES (1525591950056005633, '202205151525591948941725696', 1524407108731777026, 1523324098582265857, 1523608355519160322, '预定座位V区舒适区-1', '2022-05-15 08:00:00', '2022-05-15 10:00:00', 2, 16, 0, 3, 'P_Peaceful', '2022-05-15 05:40:40', 'P_Peaceful', '2022-05-15 05:40:40');
INSERT INTO `sr_order` VALUES (1525592672080273409, '202205151525592671121182720', 1524407108731777026, 1523324098582265857, 1523608355519160322, '预定座位V区舒适区-1', '2022-05-15 08:00:00', '2022-05-15 23:00:00', 15, 120, 0, 4, 'P_Peaceful', '2022-05-15 05:43:32', 'P_Peaceful', '2022-05-15 05:44:10');
INSERT INTO `sr_order` VALUES (1525831141713068033, '202205151525831140464529408', 1524407108731777026, 1523324098582265857, 1523608355519160322, '预定座位V区舒适区-1', '2022-05-16 08:00:00', '2022-05-16 09:00:00', 1, 8, 0, 3, 'P_Peaceful', '2022-05-15 21:31:07', 'P_Peaceful', '2022-05-15 21:31:07');

-- ----------------------------
-- Table structure for sr_order_card
-- ----------------------------
DROP TABLE IF EXISTS `sr_order_card`;
CREATE TABLE `sr_order_card`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '用户ID',
  `card_id` bigint(0) NULL DEFAULT NULL COMMENT '优惠卡ID',
  `order_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单编号',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '描述',
  `amount_total` decimal(10, 0) NULL DEFAULT NULL COMMENT '账单金额',
  `pay_status` tinyint(1) NULL DEFAULT NULL COMMENT '付款状态',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '优惠卡订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_order_card
-- ----------------------------
INSERT INTO `sr_order_card` VALUES (1526150298635657218, 1524407108731777026, 1523353949733232642, '202205161526150297764630528', '全天体验卡', 0, 0, 'P_Peaceful', '2022-05-16 18:39:20', 'P_Peaceful', '2022-05-16 18:39:20');
INSERT INTO `sr_order_card` VALUES (1526151856601489409, 1524407108731777026, 1523353949733232642, '202205161526151856032452608', '全天体验卡', 0, 0, 'P_Peaceful', '2022-05-16 18:45:32', 'P_Peaceful', '2022-05-16 18:45:32');
INSERT INTO `sr_order_card` VALUES (1526152657352908801, 1524407108731777026, 1523353949733232642, '202205161526152656465039360', '全天体验卡', 0, 1, 'P_Peaceful', '2022-05-16 18:48:43', NULL, '2022-05-16 18:48:57');
INSERT INTO `sr_order_card` VALUES (1526164752869994498, 1524407108731777026, 1526141885650362370, '202205161526164751696924672', '考试冲刺卡', 0, 1, 'P_Peaceful', '2022-05-16 19:36:47', NULL, '2022-05-16 19:36:59');

-- ----------------------------
-- Table structure for sr_order_recharge
-- ----------------------------
DROP TABLE IF EXISTS `sr_order_recharge`;
CREATE TABLE `sr_order_recharge`  (
  `id` bigint(0) NOT NULL COMMENT '主键',
  `order_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单编号',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '会员ID',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品描述',
  `amount_total` decimal(10, 2) NULL DEFAULT NULL COMMENT '支付金额',
  `recharge_total` decimal(10, 0) NULL DEFAULT NULL COMMENT '充值额度',
  `pay_status` tinyint(1) NULL DEFAULT NULL COMMENT '付款状态',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '充值订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_order_recharge
-- ----------------------------
INSERT INTO `sr_order_recharge` VALUES (1524784998900142081, '1', 1524407108731777026, 'test', 10.00, 10, 1, '', '2022-05-13 00:14:08', '', '2022-05-13 00:21:42');
INSERT INTO `sr_order_recharge` VALUES (1524788252862156801, '2', 1524407108731777026, '200', 200.00, 200, 1, '', '2022-05-13 00:27:03', '', '2022-05-13 00:27:23');
INSERT INTO `sr_order_recharge` VALUES (1525542130289315841, '202205151525542129678290944', 1524407108731777026, '账户余额充值-200', 20.00, 20, 0, 'P_Peaceful', '2022-05-15 02:22:42', 'P_Peaceful', '2022-05-15 02:22:42');
INSERT INTO `sr_order_recharge` VALUES (1525544677905371137, '202205151525544677315317760', 1524407108731777026, '账户余额充值-200', 20.00, 20, 0, 'P_Peaceful', '2022-05-15 02:32:49', 'P_Peaceful', '2022-05-15 02:32:49');
INSERT INTO `sr_order_recharge` VALUES (1525550635591266305, '202205151525550634715975680', 1524407108731777026, '账户余额充值-25001100', 0.01, 3600, 1, 'P_Peaceful', '2022-05-15 02:56:30', NULL, '2022-05-15 04:32:00');
INSERT INTO `sr_order_recharge` VALUES (1525568538877214722, '202205151525568538165514240', 1524407108731777026, '账户余额充值-25001100', 0.01, 3600, 0, 'P_Peaceful', '2022-05-15 04:07:38', 'P_Peaceful', '2022-05-15 04:07:38');
INSERT INTO `sr_order_recharge` VALUES (1525576051701882882, '202205151525576050738597888', 1524407108731777026, '账户余额充值: 20-赠送:0', 0.01, 20, 1, 'P_Peaceful', '2022-05-15 04:37:29', NULL, '2022-05-15 04:38:05');
INSERT INTO `sr_order_recharge` VALUES (1525576964998995969, '202205151525576964404809728', 1524407108731777026, '账户余额充值: 20-赠送:0', 0.01, 20, 0, 'P_Peaceful', '2022-05-15 04:41:07', 'P_Peaceful', '2022-05-15 04:41:07');
INSERT INTO `sr_order_recharge` VALUES (1525577571436634114, '202205151525577570792116224', 1524407108731777026, '账户余额充值: 20-赠送:0', 0.01, 20, 0, 'P_Peaceful', '2022-05-15 04:43:32', 'P_Peaceful', '2022-05-15 04:43:32');
INSERT INTO `sr_order_recharge` VALUES (1525577648959954945, '202205151525577648315437056', 1524407108731777026, '账户余额充值: 20-赠送:0', 0.01, 20, 0, 'P_Peaceful', '2022-05-15 04:43:50', 'P_Peaceful', '2022-05-15 04:43:50');
INSERT INTO `sr_order_recharge` VALUES (1525577714042970113, '202205151525577713352314880', 1524407108731777026, '账户余额充值: 20-赠送:0', 0.01, 20, 0, 'P_Peaceful', '2022-05-15 04:44:06', 'P_Peaceful', '2022-05-15 04:44:06');
INSERT INTO `sr_order_recharge` VALUES (1525577814408470530, '202205151525577813776535552', 1524407108731777026, '账户余额充值: 20-赠送:0', 0.01, 20, 0, 'P_Peaceful', '2022-05-15 04:44:29', 'P_Peaceful', '2022-05-15 04:44:29');
INSERT INTO `sr_order_recharge` VALUES (1525578036878548993, '202205151525578036271779840', 1524407108731777026, '账户余额充值: 20-赠送:0', 0.01, 20, 0, 'P_Peaceful', '2022-05-15 04:45:23', 'P_Peaceful', '2022-05-15 04:45:23');
INSERT INTO `sr_order_recharge` VALUES (1525578174225227777, '202205151525578173597487104', 1524407108731777026, '账户余额充值: 500-赠送:140', 0.01, 640, 1, 'P_Peaceful', '2022-05-15 04:45:55', NULL, '2022-05-15 04:46:10');
INSERT INTO `sr_order_recharge` VALUES (1525578429876445186, '202205151525578429303230464', 1524407108731777026, '账户余额充值: 500-赠送:140', 0.01, 640, 0, 'P_Peaceful', '2022-05-15 04:46:56', 'P_Peaceful', '2022-05-15 04:46:56');
INSERT INTO `sr_order_recharge` VALUES (1525578475455946753, '202205151525578474849177600', 1524407108731777026, '账户余额充值: 500-赠送:140', 0.01, 640, 1, 'P_Peaceful', '2022-05-15 04:47:07', NULL, '2022-05-15 04:47:19');
INSERT INTO `sr_order_recharge` VALUES (1525579589916073985, '202205151525579589305110528', 1524407108731777026, '账户余额充值: 20-赠送:0', 0.01, 20, 1, 'P_Peaceful', '2022-05-15 04:51:33', NULL, '2022-05-15 04:51:40');
INSERT INTO `sr_order_recharge` VALUES (1525579948948496385, '202205151525579948320755712', 1525579827343040514, '账户余额充值: 20-赠送:0', 0.01, 20, 0, NULL, '2022-05-15 04:52:58', NULL, '2022-05-15 04:52:58');
INSERT INTO `sr_order_recharge` VALUES (1525831935996805121, '202205151525831935373217792', 1524407108731777026, '账户余额充值: 2500-赠送:1100', 0.01, 3600, 0, 'P_Peaceful', '2022-05-15 21:34:17', 'P_Peaceful', '2022-05-15 21:34:17');

-- ----------------------------
-- Table structure for sr_record
-- ----------------------------
DROP TABLE IF EXISTS `sr_record`;
CREATE TABLE `sr_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '会员ID',
  `hours` double NULL DEFAULT NULL COMMENT '小时数',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学习记录排行榜表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_record
-- ----------------------------
INSERT INTO `sr_record` VALUES (1, 1, 9, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (2, 2, 10, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (3, 3, 11, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (4, 4, 12, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (5, 5, 13, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (6, 6, 14, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (7, 7, 15, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (8, 8, 16, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (9, 9, 17, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (10, 10, 18, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (11, 11, 19, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (12, 12, 20, NULL, NULL, NULL, NULL);
INSERT INTO `sr_record` VALUES (1524407108882771969, 1524407108731777026, 49, NULL, '2022-05-11 23:12:32', 'P_Peaceful', '2022-05-15 05:44:10');
INSERT INTO `sr_record` VALUES (1525579827473063938, 1525579827343040514, 0, NULL, '2022-05-15 04:52:29', NULL, '2022-05-15 04:52:29');

-- ----------------------------
-- Table structure for sr_room
-- ----------------------------
DROP TABLE IF EXISTS `sr_room`;
CREATE TABLE `sr_room`  (
  `room_id` bigint(0) NOT NULL COMMENT '自习室ID',
  `room_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '自习室名字',
  `room_addr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '自习室地址',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '自习室联系电话',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`room_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '自习室表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_room
-- ----------------------------
INSERT INTO `sr_room` VALUES (1523324098582265857, '1MiNa（重庆）大学城', '重庆市高新区虎溪街道大学城重庆科技学院', '', '', '2022-05-08 23:29:02', '', '2022-05-08 23:29:27');
INSERT INTO `sr_room` VALUES (1523594314784546818, '2MiNa（重庆）时代天街店', '重庆市渝中区大坪时代天街B馆时代星空时代星空1号楼2层', '', '', '2022-05-09 17:22:46', '', '2022-05-09 17:22:46');

-- ----------------------------
-- Table structure for sr_room_seat
-- ----------------------------
DROP TABLE IF EXISTS `sr_room_seat`;
CREATE TABLE `sr_room_seat`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `room_id` bigint(0) NULL DEFAULT NULL COMMENT '自习室ID',
  `seat_id` bigint(0) NULL DEFAULT NULL COMMENT '座位ID',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '自习室-座位关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_room_seat
-- ----------------------------
INSERT INTO `sr_room_seat` VALUES (1523706958044479489, 1523324098582265857, 1523607047621877761, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958124171266, 1523324098582265857, 1523607047772872706, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958166114305, 1523324098582265857, 1523607047835787266, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958166114306, 1523324098582265857, 1523607047911284738, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958229028866, 1523324098582265857, 1523607048032919554, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958229028867, 1523324098582265857, 1523607048095834114, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958291943425, 1523324098582265857, 1523607048158748674, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958291943426, 1523324098582265857, 1523607048158748675, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958354857986, 1523324098582265857, 1523607048221663233, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958354857987, 1523324098582265857, 1523607048221663234, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958354857988, 1523324098582265857, 1523607048221663235, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958417772545, 1523324098582265857, 1523607048284577794, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958417772546, 1523324098582265857, 1523607048284577795, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958480687105, 1523324098582265857, 1523607048347492353, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958480687106, 1523324098582265857, 1523607048347492354, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958547795969, 1523324098582265857, 1523607048347492355, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958547795970, 1523324098582265857, 1523607048414601218, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958610710529, 1523324098582265857, 1523607048414601219, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958610710530, 1523324098582265857, 1523607048414601220, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958610710531, 1523324098582265857, 1523607048494292994, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958677819394, 1523324098582265857, 1523608355519160322, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958677819395, 1523324098582265857, 1523608355607240705, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958740733953, 1523324098582265857, 1523608355670155266, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958740733954, 1523324098582265857, 1523608355670155267, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958803648513, 1523324098582265857, 1523608355733069826, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958803648514, 1523324098582265857, 1523608355733069827, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958803648515, 1523324098582265857, 1523608355733069828, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958866563073, 1523324098582265857, 1523608355800178690, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958866563074, 1523324098582265857, 1523608355800178691, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958866563075, 1523324098582265857, 1523608355863093250, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958929477634, 1523324098582265857, 1523608355863093251, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958929477635, 1523324098582265857, 1523608355930202113, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958992392193, 1523324098582265857, 1523608355930202114, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958992392194, 1523324098582265857, 1523608355930202115, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706958992392195, 1523324098582265857, 1523608355993116673, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959055306754, 1523324098582265857, 1523608355993116674, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959055306755, 1523324098582265857, 1523608356060225538, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959055306756, 1523324098582265857, 1523608356060225539, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959122415618, 1523324098582265857, 1523608356060225540, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959122415619, 1523324098582265857, 1523608356127334401, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959185330178, 1523324098582265857, 1523608792569831426, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959185330179, 1523594314784546818, 1523609710082248705, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959185330180, 1523594314784546818, 1523609710170329089, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959248244737, 1523594314784546818, 1523609710170329090, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959248244738, 1523594314784546818, 1523609710170329091, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959248244739, 1523594314784546818, 1523609710237437954, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959311159298, 1523594314784546818, 1523609710237437955, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959311159299, 1523594314784546818, 1523609710300352513, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959374073857, 1523594314784546818, 1523609710300352514, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959374073858, 1523594314784546818, 1523609710367461378, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959374073859, 1523594314784546818, 1523609710367461379, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959436988417, 1523594314784546818, 1523609710367461380, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959436988418, 1523594314784546818, 1523609710434570242, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959512485889, 1523594314784546818, 1523609710434570243, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959512485890, 1523594314784546818, 1523609710434570244, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959567011841, 1523594314784546818, 1523609710501679106, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959634120705, 1523594314784546818, 1523609710564593666, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959701229570, 1523594314784546818, 1523609710564593667, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959764144130, 1523594314784546818, 1523609710564593668, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959822864386, 1523594314784546818, 1523609710631702529, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959898361857, 1523594314784546818, 1523609710631702530, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959898361858, 1523594314784546818, 1523609957336363009, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959957082114, 1523594314784546818, 1523609957437026306, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706959957082115, 1523594314784546818, 1523609957504135169, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960024190977, 1523594314784546818, 1523609957504135170, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960024190978, 1523594314784546818, 1523609957504135171, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960024190979, 1523594314784546818, 1523609957571244033, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960087105538, 1523594314784546818, 1523609957571244034, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960087105539, 1523594314784546818, 1523609957638352898, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960087105540, 1523594314784546818, 1523609957638352899, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960154214401, 1523594314784546818, 1523609957701267458, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960154214402, 1523594314784546818, 1523609957701267459, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960154214403, 1523594314784546818, 1523609957701267460, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960221323265, 1523594314784546818, 1523609957701267461, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960221323266, 1523594314784546818, 1523609957764182018, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960221323267, 1523594314784546818, 1523609957764182019, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960288432129, 1523594314784546818, 1523609957827096577, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960288432130, 1523594314784546818, 1523609957827096578, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960351346690, 1523594314784546818, 1523609957827096579, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960351346691, 1523594314784546818, 1523609957890011138, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960351346692, 1523594314784546818, 1523609957890011139, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1523706960414261249, 1523324098582265857, 1523687875538411521, NULL, '2022-05-10 00:50:23', NULL, '2022-05-10 00:50:23');
INSERT INTO `sr_room_seat` VALUES (1525109730048585730, 1523324098582265857, 1523607047621877761, NULL, '2022-05-13 21:44:29', NULL, '2022-05-13 21:44:29');
INSERT INTO `sr_room_seat` VALUES (1525109730115694593, 1523324098582265857, 1523607047772872706, NULL, '2022-05-13 21:44:29', NULL, '2022-05-13 21:44:29');
INSERT INTO `sr_room_seat` VALUES (1525109730115694594, 1523324098582265857, 1523607047835787266, NULL, '2022-05-13 21:44:29', NULL, '2022-05-13 21:44:29');
INSERT INTO `sr_room_seat` VALUES (1525109730182803458, 1523324098582265857, 1523607047911284738, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730199580674, 1523324098582265857, 1523607048032919554, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730199580675, 1523324098582265857, 1523607048095834114, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730241523713, 1523324098582265857, 1523607048158748674, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730241523714, 1523324098582265857, 1523607048158748675, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730304438274, 1523324098582265857, 1523607048221663233, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730304438275, 1523324098582265857, 1523607048221663234, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730304438276, 1523324098582265857, 1523607048221663235, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730371547138, 1523324098582265857, 1523607048284577794, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730371547139, 1523324098582265857, 1523607048284577795, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730430267394, 1523324098582265857, 1523607048347492353, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730430267395, 1523324098582265857, 1523607048347492354, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730430267396, 1523324098582265857, 1523607048347492355, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730497376257, 1523324098582265857, 1523607048414601218, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730497376258, 1523324098582265857, 1523607048414601219, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730564485122, 1523324098582265857, 1523607048414601220, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730564485123, 1523324098582265857, 1523607048494292994, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730564485124, 1523324098582265857, 1523608355519160322, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730627399682, 1523324098582265857, 1523608355607240705, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730627399683, 1523324098582265857, 1523608355670155266, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730627399684, 1523324098582265857, 1523608355670155267, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730690314242, 1523324098582265857, 1523608355733069826, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730690314243, 1523324098582265857, 1523608355733069827, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730690314244, 1523324098582265857, 1523608355733069828, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730753228801, 1523324098582265857, 1523608355800178690, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730753228802, 1523324098582265857, 1523608355800178691, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730753228803, 1523324098582265857, 1523608355863093250, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730816143361, 1523324098582265857, 1523608355863093251, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730816143362, 1523324098582265857, 1523608355930202113, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730816143363, 1523324098582265857, 1523608355930202114, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730883252225, 1523324098582265857, 1523608355930202115, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730883252226, 1523324098582265857, 1523608355993116673, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730883252227, 1523324098582265857, 1523608355993116674, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730950361090, 1523324098582265857, 1523608356060225538, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730950361091, 1523324098582265857, 1523608356060225539, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109730950361092, 1523324098582265857, 1523608356060225540, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731021664258, 1523324098582265857, 1523608356127334401, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731021664259, 1523324098582265857, 1523608792569831426, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731076190210, 1523594314784546818, 1523609710082248705, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731076190211, 1523594314784546818, 1523609710170329089, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731143299074, 1523594314784546818, 1523609710170329090, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731143299075, 1523594314784546818, 1523609710170329091, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731210407937, 1523594314784546818, 1523609710237437954, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731210407938, 1523594314784546818, 1523609710237437955, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731210407939, 1523594314784546818, 1523609710300352513, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731281711105, 1523594314784546818, 1523609710300352514, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731281711106, 1523594314784546818, 1523609710367461378, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731348819969, 1523594314784546818, 1523609710367461379, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731348819970, 1523594314784546818, 1523609710367461380, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731403345922, 1523594314784546818, 1523609710434570242, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731403345923, 1523594314784546818, 1523609710434570243, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731470454785, 1523594314784546818, 1523609710434570244, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731470454786, 1523594314784546818, 1523609710501679106, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731470454787, 1523594314784546818, 1523609710564593666, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731533369346, 1523594314784546818, 1523609710564593667, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731533369347, 1523594314784546818, 1523609710564593668, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731596283906, 1523594314784546818, 1523609710631702529, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731596283907, 1523594314784546818, 1523609710631702530, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731596283908, 1523594314784546818, 1523609957336363009, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731659198465, 1523594314784546818, 1523609957437026306, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731659198466, 1523594314784546818, 1523609957504135169, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731659198467, 1523594314784546818, 1523609957504135170, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731726307330, 1523594314784546818, 1523609957504135171, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731726307331, 1523594314784546818, 1523609957571244033, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731726307332, 1523594314784546818, 1523609957571244034, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731793416193, 1523594314784546818, 1523609957638352898, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731793416194, 1523594314784546818, 1523609957638352899, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731856330754, 1523594314784546818, 1523609957701267458, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731856330755, 1523594314784546818, 1523609957701267459, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731856330756, 1523594314784546818, 1523609957701267460, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731919245313, 1523594314784546818, 1523609957701267461, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731919245314, 1523594314784546818, 1523609957764182018, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731982159873, 1523594314784546818, 1523609957764182019, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731982159874, 1523594314784546818, 1523609957827096577, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109731982159875, 1523594314784546818, 1523609957827096578, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732045074434, 1523594314784546818, 1523609957827096579, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732045074435, 1523594314784546818, 1523609957890011138, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732045074436, 1523594314784546818, 1523609957890011139, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732107988994, 1523324098582265857, 1523687875538411521, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732107988995, 1523594314784546818, 1525109728752545793, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732107988996, 1523594314784546818, 1525109728886763521, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732175097857, 1523594314784546818, 1525109728953872386, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732175097858, 1523594314784546818, 1525109729016786946, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732175097859, 1523594314784546818, 1525109729016786948, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732242206721, 1523594314784546818, 1525109729079701507, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732242206722, 1523594314784546818, 1525109729146810370, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732305121282, 1523594314784546818, 1525109729213919233, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732305121283, 1523594314784546818, 1525109729281028097, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732305121284, 1523594314784546818, 1525109729343942658, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732368035841, 1523594314784546818, 1525109729406857218, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732368035842, 1523594314784546818, 1525109729469771777, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732368035843, 1523594314784546818, 1525109729532686337, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732435144705, 1523594314784546818, 1525109729532686339, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732435144706, 1523594314784546818, 1525109729595600898, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732435144707, 1523594314784546818, 1525109729662709761, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732502253570, 1523594314784546818, 1525109729725624321, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732502253571, 1523594314784546818, 1525109729788538882, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732502253572, 1523594314784546818, 1525109729788538884, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525109732569362434, 1523594314784546818, 1525109729855647747, NULL, '2022-05-13 21:44:30', NULL, '2022-05-13 21:44:30');
INSERT INTO `sr_room_seat` VALUES (1525414036614176769, 1523594314784546818, 1525414036236689409, '', '2022-05-14 17:53:42', '', '2022-05-14 17:53:42');
INSERT INTO `sr_room_seat` VALUES (1525416138799411202, 1523594314784546818, 1525416138686164993, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416138866520066, 1523594314784546818, 1525416138799411203, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416138933628930, 1523594314784546818, 1525416138866520067, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139000737794, 1523594314784546818, 1525416138933628931, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139067846657, 1523594314784546818, 1525416139000737795, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139134955522, 1523594314784546818, 1525416139067846658, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139202064386, 1523594314784546818, 1525416139134955523, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139202064388, 1523594314784546818, 1525416139202064387, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139264978947, 1523594314784546818, 1525416139264978946, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139327893506, 1523594314784546818, 1525416139327893505, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139390808066, 1523594314784546818, 1525416139390808065, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139453722626, 1523594314784546818, 1525416139453722625, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139520831490, 1523594314784546818, 1525416139453722627, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139520831492, 1523594314784546818, 1525416139520831491, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139587940354, 1523594314784546818, 1525416139587940353, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139650854915, 1523594314784546818, 1525416139650854914, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139713769475, 1523594314784546818, 1525416139713769474, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139780878339, 1523594314784546818, 1525416139780878338, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139843792898, 1523594314784546818, 1525416139843792897, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');
INSERT INTO `sr_room_seat` VALUES (1525416139906707458, 1523594314784546818, 1525416139906707457, NULL, '2022-05-14 18:02:03', NULL, '2022-05-14 18:02:03');

-- ----------------------------
-- Table structure for sr_seat
-- ----------------------------
DROP TABLE IF EXISTS `sr_seat`;
CREATE TABLE `sr_seat`  (
  `seat_id` bigint(0) NOT NULL COMMENT '座位ID',
  `room_id` bigint(0) NULL DEFAULT NULL COMMENT '自习室ID',
  `seat_num` int(0) NULL DEFAULT NULL COMMENT '座位号',
  `seat_area` tinyint(1) NULL DEFAULT NULL COMMENT '座位区域 0经济区 1舒适区',
  `area_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '区域名字',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`seat_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '座位表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_seat
-- ----------------------------
INSERT INTO `sr_seat` VALUES (1523607047621877761, 1523324098582265857, 1, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607047772872706, 1523324098582265857, 2, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607047835787266, 1523324098582265857, 3, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607047911284738, 1523324098582265857, 4, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048032919554, 1523324098582265857, 5, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048095834114, 1523324098582265857, 6, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048158748674, 1523324098582265857, 7, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048158748675, 1523324098582265857, 8, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048221663233, 1523324098582265857, 9, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048221663234, 1523324098582265857, 10, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048221663235, 1523324098582265857, 11, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048284577794, 1523324098582265857, 12, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048284577795, 1523324098582265857, 13, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048347492353, 1523324098582265857, 14, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048347492354, 1523324098582265857, 15, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048347492355, 1523324098582265857, 16, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048414601218, 1523324098582265857, 17, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048414601219, 1523324098582265857, 18, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048414601220, 1523324098582265857, 19, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523607048494292994, 1523324098582265857, 20, 0, 'W区经济区', NULL, '2022-05-09 18:13:22', NULL, '2022-05-09 18:13:22');
INSERT INTO `sr_seat` VALUES (1523608355519160322, 1523324098582265857, 1, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355607240705, 1523324098582265857, 2, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355670155266, 1523324098582265857, 3, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355670155267, 1523324098582265857, 4, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355733069826, 1523324098582265857, 5, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355733069827, 1523324098582265857, 6, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355733069828, 1523324098582265857, 7, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355800178690, 1523324098582265857, 8, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355800178691, 1523324098582265857, 9, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355863093250, 1523324098582265857, 10, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355863093251, 1523324098582265857, 11, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355930202113, 1523324098582265857, 12, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355930202114, 1523324098582265857, 13, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355930202115, 1523324098582265857, 14, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355993116673, 1523324098582265857, 15, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608355993116674, 1523324098582265857, 16, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608356060225538, 1523324098582265857, 17, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608356060225539, 1523324098582265857, 18, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608356060225540, 1523324098582265857, 19, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608356127334401, 1523324098582265857, 20, 1, 'V区舒适区', NULL, '2022-05-09 18:18:34', NULL, '2022-05-09 18:18:34');
INSERT INTO `sr_seat` VALUES (1523608792569831426, 1523324098582265857, 21, 0, 'W区经济区', '', '2022-05-09 18:20:18', '', '2022-05-09 18:22:49');
INSERT INTO `sr_seat` VALUES (1523609710082248705, 1523594314784546818, 1, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710170329089, 1523594314784546818, 2, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710170329090, 1523594314784546818, 3, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710170329091, 1523594314784546818, 4, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710237437954, 1523594314784546818, 5, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710237437955, 1523594314784546818, 6, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710300352513, 1523594314784546818, 7, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710300352514, 1523594314784546818, 8, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710367461378, 1523594314784546818, 9, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710367461379, 1523594314784546818, 10, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710367461380, 1523594314784546818, 11, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710434570242, 1523594314784546818, 12, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710434570243, 1523594314784546818, 13, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710434570244, 1523594314784546818, 14, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710501679106, 1523594314784546818, 15, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710564593666, 1523594314784546818, 16, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710564593667, 1523594314784546818, 17, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710564593668, 1523594314784546818, 18, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710631702529, 1523594314784546818, 19, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1523609710631702530, 1523594314784546818, 20, 1, 'V区舒适区', NULL, '2022-05-09 18:23:57', NULL, '2022-05-09 18:23:57');
INSERT INTO `sr_seat` VALUES (1525407511417999362, 1523594314784546818, 1, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407511577382914, 1523594314784546818, 2, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407511640297473, 1523594314784546818, 3, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407511640297475, 1523594314784546818, 4, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407511703212035, 1523594314784546818, 5, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407511766126593, 1523594314784546818, 6, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407511896150018, 1523594314784546818, 7, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512021979137, 1523594314784546818, 8, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512152002562, 1523594314784546818, 9, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512210722818, 1523594314784546818, 10, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512273637378, 1523594314784546818, 11, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512340746242, 1523594314784546818, 12, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512403660801, 1523594314784546818, 13, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512529489922, 1523594314784546818, 14, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512529489924, 1523594314784546818, 15, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512588210179, 1523594314784546818, 16, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512655319043, 1523594314784546818, 17, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512714039298, 1523594314784546818, 18, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512781148162, 1523594314784546818, 19, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525407512781148164, 1523594314784546818, 20, 0, 'W区经济区', NULL, '2022-05-14 17:27:46', NULL, '2022-05-14 17:27:46');
INSERT INTO `sr_seat` VALUES (1525414036236689409, 1523594314784546818, 21, 0, 'test', '', '2022-05-14 17:53:42', '', '2022-05-14 17:53:42');

-- ----------------------------
-- Table structure for sr_seat_record
-- ----------------------------
DROP TABLE IF EXISTS `sr_seat_record`;
CREATE TABLE `sr_seat_record`  (
  `id` bigint(0) NOT NULL COMMENT '主键ID',
  `room_id` bigint(0) NULL DEFAULT NULL COMMENT '自习室ID',
  `seat_id` bigint(0) NULL DEFAULT NULL COMMENT '座位ID',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '会员ID',
  `start_time` datetime(0) NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime(0) NULL DEFAULT NULL COMMENT '结束时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '座位-记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_seat_record
-- ----------------------------
INSERT INTO `sr_seat_record` VALUES (1, 1523324098582265857, 1523607047621877761, NULL, '2022-05-10 10:00:00', '2022-05-10 15:00:00', '', NULL, '', NULL);
INSERT INTO `sr_seat_record` VALUES (1524075033038237698, 1523594314784546818, 1525407512021979137, 1, '2022-05-14 15:00:00', '2022-05-14 18:00:00', '', '2022-05-11 01:12:58', '', '2022-05-11 01:12:58');
INSERT INTO `sr_seat_record` VALUES (1524807397011984385, 1523594314784546818, 1523609710367461379, 1523324728096964609, '2022-05-14 18:00:00', '2022-05-14 20:00:00', '', '2022-05-13 01:43:08', '', '2022-05-13 01:43:08');
INSERT INTO `sr_seat_record` VALUES (1525474346859560962, 1523324098582265857, 1523608355519160322, 1524407108731777026, '2022-05-15 08:00:00', '2022-05-15 12:00:00', 'P_Peaceful', '2022-05-14 21:53:21', 'P_Peaceful', '2022-05-14 21:53:21');
INSERT INTO `sr_seat_record` VALUES (1525485928356315138, 1523324098582265857, 1523608355607240705, 1524407108731777026, '2022-05-15 08:00:00', '2022-05-15 10:00:00', 'P_Peaceful', '2022-05-14 22:39:22', 'P_Peaceful', '2022-05-14 22:39:22');
INSERT INTO `sr_seat_record` VALUES (1525519884288114690, 1523324098582265857, 1523607047621877761, 1524407108731777026, '2022-05-15 08:00:00', '2022-05-15 14:00:00', 'P_Peaceful', '2022-05-15 00:54:18', 'P_Peaceful', '2022-05-15 00:54:18');
INSERT INTO `sr_seat_record` VALUES (1525521026501951489, 1523324098582265857, 1523607047835787266, 1524407108731777026, '2022-05-15 08:00:00', '2022-05-15 10:00:00', 'P_Peaceful', '2022-05-15 00:58:50', 'P_Peaceful', '2022-05-15 00:58:50');
INSERT INTO `sr_seat_record` VALUES (1525522219575906305, 1523324098582265857, 1523607047911284738, 1524407108731777026, '2022-05-15 08:30:00', '2022-05-15 12:30:00', 'P_Peaceful', '2022-05-15 01:03:35', 'P_Peaceful', '2022-05-15 01:03:35');
INSERT INTO `sr_seat_record` VALUES (1525530961394634754, 1523324098582265857, 1523607047772872706, 1524407108731777026, '2022-05-15 08:30:00', '2022-05-15 09:30:00', 'P_Peaceful', '2022-05-15 01:38:19', 'P_Peaceful', '2022-05-15 01:38:19');
INSERT INTO `sr_seat_record` VALUES (1525591950383161346, 1523324098582265857, 1523608355519160322, 1524407108731777026, '2022-05-15 08:00:00', '2022-05-15 10:00:00', 'P_Peaceful', '2022-05-15 05:40:40', 'P_Peaceful', '2022-05-15 05:40:40');
INSERT INTO `sr_seat_record` VALUES (1525592672407429122, 1523324098582265857, 1523608355519160322, 1524407108731777026, '2022-05-15 08:00:00', '2022-05-15 23:00:00', 'P_Peaceful', '2022-05-15 05:43:32', 'P_Peaceful', '2022-05-15 05:43:32');
INSERT INTO `sr_seat_record` VALUES (1525831142174441473, 1523324098582265857, 1523608355519160322, 1524407108731777026, '2022-05-16 08:00:00', '2022-05-16 09:00:00', 'P_Peaceful', '2022-05-15 21:31:08', 'P_Peaceful', '2022-05-15 21:31:08');

-- ----------------------------
-- Table structure for sr_swiper
-- ----------------------------
DROP TABLE IF EXISTS `sr_swiper`;
CREATE TABLE `sr_swiper`  (
  `oss_id` bigint(0) NOT NULL COMMENT '对象存储主键',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '文件名',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '原名',
  `file_suffix` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '文件后缀名',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL地址',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态 0禁用 1正常',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '上传人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新人',
  `service` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'minio' COMMENT '服务商',
  PRIMARY KEY (`oss_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '轮播图表' ROW_FORMAT = Dynamic;



-- ----------------------------
-- Table structure for sr_user
-- ----------------------------
DROP TABLE IF EXISTS `sr_user`;
CREATE TABLE `sr_user`  (
  `user_id` bigint(0) NOT NULL COMMENT '会员ID',
  `openid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '微信openid',
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信昵称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信头像',
  `sex` tinyint(1) NULL DEFAULT NULL COMMENT '性别',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态 0 封禁 1 正常',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sr_user
-- ----------------------------
INSERT INTO `sr_user` VALUES (1, '1', 'test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/67bdafcbe1274da4ad7d1e8ac394e0ec.jpeg', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (2, '2', '2test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/e7ad6b2848564bed8ef3e25c8853ab03.png', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (3, '3', '3test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/b307fff2a40944c3b7ac2787a5228291.jfif', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (4, '4', '4test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/e7ad6b2848564bed8ef3e25c8853ab03.png', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (5, '5', '5-test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/6318af6a003f485d9565d3087d56095f.jpeg', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (6, '6', '6-test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/6318af6a003f485d9565d3087d56095f.jpeg', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (7, '7', '7-test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/6318af6a003f485d9565d3087d56095f.jpeg', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (8, '8', '8-test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/e7ad6b2848564bed8ef3e25c8853ab03.png', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (9, '9', '9-test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/6318af6a003f485d9565d3087d56095f.jpeg', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (10, '10', '10-test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/6318af6a003f485d9565d3087d56095f.jpeg', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (11, '11', '11-test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/e7ad6b2848564bed8ef3e25c8853ab03.png', 0, NULL, 1, NULL, NULL, NULL, NULL);
INSERT INTO `sr_user` VALUES (12, '12', '12-test-1', 'http://rbkn6fuw8.hn-bkt.clouddn.com/2022/05/09/b307fff2a40944c3b7ac2787a5228291.jfif', 0, NULL, 1, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `config_id` int(0) NOT NULL COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '参数配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', '2022-05-05 15:47:38', '', NULL, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `sys_config` VALUES (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', '2022-05-05 15:47:38', '', NULL, '初始化密码 123456');
INSERT INTO `sys_config` VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', '2022-05-05 15:47:38', '', NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `sys_config` VALUES (4, '账号自助-验证码开关', 'sys.account.captchaOnOff', 'true', 'Y', 'admin', '2022-05-05 15:47:38', '', NULL, '是否开启验证码功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 'admin', '2022-05-05 15:47:38', '', NULL, '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `sys_config` VALUES (11, 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 'admin', '2022-05-05 15:47:38', '', NULL, 'true:开启, false:关闭');

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `dept_id` bigint(0) NOT NULL COMMENT '部门id',
  `parent_id` bigint(0) NULL DEFAULT 0 COMMENT '父部门id',
  `ancestors` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '部门名称',
  `order_num` int(0) NULL DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (100, 0, '0', '米娜桑の自习室', 0, 'admin', '15288888888', 'ry@qq.com', '0', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:02:13');
INSERT INTO `sys_dept` VALUES (101, 100, '0,100', '重庆总公司', 1, '', '', '', '0', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:02:32');
INSERT INTO `sys_dept` VALUES (102, 100, '0,100', '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', '0', '2', 'admin', '2022-05-05 15:47:38', '', NULL);
INSERT INTO `sys_dept` VALUES (103, 101, '0,100,101', '研发部门', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:02:31');
INSERT INTO `sys_dept` VALUES (104, 101, '0,100,101', '市场部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:02:31');
INSERT INTO `sys_dept` VALUES (105, 101, '0,100,101', '测试部门', 3, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:02:31');
INSERT INTO `sys_dept` VALUES (106, 101, '0,100,101', '财务部门', 4, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:02:32');
INSERT INTO `sys_dept` VALUES (107, 101, '0,100,101', '运维部门', 5, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:02:32');
INSERT INTO `sys_dept` VALUES (108, 102, '0,100,102', '市场部门', 1, '若依', '15888888888', 'ry@qq.com', '0', '2', 'admin', '2022-05-05 15:47:38', '', NULL);
INSERT INTO `sys_dept` VALUES (109, 102, '0,100,102', '财务部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '2', 'admin', '2022-05-05 15:47:38', '', NULL);

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `dict_code` bigint(0) NOT NULL COMMENT '字典编码',
  `dict_sort` int(0) NULL DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO `sys_dict_data` VALUES (1, 1, '未知', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 21:41:44', '性别男');
INSERT INTO `sys_dict_data` VALUES (2, 2, '男', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 21:41:58', '性别女');
INSERT INTO `sys_dict_data` VALUES (3, 3, '女', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 21:42:05', '性别未知');
INSERT INTO `sys_dict_data` VALUES (4, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '显示菜单');
INSERT INTO `sys_dict_data` VALUES (5, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '隐藏菜单');
INSERT INTO `sys_dict_data` VALUES (6, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (7, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (12, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '系统默认是');
INSERT INTO `sys_dict_data` VALUES (13, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '系统默认否');
INSERT INTO `sys_dict_data` VALUES (14, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '通知');
INSERT INTO `sys_dict_data` VALUES (15, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '公告');
INSERT INTO `sys_dict_data` VALUES (16, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (17, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '关闭状态');
INSERT INTO `sys_dict_data` VALUES (18, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '新增操作');
INSERT INTO `sys_dict_data` VALUES (19, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '修改操作');
INSERT INTO `sys_dict_data` VALUES (20, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '删除操作');
INSERT INTO `sys_dict_data` VALUES (21, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '授权操作');
INSERT INTO `sys_dict_data` VALUES (22, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '导出操作');
INSERT INTO `sys_dict_data` VALUES (23, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '导入操作');
INSERT INTO `sys_dict_data` VALUES (24, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '强退操作');
INSERT INTO `sys_dict_data` VALUES (25, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '生成操作');
INSERT INTO `sys_dict_data` VALUES (26, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '清空操作');
INSERT INTO `sys_dict_data` VALUES (27, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '正常状态');
INSERT INTO `sys_dict_data` VALUES (28, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '停用状态');
INSERT INTO `sys_dict_data` VALUES (1523299032821764097, 0, '停用', '0', 'common_status', NULL, 'danger', 'N', '0', '', '2022-05-08 21:49:26', '', '2022-05-08 21:49:45', NULL);
INSERT INTO `sys_dict_data` VALUES (1523299073565233153, 1, '正常', '1', 'common_status', NULL, 'success', 'N', '0', '', '2022-05-08 21:49:35', '', '2022-05-08 21:49:52', NULL);
INSERT INTO `sys_dict_data` VALUES (1523312716784705537, 1, '舒适区', '1', 'area_status', NULL, 'success', 'N', '0', '', '2022-05-08 22:43:48', '', '2022-05-08 22:44:58', NULL);
INSERT INTO `sys_dict_data` VALUES (1523312811936686081, 0, '经济区', '0', 'area_status', NULL, 'primary', 'N', '0', '', '2022-05-08 22:44:11', '', '2022-05-08 22:44:52', NULL);
INSERT INTO `sys_dict_data` VALUES (1523315672774975490, 0, '未付款', '0', 'pay_status', NULL, 'warning', 'N', '0', '', '2022-05-08 22:55:33', '', '2022-05-08 22:55:33', NULL);
INSERT INTO `sys_dict_data` VALUES (1523315733638520834, 1, '已付款', '1', 'pay_status', NULL, 'success', 'N', '0', '', '2022-05-08 22:55:47', '', '2022-05-08 22:55:47', NULL);
INSERT INTO `sys_dict_data` VALUES (1523334235283890178, 2, '已取消', '2', 'pay_status', NULL, 'danger', 'N', '0', '', '2022-05-09 00:09:19', '', '2022-05-09 00:09:27', NULL);
INSERT INTO `sys_dict_data` VALUES (1523663606779568129, 3, '未消费', '3', 'pay_status', NULL, 'info', 'N', '0', '', '2022-05-09 21:58:07', '', '2022-05-09 21:58:56', NULL);
INSERT INTO `sys_dict_data` VALUES (1523663925710249985, 4, '已消费', '4', 'pay_status', NULL, 'primary', 'N', '0', '', '2022-05-09 21:59:23', '', '2022-05-09 21:59:32', NULL);

-- ----------------------------
-- Table structure for sys_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type`  (
  `dict_id` bigint(0) NOT NULL COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE INDEX `dict_type`(`dict_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典类型表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO `sys_dict_type` VALUES (1, '用户性别', 'sys_user_sex', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '用户性别列表');
INSERT INTO `sys_dict_type` VALUES (2, '菜单状态', 'sys_show_hide', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '菜单状态列表');
INSERT INTO `sys_dict_type` VALUES (3, '系统开关', 'sys_normal_disable', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '系统开关列表');
INSERT INTO `sys_dict_type` VALUES (6, '系统是否', 'sys_yes_no', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '系统是否列表');
INSERT INTO `sys_dict_type` VALUES (7, '通知类型', 'sys_notice_type', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '通知类型列表');
INSERT INTO `sys_dict_type` VALUES (8, '通知状态', 'sys_notice_status', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '通知状态列表');
INSERT INTO `sys_dict_type` VALUES (9, '操作类型', 'sys_oper_type', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '操作类型列表');
INSERT INTO `sys_dict_type` VALUES (10, '系统状态', 'sys_common_status', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '登录状态列表');
INSERT INTO `sys_dict_type` VALUES (1523297833057882114, '通用状态', 'common_status', '0', '', '2022-05-08 21:44:40', '', '2022-05-08 21:49:13', '通用状态');
INSERT INTO `sys_dict_type` VALUES (1523312633129312258, '可用区域', 'area_status', '0', '', '2022-05-08 22:43:28', '', '2022-05-08 22:43:28', NULL);
INSERT INTO `sys_dict_type` VALUES (1523315556605337602, '付款状态', 'pay_status', '0', '', '2022-05-08 22:55:05', '', '2022-05-08 22:55:05', NULL);

-- ----------------------------
-- Table structure for sys_logininfor
-- ----------------------------
DROP TABLE IF EXISTS `sys_logininfor`;
CREATE TABLE `sys_logininfor`  (
  `info_id` bigint(0) NOT NULL COMMENT '访问ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '提示消息',
  `login_time` datetime(0) NULL DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统访问记录' ROW_FORMAT = Dynamic;


-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint(0) NOT NULL COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(0) NULL DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int(0) NULL DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `query_param` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由参数',
  `is_frame` int(0) NULL DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `is_cache` int(0) NULL DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '系统管理', 0, 5, 'system', NULL, '', 1, 0, 'M', '0', '0', '', 'system', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:12:16', '系统管理目录');
INSERT INTO `sys_menu` VALUES (2, '系统监控', 0, 6, 'monitor', NULL, '', 1, 0, 'M', '0', '0', '', 'monitor', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:12:24', '系统监控目录');
INSERT INTO `sys_menu` VALUES (3, '系统工具', 0, 7, 'tool', NULL, '', 1, 0, 'M', '0', '0', '', 'tool', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:12:47', '系统工具目录');
INSERT INTO `sys_menu` VALUES (100, '用户管理', 1, 1, 'user', 'system/user/index', '', 1, 0, 'C', '0', '0', 'system:user:list', 'user', 'admin', '2022-05-05 15:47:38', '', NULL, '用户管理菜单');
INSERT INTO `sys_menu` VALUES (101, '角色管理', 1, 2, 'role', 'system/role/index', '', 1, 0, 'C', '0', '0', 'system:role:list', 'peoples', 'admin', '2022-05-05 15:47:38', '', NULL, '角色管理菜单');
INSERT INTO `sys_menu` VALUES (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', '', 1, 0, 'C', '0', '0', 'system:menu:list', 'tree-table', 'admin', '2022-05-05 15:47:38', '', NULL, '菜单管理菜单');
INSERT INTO `sys_menu` VALUES (103, '部门管理', 1, 4, 'dept', 'system/dept/index', '', 1, 0, 'C', '0', '0', 'system:dept:list', 'tree', 'admin', '2022-05-05 15:47:38', '', NULL, '部门管理菜单');
INSERT INTO `sys_menu` VALUES (104, '岗位管理', 1, 5, 'post', 'system/post/index', '', 1, 0, 'C', '0', '0', 'system:post:list', 'post', 'admin', '2022-05-05 15:47:38', '', NULL, '岗位管理菜单');
INSERT INTO `sys_menu` VALUES (105, '字典管理', 1, 6, 'dict', 'system/dict/index', '', 1, 0, 'C', '0', '0', 'system:dict:list', 'dict', 'admin', '2022-05-05 15:47:38', '', NULL, '字典管理菜单');
INSERT INTO `sys_menu` VALUES (106, '参数设置', 1, 7, 'config', 'system/config/index', '', 1, 0, 'C', '0', '0', 'system:config:list', 'edit', 'admin', '2022-05-05 15:47:38', '', NULL, '参数设置菜单');
INSERT INTO `sys_menu` VALUES (107, '通知公告', 1, 8, 'notice', 'system/notice/index', '', 1, 0, 'C', '0', '0', 'system:notice:list', 'message', 'admin', '2022-05-05 15:47:38', '', NULL, '通知公告菜单');
INSERT INTO `sys_menu` VALUES (108, '日志管理', 1, 9, 'log', '', '', 1, 0, 'M', '0', '0', '', 'log', 'admin', '2022-05-05 15:47:38', '', NULL, '日志管理菜单');
INSERT INTO `sys_menu` VALUES (109, '在线用户', 2, 1, 'online', 'monitor/online/index', '', 1, 0, 'C', '0', '0', 'monitor:online:list', 'online', 'admin', '2022-05-05 15:47:38', '', NULL, '在线用户菜单');
INSERT INTO `sys_menu` VALUES (111, '数据监控', 2, 3, 'druid', 'monitor/druid/index', '', 1, 0, 'C', '0', '0', 'monitor:druid:list', 'druid', 'admin', '2022-05-05 15:47:38', '', NULL, '数据监控菜单');
INSERT INTO `sys_menu` VALUES (113, '缓存监控', 2, 5, 'cache', 'monitor/cache/index', '', 1, 0, 'C', '0', '0', 'monitor:cache:list', 'redis', 'admin', '2022-05-05 15:47:38', '', NULL, '缓存监控菜单');
INSERT INTO `sys_menu` VALUES (114, '表单构建', 3, 1, 'build', 'tool/build/index', '', 1, 0, 'C', '0', '0', 'tool:build:list', 'build', 'admin', '2022-05-05 15:47:38', '', NULL, '表单构建菜单');
INSERT INTO `sys_menu` VALUES (115, '代码生成', 3, 2, 'gen', 'tool/gen/index', '', 1, 0, 'C', '0', '0', 'tool:gen:list', 'code', 'admin', '2022-05-05 15:47:38', '', NULL, '代码生成菜单');
INSERT INTO `sys_menu` VALUES (116, '系统接口', 3, 3, 'swagger', 'tool/swagger/index', '', 1, 0, 'C', '0', '0', 'tool:swagger:list', 'swagger', 'admin', '2022-05-05 15:47:38', '', NULL, '系统接口菜单');
INSERT INTO `sys_menu` VALUES (117, 'Admin监控', 2, 5, 'Admin', 'monitor/admin/index', '', 1, 0, 'C', '0', '0', 'monitor:admin:list', 'dashboard', 'admin', '2022-05-05 15:47:38', '', NULL, 'Admin监控菜单');
INSERT INTO `sys_menu` VALUES (118, '文件管理', 1, 10, 'oss', 'system/oss/index', '', 1, 0, 'C', '0', '0', 'system:oss:list', 'upload', 'admin', '2022-05-05 15:47:38', '', NULL, '文件管理菜单');
INSERT INTO `sys_menu` VALUES (120, '任务调度中心', 2, 5, 'XxlJob', 'monitor/xxljob/index', '', 1, 0, 'C', '0', '0', 'monitor:xxljob:list', 'job', 'admin', '2022-05-05 15:47:38', '', NULL, 'Xxl-Job控制台菜单');
INSERT INTO `sys_menu` VALUES (500, '操作日志', 108, 1, 'operlog', 'monitor/operlog/index', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list', 'form', 'admin', '2022-05-05 15:47:38', '', NULL, '操作日志菜单');
INSERT INTO `sys_menu` VALUES (501, '登录日志', 108, 2, 'logininfor', 'monitor/logininfor/index', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor', 'admin', '2022-05-05 15:47:38', '', NULL, '登录日志菜单');
INSERT INTO `sys_menu` VALUES (1001, '用户查询', 100, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:user:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1002, '用户新增', 100, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:user:add', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1003, '用户修改', 100, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1004, '用户删除', 100, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1005, '用户导出', 100, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:user:export', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1006, '用户导入', 100, 6, '', '', '', 1, 0, 'F', '0', '0', 'system:user:import', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1007, '重置密码', 100, 7, '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1008, '角色查询', 101, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:role:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1009, '角色新增', 101, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:role:add', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1010, '角色修改', 101, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1011, '角色删除', 101, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1012, '角色导出', 101, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:role:export', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1013, '菜单查询', 102, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1014, '菜单新增', 102, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1015, '菜单修改', 102, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1016, '菜单删除', 102, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1017, '部门查询', 103, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1018, '部门新增', 103, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1019, '部门修改', 103, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1020, '部门删除', 103, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1021, '岗位查询', 104, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:post:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1022, '岗位新增', 104, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:post:add', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1023, '岗位修改', 104, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1024, '岗位删除', 104, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1025, '岗位导出', 104, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:post:export', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1026, '字典查询', 105, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1027, '字典新增', 105, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:add', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1028, '字典修改', 105, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1029, '字典删除', 105, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1030, '字典导出', 105, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:dict:export', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1031, '参数查询', 106, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1032, '参数新增', 106, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:add', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1033, '参数修改', 106, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:edit', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1034, '参数删除', 106, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1035, '参数导出', 106, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:config:export', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1036, '公告查询', 107, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1037, '公告新增', 107, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:add', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1038, '公告修改', 107, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1039, '公告删除', 107, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1040, '操作查询', 500, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1041, '操作删除', 500, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1042, '日志导出', 500, 4, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1043, '登录查询', 501, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1044, '登录删除', 501, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1045, '日志导出', 501, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1046, '在线查询', 109, 1, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1047, '批量强退', 109, 2, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1048, '单条强退', 109, 3, '#', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1055, '生成查询', 115, 1, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1056, '生成修改', 115, 2, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1057, '生成删除', 115, 3, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1058, '导入代码', 115, 2, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1059, '预览代码', 115, 4, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1060, '生成代码', 115, 5, '#', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1600, '文件查询', 118, 1, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:query', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1601, '文件上传', 118, 2, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:upload', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1602, '文件下载', 118, 3, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:download', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1603, '文件删除', 118, 4, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:remove', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1604, '配置添加', 118, 5, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:add', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1605, '配置编辑', 118, 6, '#', '', '', 1, 0, 'F', '0', '0', 'system:oss:edit', '#', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1523299432647987201, '门店管理', 0, 1, 'store', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'shopping', '', '2022-05-08 21:51:01', '', '2022-05-08 21:52:55', '');
INSERT INTO `sys_menu` VALUES (1523299779399487489, '自习室管理', 1523299432647987201, 1, 'room', 'store/room/index', NULL, 1, 0, 'C', '0', '0', 'store:room:list', 'row', '', '2022-05-08 21:52:24', '', '2022-05-08 21:59:29', '');
INSERT INTO `sys_menu` VALUES (1523300651495952385, '自习室添加', 1523299779399487489, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:room:add', '#', '', '2022-05-08 21:55:52', '', '2022-05-08 21:55:52', '');
INSERT INTO `sys_menu` VALUES (1523300738817167362, '自习室编辑', 1523299779399487489, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:room:edit', '#', '', '2022-05-08 21:56:12', '', '2022-05-08 21:56:12', '');
INSERT INTO `sys_menu` VALUES (1523300935102205953, '自习室删除', 1523299779399487489, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:room:remove', '#', '', '2022-05-08 21:56:59', '', '2022-05-08 22:10:35', '');
INSERT INTO `sys_menu` VALUES (1523301438703898626, '座位管理', 1523299432647987201, 2, 'seat', 'store/seat/index', NULL, 1, 0, 'C', '0', '0', 'store:seat:list', 'cascader', '', '2022-05-08 21:58:59', '', '2022-05-08 23:30:20', '');
INSERT INTO `sys_menu` VALUES (1523301684200706049, '座位添加', 1523301438703898626, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:seat:add', '#', '', '2022-05-08 21:59:58', '', '2022-05-08 21:59:58', '');
INSERT INTO `sys_menu` VALUES (1523301762424475650, '座位编辑', 1523301438703898626, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:seat:edit', '#', '', '2022-05-08 22:00:16', '', '2022-05-08 22:00:16', '');
INSERT INTO `sys_menu` VALUES (1523301915243941889, '座位删除', 1523301438703898626, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:seat:remove', '#', '', '2022-05-08 22:00:53', '', '2022-05-08 22:10:19', '');
INSERT INTO `sys_menu` VALUES (1523303590499291138, '卡券管理', 1523299432647987201, 3, 'card', 'store/card/index', NULL, 1, 0, 'C', '0', '0', 'store:card:list', 'table', '', '2022-05-08 22:07:32', '', '2022-05-08 22:07:32', '');
INSERT INTO `sys_menu` VALUES (1523303696543879169, '卡券增加', 1523303590499291138, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:card:add', '#', '', '2022-05-08 22:07:58', '', '2022-05-08 22:07:58', '');
INSERT INTO `sys_menu` VALUES (1523303786603974658, '卡券编辑', 1523303590499291138, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:card:edit', '#', '', '2022-05-08 22:08:19', '', '2022-05-08 22:08:19', '');
INSERT INTO `sys_menu` VALUES (1523303982624772097, '卡券删除', 1523303590499291138, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:card:remove', '#', '', '2022-05-08 22:09:06', '', '2022-05-08 22:09:06', '');
INSERT INTO `sys_menu` VALUES (1523304703357194242, '订单管理', 0, 3, 'order', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'money', '', '2022-05-08 22:11:58', '', '2022-05-08 22:13:00', '');
INSERT INTO `sys_menu` VALUES (1523305413918429185, '会员管理', 0, 2, 'member', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'peoples', '', '2022-05-08 22:14:47', '', '2022-05-08 22:14:47', '');
INSERT INTO `sys_menu` VALUES (1523305725727182849, '微信用户', 1523305413918429185, 1, 'user', 'member/user/index', NULL, 1, 0, 'C', '0', '0', 'member:user:list', 'wechat', '', '2022-05-08 22:16:01', '', '2022-05-08 22:19:38', '');
INSERT INTO `sys_menu` VALUES (1523306205261959169, '预约管理', 1523304703357194242, 1, 'order', 'order/order/index', NULL, 1, 0, 'C', '0', '0', 'order:order:list', 'time', '', '2022-05-08 22:17:56', '', '2022-05-08 22:17:56', '');
INSERT INTO `sys_menu` VALUES (1523306536213516290, '充值管理', 1523304703357194242, 2, 'recharge', 'order/recharge/index', NULL, 1, 0, 'C', '0', '0', 'order:recharge:list', 'validCode', '', '2022-05-08 22:19:15', '', '2022-05-08 22:19:15', '');
INSERT INTO `sys_menu` VALUES (1523306902183317505, '用户编辑', 1523305725727182849, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'member:user:edit', '#', '', '2022-05-08 22:20:42', '', '2022-05-08 22:20:42', '');
INSERT INTO `sys_menu` VALUES (1523307217272016897, '预约查询', 1523306205261959169, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'order:order:query', '#', '', '2022-05-08 22:21:57', '', '2022-05-08 22:22:23', '');
INSERT INTO `sys_menu` VALUES (1523307405608849409, '预约编辑', 1523306205261959169, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'order:order:edit', '#', '', '2022-05-08 22:22:42', '', '2022-05-08 22:22:42', '');
INSERT INTO `sys_menu` VALUES (1523307664619704321, '充值查询', 1523306536213516290, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'order:recharge:query', '#', '', '2022-05-08 22:23:44', '', '2022-05-08 22:23:44', '');
INSERT INTO `sys_menu` VALUES (1523307729149071361, '充值编辑', 1523306536213516290, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'order:recharge:edit', '#', '', '2022-05-08 22:23:59', '', '2022-05-08 22:23:59', '');
INSERT INTO `sys_menu` VALUES (1523307990592622594, '用户查询', 1523305725727182849, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'member:user:query', '#', '', '2022-05-08 22:25:01', '', '2022-05-08 22:25:01', '');
INSERT INTO `sys_menu` VALUES (1523308114962124801, '自习室查询', 1523299779399487489, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:room:query', '#', '', '2022-05-08 22:25:31', '', '2022-05-08 22:25:31', '');
INSERT INTO `sys_menu` VALUES (1523308226140540930, '座位查询', 1523301438703898626, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:seat:query', '#', '', '2022-05-08 22:25:57', '', '2022-05-08 22:25:57', '');
INSERT INTO `sys_menu` VALUES (1523308363222978562, '卡券查询', 1523303590499291138, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:card:query', '#', '', '2022-05-08 22:26:30', '', '2022-05-08 22:26:30', '');
INSERT INTO `sys_menu` VALUES (1523309159708725249, '优惠管理', 1523299432647987201, 4, 'swiper', 'store/swiper/index', NULL, 1, 0, 'C', '0', '0', 'store:swiper:list', 'message', '', '2022-05-08 22:29:40', '', '2022-05-08 22:29:40', '');
INSERT INTO `sys_menu` VALUES (1523309256408403970, '优惠添加', 1523309159708725249, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:swiper:add', '#', '', '2022-05-08 22:30:03', '', '2022-05-08 22:30:03', '');
INSERT INTO `sys_menu` VALUES (1523309319423627266, '优惠编辑', 1523309159708725249, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:swiper:edit', '#', '', '2022-05-08 22:30:18', '', '2022-05-08 22:30:18', '');
INSERT INTO `sys_menu` VALUES (1523309420015620098, '优惠删除', 1523309159708725249, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:swiper:remove', '#', '', '2022-05-08 22:30:42', '', '2022-05-08 22:30:42', '');
INSERT INTO `sys_menu` VALUES (1523309490924523521, '优惠查询', 1523309159708725249, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'store:swiper:query', '#', '', '2022-05-08 22:30:59', '', '2022-05-08 22:30:59', '');
INSERT INTO `sys_menu` VALUES (1524042170729250816, '购卡管理', 1523304703357194242, 3, 'orderCard', 'order/orderCard/index', NULL, 1, 0, 'C', '0', '0', 'order:orderCard:list', 'component', 'admin', '2022-05-10 23:03:19', '', '2022-05-10 23:05:01', '购卡管理菜单');
INSERT INTO `sys_menu` VALUES (1524042170729250817, '购卡管理查询', 1524042170729250816, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'order:orderCard:query', '#', 'admin', '2022-05-10 23:03:19', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1524042170729250818, '购卡管理新增', 1524042170729250816, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'order:orderCard:add', '#', 'admin', '2022-05-10 23:03:19', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1524042170729250819, '购卡管理修改', 1524042170729250816, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'order:orderCard:edit', '#', 'admin', '2022-05-10 23:03:19', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1524042170729250820, '购卡管理删除', 1524042170729250816, 4, '#', '', NULL, 1, 0, 'F', '0', '0', 'order:orderCard:remove', '#', 'admin', '2022-05-10 23:03:19', '', NULL, '');
INSERT INTO `sys_menu` VALUES (1524042170729250821, '购卡管理导出', 1524042170729250816, 5, '#', '', NULL, 1, 0, 'F', '0', '0', 'order:orderCard:export', '#', 'admin', '2022-05-10 23:03:19', '', NULL, '');

-- ----------------------------
-- Table structure for sys_notice
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice`  (
  `notice_id` int(0) NOT NULL COMMENT '公告ID',
  `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob NULL COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`notice_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO `sys_notice` VALUES (1, '温馨提醒：2018-07-01 新版本发布啦', '2', 0xE696B0E78988E69CACE58685E5AEB9, '0', 'admin', '2022-05-05 15:47:38', '', NULL, '管理员');
INSERT INTO `sys_notice` VALUES (2, '维护通知：2018-07-01 系统凌晨维护', '1', 0xE7BBB4E68AA4E58685E5AEB9, '0', 'admin', '2022-05-05 15:47:38', '', NULL, '管理员');

-- ----------------------------
-- Table structure for sys_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log`  (
  `oper_id` bigint(0) NOT NULL COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '模块标题',
  `business_type` int(0) NULL DEFAULT 0 COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求方式',
  `operator_type` int(0) NULL DEFAULT 0 COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '返回参数',
  `status` int(0) NULL DEFAULT 0 COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime(0) NULL DEFAULT NULL COMMENT '操作时间',
  PRIMARY KEY (`oper_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_oss
-- ----------------------------
DROP TABLE IF EXISTS `sys_oss`;
CREATE TABLE `sys_oss`  (
  `oss_id` bigint(0) NOT NULL COMMENT '对象存储主键',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '文件名',
  `original_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '原名',
  `file_suffix` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '文件后缀名',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL地址',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '上传人',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新人',
  `service` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'minio' COMMENT '服务商',
  PRIMARY KEY (`oss_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'OSS对象存储表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_oss_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_oss_config`;
CREATE TABLE `sys_oss_config`  (
  `oss_config_id` bigint(0) NOT NULL COMMENT '主建',
  `config_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '配置key',
  `access_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'accessKey',
  `secret_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '秘钥',
  `bucket_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '桶名称',
  `prefix` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '前缀',
  `endpoint` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '访问站点',
  `is_https` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '是否https（Y=是,N=否）',
  `region` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '域',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '状态（0=正常,1=停用）',
  `ext1` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '扩展字段',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`oss_config_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '对象存储配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_oss_config
-- ----------------------------
INSERT INTO `sys_oss_config` VALUES (1, 'minio', 'ruoyi', 'ruoyi123', 'ruoyi', '', 'http://localhost:9000', 'N', '', '1', '', 'admin', '2022-05-05 15:47:38', '', '2022-05-09 00:20:20', NULL);
INSERT INTO `sys_oss_config` VALUES (3, 'aliyun', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 'http://oss-cn-beijing.aliyuncs.com', 'N', '', '1', '', 'admin', '2022-05-05 15:47:38', 'admin', '2022-05-05 15:47:38', NULL);
INSERT INTO `sys_oss_config` VALUES (4, 'qcloud', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi-1250000000', '', 'http://cos.ap-beijing.myqcloud.com', 'N', 'ap-beijing', '1', '', 'admin', '2022-05-05 15:47:38', 'admin', '2022-05-05 15:47:38', NULL);

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `post_id` bigint(0) NOT NULL COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int(0) NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES (1, 'ceo', '董事长', 1, '0', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_post` VALUES (2, 'se', '项目经理', 2, '0', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_post` VALUES (3, 'hr', '人力资源', 3, '0', 'admin', '2022-05-05 15:47:38', '', NULL, '');
INSERT INTO `sys_post` VALUES (4, 'user', '普通员工', 4, '0', 'admin', '2022-05-05 15:47:38', '', NULL, '');

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint(0) NOT NULL COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int(0) NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', '2022-05-05 15:47:38', '', NULL, '超级管理员');
INSERT INTO `sys_role` VALUES (2, '普通角色', 'common', 2, '2', 1, 1, '0', '0', 'admin', '2022-05-05 15:47:38', '', '2022-05-08 22:13:25', '普通角色');

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `role_id` bigint(0) NOT NULL COMMENT '角色ID',
  `dept_id` bigint(0) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和部门关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
INSERT INTO `sys_role_dept` VALUES (2, 100);
INSERT INTO `sys_role_dept` VALUES (2, 101);
INSERT INTO `sys_role_dept` VALUES (2, 105);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint(0) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(0) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (2, 1);
INSERT INTO `sys_role_menu` VALUES (2, 2);
INSERT INTO `sys_role_menu` VALUES (2, 3);
INSERT INTO `sys_role_menu` VALUES (2, 100);
INSERT INTO `sys_role_menu` VALUES (2, 101);
INSERT INTO `sys_role_menu` VALUES (2, 102);
INSERT INTO `sys_role_menu` VALUES (2, 103);
INSERT INTO `sys_role_menu` VALUES (2, 104);
INSERT INTO `sys_role_menu` VALUES (2, 105);
INSERT INTO `sys_role_menu` VALUES (2, 106);
INSERT INTO `sys_role_menu` VALUES (2, 107);
INSERT INTO `sys_role_menu` VALUES (2, 108);
INSERT INTO `sys_role_menu` VALUES (2, 109);
INSERT INTO `sys_role_menu` VALUES (2, 111);
INSERT INTO `sys_role_menu` VALUES (2, 113);
INSERT INTO `sys_role_menu` VALUES (2, 114);
INSERT INTO `sys_role_menu` VALUES (2, 115);
INSERT INTO `sys_role_menu` VALUES (2, 116);
INSERT INTO `sys_role_menu` VALUES (2, 500);
INSERT INTO `sys_role_menu` VALUES (2, 501);
INSERT INTO `sys_role_menu` VALUES (2, 1001);
INSERT INTO `sys_role_menu` VALUES (2, 1002);
INSERT INTO `sys_role_menu` VALUES (2, 1003);
INSERT INTO `sys_role_menu` VALUES (2, 1004);
INSERT INTO `sys_role_menu` VALUES (2, 1005);
INSERT INTO `sys_role_menu` VALUES (2, 1006);
INSERT INTO `sys_role_menu` VALUES (2, 1007);
INSERT INTO `sys_role_menu` VALUES (2, 1008);
INSERT INTO `sys_role_menu` VALUES (2, 1009);
INSERT INTO `sys_role_menu` VALUES (2, 1010);
INSERT INTO `sys_role_menu` VALUES (2, 1011);
INSERT INTO `sys_role_menu` VALUES (2, 1012);
INSERT INTO `sys_role_menu` VALUES (2, 1013);
INSERT INTO `sys_role_menu` VALUES (2, 1014);
INSERT INTO `sys_role_menu` VALUES (2, 1015);
INSERT INTO `sys_role_menu` VALUES (2, 1016);
INSERT INTO `sys_role_menu` VALUES (2, 1017);
INSERT INTO `sys_role_menu` VALUES (2, 1018);
INSERT INTO `sys_role_menu` VALUES (2, 1019);
INSERT INTO `sys_role_menu` VALUES (2, 1020);
INSERT INTO `sys_role_menu` VALUES (2, 1021);
INSERT INTO `sys_role_menu` VALUES (2, 1022);
INSERT INTO `sys_role_menu` VALUES (2, 1023);
INSERT INTO `sys_role_menu` VALUES (2, 1024);
INSERT INTO `sys_role_menu` VALUES (2, 1025);
INSERT INTO `sys_role_menu` VALUES (2, 1026);
INSERT INTO `sys_role_menu` VALUES (2, 1027);
INSERT INTO `sys_role_menu` VALUES (2, 1028);
INSERT INTO `sys_role_menu` VALUES (2, 1029);
INSERT INTO `sys_role_menu` VALUES (2, 1030);
INSERT INTO `sys_role_menu` VALUES (2, 1031);
INSERT INTO `sys_role_menu` VALUES (2, 1032);
INSERT INTO `sys_role_menu` VALUES (2, 1033);
INSERT INTO `sys_role_menu` VALUES (2, 1034);
INSERT INTO `sys_role_menu` VALUES (2, 1035);
INSERT INTO `sys_role_menu` VALUES (2, 1036);
INSERT INTO `sys_role_menu` VALUES (2, 1037);
INSERT INTO `sys_role_menu` VALUES (2, 1038);
INSERT INTO `sys_role_menu` VALUES (2, 1039);
INSERT INTO `sys_role_menu` VALUES (2, 1040);
INSERT INTO `sys_role_menu` VALUES (2, 1041);
INSERT INTO `sys_role_menu` VALUES (2, 1042);
INSERT INTO `sys_role_menu` VALUES (2, 1043);
INSERT INTO `sys_role_menu` VALUES (2, 1044);
INSERT INTO `sys_role_menu` VALUES (2, 1045);
INSERT INTO `sys_role_menu` VALUES (2, 1046);
INSERT INTO `sys_role_menu` VALUES (2, 1047);
INSERT INTO `sys_role_menu` VALUES (2, 1048);
INSERT INTO `sys_role_menu` VALUES (2, 1055);
INSERT INTO `sys_role_menu` VALUES (2, 1056);
INSERT INTO `sys_role_menu` VALUES (2, 1057);
INSERT INTO `sys_role_menu` VALUES (2, 1058);
INSERT INTO `sys_role_menu` VALUES (2, 1059);
INSERT INTO `sys_role_menu` VALUES (2, 1060);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `dept_id` bigint(0) NULL DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'sys_user' COMMENT '用户类型（sys_user系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime(0) NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime(0) NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime(0) NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 103, '', '超级管理员', 'sys_user', 'crazyLionLi@163.com', '', '0', 'http://ray7r16mm.hn-bkt.clouddn.com/2022/04/28/6ad72efe3a334965b68e421b6bcb9095.jpg', '$2a$10$WwmfQERwQgJSs4MNqFwfduXRbkS9GTRUQre99gOlNmFaMwXhvo0jG', '0', '0', '127.0.0.1', '2022-05-16 16:56:38', 'admin', '2022-04-25 17:48:05', '', '2022-05-16 16:56:38', '管理员');
-- ----------------------------
-- Table structure for sys_user_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_post`;
CREATE TABLE `sys_user_post`  (
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `post_id` bigint(0) NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`, `post_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与岗位关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO `sys_user_post` VALUES (1, 1);
INSERT INTO `sys_user_post` VALUES (2, 2);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint(0) NOT NULL COMMENT '用户ID',
  `role_id` bigint(0) NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户和角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);
INSERT INTO `sys_user_role` VALUES (2, 2);

SET FOREIGN_KEY_CHECKS = 1;
