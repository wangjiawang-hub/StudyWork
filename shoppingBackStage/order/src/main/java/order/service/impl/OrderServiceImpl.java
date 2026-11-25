package order.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.*;
import order.mapper.OrderMapper;
import order.service.CartServiceFeignClient;
import order.service.CouponFeignClient;
import order.service.MallConfigReader;
import order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private CartServiceFeignClient cartServiceFeignClient;
    @Autowired
    private CouponFeignClient couponFeignClient;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MallConfigReader mallConfigReader;

    /**
     * 创建订单（事务）：
     * Feign 取购物车 → 算券 → 算运费 → 写订单/明细 → 核销券 → 清购物车
     * payAmount = total - discount + freight，为支付权威金额
     */
    @Override
    @Transactional//int
    public Result<Order> createOrder(Long userId, Long addressId, List<Long> cartIds, Long userCouponId, String remark) {
        Map<String, List<Long>> cartBody = new HashMap<>();
        cartBody.put("cartIds", cartIds);
        Result res = cartServiceFeignClient.subCartlist(String.valueOf(userId), cartBody);
        if (res.getCode() != 200) return res;
        List<Cart> carts = objectMapper.convertValue(res.getData(), new TypeReference<List<Cart>>() {});
        if (carts.isEmpty()) {
            return Result.failure(ResultCodeEnum.FAIL, "结算清单为空");
        }
        BigDecimal total = carts.stream()
                .map(c -> c.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = BigDecimal.ZERO;
        Long couponTemplateId = null;
        if (userCouponId != null) {
            Result<Map<String, Object>> couponRes = couponFeignClient.orderDiscount(userCouponId, userId, total);
            if (couponRes.getCode() != 200) {
                return Result.failure(ResultCodeEnum.FAIL, couponRes.getMessage());
            }
            Map<String, Object> couponData = couponRes.getData();
            discount = new BigDecimal(couponData.get("discount").toString());
            if (couponData.get("couponId") != null) {
                couponTemplateId = Long.valueOf(couponData.get("couponId").toString());
            }
        }
        BigDecimal freight = mallConfigReader.calcFreight(total);
        Order order = new Order();
        order.setOrderNo("ORD" + System.currentTimeMillis() + userId);
        order.setUserId(userId);
        order.setAddressId(addressId);
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setFreightAmount(freight);
        order.setPayAmount(total.subtract(discount).add(freight));
        order.setCouponId(couponTemplateId);
        order.setRemark(remark);
        orderMapper.insertOrder(order);
        if (userCouponId != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            couponFeignClient.consume(userCouponId, order.getId());
        }
        for (Cart cart : carts) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(cart.getProductId());
            item.setSkuId(cart.getSkuId());
            item.setSkuSpec(cart.getSkuSpec());
            item.setProductName(cart.getProductName());
            item.setProductImage(cart.getProductImage());
            item.setPrice(cart.getPrice());
            item.setQuantity(cart.getQuantity());
            item.setTotalPrice(cart.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            orderMapper.insertOrderItem(item);
        }
        cartServiceFeignClient.deleteCartlist(String.valueOf(userId), cartBody);
        return Result.success(order);
    }

    @Override
    public List<Order> getOrders(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    @Override
    public Order getDetail(Long id) {
        return orderMapper.findById(id);
    }

    @Override
    public Result<Void> cancel(Long userId, Long id) {
        return orderMapper.cancel(id, userId) > 0 ? Result.success() : Result.failure(ResultCodeEnum.FAIL, "无法取消");
    }

    @Override
    public Result<Void> confirmReceipt(Long userId, Long id) {
        return orderMapper.confirmReceipt(id, userId) > 0 ? Result.success() : Result.failure(ResultCodeEnum.FAIL, "操作失败");
    }

    @Override
    public Result<Void> applyAfterSale(Long userId, Long id) {
        return orderMapper.applyAfterSale(id, userId) > 0 ? Result.success() : Result.failure(ResultCodeEnum.FAIL, "申请失败");
    }

    /** 根据订单状态拼装物流时间线（模拟，生产可对接物流 API） */
    @Override
    public List<Map<String, Object>> logisticsTrack(Long orderId) {
        Order order = orderMapper.findById(orderId);
        List<Map<String, Object>> tracks = new ArrayList<>();
        if (order == null) return tracks;
        tracks.add(track("订单已创建", order.getCreateTime()));
        if (order.getPayStatus() != null && order.getPayStatus() == 1) {
            tracks.add(track("支付成功", order.getPayTime()));
        }
        if (order.getOrderStatus() != null && order.getOrderStatus() >= 2 && StringUtils.hasText(order.getLogisticsNo())) {
            tracks.add(track("商家已发货 物流单号:" + order.getLogisticsNo(), order.getDeliveryTime()));
        }
        if (order.getOrderStatus() != null && order.getOrderStatus() >= 2) {
            tracks.add(track("运输中 - 模拟轨迹：途经福州转运中心", new Date()));
        }
        if (order.getOrderStatus() != null && order.getOrderStatus() >= 3) {
            tracks.add(track("已签收", order.getReceiptTime()));
        }
        return tracks;
    }

    /** 模拟物流地图经纬度节点（福州→厦门），前端 OrderDetail 展示 */
    @Override
    public Map<String, Object> logisticsMap(Long orderId) {
        Order order = orderMapper.findById(orderId);
        Map<String, Object> map = new HashMap<>();
        if (order == null) {
            map.put("points", Collections.emptyList());
            return map;
        }
        // 模拟物流地图轨迹（福州 -> 厦门），可对接高德/百度物流 API
        List<Map<String, Object>> points = new ArrayList<>();
        points.add(point("福州仓", 119.296494, 26.074507, "已揽收"));
        points.add(point("福州转运中心", 119.306239, 26.075302, "运输中"));
        points.add(point("厦门分拨中心", 118.089425, 24.479834, "派送中"));
        points.add(point("收货地", 118.11022, 24.490474, order.getOrderStatus() != null && order.getOrderStatus() >= 3 ? "已签收" : "待签收"));
        map.put("orderNo", order.getOrderNo());
        map.put("logisticsNo", order.getLogisticsNo());
        map.put("logisticsCompany", order.getLogisticsCompany());
        map.put("points", points);
        return map;
    }

    private Map<String, Object> point(String name, double lng, double lat, String status) {
        Map<String, Object> p = new HashMap<>();
        p.put("name", name);
        p.put("lng", lng);
        p.put("lat", lat);
        p.put("status", status);
        return p;
    }

    private Map<String, Object> track(String desc, Date time) {
        Map<String, Object> m = new HashMap<>();
        m.put("desc", desc);
        m.put("time", time);
        return m;
    }
}
