package aiservice.controller;

import aiservice.dto.ChatRequest;
import aiservice.entity.AiMessage;
import aiservice.entity.AiSession;
import aiservice.mapper.AiMessageMapper;
import aiservice.mapper.AiSessionMapper;
import aiservice.service.AiChatService;
import aiservice.service.ShoppingAgentService;
import dev.langchain4j.data.message.ChatMessage;
import jakarta.annotation.PreDestroy;
import model.Result;
import model.ResultCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 智能客服接口。
 *  POST /ai/chat        非流式问答（调试/简单调用）
 *  POST /ai/chat/stream 流式问答（SSE，前端实际使用）
 * SSE 事件 data 均为 JSON：{"type":"delta","content":"片段"} / {"type":"done","content":"全文"} / {"type":"error","message":"..."}
 * 接口在网关白名单内，uid 头可能为空（匿名）。
 */
@RestController
@RequestMapping("/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    @Autowired
    private AiChatService aiChatService;
    @Autowired
    private AiSessionMapper aiSessionMapper;
    @Autowired
    private AiMessageMapper aiMessageMapper;
    @Autowired
    private ShoppingAgentService shoppingAgentService;

    /** SSE 回调在模型 HTTP 线程触发，业务逻辑放到独立线程池，避免长时间占用容器线程 */
    private final ExecutorService ssePool = Executors.newCachedThreadPool();

    @PreDestroy
    public void shutdown() {
        ssePool.shutdownNow();
    }

    /** 非流式问答 */
 /*   @PostMapping("/chat")
    public Result<Map<String, String>> chat(@RequestBody ChatRequest request,
                                            @RequestHeader(value = "uid", required = false) String uid) {
        try {
            String sessionId = ensureSession(request, uid);
            // 先组装消息（读的是本轮之前的历史），再把本轮输入落库，避免问题重复
            List<ChatMessage> messages = aiChatService.buildMessages(
                    sessionId, request.getQuestion(), request.getImages(), Boolean.TRUE.equals(request.getUseKb()));
            saveUserMessages(sessionId, request);
            String answer = aiChatService.chat(messages);
            saveAssistantMessage(sessionId, answer);
            Map<String, String> data = new HashMap<>();
            data.put("answer", answer);
            return Result.success(data);
        } catch (Exception e) {
            log.error("AI 非流式问答失败", e);
            return Result.failure(ResultCodeEnum.FAIL, "AI 服务暂时不可用：" + e.getMessage());
        }
    }
*/
    /** 流式问答：边生成边通过 SSE 推 delta，完成时存库并推 done */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request,
                             @RequestHeader(value = "uid", required = false) String uid) {
        // 0~2 分钟超时，大模型长回答也够用
        SseEmitter emitter = new SseEmitter(120_000L);

        final String sessionId;
        try {
            sessionId = ensureSession(request, uid);
            saveUserMessages(sessionId, request);
        } catch (Exception e) {
            log.error("AI 流式问答准备失败", e);
            sendJson(emitter, Map.of("type", "error", "message", "请求准备失败：" + e.getMessage()));
            emitter.complete();
            return emitter;
        }

        // 加购意图：用户说"帮我加购 xxx"时走 AI 智能体（Function Calling，非流式）
        final boolean cartIntent = isCartIntent(request.getQuestion());
        final String question = request.getQuestion();

        ssePool.execute(() -> {
            try {
                if (cartIntent && uid != null && !uid.isBlank()) {
                    // 命中加购：调智能体执行工具，一次性推送结果
                    String answer = shoppingAgentService.chat(question, uid);
                    saveAssistantMessage(sessionId, answer);
                    // 逐段推送，前端体验接近流式
                    sendJson(emitter, Map.of("type", "delta", "content", answer));
                    sendJson(emitter, Map.of("type", "done", "content", answer));
                    emitter.complete();
                } else {
                    // 普通对话：走流式问答
                    List<ChatMessage> messages = aiChatService.buildMessages(
                            sessionId, question, request.getImages(), Boolean.TRUE.equals(request.getUseKb()));
                    aiChatService.streamChat(messages,
                            partial -> {try {
                                Thread.sleep(30);} catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                                sendJson(emitter, Map.of("type", "delta", "content", partial));},
                            fullAnswer -> {
                                saveAssistantMessage(sessionId, fullAnswer);
                                sendJson(emitter, Map.of("type", "done", "content", fullAnswer));
                                emitter.complete();
                            },
                            error -> {
                                log.error("AI 流式生成失败", error);
                                sendJson(emitter, Map.of("type", "error", "message",
                                        "AI 暂时无法回复：" + error.getMessage()));
                                emitter.complete();
                            });
                }
            } catch (Exception e) {
                log.error("AI 流式问答异常", e);
                sendJson(emitter, Map.of("type", "error", "message", e.getMessage()));
                emitter.complete();
            }
        });
        return emitter;
    }

    /**
     * 意图检测：用户想加购时走 AI 智能体（Function Calling）。
     * 覆盖"加购/加入购物车/帮我买/放到购物车/放进购物车/买个手机支架"等自然说法。
     * 排除"购物车怎么用"等纯咨询。
     */
    private boolean isCartIntent(String question) {
        if (question == null || question.isBlank()) return false;
        String q = question.trim();
        // 排除纯咨询类（怎么用/如何/在哪）
        if (q.contains("怎么") || q.contains("如何") || q.contains("在哪")
                || q.contains("为什么") || q.contains("能不能用")) return false;
        // 明确加购词
        if (q.contains("加购") || q.contains("加入购物车") || q.contains("帮我买")
                || q.contains("帮我下单") || q.contains("加一下") || q.contains("帮我加")) return true;
        // "购物车" + 动作动词（放/加/进/买/装/要/清）
        if (q.contains("购物车") && (q.contains("放") || q.contains("加") || q.contains("进")
                || q.contains("买") || q.contains("装") || q.contains("要") || q.contains("清"))) return true;
        // "买个/来一个/要一个" + 商品名词
        if ((q.contains("买个") || q.contains("买一个") || q.contains("来一个")
                || q.contains("要一个") || q.contains("来个") || q.contains("要个"))
                && (q.contains("手机") || q.contains("支架") || q.contains("耳机")
                || q.contains("壳") || q.contains("商品") || q.contains("电脑"))) return true;
        return false;
    }

    // ==================== 会话与消息落库 ====================

    /** 会话不存在时兜底创建（正常流程前端会先调 POST /ai/sessions） */
    private String ensureSession(ChatRequest request, String uid) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
            AiSession session = new AiSession();
            session.setId(sessionId);
            session.setUserId(uid == null || uid.isBlank() ? null : Long.valueOf(uid));
            session.setTitle("新会话");
            aiSessionMapper.insert(session);
        } else if (aiSessionMapper.findById(sessionId) == null) {
            AiSession session = new AiSession();
            session.setId(sessionId);
            session.setUserId(uid == null || uid.isBlank() ? null : Long.valueOf(uid));
            session.setTitle("新会话");
            aiSessionMapper.insert(session);
        }
        return sessionId;
    }

    /** 保存本轮用户输入：文本一条，图片各一条 */
    private void saveUserMessages(String sessionId, ChatRequest request) {
        if (request.getQuestion() != null && !request.getQuestion().isBlank()) {
            insertMessage(sessionId, "user", "text", request.getQuestion().trim());
            // 首条提问顺便作为会话标题
            String title = request.getQuestion().trim();
            if (title.length() > 20) {
                title = title.substring(0, 20);
            }
            aiSessionMapper.updateTitle(sessionId, title);
        }
        if (request.getImages() != null) {
            for (String dataUrl : request.getImages()) {
                if (dataUrl != null && !dataUrl.isBlank()) {
                    insertMessage(sessionId, "user", "image", dataUrl);
                }
            }
        }
    }

    private void saveAssistantMessage(String sessionId, String answer) {
        if (answer != null && !answer.isBlank()) {
            insertMessage(sessionId, "assistant", "text", answer);
        }
    }

    private void insertMessage(String sessionId, String role, String msgType, String content) {
        AiMessage msg = new AiMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setMsgType(msgType);
        msg.setContent(content);
        aiMessageMapper.insert(msg);
    }

    /** SSE 推送一个 JSON 事件 */
    private void sendJson(SseEmitter emitter, Object payload) {
        try {
            emitter.send(SseEmitter.event().data(payload, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.debug("SSE 推送失败（客户端可能已断开）：{}", e.getMessage());
        }
    }
}
