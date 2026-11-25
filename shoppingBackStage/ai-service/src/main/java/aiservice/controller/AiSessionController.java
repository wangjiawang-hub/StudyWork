package aiservice.controller;

import aiservice.entity.AiMessage;
import aiservice.entity.AiSession;
import aiservice.mapper.AiMessageMapper;
import aiservice.mapper.AiSessionMapper;
import model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * AI 会话管理。
 *  POST   /ai/sessions              新建会话（登录用户自动绑定 uid，匿名为空）
 *  GET    /ai/sessions              当前登录用户的会话列表；匿名返回空列表（前端本地维护）
 *  GET    /ai/sessions/{id}/messages 会话历史消息（含图片）
 *  DELETE /ai/sessions/{id}          删除会话及其消息
 */
@RestController
@RequestMapping("/ai/sessions")
public class AiSessionController {

    @Autowired
    private AiSessionMapper aiSessionMapper;
    @Autowired
    private AiMessageMapper aiMessageMapper;

    /** 新建会话 */
    @PostMapping
    public Result<AiSession> create(@RequestHeader(value = "uid", required = false) String uid) {
        AiSession session = new AiSession();
        session.setId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(uid == null || uid.isBlank() ? null : Long.valueOf(uid));
        session.setTitle("新会话");
        aiSessionMapper.insert(session);
        return Result.success(session);
    }

    /** 会话列表：登录用户按账号取云端历史；匿名返回空（列表由前端 localStorage 维护） */
    @GetMapping
    public Result<List<AiSession>> list(@RequestHeader(value = "uid", required = false) String uid) {
        if (uid == null || uid.isBlank()) {
            return Result.success(Collections.emptyList());
        }
        return Result.success(aiSessionMapper.listByUser(Long.valueOf(uid)));
    }

    /** 取一个会话的全部消息（按时间正序） */
    @GetMapping("/{id}/messages")
    public Result<List<AiMessage>> messages(@PathVariable String id) {
        return Result.success(aiMessageMapper.listAllBySession(id));
    }

    /** 删除会话（先删消息再删会话） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        aiMessageMapper.deleteBySession(id);
        aiSessionMapper.deleteById(id);
        return Result.success();
    }
}
