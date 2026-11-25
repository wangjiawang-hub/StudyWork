package adminservice.mapper;

import model.*;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminMapper {
    @Select("<script>SELECT * FROM user " +
            "<where>" +
            "<if test='keyword!=null and keyword!=\"\"'> AND (username LIKE CONCAT('%',#{keyword},'%') OR phone LIKE CONCAT('%',#{keyword},'%') OR email LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "</where> ORDER BY create_time DESC LIMIT #{offset}, #{size}</script>")
    List<User> listUsers(@Param("offset") int offset, @Param("size") int size, @Param("keyword") String keyword);

    @Select("<script>SELECT COUNT(*) FROM user " +
            "<where>" +
            "<if test='keyword!=null and keyword!=\"\"'> AND (username LIKE CONCAT('%',#{keyword},'%') OR phone LIKE CONCAT('%',#{keyword},'%') OR email LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "</where></script>")
    long countUsers(@Param("keyword") String keyword);

    @Select("SELECT * FROM user WHERE username=#{username}")
    User findUserByUsername(String username);

    @Select("SELECT * FROM user WHERE phone=#{phone} LIMIT 1")
    User findUserByPhone(String phone);

    @Select("SELECT * FROM user WHERE email=#{email} LIMIT 1")
    User findUserByEmail(String email);

    @Insert("INSERT INTO user(username,password,phone,email,role,status) VALUES(#{username},#{password},#{phone},#{email},#{role},#{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    @Update("UPDATE user SET username=#{username}, phone=#{phone}, email=#{email}, role=#{role}, status=#{status} WHERE id=#{id}")
    int updateUser(User user);

    @Delete("DELETE FROM user WHERE id=#{id}")
    int deleteUser(Long id);

    @Select("SELECT COUNT(*) FROM user WHERE id=#{id}")
    int countUserById(Long id);

    @Delete("DELETE FROM after_sale WHERE user_id=#{userId}")
    int deleteAfterSaleByUser(Long userId);

    @Delete("DELETE FROM review WHERE user_id=#{userId}")
    int deleteReviewsByUser(Long userId);

    @Delete("DELETE FROM `order` WHERE user_id=#{userId}")
    int deleteOrdersByUser(Long userId);

    @Select("<script>SELECT * FROM product " +
            "<where>" +
            "<if test='categoryId!=null'> AND category_id=#{categoryId}</if>" +
            "<if test='keyword!=null and keyword!=\"\"'> AND name LIKE CONCAT('%',#{keyword},'%')</if>" +
            "</where> ORDER BY create_time DESC LIMIT #{offset}, #{size}</script>")
    List<Product> listProducts(@Param("offset") int offset, @Param("size") int size,
                               @Param("categoryId") Integer categoryId, @Param("keyword") String keyword);

    @Select("<script>SELECT COUNT(*) FROM product " +
            "<where>" +
            "<if test='categoryId!=null'> AND category_id=#{categoryId}</if>" +
            "<if test='keyword!=null and keyword!=\"\"'> AND name LIKE CONCAT('%',#{keyword},'%')</if>" +
            "</where></script>")
    long countProducts(@Param("categoryId") Integer categoryId, @Param("keyword") String keyword);

    @Insert("INSERT INTO product(name,category_id,category_name,price,stock,image,description,status) " +
            "VALUES(#{name},#{categoryId},#{categoryName},#{price},#{stock},#{image},#{description},#{status})")
    int insertProduct(Product product);

    @Update("UPDATE product SET name=#{name}, category_id=#{categoryId}, category_name=#{categoryName}, price=#{price}, stock=#{stock}, status=#{status}, description=#{description}, image=#{image} WHERE id=#{id}")
    int updateProduct(Product product);

    @Delete("DELETE FROM product WHERE id=#{id}")
    int deleteProduct(Long id);

    @Update("UPDATE product SET category_name=#{name} WHERE category_id=#{id}")
    int syncCategoryName(@Param("id") Integer id, @Param("name") String name);

    @Select("<script>SELECT o.* FROM `order` o " +
            "<where><if test='status!=null'> AND o.order_status=#{status}</if></where> " +
            "ORDER BY o.create_time DESC LIMIT #{offset}, #{size}</script>")
    List<Order> listOrders(@Param("offset") int offset, @Param("size") int size, @Param("status") Integer status);

    @Select("<script>SELECT COUNT(*) FROM `order` o " +
            "<where><if test='status!=null'> AND o.order_status=#{status}</if></where></script>")
    long countOrders(@Param("status") Integer status);

    @Select("SELECT * FROM `order` WHERE id=#{id}")
    Order findOrderById(Long id);

    @Update("UPDATE `order` SET order_status=2, logistics_no=#{logisticsNo}, logistics_company=#{company}, delivery_time=NOW() " +
            "WHERE id=#{id} AND order_status=1")
    int shipOrder(@Param("id") Long id, @Param("logisticsNo") String logisticsNo, @Param("company") String company);

    @Update("UPDATE `order` SET order_status=#{status} WHERE id=#{id}")
    int updateOrderStatus(@Param("id") Long id, @Param("status") int status);

    @Select("SELECT id, config_key AS configKey, config_value AS configValue, remark FROM sys_config WHERE config_key != 'mall_categories' ORDER BY id")
    List<Map<String, Object>> listConfig();

    @Select("SELECT config_value FROM sys_config WHERE config_key=#{key}")
    String getConfigValue(String key);

    @Update("UPDATE sys_config SET config_value=#{value} WHERE config_key=#{key}")
    int updateConfig(@Param("key") String key, @Param("value") String value);

    @Insert("INSERT INTO sys_config(config_key,config_value,remark) VALUES(#{key},#{value},#{remark})")
    int insertConfig(@Param("key") String key, @Param("value") String value, @Param("remark") String remark);

    @Select("SELECT id, user_id AS userId, operation, method, params, ip, create_time AS createTime FROM sys_log ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Map<String, Object>> listLogs(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM sys_log")
    long countLogs();

    @Insert("INSERT INTO sys_log(user_id,operation,method,params,ip) VALUES(#{userId},#{operation},#{method},#{params},#{ip})")
    int insertLog(@Param("userId") Long userId, @Param("operation") String operation,
                  @Param("method") String method, @Param("params") String params, @Param("ip") String ip);

    // ---- SKU 管理 ----

    @Select("SELECT * FROM sku WHERE product_id = #{productId} ORDER BY id")
    List<Sku> listSkusByProductId(Long productId);

    @Select("SELECT * FROM sku_spec WHERE sku_id = #{skuId}")
    List<SkuSpec> listSpecsBySkuId(Long skuId);

    @Insert("INSERT INTO sku(product_id,sku_code,price,stock,status) VALUES(#{productId},#{skuCode},#{price},#{stock},#{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSku(Sku sku);

    @Insert("INSERT INTO sku_spec(sku_id,spec_name,spec_value) VALUES(#{skuId},#{specName},#{specValue})")
    int insertSkuSpec(SkuSpec spec);

    @Delete("DELETE FROM sku WHERE id = #{id}")
    int deleteSku(Long id);

    @Delete("DELETE FROM sku_spec WHERE sku_id = #{skuId}")
    int deleteSpecsBySkuId(Long skuId);

    @Delete("DELETE FROM sku WHERE product_id = #{productId}")
    int deleteSkusByProductId(Long productId);

    @Delete("DELETE FROM sku_spec WHERE sku_id IN (SELECT id FROM sku WHERE product_id = #{productId})")
    int deleteAllSpecsByProductId(Long productId);

    // ---- 优惠券管理 ----

    @Select("<script>SELECT * FROM coupon " +
            "<where>" +
            "<if test='keyword!=null and keyword!=\"\"'> AND name LIKE CONCAT('%',#{keyword},'%')</if>" +
            "</where> ORDER BY create_time DESC LIMIT #{offset}, #{size}</script>")
    List<Coupon> listCoupons(@Param("offset") int offset, @Param("size") int size, @Param("keyword") String keyword);

    @Select("<script>SELECT COUNT(*) FROM coupon " +
            "<where>" +
            "<if test='keyword!=null and keyword!=\"\"'> AND name LIKE CONCAT('%',#{keyword},'%')</if>" +
            "</where></script>")
    long countCoupons(@Param("keyword") String keyword);

    @Insert("INSERT INTO coupon(name,type,threshold,reduce_amount,discount_rate,max_discount,total_quantity,per_user_limit,expire_start,expire_end,status) " +
            "VALUES(#{name},#{type},#{threshold},#{reduceAmount},#{discountRate},#{maxDiscount},#{totalQuantity},#{perUserLimit},#{expireStart},#{expireEnd},#{status})")
    int insertCoupon(Coupon coupon);

    @Update("UPDATE coupon SET name=#{name}, type=#{type}, threshold=#{threshold}, reduce_amount=#{reduceAmount}, " +
            "discount_rate=#{discountRate}, max_discount=#{maxDiscount}, total_quantity=#{totalQuantity}, " +
            "per_user_limit=#{perUserLimit}, expire_start=#{expireStart}, expire_end=#{expireEnd}, status=#{status} " +
            "WHERE id=#{id}")
    int updateCoupon(Coupon coupon);

    /** 上下架：status 1=上架 0=下架 */
    @Update("UPDATE coupon SET status=#{status} WHERE id=#{id}")
    int updateCouponStatus(@Param("id") Long id, @Param("status") Integer status);

    @Delete("DELETE FROM coupon WHERE id=#{id}")
    int deleteCoupon(Long id);

    // ---- 数据看板统计 ----

    @Select("SELECT COUNT(*) FROM user")
    long countAllUsers();

    /** 累计销售额（已支付订单） */
    @Select("SELECT COALESCE(SUM(pay_amount),0) FROM `order` WHERE pay_status=1")
    java.math.BigDecimal sumSales();

    @Select("SELECT COUNT(*) FROM user_coupon")
    long countCouponReceived();

    @Select("SELECT COUNT(*) FROM user_coupon WHERE status=1")
    long countCouponUsed();

    /** 近 N 天每日订单量与销售额 */
    @Select("SELECT DATE_FORMAT(create_time,'%m-%d') AS name, COUNT(*) AS orders, COALESCE(SUM(pay_amount),0) AS amount " +
            "FROM `order` WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE_FORMAT(create_time,'%m-%d') ORDER BY MIN(create_time)")
    List<Map<String, Object>> orderTrend(@Param("days") int days);

    /** 商品分类分布 */
    @Select("SELECT category_name AS name, COUNT(*) AS value FROM product " +
            "GROUP BY category_id, category_name ORDER BY value DESC")
    List<Map<String, Object>> categoryDistribution();

    /** 订单状态分布：0待付款 1待发货 2已发货 3已完成 4已取消 */
    @Select("SELECT order_status AS name, COUNT(*) AS value FROM `order` GROUP BY order_status")
    List<Map<String, Object>> orderStatusDistribution();

    /** 热销商品 Top N（按销量） */
    @Select("SELECT product_name AS name, SUM(quantity) AS value FROM order_item " +
            "GROUP BY product_id, product_name ORDER BY value DESC LIMIT #{limit}")
    List<Map<String, Object>> topProducts(@Param("limit") int limit);
}
