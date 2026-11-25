package adminservice.controller;

import adminservice.mapper.ChatMapper;
import model.ChatMessage;
import model.ChatSession;
import model.Result;
import model.ResultCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人工客服 REST 接口（实时消息走 WebSocket，这里只负责会话管理和历史记录）。
 *  - 用户端：GET  /ai/customer/my-session?           取自己的会话，没有则自动创建待接入会话
 *  - 双方：  GET  /ai/customer/messages?sessionId=   查历史消息
 *  - 客服端：GET  /ai/customer/sessions              待接入列表 + 自己已接入列表
 *  - 双方：  POST /ai/customer/close?sessionId=      结束会话
 *
 * 客服"接入会话"走 WebSocket 发送 JOIN 消息（并发原子绑定），不提供 REST 接口。
 * uid/role 请求头由网关解析 JWT 后注入。
 */
@RestController
@RequestMapping("/admin/customer")
public class ChatController {

    @Autowired
    private ChatMapper chatMapper;

    /** 用户：查自己进行中/待接入的会话，没有则新建一个待接入会话 */
    @GetMapping("/my-session")
    public Result<ChatSession> mySession(@RequestHeader("uid") String uid) {
        Long userId = Long.valueOf(uid);
        ChatSession session = chatMapper.findActiveSession(userId);
        if (session == null) {
            session = new ChatSession();
            session.setUserId(userId);
            chatMapper.insertPendingSession(session);
        }
        return Result.success(session);
    }

    /** 查某个会话的历史消息（按时间正序） */
    @GetMapping("/messages")
    public Result<List<ChatMessage>> messages(@RequestParam Long sessionId) {
        return Result.success(chatMapper.listMessages(sessionId));
    }

    /** 客服：查待接入会话 + 自己已接入的会话；普通用户返回空结构 */
    @GetMapping("/sessions")
    public Result<Map<String, Object>> sessions(@RequestHeader("uid") String uid,
                                                @RequestHeader("role") String role) {
        Map<String, Object> data = new HashMap<>();
        if ("1".equals(role)) {
            Long csId = Long.valueOf(uid);
            data.put("pending", chatMapper.listPendingSessions());
            data.put("active", chatMapper.listCsActiveSessions(csId));
        }
        return Result.success(data);
    }

    /** 结束会话（status 置为 2） */
    @PostMapping("/close")
    public Result<Void> close(@RequestParam Long sessionId) {
        return chatMapper.closeSession(sessionId) > 0
                ? Result.success()
                : Result.failure(ResultCodeEnum.FAIL, "关闭失败，会话不存在");
    }
}
