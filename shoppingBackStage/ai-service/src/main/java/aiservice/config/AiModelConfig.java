package aiservice.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * langchain4j 模型装配。
 * 火山方舟兼容 OpenAI 协议，三种模型共用 base-url / api-key：
 *  - ChatModel：一次性问答（给非流式接口和 Postman 调试用）
 *  - StreamingChatModel：SSE 流式问答
 *  - EmbeddingModel：知识库文本向量化（RAG）
 */
@Configuration
public class AiModelConfig {

    /** 普通对话模型 */
    @Bean
    public ChatModel chatModel(ArkProperties p) {
        return OpenAiChatModel.builder()
                .baseUrl(p.getBaseUrl())
                .apiKey(p.getApiKey())
                .modelName(p.getChatModel())
                .temperature(p.getTemperature())
                .maxTokens(p.getMaxTokens())
                .timeout(Duration.ofSeconds(90))
                .build();
    }

    /** 流式对话模型 */
    @Bean
    public StreamingChatModel streamingChatModel(ArkProperties p) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(p.getBaseUrl())
                .apiKey(p.getApiKey())
                .modelName(p.getChatModel())
                .temperature(p.getTemperature())
                .maxTokens(p.getMaxTokens())
                .timeout(Duration.ofSeconds(90))
                .build();
    }

    /** 文本向量化模型 */
    @Bean
    public EmbeddingModel embeddingModel(ArkProperties p) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(p.getBaseUrl())
                .apiKey(p.getApiKey())
                .modelName(p.getEmbeddingModel())
                .timeout(Duration.ofSeconds(30))
                .build();
    }
}
