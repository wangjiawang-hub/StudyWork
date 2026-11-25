package userservice.controller;

import model.Address;
import model.Favorite;
import model.Result;
import model.ResultCodeEnum;
import model.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import userservice.mapper.UserBizMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
/** 用户业务：地址、收藏、评价（网关注入 uid，SQL 均带 user_id 防越权） */
public class UserBizController {
    @Autowired
    private UserBizMapper mapper;

    @GetMapping("/address/list")
    public Result<List<Address>> addressList(@RequestHeader("uid") String uid) {
        return Result.success(mapper.listAddress(Long.parseLong(uid)));
    }

    /** 新增/更新地址：首地址自动默认；设默认时先 clearDefault 保证互斥 */
    @PostMapping("/address")
    public Result<String> saveAddress(@RequestHeader("uid") String uid, @RequestBody Address address) {
        Long userId = Long.parseLong(uid);
        address.setUserId(userId);
        if (address.getId() == null && mapper.countAddress(userId) == 0) {
            address.setIsDefault(1);
        }
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            mapper.clearDefault(userId);
        }
        if (address.getId() == null) mapper.insertAddress(address);
        else mapper.updateAddress(address);
        return Result.success("保存成功");
    }

    @PutMapping("/address/default")
    public Result<String> setDefaultAddress(@RequestHeader("uid") String uid, @RequestParam Long id) {
        Long userId = Long.parseLong(uid);
        mapper.clearDefault(userId);
        if (mapper.markDefault(id, userId) <= 0) {
            return Result.failure(ResultCodeEnum.FAIL, "地址不存在");
        }
        return Result.success("已设为默认地址");
    }

    /** 删除默认地址后，自动将最新一条提升为默认 */
    @DeleteMapping("/address")
    public Result<Void> deleteAddress(@RequestHeader("uid") String uid, @RequestParam Long id) {
        Long userId = Long.parseLong(uid);
        List<Address> before = mapper.listAddress(userId);
        boolean wasDefault = before.stream().anyMatch(a -> a.getId().equals(id) && a.getIsDefault() != null && a.getIsDefault() == 1);
        mapper.deleteAddress(id, userId);
        if (wasDefault) {
            Long latestId = mapper.findLatestAddressId(userId);
            if (latestId != null) {
                mapper.clearDefault(userId);
                mapper.markDefault(latestId, userId);
            }
        }
        return Result.success();
    }

    @GetMapping("/favorite/list")
    public Result<List<Favorite>> favoriteList(@RequestHeader("uid") String uid) {
        return Result.success(mapper.listFavorite(Long.parseLong(uid)));
    }

    @PostMapping("/favorite")
    public Result<String> addFavorite(@RequestHeader("uid") String uid, @RequestParam Long productId) {
        mapper.addFavorite(Long.parseLong(uid), productId);
        return Result.success("收藏成功");
    }

    @DeleteMapping("/favorite")
    public Result<Void> removeFavorite(@RequestHeader("uid") String uid, @RequestParam Long productId) {
        mapper.removeFavorite(Long.parseLong(uid), productId);
        return Result.success();
    }

    @GetMapping("/favorite/check")
    public Result<Map<String, Boolean>> checkFavorite(@RequestHeader("uid") String uid, @RequestParam Long productId) {
        Map<String, Boolean> m = new HashMap<>();
        m.put("favorite", mapper.isFavorite(Long.parseLong(uid), productId) > 0);
        return Result.success(m);
    }

    /** 提交评价：须已完成订单、商品在订单内、每商品仅评一次 */
    @PostMapping("/review")
    public Result<String> addReview(@RequestHeader("uid") String uid, @RequestBody Review review) {
        Long userId = Long.parseLong(uid);
        review.setUserId(userId);
        if (review.getProductId() == null) {
            return Result.failure(ResultCodeEnum.FAIL, "商品不存在");
        }
        if (review.getOrderId() == null) {
            return Result.failure(ResultCodeEnum.FAIL, "请从已完成订单中评价");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            return Result.failure(ResultCodeEnum.FAIL, "请选择1-5星评分");
        }
        if (!StringUtils.hasText(review.getContent())) {
            return Result.failure(ResultCodeEnum.FAIL, "请输入评价内容");
        }
        if (mapper.findCompletedOrder(review.getOrderId(), userId) == null) {
            return Result.failure(ResultCodeEnum.FAIL, "仅已完成订单可评价");
        }
        if (mapper.findOrderItem(review.getOrderId(), review.getProductId()) == null) {
            return Result.failure(ResultCodeEnum.FAIL, "该商品不在订单中");
        }
        if (mapper.countReviewForOrderProduct(userId, review.getOrderId(), review.getProductId()) > 0) {
            return Result.failure(ResultCodeEnum.FAIL, "该商品已评价");
        }
        mapper.insertReview(review);
        return Result.success("评价成功");
    }

    @GetMapping("/review/order-items")
    public Result<List<Map<String, Object>>> orderReviewItems(@RequestHeader("uid") String uid,
                                                              @RequestParam Long orderId) {
        Long userId = Long.parseLong(uid);
        if (mapper.findCompletedOrder(orderId, userId) == null) {
            return Result.failure(ResultCodeEnum.FAIL, "订单不存在或未完成");
        }
        return Result.success(mapper.listOrderReviewItems(orderId, userId));
    }

    @GetMapping("/review/list")
    public Result<List<Review>> reviewList(@RequestParam Long productId) {
        return Result.success(mapper.listReview(productId));
    }

    @GetMapping("/review/mine")
    public Result<List<Review>> myReviewList(@RequestHeader("uid") String uid) {
        return Result.success(mapper.listMyReview(Long.parseLong(uid)));
    }
}
