package goods.service.impl;

import goods.mapper.RecommendationMapper;
import goods.service.RecommendationService;
import model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final double W_SALES = 0.35;
    private static final double W_RATING = 0.25;
    private static final double W_REVIEWS = 0.20;
    private static final double W_RECENCY = 0.15;
    private static final double W_DISCOUNT = 0.05;

    @Autowired
    private RecommendationMapper recommendationMapper;

    @Override
    public List<Product> recommend(Long userId, int limit) {
        int fetchLimit = Math.max(limit, 1);
        Map<Long, ReviewStat> reviewStats = loadReviewStats();
        List<Product> allOnSale = recommendationMapper.findAllOnSale();
        if (allOnSale.isEmpty()) {
            return List.of();
        }

        Map<Long, Double> globalScores = buildGlobalScores(allOnSale, reviewStats);

        if (userId == null) {
            return pickTopProducts(allOnSale, globalScores, fetchLimit);
        }

        Set<Long> excluded = new HashSet<>(recommendationMapper.findUserInteractedProductIds(userId));
        Map<Long, Double> scores = new LinkedHashMap<>();

        // 1. 品类偏好：在用户偏好的分类里按全局质量分加权
        List<Map<String, Object>> categoryPrefs = recommendationMapper.findUserCategoryPreferences(userId);
        double maxCatScore = categoryPrefs.stream()
                .mapToDouble(m -> toDouble(m.get("score")))
                .max().orElse(1.0);
        Map<Integer, Double> catWeight = new HashMap<>();
        for (Map<String, Object> pref : categoryPrefs) {
            Integer catId = toInt(pref.get("categoryId"));
            if (catId != null) {
                catWeight.put(catId, toDouble(pref.get("score")) / maxCatScore);
            }
        }
        for (Product p : allOnSale) {
            if (excluded.contains(p.getId())) continue;
            Double cw = catWeight.get(p.getCategoryId());
            if (cw != null && cw > 0) {
                scores.merge(p.getId(), cw * 0.45 + globalScores.getOrDefault(p.getId(), 0.0) * 0.15, Double::sum);
            }
        }

        // 2. 同单搭配购买
        addRankedSignal(scores, recommendationMapper.findCoPurchaseProducts(userId, fetchLimit * 3),
                excluded, 0.35, "cnt");

        // 3. 协同过滤：相似用户购买
        addRankedSignal(scores, recommendationMapper.findSimilarUserProducts(userId, fetchLimit * 3),
                excluded, 0.30, "cnt");

        // 4. 不足时用全局分补齐，保证多样性（按品类轮询）
        if (scores.size() < fetchLimit) {
            List<Product> globalRanked = pickTopProducts(allOnSale, globalScores, allOnSale.size());
            Set<Integer> usedCategories = new HashSet<>();
            for (Product p : globalRanked) {
                if (scores.size() >= fetchLimit * 2) break;
                if (excluded.contains(p.getId()) || scores.containsKey(p.getId())) continue;
                Integer cat = p.getCategoryId();
                if (cat != null && usedCategories.contains(cat) && scores.size() >= fetchLimit) continue;
                scores.put(p.getId(), globalScores.getOrDefault(p.getId(), 0.0));
                if (cat != null) usedCategories.add(cat);
            }
        }

        if (scores.isEmpty()) {
            return pickTopProducts(allOnSale, globalScores, fetchLimit);
        }

        List<Long> topIds = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(fetchLimit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return orderProducts(recommendationMapper.findByIds(topIds), topIds);
    }

    private void addRankedSignal(Map<Long, Double> scores, List<Map<String, Object>> ranked,
                                 Set<Long> excluded, double weight, String countKey) {
        if (ranked == null || ranked.isEmpty()) return;
        int n = ranked.size();
        for (int i = 0; i < n; i++) {
            Long productId = toLong(ranked.get(i).get("productId"));
            if (productId == null || excluded.contains(productId)) continue;
            double rankScore = (n - i) / (double) n;
            double countBoost = Math.min(toDouble(ranked.get(i).get(countKey)) / 5.0, 1.0);
            scores.merge(productId, weight * (rankScore * 0.7 + countBoost * 0.3), Double::sum);
        }
    }

    private Map<Long, ReviewStat> loadReviewStats() {
        Map<Long, ReviewStat> map = new HashMap<>();
        for (Map<String, Object> row : recommendationMapper.findReviewStats()) {
            Long pid = toLong(row.get("productId"));
            if (pid != null) {
                map.put(pid, new ReviewStat(toDouble(row.get("avgRating")), toInt(row.get("reviewCount"))));
            }
        }
        return map;
    }

    /** 多因子全局分：销量 + 评分 + 评论数 + 新鲜度 + 折扣力度 */
    private Map<Long, Double> buildGlobalScores(List<Product> products, Map<Long, ReviewStat> reviewStats) {
        int maxSales = products.stream()
                .mapToInt(p -> p.getSales() != null ? p.getSales() : 0)
                .max().orElse(1);
        Map<Long, Double> scores = new HashMap<>();
        long now = System.currentTimeMillis();
        for (Product p : products) {
            ReviewStat rs = reviewStats.getOrDefault(p.getId(), ReviewStat.DEFAULT);
            double salesNorm = (p.getSales() != null ? p.getSales() : 0) / (double) maxSales;
            double ratingNorm = rs.avgRating / 5.0;
            double reviewNorm = Math.min(rs.reviewCount / 20.0, 1.0);
            double recencyNorm = 1.0;
            if (p.getCreateTime() != null) {
                long days = Math.max(0, (now - p.getCreateTime().getTime()) / (86400000L));
                recencyNorm = 1.0 / (1.0 + days / 30.0);
            }
            double discountNorm = 0.0;
            if (p.getOriginalPrice() != null && p.getPrice() != null
                    && p.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0
                    && p.getOriginalPrice().compareTo(p.getPrice()) > 0) {
                discountNorm = p.getOriginalPrice().subtract(p.getPrice())
                        .divide(p.getOriginalPrice(), 4, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            double score = W_SALES * salesNorm + W_RATING * ratingNorm + W_REVIEWS * reviewNorm
                    + W_RECENCY * recencyNorm + W_DISCOUNT * discountNorm;
            scores.put(p.getId(), score);
        }
        return scores;
    }

    private List<Product> pickTopProducts(List<Product> products, Map<Long, Double> scores, int limit) {
        return products.stream()
                .sorted(Comparator.comparingDouble((Product p) -> scores.getOrDefault(p.getId(), 0.0)).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<Product> orderProducts(List<Product> products, List<Long> orderedIds) {
        Map<Long, Product> map = products.stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        List<Product> result = new ArrayList<>();
        for (Long id : orderedIds) {
            Product p = map.get(id);
            if (p != null) result.add(p);
        }
        return result;
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.parseLong(v.toString());
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(v.toString());
    }

    private static double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        return Double.parseDouble(v.toString());
    }

    private static class ReviewStat {
        static final ReviewStat DEFAULT = new ReviewStat(3.0, 0);
        final double avgRating;
        final int reviewCount;

        ReviewStat(double avgRating, int reviewCount) {
            this.avgRating = avgRating;
            this.reviewCount = reviewCount;
        }
    }
}
