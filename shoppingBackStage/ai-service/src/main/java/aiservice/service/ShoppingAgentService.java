package aiservice.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI 加购智能体编排：用 langchain4j AiServices 绑定对话模型与购物工具。
 * 模型自主决定调用 searchProducts / addToCart 完成"用户一句话加购"。
 * 非流式（加购场景需要确认结果，流式反而不好处理工具调用）。
 */
@Service
public class ShoppingAgentService {
    private static final Logger log = LoggerFactory.getLogger(ShoppingAgentService.class);

    @Autowired
    private ChatModel chatModel;
    @Autowired
    private CartTools cartTools;

    /**
     * 执行加购对话。
     * @param userMessage 用户原话，如"帮我加购 iPhone"
     * @param uid 当前用户 ID（加购必需）
     * @return AI 最终回复（已执行工具调用）
     */
    public String chat(String userMessage, String uid) {
        CartTools.CURRENT_UID.set(uid);
        try {
            ShoppingAssistant assistant = AiServices.builder(ShoppingAssistant.class)
                    .chatModel(chatModel)
                    .tools(cartTools)
                    .build();
            String answer = assistant.chat(userMessage);
            log.info("AI 加购智能体回复：{}", answer);
            return answer;
        } finally {
            CartTools.CURRENT_UID.remove();
        }
    }
}
