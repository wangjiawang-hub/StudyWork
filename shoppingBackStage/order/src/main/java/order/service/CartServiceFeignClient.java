package order.service;

import model.Result;
import order.service.impl.CartServiceFeignClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "carts-server", fallback = CartServiceFeignClientFallback.class)
/** order 服务调用购物车：结算取子集、下单后批量删除 */
public interface CartServiceFeignClient {
    @PostMapping("/cart/sublist")
    Result subCartlist(@RequestHeader("uid") String uid, @RequestBody Map<String, List<Long>> body);

    @PostMapping("/cart/deletelist")
    Result deleteCartlist(@RequestHeader("uid") String uid, @RequestBody Map<String, List<Long>> body);
}
