package aiservice.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * langchain4j AI 智能体接口：用户对 AI 说"帮我加购 xxx"，AI 自动调用搜索/加购工具完成操作。
 * 由 ShoppingAgentService 用 AiServices.builder 构建，绑定 chatModel 和 CartTools。
 */
public interface ShoppingAssistant {

    @SystemMessage("你是 ShopMall 购物助手。用户想加购商品时，先调用搜索工具找到商品，"
            + "再调用加购工具加入购物车，最后用一句中文告诉用户结果（商品名、数量）。"
            + "如果搜不到商品，请如实告知。不要编造商品信息。")
    String chat(@UserMessage String userMessage);
}
