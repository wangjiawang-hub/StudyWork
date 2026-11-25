package goods.mapper;

import model.Product;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper {

    @Select("SELECT * FROM product WHERE status = 1 ORDER BY sales DESC LIMIT #{limit}")
    List<Product> findHot(@Param("limit") int limit);

    @Select("SELECT DISTINCT category_id AS categoryId, category_name AS categoryName FROM product WHERE status=1 AND category_id IS NOT NULL")
    List<Map<String, Object>> listCategories();

    @Select("SELECT * FROM product WHERE status = 1 ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<Product> findPage(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM product WHERE status = 1")
    long countOnSale();

    @Select("SELECT * FROM product WHERE id = #{id}")
    Product findById(Long id);

    @Select("<script>SELECT * FROM product WHERE status = 1 " +
            "<if test='keyword!=null and keyword!=\"\"'> AND name LIKE CONCAT('%',#{keyword},'%')</if>" +
            "<if test='categoryId!=null'> AND category_id = #{categoryId}</if>" +
            " ORDER BY sales DESC LIMIT #{offset}, #{size}</script>")
    List<Product> search(@Param("keyword") String keyword, @Param("categoryId") Integer categoryId,
                         @Param("offset") int offset, @Param("size") int size);

    @Select("<script>SELECT COUNT(*) FROM product WHERE status = 1 " +
            "<if test='keyword!=null and keyword!=\"\"'> AND name LIKE CONCAT('%',#{keyword},'%')</if>" +
            "<if test='categoryId!=null'> AND category_id = #{categoryId}</if></script>")
    long searchCount(@Param("keyword") String keyword, @Param("categoryId") Integer categoryId);

    @Insert("INSERT INTO product(name,category_id,category_name,price,original_price,stock,image,description,status) " +
            "VALUES(#{name},#{categoryId},#{categoryName},#{price},#{originalPrice},#{stock},#{image},#{description},#{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Update("UPDATE product SET name=#{name},category_id=#{categoryId},category_name=#{categoryName}," +
            "price=#{price},stock=#{stock},image=#{image},description=#{description},status=#{status} WHERE id=#{id}")
    int update(Product product);

    @Update("UPDATE product SET stock = stock - #{qty} WHERE id = #{id} AND stock >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);
}
