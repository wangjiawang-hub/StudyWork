package carts.mapper;

import model.Cart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CartMapper {

    @Select("SELECT c.*, p.name AS product_name, p.image AS product_image, " +
            "COALESCE(s.price, p.price) AS price, COALESCE(s.stock, p.stock) AS stock " +
            "FROM cart c " +
            "LEFT JOIN product p ON c.product_id = p.id " +
            "LEFT JOIN sku s ON c.sku_id = s.id " +
            "WHERE c.user_id = #{userId}")
    List<Cart> findByUserId(Long userId);

    @Select("SELECT c.*, p.name AS product_name, p.image AS product_image, " +
            "COALESCE(s.price, p.price) AS price, COALESCE(s.stock, p.stock) AS stock " +
            "FROM cart c " +
            "LEFT JOIN product p ON c.product_id = p.id " +
            "LEFT JOIN sku s ON c.sku_id = s.id " +
            "WHERE c.user_id = #{userId} AND c.id IN (${ids})")
    List<Cart> findByIds(@Param("userId") Long userId, @Param("ids") String ids);

    @Select("SELECT * FROM cart WHERE user_id = #{userId} AND product_id = #{productId} " +
            "AND #{skuType} = (CASE WHEN sku_id IS NULL THEN 'noSku' ELSE 'hasSku' END) " +
            "AND (#{skuId} IS NULL OR sku_id = #{skuId})")
    Cart findByUserAndProductSku(@Param("userId") Long userId, @Param("productId") Long productId,
                                 @Param("skuId") Long skuId, @Param("skuType") String skuType);

    @Insert("INSERT INTO cart(user_id, product_id, sku_id, sku_spec, quantity, selected) " +
            "VALUES(#{userId},#{productId},#{skuId},#{skuSpec},#{quantity},1)")
    int insert(Cart cart);

    @Update("UPDATE cart SET quantity = #{quantity} WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("quantity") int quantity);

    @Update("UPDATE cart SET selected = #{selected} WHERE id = #{id} AND user_id = #{userId}")
    int updateSelected(@Param("id") Long id, @Param("userId") Long userId, @Param("selected") int selected);

    @Delete("DELETE FROM cart WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("DELETE FROM cart WHERE user_id = #{userId} AND id IN (${ids})")
    int deleteByIds(@Param("userId") Long userId, @Param("ids") String ids);
}
