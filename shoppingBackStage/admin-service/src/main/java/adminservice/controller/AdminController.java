package adminservice.controller;

import adminservice.mapper.AdminMapper;
import adminservice.service.AdminUserService;
import adminservice.service.CategoryConfigService;
import adminservice.service.MallConfigService;
import model.Coupon;
import model.Order;
import model.PageResult;
import model.Product;
import model.Result;
import model.ResultCodeEnum;
import model.Sku;
import model.SkuSpec;
import model.SkuVO;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import utils.PasswordUtil;
import utils.AuthValidation;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
/** 后台管理：用户/分类/商品/订单/配置/日志；除 /config/public 外均需 role=1 */
public class AdminController {
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private CategoryConfigService categoryConfigService;
    @Autowired
    private AdminUserService adminUserService;
    @Autowired
    private MallConfigService mallConfigService;

    private boolean isAdmin(String role) {
        return "1".equals(role);
    }

    private <T> Result<T> forbidden() {
        return Result.failure(ResultCodeEnum.FAIL, "无管理员权限");
    }

    private void writeLog(String uid, String operation, String method, String params) {
        Long userId = StringUtils.hasText(uid) ? Long.parseLong(uid) : null;
        adminMapper.insertLog(userId, operation, method, params, null);
    }

    @GetMapping("/users")
    public Result<PageResult<User>> users(@RequestHeader(value = "role", defaultValue = "0") String role,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String keyword) {
        if (!isAdmin(role)) return forbidden();
        int offset = (page - 1) * size;
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<User> records = adminMapper.listUsers(offset, size, kw);
        records.forEach(u -> u.setPassword(null));
        long total = adminMapper.countUsers(kw);
        return Result.success(PageResult.of(records, total, page, size));
    }

    @PostMapping("/users")
    public Result<String> createUser(@RequestHeader(value = "role", defaultValue = "0") String role,
                                     @RequestHeader(value = "uid", required = false) String uid,
                                     @RequestBody User user) {
        if (!isAdmin(role)) return forbidden();
        Result<Void> check = validateUserForSave(user, null);
        if (check != null) return Result.failure(ResultCodeEnum.FAIL, check.getMessage());
        if (!StringUtils.hasText(user.getPassword())) {
            return Result.failure(ResultCodeEnum.FAIL, "密码不能为空");
        }
        if (user.getPassword().length() < 6) {
            return Result.failure(ResultCodeEnum.FAIL, "密码至少6位");
        }
        if (adminMapper.findUserByUsername(user.getUsername()) != null) {
            return Result.failure(ResultCodeEnum.FAIL, "用户名已存在");
        }
        user.setPassword(PasswordUtil.encode(user.getPassword()));
        if (user.getRole() == null) user.setRole(0);
        if (user.getStatus() == null) user.setStatus(1);
        adminMapper.insertUser(user);
        writeLog(uid, "新增用户", "POST", "username=" + user.getUsername());
        return Result.success("创建成功");
    }

    @PutMapping("/users")
    public Result<Void> updateUser(@RequestHeader(value = "role", defaultValue = "0") String role,
                                   @RequestHeader(value = "uid", required = false) String uid,
                                   @RequestBody User user) {
        if (!isAdmin(role)) return forbidden();
        if (user.getId() == null) {
            return Result.failure(ResultCodeEnum.FAIL, "用户 ID 不能为空");
        }
        if (adminMapper.countUserById(user.getId()) <= 0) {
            return Result.failure(ResultCodeEnum.FAIL, "用户不存在");
        }
        Result<Void> check = validateUserForSave(user, user.getId());
        if (check != null) return check;
        adminMapper.updateUser(user);
        writeLog(uid, "更新用户", "PUT", "id=" + user.getId());
        return Result.success();
    }

