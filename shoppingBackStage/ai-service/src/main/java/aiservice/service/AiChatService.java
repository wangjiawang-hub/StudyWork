package aiservice.service;

import aiservice.mapper.AiMessageMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * AI 对话编排：组装 system 提示词 + 历史消息 + 本轮（文本/图片）输入，
 * 底层调用 langchain4j 的火山方舟模型。
 */
@Service
public class AiChatService {

    /** 携带历史消息的最大条数（user+assistant 合计），控制上下文长度 */
    private static final int MAX_HISTORY = 10;

    /** 商城客服基础人设与业务规则 */
    private static final String BASE_SYSTEM_PROMPT =
            "你是 ShopMall 购物商城的智能客服助手。请用简洁、友好、专业的中文回答用户问题，"
                    + "重点解答运费、退货、支付、优惠券、物流、商品选购、订单状态等购物相关问题。"
                    + "商城规则：满99元包邮；支持7天无理由退货；支持支付宝支付；可在优惠券中心领券；"
                    + "发货后可在订单详情查看物流。若问题与商城无关，请礼貌引导用户咨询购物相关话题。";

    @Autowired
    private ChatModel chatModel;
    @Autowired
    private StreamingChatModel streamingChatModel;
    @Autowired
    private AiMessageMapper aiMessageMapper;
    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * 组装发给大模型的消息列表。
     *
     * @param sessionId 会话 ID（从库里取最近文本历史）
     * @param question  本轮文字问题（图片场景下可能为空串）
     * @param images    本轮图片 data URL 列表
     * @param useKb     是否启用知识库增强
     */
    public List<ChatMessage> buildMessages(String sessionId, String question,
                                           List<String> images, boolean useKb) {
        List<ChatMessage> messages = new ArrayList<>();

        // 1. system 提示词；开启知识库且有命中时，把检索片段作为回答依据
        StringBuilder system = new StringBuilder(BASE_SYSTEM_PROMPT);
        if (useKb) {
            String context = knowledgeService.retrieveContext(question);
            if (!context.isBlank()) {
                system.append("\n\n以下是商城知识库中可能相关的资料，请优先依据资料回答；"
                        + "若资料不足以回答，再结合通用知识并说明：\n").append(context);
            }
        }
        messages.add(SystemMessage.from(system.toString()));

        // 2. 最近若干轮文本历史（图片历史不传，控制请求体大小）
        List<aiservice.entity.AiMessage> history = aiMessageMapper.listTextBySession(sessionId);
        if (history.size() > MAX_HISTORY) {
            history = history.subList(history.size() - MAX_HISTORY, history.size());
        }
        for (aiservice.entity.AiMessage m : history) {
            if ("assistant".equals(m.getRole())) {
                messages.add(AiMessage.from(m.getContent()));
            } else {
                messages.add(UserMessage.from(m.getContent()));
            }
        }

        // 3. 本轮输入：文字 + 图片（多模态）
        List<dev.langchain4j.data.message.Content> contents = new ArrayList<>();
        if (question != null && !question.isBlank()) {
            contents.add(TextContent.from(question.trim()));
        }
        if (images != null) {
            for (String dataUrl : images) {
                if (dataUrl != null && dataUrl.startsWith("data:image/")) {
                    contents.add(ImageContent.from(parseImage(dataUrl)));
                }
            }
        }
        if (contents.isEmpty()) {
            contents.add(TextContent.from(""));
        }
        messages.add(UserMessage.from(contents.toArray(new dev.langchain4j.data.message.Content[0])));
        return messages;
    }

    /** 非流式问答（Postman 调试 / 简单调用） */
    public String chat(List<ChatMessage> messages) {
        ChatResponse response = chatModel.chat(ChatRequest.builder().messages(messages).build());
        return response.aiMessage().text();
    }

    /**
     * 流式问答。
     *
     * @param onPartial 每收到一个文本片段时回调
     * @param onComplete 全部完成回调，参数为拼接好的完整回答
     * @param onError 异常回调
     */
    public void streamChat(List<ChatMessage> messages,
                           Consumer<String> onPartial,
                           Consumer<String> onComplete,
                           Consumer<Throwable> onError) {
        StringBuilder answer = new StringBuilder();
        streamingChatModel.chat(ChatRequest.builder().messages(messages).build(),
                new StreamingChatResponseHandler() {
                    @Override
                    public void onPartialResponse(String partial) {
                        answer.append(partial);
                        onPartial.accept(partial);
                    }

                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        onComplete.accept(answer.toString());
                    }

                    @Override
                    public void onError(Throwable error) {
                        onError.accept(error);
                    }
                });
    }

    /** 把 data:image/png;base64,xxxx 解析成 langchain4j 的 Image */
    private dev.langchain4j.data.image.Image parseImage(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        String meta = comma > 0 ? dataUrl.substring(5, dataUrl.indexOf(';')) : "image/png";
        String base64 = comma > 0 ? dataUrl.substring(comma + 1) : dataUrl;
        return dev.langchain4j.data.image.Image.builder()
                .base64Data(base64)
                .mimeType(meta)
                .build();
    }
}
