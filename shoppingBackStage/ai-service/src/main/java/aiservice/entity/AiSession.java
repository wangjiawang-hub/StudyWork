package aiservice.entity;

import lombok.Data;

import java.util.Date;

/** AI 对话会话。匿名会话 userId 为空，登录后按账号关联 */
@Data
public class AiSession {
    /** 前端生成的 UUID（32 位无横线） */
    private String id;
    private Long userId;

    private String title;
    private Date createTime;
    private Date updateTime;
}
