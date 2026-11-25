package userservice.mapper;

import model.Address;
import model.Favorite;
import model.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserBizMapper {
    @Select("SELECT * FROM address WHERE user_id=#{userId} ORDER BY is_default DESC, id DESC")
    List<Address> listAddress(Long userId);

    @Insert("INSERT INTO address(user_id,receiver_name,phone,province,city,district,detail,is_default) " +
            "VALUES(#{userId},#{receiverName},#{phone},#{province},#{city},#{district},#{detail},#{isDefault})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAddress(Address address);

    @Update("UPDATE address SET receiver_name=#{receiverName},phone=#{phone},province=#{province},city=#{city}," +
            "district=#{district},detail=#{detail},is_default=#{isDefault} WHERE id=#{id} AND user_id=#{userId}")
    int updateAddress(Address address);

    @Delete("DELETE FROM address WHERE id=#{id} AND user_id=#{userId}")
    int deleteAddress(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE address SET is_default=0 WHERE user_id=#{userId}")
    int clearDefault(Long userId);

    @Update("UPDATE address SET is_default=1 WHERE id=#{id} AND user_id=#{userId}")
    int markDefault(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM address WHERE user_id=#{userId}")
    int countAddress(Long userId);

    @Select("SELECT id FROM address WHERE user_id=#{userId} ORDER BY id DESC LIMIT 1")
    Long findLatestAddressId(Long userId);

    @Select("SELECT f.*, p.name AS product_name, p.image AS product_image, p.price " +
            "FROM favorite f LEFT JOIN product p ON f.product_id=p.id WHERE f.user_id=#{userId}")
    List<Favorite> listFavorite(Long userId);

    @Insert("INSERT IGNORE INTO favorite(user_id, product_id) VALUES(#{userId},#{productId})")
    int addFavorite(@Param("userId") Long userId, @Param("productId") Long productId);

    @Delete("DELETE FROM favorite WHERE user_id=#{userId} AND product_id=#{productId}")
    int removeFavorite(@Param("userId") Long userId, @Param("productId") Long productId);

    @Select("SELECT COUNT(*) FROM favorite WHERE user_id=#{userId} AND product_id=#{productId}")
    int isFavorite(@Param("userId") Long userId, @Param("productId") Long productId);

    /** 校验订单是否属于用户且已完成（order_status=3） */
    @Select("SELECT id FROM `order` WHERE id=#{orderId} AND user_id=#{userId} AND order_status=3")
    Long findCompletedOrder(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Select("SELECT id FROM order_item WHERE order_id=#{orderId} AND product_id=#{productId} LIMIT 1")
    Long findOrderItem(@Param("orderId") Long orderId, @Param("productId") Long productId);

    @Select("SELECT COUNT(*) FROM review WHERE user_id=#{userId} AND order_id=#{orderId} AND product_id=#{productId}")
    int countReviewForOrderProduct(@Param("userId") Long userId, @Param("orderId") Long orderId, @Param("productId") Long productId);

    @Select("SELECT oi.id AS orderItemId, oi.product_id AS productId, oi.product_name AS productName, " +
            "oi.product_image AS productImage, oi.quantity, " +
            "CASE WHEN r.id IS NULL THEN 0 ELSE 1 END AS reviewed, " +
            "r.id AS reviewId, r.rating, r.content " +
            "FROM order_item oi " +
            "LEFT JOIN review r ON r.order_id=oi.order_id AND r.product_id=oi.product_id AND r.user_id=#{userId} " +
            "WHERE oi.order_id=#{orderId} ORDER BY oi.id")
    List<java.util.Map<String, Object>> listOrderReviewItems(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Insert("INSERT INTO review(user_id,product_id,order_id,rating,content,is_anonymous,status) " +
            "VALUES(#{userId},#{productId},#{orderId},#{rating},#{content},COALESCE(#{isAnonymous},0),1)")
    int insertReview(Review review);

    @Select("SELECT r.*, u.username FROM review r LEFT JOIN user u ON r.user_id=u.id WHERE r.product_id=#{productId} ORDER BY r.create_time DESC")
    List<Review> listReview(Long productId);

    @Select("SELECT r.*, p.name AS product_name, p.image AS product_image " +
            "FROM review r LEFT JOIN product p ON r.product_id=p.id " +
            "WHERE r.user_id=#{userId} ORDER BY r.create_time DESC")
    List<Review> listMyReview(Long userId);
}
