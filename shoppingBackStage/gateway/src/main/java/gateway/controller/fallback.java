package gateway.controller;

import model.Result;
import model.ResultCodeEnum;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class fallback {
    @RequestMapping("/authfallback")
    public Result authfallback() {
        return Result.failure(ResultCodeEnum.SERVER_ERROR, "认证服务异常");
    }
    @RequestMapping("/goodsfallback")
    public Result goodsfallback() {
        return Result.failure(ResultCodeEnum.SERVER_ERROR,"商品服务异常");
    }
    @RequestMapping("/cartfallback")
    public Result cartfallback() {
        return Result.failure(ResultCodeEnum.SERVER_ERROR,"购物车服务异常");
    }
    @RequestMapping("/orderfallback")
    public Result orderfallback() {
        return Result.failure(ResultCodeEnum.SERVER_ERROR,"订单服务异常");
    }
    @RequestMapping("/payfallback")
    public Result payfallback() {
        return Result.failure(ResultCodeEnum.SERVER_ERROR,"支付服务异常");
    }
}
