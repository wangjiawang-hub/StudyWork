/*
 Navicat Premium Data Transfer

 Source Server         : dp
 Source Server Type    : MySQL
 Source Server Version : 80028
 Source Host           : localhost:3306
 Source Schema         : shopping_mall

 Target Server Type    : MySQL
 Target Server Version : 80028
 File Encoding         : 65001

 Date: 17/09/2026 13:16:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for address
-- ----------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `province` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `district` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `detail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_default` tinyint NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of address
-- ----------------------------
INSERT INTO `address` VALUES (1, 3, '游明上', '19859021888', '香港 — Hong Kong', '香港島', '阿大', '萨斯达到', 0, '2026-06-02 14:57:09', '2026-06-02 14:57:09');
INSERT INTO `address` VALUES (2, 6, '阿大撒丁岛', '19859021999', '香港 — Hong Kong', '香港島', '申奥大使', '安大队', 0, '2026-06-02 15:38:54', '2026-06-02 15:38:54');
INSERT INTO `address` VALUES (3, 7, '11', '1', '1', '', '', '1', 1, '2026-09-17 12:42:15', '2026-09-17 12:42:15');

-- ----------------------------
-- Table structure for after_sale
-- ----------------------------
DROP TABLE IF EXISTS `after_sale`;
CREATE TABLE `after_sale`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` tinyint NULL DEFAULT 0 COMMENT '0待处理 1已同意 2已拒绝',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of after_sale
-- ----------------------------

-- ----------------------------
-- Table structure for cart
-- ----------------------------
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_id` bigint NULL DEFAULT NULL COMMENT 'SKU ID',
  `sku_spec` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规格描述(如:红色-XL)',
  `quantity` int NOT NULL DEFAULT 1,
  `selected` tinyint NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_product`(`user_id`, `product_id`) USING BTREE,
  INDEX `fk_cart_product`(`product_id`) USING BTREE,
  CONSTRAINT `fk_cart_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cart
-- ----------------------------

-- ----------------------------
-- Table structure for coupon
-- ----------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` tinyint NOT NULL,
  `threshold` decimal(10, 2) NULL DEFAULT 0.00,
  `reduce_amount` decimal(10, 2) NULL DEFAULT NULL,
  `discount_rate` decimal(3, 2) NULL DEFAULT NULL,
  `max_discount` decimal(10, 2) NULL DEFAULT NULL,
  `total_quantity` int NULL DEFAULT 0,
  `used_quantity` int NULL DEFAULT 0,
  `per_user_limit` int NULL DEFAULT 1,
  `expire_start` date NOT NULL,
  `expire_end` date NOT NULL,
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of coupon
-- ----------------------------
INSERT INTO `coupon` VALUES (1, '满100减10', 0, 100.00, 10.00, NULL, NULL, 1000, 0, 1, '2026-06-01', '2026-07-01', 1, '2026-06-01 15:44:17');
INSERT INTO `coupon` VALUES (2, '满200减30', 0, 200.00, 30.00, NULL, NULL, 500, 0, 1, '2026-06-01', '2026-07-01', 1, '2026-06-01 15:44:17');
INSERT INTO `coupon` VALUES (3, '满100减10', 0, 100.00, 10.00, NULL, NULL, 1000, 0, 1, '2026-06-01', '2026-07-01', 1, '2026-06-01 15:45:11');
INSERT INTO `coupon` VALUES (4, '满200减30', 0, 200.00, 30.00, NULL, NULL, 500, 0, 1, '2026-06-01', '2026-07-01', 1, '2026-06-01 15:45:11');
INSERT INTO `coupon` VALUES (5, '满100减10', 0, 100.00, 10.00, NULL, NULL, 1000, 0, 1, '2026-06-02', '2026-07-02', 1, '2026-06-02 15:22:56');
INSERT INTO `coupon` VALUES (6, '满200减30', 0, 200.00, 30.00, NULL, NULL, 500, 0, 1, '2026-06-02', '2026-07-02', 1, '2026-06-02 15:22:56');
INSERT INTO `coupon` VALUES (7, '满100减10', 0, 100.00, 10.00, NULL, NULL, 1000, 0, 1, '2026-06-02', '2026-07-02', 1, '2026-06-02 15:38:02');
INSERT INTO `coupon` VALUES (8, '满200减30', 0, 200.00, 30.00, NULL, NULL, 500, 0, 1, '2026-06-02', '2026-07-02', 1, '2026-06-02 15:38:02');

-- ----------------------------
-- Table structure for favorite
-- ----------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_product`(`user_id`, `product_id`) USING BTREE,
  INDEX `fk_favorite_product`(`product_id`) USING BTREE,
  CONSTRAINT `fk_favorite_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of favorite
-- ----------------------------
INSERT INTO `favorite` VALUES (1, 6, 3, '2026-06-03 09:33:41');
INSERT INTO `favorite` VALUES (2, 6, 12, '2026-06-03 09:33:50');

-- ----------------------------
-- Table structure for order
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `address_id` bigint NOT NULL,
  `total_amount` decimal(10, 2) NOT NULL,
  `pay_amount` decimal(10, 2) NOT NULL,
  `coupon_id` bigint NULL DEFAULT NULL,
  `discount_amount` decimal(10, 2) NULL DEFAULT 0.00,
  `freight_amount` decimal(10, 2) NULL DEFAULT 0.00,
  `pay_status` tinyint NULL DEFAULT 0,
  `order_status` tinyint NULL DEFAULT 0,
  `logistics_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `logistics_company` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `pay_time` datetime NULL DEFAULT NULL,
  `delivery_time` datetime NULL DEFAULT NULL,
  `receipt_time` datetime NULL DEFAULT NULL,
  `cancel_time` datetime NULL DEFAULT NULL,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no`) USING BTREE,
  INDEX `fk_order_user`(`user_id`) USING BTREE,
  INDEX `fk_order_address`(`address_id`) USING BTREE,
  CONSTRAINT `fk_order_address` FOREIGN KEY (`address_id`) REFERENCES `address` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order
-- ----------------------------
INSERT INTO `order` VALUES (1, 'ORD17803834378983', 3, 1, 138.00, 138.00, NULL, 0.00, 0.00, 0, 4, NULL, NULL, NULL, NULL, NULL, '2026-06-02 15:08:37', NULL, '2026-06-02 14:57:17', '2026-06-02 15:08:37');
INSERT INTO `order` VALUES (2, 'ORD17803841351713', 3, 1, 138.00, 138.00, NULL, 0.00, 0.00, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-06-02 15:08:55', '2026-06-02 15:08:55');
INSERT INTO `order` VALUES (3, 'ORD17803845447703', 3, 1, 69.00, 69.00, NULL, 0.00, 0.00, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '2026-06-02 15:15:44', '2026-06-02 15:15:44');
INSERT INTO `order` VALUES (4, 'ORD17803859483536', 6, 2, 69.00, 69.00, NULL, 0.00, 0.00, 1, 1, NULL, NULL, '2026-06-03 09:27:37', NULL, NULL, NULL, NULL, '2026-06-02 15:39:08', '2026-06-03 09:27:37');
INSERT INTO `order` VALUES (5, 'ORD17804486622066', 6, 2, 157.90, 147.90, 1, 10.00, 0.00, 1, 1, NULL, NULL, '2026-06-03 09:27:20', NULL, NULL, NULL, NULL, '2026-06-03 09:04:22', '2026-06-03 09:27:20');
INSERT INTO `order` VALUES (6, 'ORD17804492566706', 6, 2, 69.00, 69.00, NULL, 0.00, 0.00, 1, 1, NULL, NULL, '2026-06-03 09:27:11', NULL, NULL, NULL, NULL, '2026-06-03 09:14:16', '2026-06-03 09:27:11');
INSERT INTO `order` VALUES (7, 'ORD17804499585076', 6, 2, 69.00, 69.00, NULL, 0.00, 0.00, 0, 4, NULL, NULL, NULL, NULL, NULL, '2026-06-03 09:26:47', NULL, '2026-06-03 09:25:58', '2026-06-03 09:26:47');
INSERT INTO `order` VALUES (8, 'ORD17804500687806', 6, 2, 29.90, 29.90, NULL, 0.00, 0.00, 1, 1, NULL, NULL, '2026-06-03 09:28:18', NULL, NULL, NULL, NULL, '2026-06-03 09:27:48', '2026-06-03 09:28:18');
INSERT INTO `order` VALUES (12, 'ORD17896207913447', 7, 3, 19.90, 29.90, NULL, 0.00, 10.00, 1, 2, 'SF1789621167073', '顺丰速运(模拟)', '2026-09-17 12:54:37', '2026-09-17 12:59:27', NULL, NULL, '', '2026-09-17 12:53:11', '2026-09-17 12:59:27');

-- ----------------------------
-- Table structure for order_item
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_id` bigint NULL DEFAULT NULL COMMENT 'SKU ID',
  `sku_spec` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '规格描述',
  `product_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `price` decimal(10, 2) NOT NULL,
  `quantity` int NOT NULL,
  `total_price` decimal(10, 2) NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_orderitem_order`(`order_id`) USING BTREE,
  INDEX `fk_orderitem_product`(`product_id`) USING BTREE,
  CONSTRAINT `fk_orderitem_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_orderitem_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_item
-- ----------------------------
INSERT INTO `order_item` VALUES (1, 1, 3, NULL, NULL, '保温杯 500ml', 'https://tse2-mm.cn.bing.net/th/id/OIP-C.P72nl7aaZYWRwGZtCMd-cgHaHa?w=217&h=218&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3', 69.00, 1, 69.00, '2026-06-02 14:57:17');
INSERT INTO `order_item` VALUES (2, 1, 7, NULL, NULL, 'LED护眼台灯', 'https://tse3-mm.cn.bing.net/th/id/OIP-C.dQBaGEIFDldcSR-v4vFcrAHaHa?w=201&h=201&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', 69.00, 1, 69.00, '2026-06-02 14:57:17');
INSERT INTO `order_item` VALUES (3, 2, 3, NULL, NULL, '保温杯 500ml', 'https://tse2-mm.cn.bing.net/th/id/OIP-C.P72nl7aaZYWRwGZtCMd-cgHaHa?w=217&h=218&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3', 69.00, 2, 138.00, '2026-06-02 15:08:55');
INSERT INTO `order_item` VALUES (4, 3, 7, NULL, NULL, 'LED护眼台灯', 'https://tse3-mm.cn.bing.net/th/id/OIP-C.dQBaGEIFDldcSR-v4vFcrAHaHa?w=201&h=201&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', 69.00, 1, 69.00, '2026-06-02 15:15:44');
INSERT INTO `order_item` VALUES (5, 4, 7, NULL, NULL, 'LED护眼台灯', 'https://tse3-mm.cn.bing.net/th/id/OIP-C.dQBaGEIFDldcSR-v4vFcrAHaHa?w=201&h=201&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', 69.00, 1, 69.00, '2026-06-02 15:39:08');
INSERT INTO `order_item` VALUES (6, 5, 3, NULL, NULL, '保温杯 500ml', 'https://tse2-mm.cn.bing.net/th/id/OIP-C.P72nl7aaZYWRwGZtCMd-cgHaHa?w=217&h=218&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3', 69.00, 2, 138.00, '2026-06-03 09:04:22');
INSERT INTO `order_item` VALUES (7, 5, 12, NULL, NULL, 'USB-C 数据线', 'https://tse4-mm.cn.bing.net/th/id/OIP-C.90gEJ5YuQ1O33T7dYZbItgHaEl?w=293&h=181&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', 19.90, 1, 19.90, '2026-06-03 09:04:22');
INSERT INTO `order_item` VALUES (8, 6, 3, NULL, NULL, '保温杯 500ml', 'https://tse2-mm.cn.bing.net/th/id/OIP-C.P72nl7aaZYWRwGZtCMd-cgHaHa?w=217&h=218&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3', 69.00, 1, 69.00, '2026-06-03 09:14:16');
INSERT INTO `order_item` VALUES (9, 7, 3, NULL, NULL, '保温杯 500ml', 'https://tse2-mm.cn.bing.net/th/id/OIP-C.P72nl7aaZYWRwGZtCMd-cgHaHa?w=217&h=218&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3', 69.00, 1, 69.00, '2026-06-03 09:25:58');
INSERT INTO `order_item` VALUES (10, 8, 16, NULL, NULL, '手机支架', 'https://tse4-mm.cn.bing.net/th/id/OIP-C.MzA5FdAsTbsN9CNSPY4zmAHaEK?w=333&h=187&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', 29.90, 1, 29.90, '2026-06-03 09:27:48');
INSERT INTO `order_item` VALUES (11, 12, 12, NULL, NULL, 'USB-C 数据线', 'https://tse4-mm.cn.bing.net/th/id/OIP-C.90gEJ5YuQ1O33T7dYZbItgHaEl?w=293&h=181&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', 19.90, 1, 19.90, '2026-09-17 12:53:11');

-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '商品名称',
  `category_id` int NULL DEFAULT NULL COMMENT '分类ID',
  `category_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类名称',
  `price` decimal(10, 2) NOT NULL COMMENT '售价',
  `original_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '原价',
  `stock` int NOT NULL DEFAULT 0 COMMENT '库存',
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主图URL',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '多图JSON数组',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '商品详情',
  `sales` int NULL DEFAULT 0 COMMENT '销量',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-下架 1-上架',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_sales`(`sales`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '商品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (1, '无线蓝牙耳机', 1, '数码', 199.00, 299.00, 100, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.Q9DraW6uCnGC9PZnEQkK6gHaEs?w=289&h=184&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3', NULL, '高品质降噪蓝牙耳机，续航持久', 520, 1, '2026-06-02 16:23:17', '2026-06-02 16:30:36');
INSERT INTO `product` VALUES (2, '运动跑步鞋', 2, '运动', 359.00, 499.00, 80, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.ryIyRMPyPXcBtw_3TXImAQHaHa?w=179&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3', NULL, '轻便透气运动鞋，缓震防滑', 310, 1, '2026-06-02 16:23:17', '2026-06-02 16:30:36');
INSERT INTO `product` VALUES (3, '保温杯 500ml', 3, '家居', 69.00, 99.00, 200, 'https://tse2-mm.cn.bing.net/th/id/OIP-C.P72nl7aaZYWRwGZtCMd-cgHaHa?w=217&h=218&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3', NULL, '304不锈钢保温杯，长效保温', 890, 1, '2026-06-02 16:23:17', '2026-06-02 16:30:36');
INSERT INTO `product` VALUES (4, '机械键盘', 1, '数码', 299.00, 399.00, 50, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.yCEKhtTHMZSITV1pLUCWRAHaG5?w=181&h=180&c=7&r=0&o=7&dpr=1.5&pid=1.7&rm=3', NULL, '青轴机械键盘，炫酷背光', 180, 1, '2026-06-02 16:23:17', '2026-06-02 16:30:36');
INSERT INTO `product` VALUES (5, '无线鼠标', 1, '数码', 89.00, 129.00, 150, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.60HwgtlTemmzYV8hmbtVEQHaHa?w=193&h=194&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '静音无线鼠标，人体工学设计', 230, 1, '2026-06-02 16:23:17', '2026-06-02 16:30:36');
INSERT INTO `product` VALUES (6, '瑜伽垫', 2, '运动', 59.00, 99.00, 120, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.e75IHOnC3QRi3VHkBPGllQHaI6?w=223&h=192&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '加厚防滑瑜伽垫，环保材质', 180, 1, '2026-06-02 16:23:17', '2026-06-02 16:30:36');
INSERT INTO `product` VALUES (7, 'LED护眼台灯', 3, '家居', 129.00, 199.00, 60, 'https://tse3-mm.cn.bing.net/th/id/OIP-C.dQBaGEIFDldcSR-v4vFcrAHaHa?w=201&h=201&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '三档调光，无蓝光危害', 95, 1, '2026-06-02 16:23:17', '2026-06-02 16:30:36');
INSERT INTO `product` VALUES (8, '充电宝 20000mAh', 1, '数码', 99.00, 149.00, 200, 'https://tse1-mm.cn.bing.net/th/id/OIP-C.qunZs2BCS_Gj6-DhhxuuWAHaHa?w=192&h=192&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '大容量快充，轻薄便携', 340, 1, '2026-06-02 16:23:17', '2026-06-02 16:30:36');
INSERT INTO `product` VALUES (9, '智能手环', 1, '数码', 159.00, 229.00, 120, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.Z2EASkLZ1bUmPuqqlU496AHaGd?w=228&h=200&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '心率监测，睡眠追踪', 210, 1, '2026-06-02 16:23:17', '2026-06-02 16:31:34');
INSERT INTO `product` VALUES (10, '羽毛球拍', 2, '运动', 129.00, 199.00, 80, 'https://tse3-mm.cn.bing.net/th/id/OIP-C.CaDy2PpHlGWDnGGVGSW9XgHaIj?w=138&h=196&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '碳素材质，进攻型', 150, 1, '2026-06-02 16:23:17', '2026-06-02 16:32:38');
INSERT INTO `product` VALUES (11, '电热水壶', 3, '家居', 79.00, 129.00, 100, 'https://tse2-mm.cn.bing.net/th/id/OIP-C.lvAHYa3zzHI_sCbjGGeadQHaHa?w=212&h=212&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '304不锈钢，自动断电', 430, 1, '2026-06-02 16:23:17', '2026-06-02 16:33:05');
INSERT INTO `product` VALUES (12, 'USB-C 数据线', 1, '数码', 19.90, 39.90, 500, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.90gEJ5YuQ1O33T7dYZbItgHaEl?w=293&h=181&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '快充数据线，编织材质', 1200, 1, '2026-06-02 16:23:17', '2026-06-02 16:30:36');
INSERT INTO `product` VALUES (13, '平板保护套', 1, '数码', 49.00, 89.00, 200, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.cb3K7-hiyfy-DRaRPkV0YQHaHa?w=205&h=204&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '智能唤醒，三折支架', 320, 1, '2026-06-02 16:23:17', '2026-06-02 16:33:31');
INSERT INTO `product` VALUES (14, '跳绳', 2, '运动', 29.00, 59.00, 300, 'https://tse3-mm.cn.bing.net/th/id/OIP-C.fio6F5IhGHC7cMgcCaQ1MwHaE7?w=232&h=180&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '轴承跳绳，可调节长度', 560, 1, '2026-06-02 16:23:17', '2026-06-02 16:34:21');
INSERT INTO `product` VALUES (15, '收纳箱', 3, '家居', 39.90, 69.90, 150, 'https://tse3-mm.cn.bing.net/th/id/OIP-C.Wk3uBuKgQoBitee2WTHO_AHaHa?w=187&h=187&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '大容量，带滑轮', 280, 1, '2026-06-02 16:23:17', '2026-06-02 16:34:42');
INSERT INTO `product` VALUES (16, '手机支架', 1, '数码', 29.90, 49.90, 400, 'https://tse4-mm.cn.bing.net/th/id/OIP-C.MzA5FdAsTbsN9CNSPY4zmAHaEK?w=333&h=187&c=7&r=0&o=7&cb=thfc1falcon&dpr=1.5&pid=1.7&rm=3', NULL, '桌面可折叠，稳固防滑', 890, 1, '2026-06-02 16:23:17', '2026-06-02 16:35:34');

-- ----------------------------
-- Table structure for review
-- ----------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `order_id` bigint NULL DEFAULT NULL,
  `order_item_id` bigint NULL DEFAULT NULL,
  `rating` int NOT NULL,
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `reply` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `is_anonymous` tinyint NULL DEFAULT 0,
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_review_user`(`user_id`) USING BTREE,
  INDEX `fk_review_product`(`product_id`) USING BTREE,
  INDEX `idx_review_order`(`order_id`) USING BTREE,
  CONSTRAINT `fk_review_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of review
-- ----------------------------

-- ----------------------------
-- Table structure for sku
-- ----------------------------
DROP TABLE IF EXISTS `sku`;
CREATE TABLE `sku`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '所属商品ID',
  `sku_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'SKU编码',
  `price` decimal(10, 2) NOT NULL COMMENT 'SKU售价',
  `stock` int NOT NULL DEFAULT 0 COMMENT 'SKU库存',
  `status` tinyint NULL DEFAULT 1 COMMENT '0禁用 1启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sku_product`(`product_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'SKU表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sku
-- ----------------------------
INSERT INTO `sku` VALUES (1, 1, 'EAR-BLK-STD', 199.00, 50, 1, '2026-06-02 16:30:00');
INSERT INTO `sku` VALUES (2, 1, 'EAR-BLK-PRO', 259.00, 30, 1, '2026-06-02 16:30:00');
INSERT INTO `sku` VALUES (3, 1, 'EAR-WHT-STD', 199.00, 40, 1, '2026-06-02 16:30:00');
INSERT INTO `sku` VALUES (4, 1, 'EAR-WHT-PRO', 259.00, 20, 1, '2026-06-02 16:30:00');

-- ----------------------------
-- Table structure for sku_spec
-- ----------------------------
DROP TABLE IF EXISTS `sku_spec`;
CREATE TABLE `sku_spec`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `spec_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规格名(如:颜色)',
  `spec_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规格值(如:红色)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_spec_sku`(`sku_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'SKU规格中间表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sku_spec
-- ----------------------------
INSERT INTO `sku_spec` VALUES (1, 1, '颜色', '黑色');
INSERT INTO `sku_spec` VALUES (2, 1, '版本', '标准版');
INSERT INTO `sku_spec` VALUES (3, 2, '颜色', '黑色');
INSERT INTO `sku_spec` VALUES (4, 2, '版本', 'Pro版');
INSERT INTO `sku_spec` VALUES (5, 3, '颜色', '白色');
INSERT INTO `sku_spec` VALUES (6, 3, '版本', '标准版');
INSERT INTO `sku_spec` VALUES (7, 4, '颜色', '白色');
INSERT INTO `sku_spec` VALUES (8, 4, '版本', 'Pro版');

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_key`(`config_key`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, 'site_name', '购物商城', '站点名称');
INSERT INTO `sys_config` VALUES (2, 'free_shipping_amount', '99', '包邮门槛（元）');
INSERT INTO `sys_config` VALUES (3, 'shipping_fee', '10', '未达包邮门槛时的运费（元）');
INSERT INTO `sys_config` VALUES (4, 'mall_categories', '[{\"categoryId\":1,\"categoryName\":\"数码\"},{\"categoryId\":2,\"categoryName\":\"运动\"},{\"categoryId\":3,\"categoryName\":\"家居\"}]', '商品分类配置(JSON)');

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NULL DEFAULT NULL,
  `operation` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_log
-- ----------------------------
INSERT INTO `sys_log` VALUES (1, 1, '更新用户', 'PUT', 'id=7', NULL, '2026-09-17 12:58:53');
INSERT INTO `sys_log` VALUES (2, 1, '模拟发货', 'POST', 'orderId=12,no=SF1789621167073', NULL, '2026-09-17 12:59:27');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密密码',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `role` tinyint NULL DEFAULT 0 COMMENT '角色：0-普通用户 1-管理员',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone`) USING BTREE,
  UNIQUE INDEX `uk_email`(`email`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '1aa6c77c191bf0273718b6dbd9b60a84', '13800000000', NULL, NULL, 1, 1, '2026-06-01 15:44:17', '2026-06-01 15:44:17');
INSERT INTO `user` VALUES (3, 'u19859021880', 'a4ec9621016562b157ed071d3c8a3f5b', '19859021880', NULL, NULL, 0, 1, '2026-06-02 09:41:39', '2026-06-02 09:41:39');
INSERT INTO `user` VALUES (6, 'you', 'b6340a43846d9e3731c87e2a699ef64b', NULL, NULL, NULL, 0, 1, '2026-06-02 15:36:23', '2026-06-02 15:36:23');
INSERT INTO `user` VALUES (7, 'wjw', 'f7708f313be6fb30573a54774571ca4f', NULL, NULL, NULL, 1, 1, '2026-09-17 10:59:10', '2026-09-17 12:58:53');

-- ----------------------------
-- Table structure for user_coupon
-- ----------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `coupon_id` bigint NOT NULL,
  `status` tinyint NULL DEFAULT 0,
  `use_time` datetime NULL DEFAULT NULL,
  `order_id` bigint NULL DEFAULT NULL,
  `receive_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_usercoupon_user`(`user_id`) USING BTREE,
  INDEX `fk_usercoupon_coupon`(`coupon_id`) USING BTREE,
  CONSTRAINT `fk_usercoupon_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_usercoupon_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_coupon
-- ----------------------------
INSERT INTO `user_coupon` VALUES (1, 6, 7, 0, NULL, NULL, '2026-06-03 09:03:56');
INSERT INTO `user_coupon` VALUES (2, 6, 8, 0, NULL, NULL, '2026-06-03 09:03:58');
INSERT INTO `user_coupon` VALUES (3, 6, 5, 0, NULL, NULL, '2026-06-03 09:03:58');
INSERT INTO `user_coupon` VALUES (4, 6, 6, 0, NULL, NULL, '2026-06-03 09:03:59');
INSERT INTO `user_coupon` VALUES (5, 6, 3, 0, NULL, NULL, '2026-06-03 09:04:00');
INSERT INTO `user_coupon` VALUES (6, 6, 4, 0, NULL, NULL, '2026-06-03 09:04:01');
INSERT INTO `user_coupon` VALUES (7, 6, 1, 1, '2026-06-03 09:04:22', 5, '2026-06-03 09:04:01');
INSERT INTO `user_coupon` VALUES (8, 6, 2, 0, NULL, NULL, '2026-06-03 09:04:02');

SET FOREIGN_KEY_CHECKS = 1;

-- ============ 人工客服聊天表 ============
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `cs_id` bigint NULL COMMENT '客服ID（管理员ID），未接入为 NULL',
  `status` tinyint DEFAULT 0 COMMENT '0=待接入 1=进行中 2=已关闭',
  `unread_count` int DEFAULT 0 COMMENT '客服未读消息数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_cs` (`cs_id`)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `sender_type` tinyint NOT NULL COMMENT '0=user 1=cs',
  `content` text NOT NULL COMMENT '消息内容',
  `is_read` tinyint DEFAULT 0 COMMENT '0=未读 1=已读',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============ AI 智能客服相关表 ============
-- AI 会话：id 由前端生成 UUID；匿名会话 user_id 为空，登录用户绑定账号
DROP TABLE IF EXISTS `ai_session`;
CREATE TABLE `ai_session` (
  `id` varchar(32) NOT NULL COMMENT '会话ID（UUID）',
  `user_id` bigint NULL DEFAULT NULL COMMENT '登录用户ID，匿名为空',
  `title` varchar(100) NOT NULL DEFAULT '新会话' COMMENT '会话标题（取首条提问）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_session_user` (`user_id`)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI 消息：role=user/assistant；msg_type=text/image，图片消息 content 存 data URL(base64)
DROP TABLE IF EXISTS `ai_message`;
CREATE TABLE `ai_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(32) NOT NULL COMMENT '会话ID',
  `role` varchar(16) NOT NULL COMMENT 'user / assistant',
  `msg_type` varchar(10) NOT NULL DEFAULT 'text' COMMENT 'text / image',
  `content` mediumtext NOT NULL COMMENT '文本内容或图片 data URL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_message_session` (`session_id`)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- AI 知识库：管理员录入原文，服务启动/变更时切块向量化到内存索引
DROP TABLE IF EXISTS `ai_knowledge`;
CREATE TABLE `ai_knowledge` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(200) NOT NULL COMMENT '知识标题',
  `content` text NOT NULL COMMENT '知识正文',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
