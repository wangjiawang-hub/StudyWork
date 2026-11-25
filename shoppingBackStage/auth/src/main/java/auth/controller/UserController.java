package auth.controller;

import auth.service.CaptchaService;
import auth.service.UserService;
import exception.BusinessException;
import model.Result;
import model.ResultCodeEnum;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import utils.JwtUtil;
import utils.PasswordUtil;
import utils.AuthValidation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
/** 认证服务：注册、登录、验证码、个人资料；登录成功签发 JWT 并写入 Redis */
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private CaptchaService captchaService;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha(@RequestParam String key) {
        String code = captchaService.generateImageCode(key);
        Map<String, String> data = new HashMap<>();
        data.put("key", key);
        data.put("code", code);
        return Result.success(data);
    }

    @PostMapping("/sendCode")
    public Result<String> sendCode(@RequestBody Map<String, String> body) {
        String target = body.get("target");
        String type = body.getOrDefault("type", "phone");
        if (!StringUtils.hasText(target)) {
            return Result.failure(ResultCodeEnum.FAIL, "请输入手机号或邮箱");
        }
        captchaService.sendSmsOrEmailCode(target, type);
        return Result.success("验证码已发送");
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody Map<String, String> body) {
        String username = normalizeBlank(body.get("username"));
        String password = body.get("password");
        String captchaKey = body.get("captchaKey");
        String captchaCode = body.get("captchaCode");
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return Result.failure(ResultCodeEnum.FAIL, "用户名和密码不能为空");
        }
        if (!AuthValidation.isUsername(username)) {
            return Result.failure(ResultCodeEnum.FAIL, "用户名为2-20位，支持中文、字母、数字、下划线");
        }
        if (password.length() < 6) {
            return Result.failure(ResultCodeEnum.FAIL, "密码至少6位");
        }
        if (!StringUtils.hasText(captchaKey) || !captchaService.verifyImageCode(captchaKey, captchaCode)) {
            return Result.failure(ResultCodeEnum.FAIL, "验证码错误");
        }
        if (userService.findByUsername(username) != null) {
            return Result.failure(ResultCodeEnum.FAIL, "用户名已存在");
        }
        String phone = normalizeBlank(body.get("phone"));
        String email = normalizeBlank(body.get("email"));
        if (phone != null && !AuthValidation.isPhone(phone)) {
            return Result.failure(ResultCodeEnum.FAIL, "请输入正确的11位手机号");
        }
        if (email != null && !AuthValidation.isEmail(email)) {
            return Result.failure(ResultCodeEnum.FAIL, "请输入正确的邮箱地址");
        }
        if (phone != null && userService.findByPhone(phone) != null) {
            return Result.failure(ResultCodeEnum.FAIL, "手机号已被注册");
        }
        if (email != null && userService.findByEmail(email) != null) {
            return Result.failure(ResultCodeEnum.FAIL, "邮箱已被注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setPhone(phone);
        user.setEmail(email);
        userService.register(user);
        return Result.success("注册成功");
    }

    private String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    @PostMapping("/login")
    public Result<User> login(@RequestBody Map<String, String> body) {
        String captchaKey = body.get("captchaKey");
        String captchaCode = body.get("captchaCode");
        if (!StringUtils.hasText(captchaKey) || !captchaService.verifyImageCode(captchaKey, captchaCode)) {
            return Result.failure(ResultCodeEnum.FAIL, "验证码错误");
        }
        String password = body.get("password");
        if (!StringUtils.hasText(password)) {
            return Result.failure(ResultCodeEnum.FAIL, "请输入密码");
        }
        String loginType = body.getOrDefault("loginType", "username");
        User dbUser;
        if ("phone".equals(loginType)) {
            String phone = normalizeBlank(body.get("phone"));
            if (!AuthValidation.isPhone(phone)) {
                return Result.failure(ResultCodeEnum.FAIL, "请输入正确的11位手机号");
            }
            dbUser = userService.findByPhone(phone);
            if (dbUser == null) {
                return Result.failure(ResultCodeEnum.FAIL, "该手机号未注册，请先注册");
            }
        } else if ("email".equals(loginType)) {
            String email = normalizeBlank(body.get("email"));
            if (!AuthValidation.isEmail(email)) {
                return Result.failure(ResultCodeEnum.FAIL, "请输入正确的邮箱地址");
            }
            dbUser = userService.findByEmail(email);
            if (dbUser == null) {
                return Result.failure(ResultCodeEnum.FAIL, "该邮箱未注册，请先注册");
            }
        } else {
            String username = normalizeBlank(body.get("username"));
            if (!StringUtils.hasText(username)) {
                return Result.failure(ResultCodeEnum.FAIL, "请输入用户名");
            }
            dbUser = userService.findByUsername(username);
            if (dbUser == null) {
                return Result.failure(ResultCodeEnum.FAIL, "用户不存在");
            }
        }
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            return Result.failure(ResultCodeEnum.FAIL, "账号已禁用");
        }
        if (!PasswordUtil.matches(password, dbUser.getPassword())) {
            return Result.failure(ResultCodeEnum.FAIL, "密码错误");
        }
        return Result.success(buildLoginUser(dbUser));
    }

    /** 邮箱验证码登录：免密码，凭邮箱 + 邮箱验证码登录 */
    @PostMapping("/login/code")
    public Result<User> loginByCode(@RequestBody Map<String, String> body) {
        String email = normalizeBlank(body.get("email"));
        String code = body.get("code");
        if (!AuthValidation.isEmail(email)) {
            return Result.failure(ResultCodeEnum.FAIL, "请输入正确的邮箱地址");
        }
        if (!StringUtils.hasText(code)) {
            return Result.failure(ResultCodeEnum.FAIL, "请输入邮箱验证码");
        }
        if (!captchaService.verifyCode(email, "email", code)) {
            return Result.failure(ResultCodeEnum.FAIL, "验证码错误或已过期");
        }
        User dbUser = userService.findByEmail(email);
        if (dbUser == null) {
            return Result.failure(ResultCodeEnum.FAIL, "该邮箱未注册，请先注册");
        }
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            return Result.failure(ResultCodeEnum.FAIL, "账号已禁用");
        }
        return Result.success(buildLoginUser(dbUser));
    }

    @PostMapping("/admin/login")
    public Result<User> adminLogin(@RequestBody Map<String, String> body) {
        if (!StringUtils.hasText(body.get("username")) || !StringUtils.hasText(body.get("password"))) {
            return Result.failure(ResultCodeEnum.FAIL, "用户名和密码不能为空");
        }
        User dbUser = userService.findByUsername(body.get("username"));
        if (dbUser == null) {
            return Result.failure(ResultCodeEnum.FAIL, "用户不存在");
        }
        if (dbUser.getRole() == null || dbUser.getRole() != 1) {
            return Result.failure(ResultCodeEnum.FAIL, "无管理员权限，请使用管理员账号登录");
        }
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            return Result.failure(ResultCodeEnum.FAIL, "账号已禁用");
        }
        if (!PasswordUtil.matches(body.get("password"), dbUser.getPassword())) {
            return Result.failure(ResultCodeEnum.FAIL, "密码错误");
        }
        return Result.success(buildLoginUser(dbUser));
    }

    /** 组装登录响应：生成 JWT（id/role/uuid），Redis 存 12h，密码字段置空 */
    private User buildLoginUser(User dbUser) {
        Map<String, Object> claims = new HashMap<>();
        String userId = String.valueOf(dbUser.getId());
        claims.put("id", userId);
        claims.put("role", dbUser.getRole() == null ? 0 : dbUser.getRole());
        String uuid = UUID.randomUUID().toString();
        claims.put("uuid", uuid);
        String token = JwtUtil.genToken(claims);
        try {
            if (stringRedisTemplate != null) {
                stringRedisTemplate.delete(userId);
                stringRedisTemplate.opsForValue().set(userId, token, 12, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.warn("Redis 不可用，登录 Token 未写入 Redis: {}", e.getMessage());
        }
        dbUser.setToken(token);
        dbUser.setPassword(null);
        return dbUser;
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "uid", required = false) String userId) {
        if (userId != null && stringRedisTemplate != null) {
            try {
                stringRedisTemplate.delete(userId);
            } catch (Exception ignored) {
            }
        }
        return Result.success();
    }

    @GetMapping("/profile")
    public Result<User> profile(@RequestHeader("uid") String uid) {
        User user = userService.findById(Long.parseLong(uid));
        if (user != null) user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestHeader("uid") String uid, @RequestBody User user) {
        user.setId(Long.parseLong(uid));
        userService.updateProfile(user);
        return Result.success("更新成功");
    }

    @PutMapping("/password")
    public Result<String> updatePassword(@RequestHeader("uid") String uid, @RequestBody Map<String, String> body) {
        try {
            userService.updatePassword(Long.parseLong(uid), body.get("oldPassword"), body.get("newPassword"));
            return Result.success("密码修改成功");
        } catch (IllegalArgumentException e) {
            return Result.failure(ResultCodeEnum.FAIL, e.getMessage());
        }
    }

    @GetMapping("/test-exception")
    public Result<Void> testException() {
        throw new BusinessException(ResultCodeEnum.BAD_REQUEST, "这是测试异常");
    }
}
