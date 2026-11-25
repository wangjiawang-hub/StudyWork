package aiservice.client;

import model.PageResult;
import model.Product;
import model.Result;
import model.SkuVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * AI 加购智能体调用商品服务：搜索商品、查 SKU。
 * goods-server 对外路径前缀 /goods；search 为白名单免登录接口。
 */
@FeignClient(name = "goods-server")
public interface GoodsFeignClient {

    /** 按关键词搜索商品（供 AI 找到用户想要的商品 id） */
    @GetMapping("/goods/search")
    Result<PageResult<Product>> search(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer categoryId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "5") int size);

    /** 查询商品的 SKU 列表（含规格），用于找到第一个可用 SKU 自动加购 */
    @GetMapping("/goods/skus")
    Result<List<SkuVO>> skus(@RequestParam Long productId);
}
