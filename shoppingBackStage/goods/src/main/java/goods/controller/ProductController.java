package goods.controller;

import goods.service.ProductService;
import model.PageResult;
import model.Product;
import model.Result;
import model.SkuVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goods")
/** 商品浏览 API（白名单免登录）：列表、搜索、详情、热销、推荐 */
public class ProductController {
    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    public Result<PageResult<Product>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return Result.success(productService.page(page, size));
    }

    @GetMapping("/detail")
    public Result<Product> detail(@RequestParam Long id) {
        return Result.success(productService.detail(id));
    }

    @GetMapping("/search")
    public Result<PageResult<Product>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return Result.success(productService.search(keyword, categoryId, page, size));
    }

    @GetMapping("/recommend")
    public Result<List<Product>> recommend(
            @RequestHeader(value = "uid", required = false) String uid,
            @RequestParam(defaultValue = "8") int limit) {
        Long userId = parseUserId(uid);
        return Result.success(productService.recommend(userId, limit));
    }

    private Long parseUserId(String uid) {
        if (uid == null || uid.isBlank()) return null;
        try {
            return Long.parseLong(uid.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @GetMapping("/hot")
    public Result<List<Product>> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.success(productService.hot(limit));
    }

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        return Result.success(productService.categories());
    }

    /** 查询商品的 SKU 列表（含规格值），供详情页渲染规格选择 */
    @GetMapping("/skus")
    public Result<List<SkuVO>> skus(@RequestParam Long productId) {
        return Result.success(productService.getSkus(productId));
    }
}
