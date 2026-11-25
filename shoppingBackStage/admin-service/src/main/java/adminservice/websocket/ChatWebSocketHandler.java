package adminservice.websocket;

import adminservice.mapper.ChatMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.ChatMessage;
import model.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 人工客服 WebSocket 消息处理器：用户与客服之间的实时消息路由。
 *
 * 客户端 → 服务端的消息协议：
 *  客服 JOIN  {"type":"JOIN",  "sessionId":123}            接入待接入会话
 *  双方 CHAT  {"type":"CHAT",  "sessionId":123,"content":""} 发送消息
 *  任意 CLOSE {"type":"CLOSE", "sessionId":123}            结束会话
 *
 * 服务端 → 客户端：
 *  {"type":"JOIN|CHAT|CLOSE|ERROR", ...}  字段与上对应，CHAT 另带 senderId/senderType/createTime
 *
 * 客服不在线时用户照发，消息入库且会话 unread_count +1（留言模式）。
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    @Autowired
    private ChatMapper chatMapper;
    @Autowired
    private ObjectMapper objectMapper;

    /** 在线客服连接（role=1） */
    private final CopyOnWriteArrayList<WebSocketSession> onlineCs = new CopyOnWriteArrayList<>();
    /** 在线用户：uid -> 连接 */
    private final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    /** 客服连接 -> 当前接入的用户 userId（一个客服连接同时处理一个会话） */
    private final ConcurrentHashMap<String, Long> csBoundUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, Object> attrs = session.getAttributes();
        Long uid = (Long) attrs.get("uid");
        if (uid == null) {
            log.warn("WebSocket 连接无 uid，关闭");
            try { session.close(); } catch (Exception e) {}
            return;
        }
        int role = (int) attrs.getOrDefault("role", 0);
        if (role == 1) {
            onlineCs.add(session);
            log.info("客服上线 uid={}", uid);
        } else {
            userSessions.put(uid, session);
            log.info("用户连接 uid={}", uid);
        }
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 单条消息解析/处理失败只记录日志，不拖垮整条连接
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String type = node.path("type").asText();
            long sessionId = node.path("sessionId").asLong();
            switch (type) {
                case "JOIN":  handleJoin(session, sessionId); break;
                case "CHAT":  handleChat(session, sessionId, node.path("content").asText()); break;
                case "CLOSE": handleClose(sessionId); break;
                default:      log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", message.getPayload(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        onlineCs.remove(session);
        csBoundUser.remove(session.getId());
        if (!isCs(session)) {
            userSessions.remove(uid(session));
        }
        log.info("连接关闭 uid={} status={}", uid(session), status);
    }

    // ==================== 消息处理 ====================

    /** 客服接入会话：SQL 带 cs_id IS NULL 条件，多个客服抢接只有一个成功 */
    private void handleJoin(WebSocketSession csSession, long sessionId) throws Exception {
        long csId = uid(csSession);
        int rows = chatMapper.bindCs(sessionId, csId);
        if (rows == 0) {
            send(csSession, Map.of("type", "ERROR", "message", "该会话已被其他客服接入"));
            return;
        }
        ChatSession target = chatMapper.findById(sessionId);
        if (target == null) {
            send(csSession, Map.of("type", "ERROR", "message", "会话不存在"));
            return;
        }
        // 记录客服连接与用户的绑定，清零用户留言未读数，并通知用户客服已接入
        csBoundUser.put(csSession.getId(), target.getUserId());
        chatMapper.resetUnread(sessionId);
        sendToUser(target.getUserId(), Map.of("type", "JOIN", "sessionId", sessionId));
    }

    /** 双方聊天消息：先入库，再实时投递给对方 */
    private void handleChat(WebSocketSession session, long sessionId, String content) throws Exception {
        long uid = uid(session);
        boolean fromCs = isCs(session);

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSenderId(uid);
        msg.setSenderType(fromCs ? 1 : 0);
        msg.setContent(content);
        chatMapper.insertMessage(msg);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "CHAT");
        payload.put("sessionId", sessionId);
        payload.put("senderId", uid);
        payload.put("senderType", msg.getSenderType());
        payload.put("content", content);
        payload.put("createTime", System.currentTimeMillis());

        if (fromCs) {
            // 客服 → 用户；同时把用户之前发的消息标记为已读
            Long userId = csBoundUser.get(session.getId());
            if (userId != null) {
                sendToUser(userId, payload);
            }
            chatMapper.markRead(sessionId, 0);
            chatMapper.resetUnread(sessionId);
        } else {
            // 用户 → 在线客服；客服不在线则留言，未读数 +1
            WebSocketSession csSession = findCsByUser(uid);
            if (csSession != null) {
                send(csSession, payload);
            } else {
                chatMapper.incrUnread(sessionId);
            }
        }
    }

    /** 结束会话：更新状态并通知双方 */
    private void handleClose(long sessionId) throws Exception {
        chatMapper.closeSession(sessionId);
        ChatSession target = chatMapper.findById(sessionId);
        if (target == null) {
            return;
        }
        Map<String, Object> payload = Map.of("type", "CLOSE", "sessionId", sessionId);
        sendToUser(target.getUserId(), payload);
        WebSocketSession csSession = findCsByUser(target.getUserId());
        if (csSession != null) {
            send(csSession, payload);
            csBoundUser.remove(csSession.getId());
        }
    }

    // ==================== 投递工具 ====================

    /** 找到接入指定用户的客服连接 */
    private WebSocketSession findCsByUser(Long userId) {
        for (Map.Entry<String, Long> entry : csBoundUser.entrySet()) {
            if (entry.getValue().equals(userId)) {
                String csSessionId = entry.getKey();
                return onlineCs.stream()
                        .filter(s -> s.getId().equals(csSessionId))
                        .findFirst()
                        .orElse(null);
            }
        }
        return null;
    }

    private void sendToUser(Long userId, Map<String, Object> payload) {
        send(userSessions.get(userId), payload);
    }

    /** 向一个连接发 JSON，连接已关闭或发送失败时仅记录日志 */
    private void send(WebSocketSession session, Map<String, Object> payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception e) {
            log.error("WebSocket 消息投递失败 sessionId={}", session.getId(), e);
        }
    }

    // ==================== 会话属性 ====================

    private long uid(WebSocketSession session) {
        Object val = session.getAttributes().get("uid");
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return 0;
    }

    private boolean isCs(WebSocketSession session) {
        return (int) session.getAttributes().getOrDefault("role", 0) == 1;
    }
}