    /** 校验用户名、手机、邮箱格式及唯一性；空手机/邮箱会转为 null */
    private Result<Void> validateUserForSave(User user, Long excludeUserId) {
        user.setUsername(AuthValidation.trimToNull(user.getUsername()));
        user.setPhone(AuthValidation.trimToNull(user.getPhone()));
        user.setEmail(AuthValidation.trimToNull(user.getEmail()));
        if (!StringUtils.hasText(user.getUsername())) {
            return Result.failure(ResultCodeEnum.FAIL, "用户名不能为空");
        }
        if (!AuthValidation.isUsername(user.getUsername())) {
            return Result.failure(ResultCodeEnum.FAIL, "用户名为2-20位，支持中文、字母、数字、下划线");
        }
        if (user.getPhone() != null && !AuthValidation.isPhone(user.getPhone())) {
            return Result.failure(ResultCodeEnum.FAIL, "请输入正确的11位手机号");
        }
        if (user.getEmail() != null && !AuthValidation.isEmail(user.getEmail())) {
            return Result.failure(ResultCodeEnum.FAIL, "请输入正确的邮箱地址");
        }
        User byName = adminMapper.findUserByUsername(user.getUsername());
        if (byName != null && (excludeUserId == null || !excludeUserId.equals(byName.getId()))) {
            return Result.failure(ResultCodeEnum.FAIL, "用户名已存在");
        }
        if (user.getPhone() != null) {
            User byPhone = adminMapper.findUserByPhone(user.getPhone());
            if (byPhone != null && (excludeUserId == null || !excludeUserId.equals(byPhone.getId()))) {
                return Result.failure(ResultCodeEnum.FAIL, "手机号已被使用");
            }
        }
        if (user.getEmail() != null) {
            User byEmail = adminMapper.findUserByEmail(user.getEmail());
            if (byEmail != null && (excludeUserId == null || !excludeUserId.equals(byEmail.getId()))) {
                return Result.failure(ResultCodeEnum.FAIL, "邮箱已被使用");
            }
        }
        return null;
    }

    @DeleteMapping("/users")
    public Result<Void> deleteUser(@RequestHeader(value = "role", defaultValue = "0") String role,
                                   @RequestHeader(value = "uid", required = false) String uid,
                                   @RequestParam Long id) {
        if (!isAdmin(role)) return forbidden();
        if (StringUtils.hasText(uid) && id.toString().equals(uid)) {
            return Result.failure(ResultCodeEnum.FAIL, "不能删除当前登录账号");
        }
        if (!adminUserService.deleteUser(id)) {
            return Result.failure(ResultCodeEnum.FAIL, "用户不存在");
        }
        writeLog(uid, "删除用户", "DELETE", "id=" + id);
        return Result.success();
    }

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories(@RequestHeader(value = "role", defaultValue = "0") String role) {
        if (!isAdmin(role)) return forbidden();
        return Result.success(categoryConfigService.listCategories());
    }

    @PostMapping("/categories")
    public Result<Map<String, Object>> saveCategory(@RequestHeader(value = "role", defaultValue = "0") String role,
                                                    @RequestHeader(value = "uid", required = false) String uid,
                                                    @RequestBody Map<String, Object> body) {
        if (!isAdmin(role)) return forbidden();
        try {
            Integer id = body.get("categoryId") == null ? null : ((Number) body.get("categoryId")).intValue();
            String name = body.get("categoryName") == null ? null : body.get("categoryName").toString();
            Map<String, Object> saved = categoryConfigService.saveCategory(id, name);
            writeLog(uid, id == null ? "新增分类" : "更新分类", "POST", String.valueOf(saved));
            return Result.success(saved);
        } catch (IllegalArgumentException e) {
            return Result.failure(ResultCodeEnum.FAIL, e.getMessage());
        }
    }

    @DeleteMapping("/categories")
    public Result<Void> deleteCategory(@RequestHeader(value = "role", defaultValue = "0") String role,
                                       @RequestHeader(value = "uid", required = false) String uid,
                                       @RequestParam Integer id) {
        if (!isAdmin(role)) return forbidden();
        try {
            categoryConfigService.deleteCategory(id);
            writeLog(uid, "删除分类", "DELETE", "id=" + id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.failure(ResultCodeEnum.FAIL, e.getMessage());
        }
    }

    @GetMapping("/products")
    public Result<PageResult<Product>> products(@RequestHeader(value = "role", defaultValue = "0") String role,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) Integer categoryId,
                                                @RequestParam(required = false) String keyword) {
        if (!isAdmin(role)) return forbidden();
        int offset = (page - 1) * size;
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<Product> records = adminMapper.listProducts(offset, size, categoryId, kw);
        long total = adminMapper.countProducts(categoryId, kw);
        return Result.success(PageResult.of(records, total, page, size));
    }

