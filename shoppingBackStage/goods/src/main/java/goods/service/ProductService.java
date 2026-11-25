package goods.service;

import model.PageResult;
import model.Product;
import model.SkuVO;

import java.util.List;
import java.util.Map;

public interface ProductService {
    PageResult<Product> page(int page, int size);
    Product detail(Long id);
    PageResult<Product> search(String keyword, Integer categoryId, int page, int size);
    List<Product> recommend(Long userId, int limit);
    List<Product> hot(int limit);
    List<Map<String, Object>> categories();
    void save(Product product);
    void update(Product product);
    List<SkuVO> getSkus(Long productId);
}
