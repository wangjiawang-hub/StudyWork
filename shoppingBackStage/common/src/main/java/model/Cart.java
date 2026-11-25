package model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Cart {
    private Long id;
    private Long userId;
    private Long productId;
    private Long skuId;
    private String skuSpec;
    private Integer quantity;
    private Integer selected;
    private Date createTime;
    private Date updateTime;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer stock;
}
