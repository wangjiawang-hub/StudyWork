package model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Sku {
    private Long id;
    private Long productId;
    private String skuCode;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    private Date createTime;
}
