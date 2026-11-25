package goods.service;

import model.Product;

import java.util.List;

public interface RecommendationService {

    /**
     * 智能推荐：登录用户走个性化 + 协同过滤；未登录走多因子综合排序。
     */
    List<Product> recommend(Long userId, int limit);
}
