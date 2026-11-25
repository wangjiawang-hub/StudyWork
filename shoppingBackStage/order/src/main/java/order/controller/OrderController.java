package order.controller;

import model.Order;
import model.Result;
import order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
/** 订单：创建、列表、详情、取消/确认/售后、物流（创建时 Feign 调 cart/coupon） */
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public Result<Order> create(@RequestHeader("uid") String uid, @RequestBody Map<String, Object> body) {
        Long addressId = Long.valueOf(body.get("addressId").toString());
        List<Long> cartIds = ((List<?>) body.get("cartIds")).stream()
                .map(o -> Long.valueOf(o.toString())).collect(java.util.stream.Collectors.toList());
        Long couponId = body.get("userCouponId") == null ? null : Long.valueOf(body.get("userCouponId").toString());
        String remark = body.get("remark") == null ? null : body.get("remark").toString();
        return orderService.createOrder(Long.parseLong(uid), addressId, cartIds, couponId, remark);
    }

    @GetMapping("/list")
    public Result<List<Order>> list(@RequestHeader("uid") String uid) {
        return Result.success(orderService.getOrders(Long.parseLong(uid)));
    }

    @GetMapping("/detail")
    public Result<Order> detail(@RequestParam Long id) {
        return Result.success(orderService.getDetail(id));
    }

    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestHeader("uid") String uid, @RequestParam Long id) {
        return orderService.cancel(Long.parseLong(uid), id);
    }

    @PostMapping("/confirm")
    public Result<Void> confirm(@RequestHeader("uid") String uid, @RequestParam Long id) {
        return orderService.confirmReceipt(Long.parseLong(uid), id);
    }

    @PostMapping("/afterSale")
    public Result<Void> afterSale(@RequestHeader("uid") String uid, @RequestParam Long id) {
        return orderService.applyAfterSale(Long.parseLong(uid), id);
    }

    @GetMapping("/logistics")
    public Result<List<Map<String, Object>>> logistics(@RequestParam Long orderId) {
        return Result.success(orderService.logisticsTrack(orderId));
    }

    @GetMapping("/logistics/map")
    public Result<Map<String, Object>> logisticsMap(@RequestParam Long orderId) {
        return Result.success(orderService.logisticsMap(orderId));
    }
}
