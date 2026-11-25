package order.mapper;

import model.Order;
import model.OrderItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO `order`(order_no,user_id,address_id,total_amount,pay_amount,coupon_id,discount_amount,freight_amount,pay_status,order_status,remark) " +
            "VALUES(#{orderNo},#{userId},#{addressId},#{totalAmount},#{payAmount},#{couponId},#{discountAmount},#{freightAmount},0,0,#{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOrder(Order order);

    @Insert("INSERT INTO order_item(order_id,product_id,sku_id,sku_spec,product_name,product_image,price,quantity,total_price) " +
            "VALUES(#{orderId},#{productId},#{skuId},#{skuSpec},#{productName},#{productImage},#{price},#{quantity},#{totalPrice})")
    int insertOrderItem(OrderItem item);

    List<Order> findByUserId(Long userId);

    Order findById(Long id);

    @Select("SELECT * FROM `order` WHERE order_no = #{orderNo}")
    Order findByOrderNo(String orderNo);

    @Update("UPDATE `order` SET pay_status=1, order_status=1, pay_time=NOW() WHERE order_no=#{orderNo}")
    int markPaid(String orderNo);

    @Update("UPDATE `order` SET order_status=4, cancel_time=NOW() WHERE id=#{id} AND user_id=#{userId} AND order_status=0")
    int cancel(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE `order` SET order_status=2, logistics_no=#{logisticsNo}, logistics_company=#{company}, delivery_time=NOW() WHERE id=#{id}")
    int ship(@Param("id") Long id, @Param("logisticsNo") String logisticsNo, @Param("company") String company);

    @Update("UPDATE `order` SET order_status=3, receipt_time=NOW() WHERE id=#{id} AND user_id=#{userId}")
    int confirmReceipt(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE `order` SET order_status=5 WHERE id=#{id} AND user_id=#{userId}")
    int applyAfterSale(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("DELETE FROM `order` WHERE id=#{id}")
    int deleteOrder(Long id);

    @Delete("DELETE FROM order_item WHERE order_id=#{orderId}")
    int deleteItems(Long orderId);
}
