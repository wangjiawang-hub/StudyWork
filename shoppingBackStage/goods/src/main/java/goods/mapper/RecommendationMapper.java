package goods.mapper;

import model.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecommendationMapper {

    /** 用户已交互商品（购买/收藏/购物车），推荐时排除 */
    @Select("SELECT DISTINCT product_id FROM (" +
            "SELECT oi.product_id FROM order_item oi INNER JOIN `order` o ON oi.order_id = o.id " +
            "WHERE o.user_id = #{userId} AND o.pay_status = 1 " +
            "UNION SELECT product_id FROM favorite WHERE user_id = #{userId} " +
            "UNION SELECT product_id FROM cart WHERE user_id = #{userId}" +
            ") t")
    List<Long> findUserInteractedProductIds(@Param("userId") Long userId);

    /** 用户品类偏好：购买(5) + 收藏(4) + 购物车(3) + 好评(2) */
    @Select("SELECT category_id AS categoryId, SUM(weight) AS score FROM (" +
            "SELECT p.category_id, 5.0 AS weight FROM order_item oi " +
            "INNER JOIN `order` o ON oi.order_id = o.id " +
            "INNER JOIN product p ON oi.product_id = p.id " +
            "WHERE o.user_id = #{userId} AND o.pay_status = 1 AND p.category_id IS NOT NULL " +
            "UNION ALL " +
            "SELECT p.category_id, 4.0 AS weight FROM favorite f " +
            "INNER JOIN product p ON f.product_id = p.id " +
            "WHERE f.user_id = #{userId} AND p.category_id IS NOT NULL " +
            "UNION ALL " +
            "SELECT p.category_id, 3.0 AS weight FROM cart c " +
            "INNER JOIN product p ON c.product_id = p.id " +
            "WHERE c.user_id = #{userId} AND p.category_id IS NOT NULL " +
            "UNION ALL " +
            "SELECT p.category_id, 2.0 AS weight FROM review r " +
            "INNER JOIN product p ON r.product_id = p.id " +
            "WHERE r.user_id = #{userId} AND r.rating >= 4 AND p.category_id IS NOT NULL" +
            ") pref GROUP BY category_id ORDER BY score DESC")
    List<Map<String, Object>> findUserCategoryPreferences(@Param("userId") Long userId);

    /** 与用户购买商品同单出现的其他商品（搭配购买） */
    @Select("SELECT oi2.product_id AS productId, COUNT(*) AS cnt FROM order_item oi_user " +
            "INNER JOIN `order` o_user ON oi_user.order_id = o_user.id " +
            "INNER JOIN order_item oi2 ON oi2.order_id = o_user.id AND oi2.product_id <> oi_user.product_id " +
            "INNER JOIN product p ON oi2.product_id = p.id AND p.status = 1 " +
            "WHERE o_user.user_id = #{userId} AND o_user.pay_status = 1 " +
            "GROUP BY oi2.product_id ORDER BY cnt DESC LIMIT #{limit}")
    List<Map<String, Object>> findCoPurchaseProducts(@Param("userId") Long userId, @Param("limit") int limit);

    /** 协同过滤：买过相同商品的其他用户还买了什么 */
    @Select("SELECT oi2.product_id AS productId, COUNT(DISTINCT o_other.user_id) AS cnt " +
            "FROM order_item oi_me " +
            "INNER JOIN `order` o_me ON oi_me.order_id = o_me.id AND o_me.user_id = #{userId} AND o_me.pay_status = 1 " +
            "INNER JOIN order_item oi_other ON oi_other.product_id = oi_me.product_id " +
            "INNER JOIN `order` o_other ON oi_other.order_id = o_other.id AND o_other.user_id <> #{userId} AND o_other.pay_status = 1 " +
            "INNER JOIN order_item oi2 ON oi2.order_id = o_other.id " +
            "INNER JOIN product p ON oi2.product_id = p.id AND p.status = 1 " +
            "WHERE oi2.product_id NOT IN (" +
            "  SELECT DISTINCT oi.product_id FROM order_item oi " +
            "  INNER JOIN `order` o ON oi.order_id = o.id WHERE o.user_id = #{userId}" +
            ") " +
            "GROUP BY oi2.product_id ORDER BY cnt DESC LIMIT #{limit}")
    List<Map<String, Object>> findSimilarUserProducts(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT * FROM product WHERE status = 1")
    List<Product> findAllOnSale();

    @Select("SELECT product_id AS productId, AVG(rating) AS avgRating, COUNT(*) AS reviewCount " +
            "FROM review WHERE status = 1 GROUP BY product_id")
    List<Map<String, Object>> findReviewStats();

    @Select("<script>SELECT * FROM product WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Product> findByIds(@Param("ids") List<Long> ids);
}
