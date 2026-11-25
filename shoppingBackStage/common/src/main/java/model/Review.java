package model;

import lombok.Data;

import java.util.Date;

@Data
public class Review {
    private Long id;
    private Long userId;
    private Long productId;
    private Long orderId;
    private Long orderItemId;
    private Integer rating;
    private String content;
    private String images;
    private String reply;
    private Integer isAnonymous;
    private Integer status;
    private Date createTime;
    private Date updateTime;
    private String username;
    private String productName;
    private String productImage;
}
