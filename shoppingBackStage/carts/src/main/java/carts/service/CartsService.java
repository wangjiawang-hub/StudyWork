package carts.service;

import model.Cart;
import model.Result;

import java.util.List;

public interface CartsService {
    Result<String> add(Long userId, Long productId, int quantity, Long skuId, String skuSpec);
    List<Cart> list(Long userId);
    Result<Void> delete(Long userId, Long id);
    Result<Void> update(Long userId, Long id, int quantity);
    Result<List<Cart>> subList(Long userId, List<Long> cartIds);
    Result<Void> deleteList(Long userId, List<Long> cartIds);
}
