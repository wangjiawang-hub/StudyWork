package order.service.impl;

import model.Result;
import model.ResultCodeEnum;
import order.service.CouponFeignClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class CouponFeignClientFallback implements CouponFeignClient {
    @Override
    public Result<Map<String, Object>> orderDiscount(Long userCouponId, Long userId, BigDecimal total) {
        return Result.failure(ResultCodeEnum.FAIL, "优惠券服务不可用");
    }

    @Override
    public Result<Void> consume(Long userCouponId, Long orderId) {
        return Result.failure(ResultCodeEnum.FAIL, "优惠券服务不可用");
    }
}
