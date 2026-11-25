package model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 聚合视图：返回给前端时同时携带规格列表
 */
@Data
public class SkuVO {
    private Long id;
    private Long productId;
    private String skuCode;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    private List<SkuSpec> specs;//关键，返回 SkuVO+ 规格列表，一次请求全拿到
}
