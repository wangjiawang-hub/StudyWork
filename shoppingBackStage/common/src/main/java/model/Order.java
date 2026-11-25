package model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long addressId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Long couponId;
    private BigDecimal discountAmount;
    private BigDecimal freightAmount;
    private Integer payStatus;
    private Integer orderStatus;
    private String logisticsNo;
    private String logisticsCompany;
    private Date payTime;
    private Date deliveryTime;
    private Date receiptTime;
    private Date cancelTime;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private List<OrderItem> items;
    private String receiverName;
    private String receiverPhone;
    private String fullAddress;
}
