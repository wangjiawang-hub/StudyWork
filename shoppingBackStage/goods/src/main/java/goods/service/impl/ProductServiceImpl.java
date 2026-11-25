package goods.service.impl;

import goods.mapper.ProductMapper;
import goods.mapper.SkuMapper;
import goods.service.ProductService;
import goods.service.RecommendationService;
import model.PageResult;
import model.Product;
import model.Sku;
import model.SkuSpec;
import model.SkuVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private RecommendationService recommendationService;
    @Autowired
    private SkuMapper skuMapper;

    @Override
    public PageResult<Product> page(int page, int size) {
        int offset = (page - 1) * size;
        return PageResult.of(productMapper.findPage(offset, size), productMapper.countOnSale(), page, size);
    }

    @Override
    public Product detail(Long id) {
        return productMapper.findById(id);
    }

    @Override
    public PageResult<Product> search(String keyword, Integer categoryId, int page, int size) {
        int offset = (page - 1) * size;
        List<Product> list = productMapper.search(keyword, categoryId, offset, size);
        long total = productMapper.searchCount(keyword, categoryId);
        return PageResult.of(list, total, page, size);
    }

    /** 智能推荐：登录个性化 + 未登录多因子综合排序 */
    @Override
    public List<Product> recommend(Long userId, int limit) {
        return recommendationService.recommend(userId, limit);
    }

    @Override
    public List<Product> hot(int limit) {
        return productMapper.findHot(limit);
    }

    @Override
    public List<Map<String, Object>> categories() {
        return productMapper.listCategories();
    }

    @Override
    public void save(Product product) {
        productMapper.insert(product);
    }

    @Override
    public void update(Product product) {
        productMapper.update(product);
    }

    /** 查询商品的所有 SKU + 关联规格值，聚合为 SkuVO 列表 */
    @Override
    public List<SkuVO> getSkus(Long productId) {
        List<Sku> skus = skuMapper.findByProductId(productId);
        if (skus.isEmpty()) return Collections.emptyList();
        List<SkuSpec> allSpecs = skuMapper.findAllSpecsByProductId(productId);
        Map<Long, List<SkuSpec>> specMap = allSpecs.stream()
                .collect(Collectors.groupingBy(SkuSpec::getSkuId));
        List<SkuVO> result = new ArrayList<>();
        for (Sku sku : skus) {
            SkuVO vo = new SkuVO();
            vo.setId(sku.getId());
            vo.setProductId(sku.getProductId());
            vo.setSkuCode(sku.getSkuCode());
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock());
            vo.setStatus(sku.getStatus());
            vo.setSpecs(specMap.getOrDefault(sku.getId(), Collections.emptyList()));
            result.add(vo);
        }
        return result;
    }
}
