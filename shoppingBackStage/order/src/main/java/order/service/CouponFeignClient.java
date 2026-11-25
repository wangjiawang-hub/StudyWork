package order.service;

import model.Result;
import order.service.impl.CouponFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "coupon-service", fallback = CouponFeignClientFallback.class)
/** order 服务调用优惠券：下单计算抵扣、核销 user_coupon */
public interface CouponFeignClient {

    @GetMapping("/coupon/order/discount")
    Result<Map<String, Object>> orderDiscount(@RequestParam("userCouponId") Long userCouponId,
                                             @RequestParam("userId") Long userId,
                                             @RequestParam("total") BigDecimal total);

    @PostMapping("/coupon/order/consume")
    Result<Void> consume(@RequestParam("userCouponId") Long userCouponId,
                         @RequestParam("orderId") Long orderId);
}
