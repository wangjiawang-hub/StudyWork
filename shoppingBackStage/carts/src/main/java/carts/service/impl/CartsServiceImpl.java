package carts.service.impl;

import carts.mapper.CartMapper;
import carts.service.CartsService;
import model.Cart;
import model.Result;
import model.ResultCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartsServiceImpl implements CartsService {
    @Autowired
    private CartMapper cartMapper;
    /** 同一用户同一商品同一SKU重复加购：累加数量，不新增行 */
    @Override
    public Result<String> add(Long userId, Long productId, int quantity, Long skuId, String skuSpec) {
        String skuType = skuId == null ? "noSku" : "hasSku";
        Cart exist = cartMapper.findByUserAndProductSku(userId, productId, skuId, skuType);
        if (exist != null) {
            cartMapper.updateQuantity(exist.getId(), exist.getQuantity() + quantity);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setSkuId(skuId);
            cart.setSkuSpec(skuSpec);
            cart.setQuantity(quantity);
            cartMapper.insert(cart);
        }
        return Result.success("加入购物车成功");
    }

    @Override
    public List<Cart> list(Long userId) {
        return cartMapper.findByUserId(userId);
    }

    @Override
    public Result<Void> delete(Long userId, Long id) {
        cartMapper.delete(id, userId);
        return Result.success();
    }

    @Override
    public Result<Void> update(Long userId, Long id, int quantity) {
        cartMapper.updateQuantity(id, quantity);
        return Result.success();
    }

    /** 供 order 服务 Feign 调用：按 cartIds 取结算子集 */
    @Override
    public Result<List<Cart>> subList(Long userId, List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            return Result.failure(ResultCodeEnum.FAIL, "购物车项为空");
        }
        String ids = cartIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return Result.success(cartMapper.findByIds(userId, ids));
    }

    @Override
    public Result<Void> deleteList(Long userId, List<Long> cartIds) {
        if (cartIds != null && !cartIds.isEmpty()) {
            String ids = cartIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            cartMapper.deleteByIds(userId, ids);
        }
        return Result.success();
    }
}
