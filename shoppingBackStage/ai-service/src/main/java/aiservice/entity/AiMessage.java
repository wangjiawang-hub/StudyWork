package aiservice.entity;

import lombok.Data;

import java.util.Date;

/** AI 对话消息。role: user/assistant；msgType: text/image（图片 content 为 data URL） */
@Data
public class AiMessage {
    private Long id;
    private String sessionId;
    private String role;
    private String msgType;
    private String content;
    private Date createTime;
}
