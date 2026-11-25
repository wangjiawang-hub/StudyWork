package couponservice.mapper;

import model.Coupon;
import model.UserCoupon;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CouponMapper {
    @Select("SELECT * FROM coupon WHERE status=1 AND expire_end >= CURDATE() ORDER BY create_time DESC")
    List<Coupon> listAvailable();

    @Select("SELECT * FROM user_coupon WHERE user_id=#{userId} ORDER BY receive_time DESC")
    List<UserCoupon> listUserCoupons(Long userId);

    @Select("SELECT * FROM user_coupon WHERE id=#{id} AND user_id=#{userId}")
    UserCoupon findUserCoupon(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_coupon WHERE user_id=#{userId} AND coupon_id=#{couponId}")
    int countUserCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Select("SELECT * FROM coupon WHERE id=#{id}")
    Coupon findById(Long id);

    @Insert("INSERT INTO user_coupon(user_id, coupon_id, status) VALUES(#{userId},#{couponId},0)")
    int receive(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Update("UPDATE user_coupon SET status=1, use_time=NOW(), order_id=#{orderId} WHERE id=#{id}")
    int useCoupon(@Param("id") Long id, @Param("orderId") Long orderId);

    /**
     * 计算优惠券抵扣：type=0 满减，否则折扣率；需满足 threshold 门槛。
     * 下单时 order 服务 Feign 调用 /coupon/order/discount 再次校验。
     */
    default BigDecimal calcDiscount(Coupon coupon, BigDecimal total) {
        if (coupon == null || total.compareTo(coupon.getThreshold()) < 0) {
            return BigDecimal.ZERO;
        }
        if (coupon.getType() == 0) {
            return coupon.getReduceAmount();
        }
        BigDecimal discount = total.multiply(BigDecimal.ONE.subtract(coupon.getDiscountRate()));
        if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
            return coupon.getMaxDiscount();
        }
        return discount;
    }
}
