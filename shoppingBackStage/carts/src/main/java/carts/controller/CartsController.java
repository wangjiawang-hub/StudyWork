package carts.controller;

import carts.service.CartsService;
import model.Cart;
import model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
public class CartsController {
    @Autowired
    private CartsService cartsService;

    @PostMapping("/add")
    public Result<String> add(@RequestHeader("uid") String uid, @RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        int quantity = body.get("quantity") == null ? 1 : Integer.parseInt(body.get("quantity").toString());
        Long skuId = body.get("skuId") != null ? Long.valueOf(body.get("skuId").toString()) : null;
        String skuSpec = body.get("skuSpec") != null ? body.get("skuSpec").toString() : null;
        return cartsService.add(Long.parseLong(uid), productId, quantity, skuId, skuSpec);
    }

    @GetMapping("/list")
    public Result<List<Cart>> list(@RequestHeader("uid") String uid) {
        return Result.success(cartsService.list(Long.parseLong(uid)));
    }

    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestHeader("uid") String uid, @RequestParam Long id) {
        return cartsService.delete(Long.parseLong(uid), id);
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestHeader("uid") String uid, @RequestBody Map<String, Object> body) {
        return cartsService.update(Long.parseLong(uid), Long.valueOf(body.get("id").toString()),
                Integer.parseInt(body.get("quantity").toString()));
    }

    @PostMapping("/sublist")
    public Result<List<Cart>> subList(@RequestHeader("uid") String uid, @RequestBody Map<String, List<Long>> body) {
        return cartsService.subList(Long.parseLong(uid), body.get("cartIds"));
    }

    @PostMapping("/deletelist")
    public Result<Void> deleteList(@RequestHeader("uid") String uid, @RequestBody Map<String, List<Long>> body) {
        return cartsService.deleteList(Long.parseLong(uid), body.get("cartIds"));
    }
}
