package couponservice.controller;

import couponservice.mapper.CouponMapper;
import model.Coupon;
import model.Result;
import model.ResultCodeEnum;
import model.UserCoupon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coupon")
public class CouponController {
    @Autowired
    private CouponMapper couponMapper;

    @GetMapping("/list")
    public Result<List<Coupon>> list() {
        return Result.success(couponMapper.listAvailable());
    }

    @GetMapping("/my")
    public Result<List<UserCoupon>> my(@RequestHeader("uid") String uid) {
        List<UserCoupon> list = couponMapper.listUserCoupons(Long.parseLong(uid));
        for (UserCoupon uc : list) {
            uc.setCoupon(couponMapper.findById(uc.getCouponId()));
        }
        return Result.success(list);
    }

    @PostMapping("/receive")
    public Result<String> receive(@RequestHeader("uid") String uid, @RequestParam Long couponId) {
        Long userId = Long.parseLong(uid);
        Coupon coupon = couponMapper.findById(couponId);
        if (coupon == null) return Result.failure(ResultCodeEnum.FAIL, "优惠券不存在");
        if (couponMapper.countUserCoupon(userId, couponId) >= coupon.getPerUserLimit()) {
            return Result.failure(ResultCodeEnum.FAIL, "已达领取上限");
        }
        couponMapper.receive(userId, couponId);
        return Result.success("领取成功");
    }

    @GetMapping("/calc")
    public Result<Map<String, Object>> calc(@RequestParam Long couponId, @RequestParam BigDecimal total) {
        Coupon coupon = couponMapper.findById(couponId);
        BigDecimal discount = couponMapper.calcDiscount(coupon, total);
        Map<String, Object> data = new HashMap<>();
        data.put("discount", discount);
        data.put("payAmount", total.subtract(discount));
        return Result.success(data);
    }

    /** 下单时按用户券计算抵扣（供订单服务 Feign 调用） */
    @GetMapping("/order/discount")
    public Result<Map<String, Object>> orderDiscount(@RequestParam Long userCouponId,
                                                     @RequestParam Long userId,
                                                     @RequestParam BigDecimal total) {
        UserCoupon uc = couponMapper.findUserCoupon(userCouponId, userId);
        if (uc == null || uc.getStatus() != null && uc.getStatus() != 0) {
            return Result.failure(ResultCodeEnum.FAIL, "优惠券不可用");
        }
        Coupon coupon = couponMapper.findById(uc.getCouponId());
        if (coupon == null) {
            return Result.failure(ResultCodeEnum.FAIL, "优惠券不存在");
        }
        if (total.compareTo(coupon.getThreshold()) < 0) {
            return Result.failure(ResultCodeEnum.FAIL,
                    "订单未满" + coupon.getThreshold() + "元，无法使用该优惠券");
        }
        BigDecimal discount = couponMapper.calcDiscount(coupon, total);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.failure(ResultCodeEnum.FAIL, "优惠券不满足使用条件");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("discount", discount);
        data.put("payAmount", total.subtract(discount));
        data.put("couponId", uc.getCouponId());
        return Result.success(data);
    }

    @PostMapping("/order/consume")
    public Result<Void> consume(@RequestParam Long userCouponId, @RequestParam Long orderId) {
        couponMapper.useCoupon(userCouponId, orderId);
        return Result.success();
    }
}
