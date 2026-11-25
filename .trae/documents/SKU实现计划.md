# SKU 全链路实现计划

## Context

当前项目的商品只有单一的 `product` 表，无规格维度概念（所有商品只有一个价格/库存）。用户希望引入标准电商 SKU 模型：通过 `sku_spec` 中间表实现**弹性多维度规格**（每个商品可自定义"颜色"、"尺码"、"存储"等任意数量的规格维度），并覆盖**后台管理录入 → 前端详情页选择 → 购物车记录 → 订单保存**全链路。

## 数据库设计（新增 2 张表，不改 product 表）

### 表1: `sku` — SKU 具体规格项（每行 = 一个可购买单元）

```sql
CREATE TABLE `sku` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '所属商品ID',
  `sku_code` varchar(100) DEFAULT NULL COMMENT 'SKU编码',
  `price` decimal(10,2) NOT NULL COMMENT 'SKU售价',
  `stock` int NOT NULL DEFAULT 0 COMMENT 'SKU库存',
  `status` tinyint DEFAULT 1 COMMENT '0禁用 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU表';
```

### 表2: `sku_spec` — 中间表（SKU 与规格值的多对多关联）

```sql
CREATE TABLE `sku_spec` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku_id` bigint NOT NULL COMMENT 'SKU ID',
  `spec_name` varchar(50) NOT NULL COMMENT '规格名(如:颜色)',
  `spec_value` varchar(100) NOT NULL COMMENT '规格值(如:红色)',
  PRIMARY KEY (`id`),
  KEY `idx_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU规格中间表';
```

**设计说明**：`sku_spec` 不建独立的规格名/规格值表，而是直接在中间表中存储 `spec_name` + `spec_value` 字符串，这是淘宝/京东早期采用的简化方案，适合答辩演示——避免了过度规范化带来的多表 JOIN 复杂度，同时保留了弹性多维度的能力。

## 需修改的文件清单

### 后端 (shoppingBackStage)

#### 1. common 模块 — 新增实体类
- **新增** `common/src/main/java/model/Sku.java` — SKU 实体（id, productId, skuCode, price, stock, status）
- **新增** `common/src/main/java/model/SkuSpec.java` — 中间表实体（id, skuId, specName, specValue）
- **新增** `common/src/main/java/model/SkuVO.java` — 详情页返回的聚合视图（SKU信息 + List<SkuSpec> specs）
- **修改** `common/src/main/java/model/Cart.java` — 新增字段 `skuId`, `skuSpec`（字符串描述，如"红色-XL"）
- **修改** `common/src/main/java/model/OrderItem.java` — 新增字段 `skuId`, `skuSpec`

#### 2. goods 模块 — SKU 查询接口
- **新增** `goods/src/main/java/goods/mapper/SkuMapper.java` — SKU 查询 Mapper
  - `findByProductId(Long productId)` → 查询商品所有 SKU + 关联的规格值
- **修改** `goods/src/main/java/goods/controller/ProductController.java` — 新增 `GET /goods/skus?productId=xx` 接口
- **修改** `goods/src/main/java/goods/service/ProductService.java` — 新增 `getSkus(Long productId)` 方法
- **修改** `goods/src/main/java/goods/service/impl/ProductServiceImpl.java` — 实现 `getSkus`

#### 3. admin-service 模块 — SKU 管理接口
- **修改** `admin-service/src/main/java/adminservice/mapper/AdminMapper.java` — 新增 SKU 批量插入/删除/查询方法
- **修改** `admin-service/src/main/java/adminservice/controller/AdminController.java`
  - `POST /admin/skus` — 批量保存SKU（含规格项）
  - `GET /admin/skus?productId=xx` — 查询商品的SKU列表
  - `DELETE /admin/skus/{id}` — 删除单个SKU

#### 4. carts 模块 — 购物车支持 SKU
- **修改** `carts/src/main/java/carts/mapper/CartMapper.java`
  - `findByUserAndProduct` 改为 `findByUserAndProductSku(userId, productId, skuId)` — 同一商品不同SKU算不同购物车项
  - `insert` 新增 `sku_id`, `sku_spec` 字段
  - `findByUserId` JOIN `sku` 表获取 SKU 价格和库存
- **修改** `carts/src/main/java/carts/controller/CartsController.java` — `add` 接口新增 `skuId` 参数
- **修改** `carts/src/main/java/carts/service/impl/CartsServiceImpl.java` — 适配 skuId 逻辑

#### 5. order 模块 — 订单保存 SKU 信息
- **修改** `order/src/main/java/order/mapper/OrderMapper.java` — `insertOrderItem` 新增 `sku_id`, `sku_spec` 字段
- **修改** `order/src/main/java/order/service/impl/OrderServiceImpl.java` — 创建订单时从 Cart 复制 skuId/skuSpec 到 OrderItem

#### 6. SQL
- **修改** `shoppingBackStage/sql/shopping_mall.sql` — 追加 `sku` 和 `sku_spec` 建表语句
- **修改** `cart` 表 — 新增 `sku_id bigint DEFAULT NULL`, `sku_spec varchar(200) DEFAULT NULL`
- **修改** `order_item` 表 — 新增 `sku_id bigint DEFAULT NULL`, `sku_spec varchar(200) DEFAULT NULL`

### 前端 (shoppingForeStage)

#### 7. 后台商品管理 — SKU 录入
- **修改** `shoppingForeStage/src/views/admin/Products.vue` — 编辑对话框中新增 SKU 规格配置区域
  - 规格名/规格值的动态表单（允许添加多个规格维度，每个维度多个值）
  - 自动生成 SKU 组合表格（每行一个组合，可设置价格/库存）
  - 保存时将 SKU 列表一起提交到 `POST /admin/skus`

#### 8. 前端商品详情页 — SKU 选择
- **修改** `shoppingForeStage/src/views/ProductDetail.vue`
  - 加载商品后调用 `GET /goods/skus?productId=xx` 获取 SKU 数据
  - 渲染规格选择按钮组（如"颜色: 红 蓝" "尺码: S M L"）
  - 选中规格组合后，显示对应的 SKU 价格和库存
  - "加入购物车"时携带 `skuId` 参数

#### 9. 购物车 — 展示 SKU 信息
- **修改** `shoppingForeStage/src/views/Cart.vue` — 在商品名后展示 `skuSpec`（如"红色 / XL"）

#### 10. 结算页 — 展示 SKU 信息
- **修改** `shoppingForeStage/src/views/Checkout.vue` — 结算商品列表中展示 `skuSpec`

## 实现步骤（建议顺序）

1. **数据库**：在 SQL 文件中追加建表语句和 ALTER 语句，手动在 MySQL 中执行
2. **common 模块**：新增 Sku/SkuSpec 实体类，修改 Cart/OrderItem
3. **goods 模块**：新增 SkuMapper + 前端查询接口
4. **admin-service 模块**：SKU 管理接口（批量保存/查询/删除）
5. **carts 模块**：购物车适配 skuId
6. **order 模块**：订单保存 skuId/skuSpec
7. **前端后台**：Products.vue SKU 录入表单
8. **前端前台**：ProductDetail.vue SKU 选择 + Cart.vue/Checkout.vue 展示

## 验证方式

1. 在后台管理中创建一个带多规格的商品（如手机：颜色×存储），保存 SKU
2. 在前台商品详情页看到规格按钮，选择后价格/库存随之变化
3. 加入购物车，购物车列表展示规格信息（如"黑色 / 256G"）
4. 结算下单，订单详情中 OrderItem 包含 skuSpec
5. 验证无 SKU 的商品（旧数据）仍可正常加购下单（skuId=null 兼容）
