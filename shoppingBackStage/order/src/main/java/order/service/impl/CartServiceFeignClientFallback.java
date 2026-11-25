package order.service.impl;

import model.Result;
import model.ResultCodeEnum;
import order.service.CartServiceFeignClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CartServiceFeignClientFallback implements CartServiceFeignClient {
    @Override
    public Result subCartlist(String uid, Map<String, List<Long>> body) {
        return Result.failure(ResultCodeEnum.FAIL, "购物车服务不可用");
    }

    @Override
    public Result deleteCartlist(String uid, Map<String, List<Long>> body) {
        return Result.failure(ResultCodeEnum.FAIL, "购物车服务不可用");
    }
}
