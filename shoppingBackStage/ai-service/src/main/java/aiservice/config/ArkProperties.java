package aiservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 火山方舟大模型连接参数，对应配置前缀 volcengine.ark */
@Data
@Component
@ConfigurationProperties(prefix = "volcengine.ark")
public class ArkProperties {
    /** 火山方舟 API Key，可通过环境变量 ARK_API_KEY 覆盖 */
    private String apiKey = "";
    /** OpenAI 兼容接口地址 */
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";
    /** 对话模型 ID（需支持多模态理解才能识别图片） */
    private String chatModel = "doubao-seed-2-0-code-preview-260215";
    /** 文本向量化模型 ID（RAG 用，需在方舟控制台开通） */
    private String embeddingModel = "doubao-embedding-text-240715";
    private Double temperature = 0.7;
    private Integer maxTokens = 2048;
}
