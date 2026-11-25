package aiservice.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 问答请求体。
 * sessionId：前端创建会话后获得；question：文字问题；
 * images：图片 data URL 列表（data:image/png;base64,xxx），可空，即多模态输入；
 * useKb：是否先用知识库检索增强（RAG）。
 */
@Data
public class ChatRequest {
    private String sessionId;
    private String question;
    private List<String> images = new ArrayList<>();
    private Boolean useKb = false;
}