    @PostMapping("/products")
    public Result<String> saveProduct(@RequestHeader(value = "role", defaultValue = "0") String role,
                                        @RequestHeader(value = "uid", required = false) String uid,
                                        @RequestBody Product product) {
        if (!isAdmin(role)) return forbidden();
        if (!StringUtils.hasText(product.getName())) {
            return Result.failure(ResultCodeEnum.FAIL, "商品名称不能为空");
        }
        if (product.getId() == null) {
            adminMapper.insertProduct(product);
            writeLog(uid, "新增商品", "POST", product.getName());
        } else {
            adminMapper.updateProduct(product);
            writeLog(uid, "更新商品", "PUT", "id=" + product.getId());
        }
        return Result.success("保存成功");
    }

    @DeleteMapping("/products")
    public Result<Void> deleteProduct(@RequestHeader(value = "role", defaultValue = "0") String role,
                                      @RequestHeader(value = "uid", required = false) String uid,
                                      @RequestParam Long id) {
        if (!isAdmin(role)) return forbidden();
        adminMapper.deleteProduct(id);
        writeLog(uid, "删除商品", "DELETE", "id=" + id);
        return Result.success();
    }

    @GetMapping("/orders")
    public Result<PageResult<Order>> orders(@RequestHeader(value = "role", defaultValue = "0") String role,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) Integer status) {
        if (!isAdmin(role)) return forbidden();
        int offset = (page - 1) * size;
        List<Order> records = adminMapper.listOrders(offset, size, status);
        long total = adminMapper.countOrders(status);
        return Result.success(PageResult.of(records, total, page, size));
    }

    @PutMapping("/orders/status")
    public Result<Void> orderStatus(@RequestHeader(value = "role", defaultValue = "0") String role,
                                    @RequestHeader(value = "uid", required = false) String uid,
                                    @RequestParam Long id, @RequestParam int status) {
        if (!isAdmin(role)) return forbidden();
        adminMapper.updateOrderStatus(id, status);
        writeLog(uid, "更新订单状态", "PUT", "id=" + id + ",status=" + status);
        return Result.success();
    }

    @PostMapping("/orders/ship")
    public Result<Map<String, String>> shipOrder(@RequestHeader(value = "role", defaultValue = "0") String role,
                                                 @RequestHeader(value = "uid", required = false) String uid,
                                                 @RequestParam Long id) {
        if (!isAdmin(role)) return forbidden();
        Order order = adminMapper.findOrderById(id);
        if (order == null) {
            return Result.failure(ResultCodeEnum.FAIL, "订单不存在");
        }
        if (order.getOrderStatus() == null || order.getOrderStatus() != 1) {
            return Result.failure(ResultCodeEnum.FAIL, "仅待发货订单可模拟发货");
        }
        String logisticsNo = "SF" + System.currentTimeMillis();
        String company = "顺丰速运(模拟)";
        if (adminMapper.shipOrder(id, logisticsNo, company) <= 0) {
            return Result.failure(ResultCodeEnum.FAIL, "发货失败");
        }
        writeLog(uid, "模拟发货", "POST", "orderId=" + id + ",no=" + logisticsNo);
        Map<String, String> data = new HashMap<>();
        data.put("logisticsNo", logisticsNo);
        data.put("logisticsCompany", company);
        return Result.success(data);
    }

    /** 前台可读：站点名、包邮门槛、运费（Home/Checkout 依赖） */
    @GetMapping("/config/public")
    public Result<Map<String, Object>> publicConfig() {
        return Result.success(mallConfigService.publicConfig());
    }

    @GetMapping("/config")
    public Result<List<Map<String, Object>>> config(@RequestHeader(value = "role", defaultValue = "0") String role) {
        if (!isAdmin(role)) return forbidden();
        return Result.success(adminMapper.listConfig());
    }

    @PutMapping("/config")
    public Result<Void> updateConfig(@RequestHeader(value = "role", defaultValue = "0") String role,
                                     @RequestHeader(value = "uid", required = false) String uid,
                                     @RequestParam String key, @RequestParam String value) {
        if (!isAdmin(role)) return forbidden();
        if ("mall_categories".equals(key)) {
            return Result.failure(ResultCodeEnum.FAIL, "请通过分类管理维护商品分类");
        }
        adminMapper.updateConfig(key, value);
        writeLog(uid, "更新配置", "PUT", key + "=" + value);
        return Result.success();
    }

    @GetMapping("/logs")
    public Result<PageResult<Map<String, Object>>> logs(@RequestHeader(value = "role", defaultValue = "0") String role,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        if (!isAdmin(role)) return forbidden();
        int offset = (page - 1) * size;
        return Result.success(PageResult.of(adminMapper.listLogs(offset, size), adminMapper.countLogs(), page, size));
    }

    // ==================== SKU 管理 ====================

    /** 查询商品的 SKU 列表（含规格值） */
    @GetMapping("/skus")
    public Result<List<SkuVO>> listSkus(@RequestHeader(value = "role", defaultValue = "0") String role,
                                        @RequestParam Long productId) {
        if (!isAdmin(role)) return forbidden();
        List<Sku> skus = adminMapper.listSkusByProductId(productId);
        List<SkuVO> result = new ArrayList<>();
        for (Sku sku : skus) {
            SkuVO vo = new SkuVO();
            vo.setId(sku.getId());
            vo.setProductId(sku.getProductId());
            vo.setSkuCode(sku.getSkuCode());
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock());
            vo.setStatus(sku.getStatus());
            vo.setSpecs(adminMapper.listSpecsBySkuId(sku.getId()));
            result.add(vo);
        }
        return Result.success(result);
    }

    /** 批量保存商品 SKU（先删旧再插新，事务） */
    @PostMapping("/skus")
    @Transactional
    public Result<String> saveSkus(@RequestHeader(value = "role", defaultValue = "0") String role,
                                   @RequestHeader(value = "uid", required = false) String uid,
                                   @RequestBody Map<String, Object> body) {
        if (!isAdmin(role)) return forbidden();
        Long productId = Long.valueOf(body.get("productId").toString());
        List<Map<String, Object>> skuList = (List<Map<String, Object>>) body.get("skus");
        if (skuList == null) skuList = Collections.emptyList();

        // 先删旧的
        adminMapper.deleteAllSpecsByProductId(productId);
        adminMapper.deleteSkusByProductId(productId);

        // 再插新的
        for (Map<String, Object> item : skuList) {
            Sku sku = new Sku();
            sku.setProductId(productId);
            sku.setSkuCode((String) item.get("skuCode"));
            sku.setPrice(new java.math.BigDecimal(item.get("price").toString()));
            sku.setStock(Integer.parseInt(item.get("stock").toString()));
            sku.setStatus(1);
            adminMapper.insertSku(sku);
            List<Map<String, Object>> specs = (List<Map<String, Object>>) item.get("specs");
            if (specs != null) {
                for (Map<String, Object> spec : specs) {
                    SkuSpec ss = new SkuSpec();
                    ss.setSkuId(sku.getId());
                    ss.setSpecName((String) spec.get("specName"));
                    ss.setSpecValue((String) spec.get("specValue"));
                    adminMapper.insertSkuSpec(ss);
                }
            }
        }
        writeLog(uid, "保存SKU", "POST", "productId=" + productId);
        return Result.success("SKU保存成功");
    }

    /** 删除单个 SKU */
    @DeleteMapping("/skus")
    public Result<Void> deleteSku(@RequestHeader(value = "role", defaultValue = "0") String role,
                                   @RequestHeader(value = "uid", required = false) String uid,
                                   @RequestParam Long id) {
        if (!isAdmin(role)) return forbidden();
        adminMapper.deleteSpecsBySkuId(id);
        adminMapper.deleteSku(id);
        writeLog(uid, "删除SKU", "DELETE", "id=" + id);
        return Result.success();
    }

    // ---- 优惠券管理 ----

    @GetMapping("/coupons")
    public Result<PageResult<Coupon>> coupons(@RequestHeader(value = "role", defaultValue = "0") String role,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword) {
        if (!isAdmin(role)) return forbidden();
        int offset = (page - 1) * size;
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return Result.success(PageResult.of(adminMapper.listCoupons(offset, size, kw),
                adminMapper.countCoupons(kw), page, size));
    }

    /** 新增/更新优惠券（带 id 即更新） */
    @PostMapping("/coupons")
    public Result<String> saveCoupon(@RequestHeader(value = "role", defaultValue = "0") String role,
                                     @RequestHeader(value = "uid", required = false) String uid,
                                     @RequestBody Coupon coupon) {
        if (!isAdmin(role)) return forbidden();
        // 基础校验
        if (!StringUtils.hasText(coupon.getName())) {
            return Result.failure(ResultCodeEnum.FAIL, "优惠券名称不能为空");
        }
        if (coupon.getType() == null || (coupon.getType() != 0 && coupon.getType() != 1)) {
            return Result.failure(ResultCodeEnum.FAIL, "类型必须为满减或折扣");
        }
        if (coupon.getType() == 0) {
            // 满减券：必须填写减免金额
            if (coupon.getReduceAmount() == null || coupon.getReduceAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                return Result.failure(ResultCodeEnum.FAIL, "请填写有效的减免金额");
            }
        } else {
            // 折扣券：折扣率必须在 (0,1) 之间
            if (coupon.getDiscountRate() == null || coupon.getDiscountRate().compareTo(java.math.BigDecimal.ZERO) <= 0
                    || coupon.getDiscountRate().compareTo(java.math.BigDecimal.ONE) >= 0) {
                return Result.failure(ResultCodeEnum.FAIL, "折扣率必须在 0~1 之间（如 0.85 表示 85 折）");
            }
        }
        if (coupon.getExpireStart() == null || coupon.getExpireEnd() == null
                || coupon.getExpireEnd().before(coupon.getExpireStart())) {
            return Result.failure(ResultCodeEnum.FAIL, "有效期不正确");
        }
        if (coupon.getTotalQuantity() == null || coupon.getTotalQuantity() < 0) coupon.setTotalQuantity(0);
        if (coupon.getPerUserLimit() == null || coupon.getPerUserLimit() < 1) coupon.setPerUserLimit(1);
        if (coupon.getStatus() == null) coupon.setStatus(1);

        if (coupon.getId() == null) {
            adminMapper.insertCoupon(coupon);
            writeLog(uid, "新增优惠券", "POST", "name=" + coupon.getName());
            return Result.success("创建成功");
        }
        if (adminMapper.updateCoupon(coupon) <= 0) {
            return Result.failure(ResultCodeEnum.FAIL, "优惠券不存在");
        }
        writeLog(uid, "更新优惠券", "POST", "id=" + coupon.getId());
        return Result.success("更新成功");
    }

    /** 上下架优惠券 */
    @PutMapping("/coupons/status")
    public Result<Void> updateCouponStatus(@RequestHeader(value = "role", defaultValue = "0") String role,
                                           @RequestHeader(value = "uid", required = false) String uid,
                                           @RequestParam Long id, @RequestParam Integer status) {
        if (!isAdmin(role)) return forbidden();
        if (status == null || (status != 0 && status != 1)) {
            return Result.failure(ResultCodeEnum.FAIL, "状态不合法");
        }
        if (adminMapper.updateCouponStatus(id, status) <= 0) {
            return Result.failure(ResultCodeEnum.FAIL, "优惠券不存在");
        }
        writeLog(uid, status == 1 ? "上架优惠券" : "下架优惠券", "PUT", "id=" + id);
        return Result.success();
    }

    @DeleteMapping("/coupons")
    public Result<Void> deleteCoupon(@RequestHeader(value = "role", defaultValue = "0") String role,
                                     @RequestHeader(value = "uid", required = false) String uid,
                                     @RequestParam Long id) {
        if (!isAdmin(role)) return forbidden();
        if (adminMapper.deleteCoupon(id) <= 0) {
            return Result.failure(ResultCodeEnum.FAIL, "优惠券不存在");
        }
        writeLog(uid, "删除优惠券", "DELETE", "id=" + id);
        return Result.success();
    }

    // ---- 数据看板统计 ----

    /** 聚合看板数据：核心指标 + 近7天趋势 + 分布统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestHeader(value = "role", defaultValue = "0") String role) {
        if (!isAdmin(role)) return forbidden();
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", adminMapper.countAllUsers());
        data.put("productCount", adminMapper.countProducts(null, null));
        data.put("orderCount", adminMapper.countOrders(null));
        data.put("salesTotal", adminMapper.sumSales());
        data.put("couponReceived", adminMapper.countCouponReceived());
        data.put("couponUsed", adminMapper.countCouponUsed());
        data.put("orderTrend", adminMapper.orderTrend(7));
        data.put("categoryDist", adminMapper.categoryDistribution());
        data.put("orderStatusDist", adminMapper.orderStatusDistribution());
        data.put("topProducts", adminMapper.topProducts(5));
        return Result.success(data);
    }
}
