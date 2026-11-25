package aiservice.entity;

import lombok.Data;

import java.util.Date;

/** 知识库文档（管理员录入原文，切块向量化在内存中完成） */
@Data
public class Knowledge {
    private Long id;
    private String title;
    private String content;
    private Date createTime;
}
