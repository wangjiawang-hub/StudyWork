package model;

import lombok.Data;

import java.util.Date;

/**
 * 聊天消息。senderType: 0=user 1=cs
 */
@Data
public class ChatMessage {
    private Long id;
    private Long sessionId;
    private Long senderId;
    private Integer senderType;
    private String content;
    private Integer isRead;
    private Date createTime;
}
