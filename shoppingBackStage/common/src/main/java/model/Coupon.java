package model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Coupon {
    private Long id;
    private String name;
    private Integer type;
    private BigDecimal threshold;
    private BigDecimal reduceAmount;
    private BigDecimal discountRate;
    private BigDecimal maxDiscount;
    private Integer totalQuantity;
    private Integer usedQuantity;
    private Integer perUserLimit;
    private Date expireStart;
    private Date expireEnd;
    private Integer status;
    private Date createTime;
}
