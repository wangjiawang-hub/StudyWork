package model;

import lombok.Data;

import java.util.Date;

@Data
public class UserCoupon {
    private Long id;
    private Long userId;
    private Long couponId;
    private Integer status;
    private Date useTime;
    private Long orderId;
    private Date receiveTime;
    private Coupon coupon;
}
