package goods.mapper;

import model.Sku;
import model.SkuSpec;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SkuMapper {

    /** 查询商品的所有启用 SKU */
    @Select("SELECT * FROM sku WHERE product_id = #{productId} AND status = 1 ORDER BY id")
    List<Sku> findByProductId(Long productId);

    /** 查询某 SKU 的所有规格项 */
    @Select("SELECT * FROM sku_spec WHERE sku_id = #{skuId}")
    List<SkuSpec> findSpecsBySkuId(Long skuId);

    /** 查询商品所有 SKU 的所有规格项（一次查全部，减少 N+1） */
    @Select("SELECT ss.* FROM sku_spec ss INNER JOIN sku s ON ss.sku_id = s.id WHERE s.product_id = #{productId}")
    List<SkuSpec> findAllSpecsByProductId(Long productId);

    /** 根据 ID 查 SKU */
    @Select("SELECT * FROM sku WHERE id = #{id}")
    Sku findById(Long id);
}
