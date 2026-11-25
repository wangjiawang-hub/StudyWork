package aiservice.service;

import aiservice.client.CartsFeignClient;
import aiservice.client.GoodsFeignClient;
import dev.langchain4j.agent.tool.Tool;
import model.PageResult;
import model.Product;
import model.Result;
import model.SkuVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 加购工具集：langchain4j 通过 @Tool 注解识别，模型自动决定调用。
 * 工具执行结果以字符串返回给模型，模型据此生成最终回复。
 * uid 通过 ThreadLocal 传入（加购需要当前用户身份）。
 */
@Component
public class CartTools {
    private static final Logger log = LoggerFactory.getLogger(CartTools.class);

    @Autowired
    private GoodsFeignClient goodsFeignClient;
    @Autowired
    private CartsFeignClient cartsFeignClient;

    /** 当前请求的用户 ID，由 ShoppingAgentService 在调用前设置 */
    public static final ThreadLocal<String> CURRENT_UID = new ThreadLocal<>();

    /** 搜索商品：返回商品列表文本（id、名称、价格），供模型选择 */
    @Tool("根据关键词搜索商品，返回匹配的商品列表（含商品ID、名称、价格）")
    public String searchProducts(String keyword) {
        try {
            Result<PageResult<Product>> res = goodsFeignClient.search(keyword, null, 1, 5);
            if (res == null || res.getData() == null) return "搜索失败";
            List<Product> records = res.getData().getRecords();
            if (records == null || records.isEmpty()) return "未找到相关商品";
            StringBuilder sb = new StringBuilder("找到以下商品：\n");
            for (Product p : records) {
                sb.append("- ID=").append(p.getId())
                  .append(" 名称=").append(p.getName())
                  .append(" 价格=").append(p.getPrice()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("AI 工具 searchProducts 失败：{}", e.getMessage());
            return "搜索失败：" + e.getMessage();
        }
    }

    /** 加入购物车：quantity 默认 1；无 SKU 时取第一个可用 SKU */
    @Tool("将商品加入购物车。参数：productId 商品ID，quantity 数量")
    public String addToCart(long productId, int quantity) {
        String uid = CURRENT_UID.get();
        if (uid == null || uid.isBlank()) return "未登录，无法加购";
        if (quantity < 1) quantity = 1;
        try {
            // 查第一个可用 SKU，带规格快照
            Long skuId = null;
            String skuSpec = null;
            Result<List<SkuVO>> skuRes = goodsFeignClient.skus(productId);
            if (skuRes != null && skuRes.getData() != null && !skuRes.getData().isEmpty()) {
                SkuVO sku = skuRes.getData().get(0);
                skuId = sku.getId();
                if (sku.getSpecs() != null && !sku.getSpecs().isEmpty()) {
                    StringBuilder spec = new StringBuilder();
                    sku.getSpecs().forEach(s -> spec.append(s.getSpecName()).append(":").append(s.getSpecValue()).append(" "));
                    skuSpec = spec.toString().trim();
                }
            }
            Map<String, Object> body = new HashMap<>();
            body.put("productId", productId);
            body.put("quantity", quantity);
            if (skuId != null) body.put("skuId", skuId);
            if (skuSpec != null) body.put("skuSpec", skuSpec);
            Result<String> res = cartsFeignClient.add(uid, body);
            if (res != null && res.getCode() != null && res.getCode() == 200) {
                return "加购成功（数量=" + quantity + "）";
            }
            return "加购失败：" + (res == null ? "无响应" : res.getMessage());
        } catch (Exception e) {
            log.warn("AI 工具 addToCart 失败：{}", e.getMessage());
            return "加购失败：" + e.getMessage();
        }
    }
}
