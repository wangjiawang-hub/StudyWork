package order.service;

import model.Order;
import model.Result;

import java.util.List;
import java.util.Map;

public interface OrderService {
    Result<Order> createOrder(Long userId, Long addressId, List<Long> cartIds, Long userCouponId, String remark);
    List<Order> getOrders(Long userId);
    Order getDetail(Long id);
    Result<Void> cancel(Long userId, Long id);
    Result<Void> confirmReceipt(Long userId, Long id);
    Result<Void> applyAfterSale(Long userId, Long id);
    List<Map<String, Object>> logisticsTrack(Long orderId);

    Map<String, Object> logisticsMap(Long orderId);
}
