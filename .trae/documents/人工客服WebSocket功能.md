# 人工客服 WebSocket 功能实现计划

## Context
项目当前只有 AI 客服（`/ai/chat` REST 接口），无人工客服。本次新增基于 WebSocket 的实时人工客服：用户端发消息实时推送到在线客服，客服回复实时推回用户；客服不在线时留言入库，客服上线后接入会话可见历史。客服由 role=1 的管理员承担，部署在 ai-service 模块，复用其"客服服务"定位。

## 关键约束
- Spring Cloud Gateway（WebFlux）原生支持 WebSocket 路由，需用 `lb:ws://` 协议头
- Gateway 的 AuthFilter 会拦 WebSocket Upgrade，握手鉴权路径 `/ws/**` 加入白名单，握手鉴权交给 ai-service 的 HandshakeInterceptor 用 `?token=` query 完成
- ai-service 当前无 DB（注释"无数据库"），需启用共享 datasource 并加 mybatis + mysql 依赖
- JwtUtil（common 模块）用 auth0-jwt，`parseToken(token)` 返回 claims Map，含 `id`/`role`
- 浏览器原生 WebSocket 不能加 header，token 走 URL query

## 1. 数据库

追加到 `shoppingBackStage/sql/shopping_mall.sql` 末尾：

```sql
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `cs_id` bigint NULL,
  `status` tinyint DEFAULT 0 COMMENT '0=待接入 1=进行中 2=已关闭',
  `unread_count` int DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`), KEY `idx_cs` (`cs_id`)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  `sender_type` tinyint NOT NULL COMMENT '0=user 1=cs',
  `content` text NOT NULL,
  `is_read` tinyint DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 2. 后端 ai-service 改造

### 2.1 pom.xml 新增依赖
`shoppingBackStage/ai-service/pom.xml` 在 `</dependencies>` 前加：
- `spring-boot-starter-websocket`
- `mybatis-spring-boot-starter`
- `mysql-connector-j`（runtime）

### 2.2 启动类
`aiservice/AiServiceApplication.java` 加 `@MapperScan("aiservice.mapper")`

### 2.3 Nacos 配置
`shoppingBackStage/nacos/ai-service-dev.yml` 删除"# ai 服务无数据库"注释，使其继承共享 `application-dev.yml` 的 datasource（无需显式写）

### 2.4 实体类（放 common 模块，仿现有 Lombok 风格）
- `common/src/main/java/model/ChatSession.java`：`id, userId, csId, status, unreadCount, createTime, updateTime`
- `common/src/main/java/model/ChatMessage.java`：`id, sessionId, senderId, senderType, content, isRead, createTime`

### 2.5 WebSocket 配置
`aiservice/config/WebSocketConfig.java`：`@Configuration implements WebSocketConfigurer`，注册 `ChatWebSocketHandler` 到 `/ws/chat`，`setAllowedOriginPatterns("*")`

### 2.6 握手鉴权
`aiservice/config/ChatHandshakeInterceptor.java`（`implements HandshakeInterceptor`）：
- `beforeHandshake` 从 `request.getURI().getQuery()` 解析 `token`，`JwtUtil.parseToken` 取 `id`/`role`，put 到 `attributes`
- 解析失败返回 401 拒绝握手

### 2.7 消息处理器
`aiservice/websocket/ChatWebSocketHandler.java`（`extends TextWebSocketHandler`）：
- 静态 `CopyOnWriteArrayList<WebSocketSession> onlineCsSessions`
- 静态 `ConcurrentHashMap<Long, WebSocketSession> userSessions`
- `afterConnectionEstablished`：按 attributes.role 区分 user/cs 注册
- `handleTextMessage`：JSON 解析 `{type, sessionId, content}`
  - `CHAT` from user：入库；若 `onlineCsSessions` 非空 → 推给会话绑定的客服；否则 `unread_count+1`
  - `CHAT` from cs：入库、推给 `userSessions.get(userId)`、`unread_count=0`
  - `JOIN` from cs：`UPDATE chat_session SET cs_id=? WHERE id=? AND cs_id IS NULL`（防并发接单），绑定 session 与 userId 双向
  - `CLOSE`：`update status=2`，移除 session
- `afterConnectionClosed`：从集合移除

### 2.8 Mapper（注解式，仿 user-service 风格，无 XML）
`aiservice/mapper/ChatMapper.java`：
- `findOrCreatePendingSession(userId)`
- `listPendingSessions()` / `listCsActiveSessions(csId)`
- `listMessages(sessionId)`
- `insertMessage(ChatMessage)`
- `bindCs(sessionId, csId)`（带 `AND cs_id IS NULL` 防并发）
- `closeSession(sessionId)`
- `markRead(sessionId, senderType)`

### 2.9 REST 控制器
`aiservice/controller/ChatController.java`（`@RequestMapping("/ai/chat")`）：
- `GET /sessions`：role==1 查待接入 + 已接入
- `GET /messages?sessionId=`：查历史
- `GET /my-session`：用户自动建/取待接入会话
- `POST /close`
- uid/role 从 `@RequestHeader("uid")` / `@RequestHeader("role")` 取（Gateway 注入）

## 3. Gateway 配置

`shoppingBackStage/nacos/gateway-dev.yml`：
- 加路由 `/ws/**` → `lb:ws://ai-service`
- 白名单加 `/ws/**`

## 4. 前端用户端（shoppingForeStage）

- `vite.config.js` proxy 加 `'/ws': { target: 'http://localhost:8080', ws: true, changeOrigin: true }`
- `src/router/index.js` 加 `{ path:'/chat', component:()=>import('../views/Chat.vue'), meta:{auth:true} }`
- `src/components/AppHeader.vue` navItems 末尾加 `{ path:'/chat', label:'客服', icon:'Service' }`
- `src/views/Chat.vue`：
  - onMounted 拉 `GET /ai/chat/my-session` 历史消息
  - 建 `new WebSocket(\`ws://${location.host}/ws/chat?token=${userStore.token}\`)`
  - onmessage 追加渲染 + nextTick 滚底
  - onUnmounted `ws.close()`

## 5. 前端客服端

- `src/router/index.js` admin children 加 `{ path:'chat', component:()=>import('../views/admin/Chat.vue'), meta:{admin:true} }`
- `src/views/admin/Layout.vue` sidebar nav 加 `<router-link to="/admin/chat"><el-icon><Service/></el-icon> 客服会话</router-link>`
- `src/views/admin/Chat.vue`：左侧会话列表（待接入 + 已接入），右侧消息区 + 输入框，WS 用管理员 token

## 6. 验证

1. 用户端登录 → 顶部"客服" → 发消息 → 应出现"客服未在线，已留言"
2. 管理员登录后台 → 客服会话 → 看到待接入会话 → 点击接入 → 实时对话
3. 关闭浏览器重开 → 历史消息可见
4. 关闭会话 → 状态变"已关闭"
5. 两个管理员同时点接入同一会话 → 只有一个成功（并发防护）

## 坑点
1. WebSocket 握手无 Authorization header，必须走 `?token=` query；`/ws/**` 加入白名单避免 AuthFilter 拦截 Upgrade
2. Gateway 路由用 `lb:ws://` 而非 `lb://`，否则丢 Upgrade 头
3. vite proxy 必须 `ws: true`
4. ai-service 需确认内嵌容器是 Tomcat（默认），web + websocket 共存无冲突
5. `my-session` 接口要先查后建，避免用户多次进入产生多个待接入会话
6. JOIN 必须用 `WHERE cs_id IS NULL` 防并发接单
