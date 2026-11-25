package model;

import lombok.Data;

import java.util.Date;

/**
 * 人工客服会话。status: 0=待接入 1=进行中 2=已关闭
 */
@Data
public class ChatSession {
    private Long id;
    private Long userId;
    private Long csId;
    private Integer status;
    private Integer unreadCount;
    private Date createTime;
    private Date updateTime;

    /** 关联展示字段 */
    private String username;
}
