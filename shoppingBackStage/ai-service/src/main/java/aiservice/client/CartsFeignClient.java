package aiservice.client;

import model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * AI 加购智能体调用购物车服务：加购。
 * carts-server 路径前缀 /cart；uid 由网关注入，AI 服务内部调用时直接透传。
 */
@FeignClient(name = "carts-server")
public interface CartsFeignClient {

    @PostMapping("/cart/add")
    Result<String> add(@RequestHeader("uid") String uid, @RequestBody Map<String, Object> body);
}
