# Nacos 配置清单

命名空间 ID：`5b364fd6-338f-4e19-9a45-738cc3938d1c`  
Group：一律 `DEFAULT_GROUP`  
Profile：`dev`（与各服务 bootstrap.yml 中 `spring.profiles.active` 一致）

## 加载规则

每个服务启动时会自动拉取：

1. **application-dev.yml**（共享，bootstrap 中 shared-configs 引用）
2. **{服务名}-dev.yml**（如 auth-server-dev.yml，规则：`spring.application.name` + `-` + `profile` + `.yml`）

Nacos 配置 **覆盖** 本地 `application.yml` 中同名项。  
文件不存在或为空 → 仅 WARN，仍使用本地 `application.yml`。

## 配置文件一览

| Data ID | 是否必须 | 建议内容 |
|---------|----------|----------|
| application-dev.yml | 推荐 | MySQL 公共连接（所有有库的服务共用） |
| gateway-dev.yml | **必须** | 网关路由、白名单、Redis |
| auth-server-dev.yml | 可选 | Redis、captcha；或留空 |
| goods-server-dev.yml | 可选 | 留空或 mybatis |
| carts-server-dev.yml | 可选 | 留空 |
| order-server-dev.yml | 可选 | RabbitMQ、shopping.mq |
| pay-server-dev.yml | **建议** | 支付宝沙箱密钥、notifyUrl |
| user-service-dev.yml | **需新增** | 留空即可 |
| coupon-service-dev.yml | **需新增** | 留空即可 |
| admin-service-dev.yml | **需新增** | 留空即可 |
| ai-service-dev.yml | **需新增** | 留空即可 |

## 两种用法（二选一）

### 方案 A：轻量（适合本地开发）

- Nacos 只维护 **gateway-dev.yml**（必改）
- 其余 `*-dev.yml` **不建或建空文件**，数据库/Redis 用各服务本地 `application.yml`
- 消除 WARN：每个服务建一个空 `{}` 或 `# empty` 的 dev 文件

### 方案 B：集中管理（适合演示/部署）

- **application-dev.yml** 写 MySQL
- **pay-server-dev.yml** 写支付宝
- **gateway-dev.yml** 写网关
- 各业务 dev.yml 只写 mybatis、redis、mq 等差异

## 注意事项

1. **不要**在 Nacos 里写 `server.port`、`spring.application.name`（已在 bootstrap 固定）
2. **pay-server-dev.yml** 的 `notifyUrl` 应指向网关：`http://127.0.0.1:8080/alipay/notify`
3. 修改 Nacos 后需 **重启对应服务**（或开启 refresh 的配置项）
4. 密码不要提交 Git；Nacos 控制台填写真实值

## 服务名与 Data ID 对应

| 服务 | spring.application.name | Nacos Data ID |
|------|-------------------------|---------------|
| 认证 | auth-server | auth-server-dev.yml |
| 商品 | goods-server | goods-server-dev.yml |
| 购物车 | carts-server | carts-server-dev.yml |
| 订单 | order-server | order-server-dev.yml |
| 支付 | pay-server | pay-server-dev.yml |
| 用户 | user-service | user-service-dev.yml |
| 优惠券 | coupon-service | coupon-service-dev.yml |
| 后台 | admin-service | admin-service-dev.yml |
| AI | ai-service | ai-service-dev.yml |
| 网关 | gateway | gateway-dev.yml |
