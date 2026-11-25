package auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
/** 验证码：Redis 优先，不可用时降级内存；图形码验证后立即删除防重放 */
public class CaptchaService {
    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String CODE_PREFIX = "code:";

    private final ConcurrentHashMap<String, String> memoryStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> memoryExpire = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private StringRedisTemplate redis;

    // JavaMailSender 由 spring-boot-starter-mail 提供；未配置邮箱时为 null，邮件验证码不可用
    @Autowired(required = false)
    private JavaMailSender mailSender;

    // 发件人地址，与 spring.mail.username 保持一致（QQ 要求 From = 登录账号）
    @Value("${spring.mail.username:}")
    private String mailFrom;

    // 控制台打印验证码开关：开发环境可设 true 方便调试
    @Value("${shopping.captcha.console:false}")
    private boolean consolePrint;

    public String generateImageCode(String key) {
        String code = String.format("%04d", new Random().nextInt(10000));
        save(CAPTCHA_PREFIX + key, code, 5);
        return code;
    }

    public boolean verifyImageCode(String key, String code) {
        String fullKey = CAPTCHA_PREFIX + key;
        String stored = get(fullKey);
        if (stored == null) return false;
        delete(fullKey);
        return stored.equalsIgnoreCase(code);
    }

    /**
     * 发送邮箱验证码：生成 6 位随机码，存入 Redis（5 分钟有效），通过 QQ SMTP 发送。
     * 未配置 JavaMailSender 时仅在控制台打印（开发降级）。
     */
    public void sendSmsOrEmailCode(String target, String type) {
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        String fullKey = CODE_PREFIX + type + ":" + target;
        save(fullKey, code, 5);
        // 开发模式：控制台直接打印，方便拿验证码
        if (consolePrint) {
            log.info("【验证码】{} -> {} (5分钟内有效)", target, code);
        }
        // 邮件类型：真正发送邮件
        if ("email".equals(type) && mailSender != null && mailFrom != null && !mailFrom.isEmpty()) {
            sendEmail(target, code);
        }
    }

    /** 用 JavaMailSender 发送验证码邮件；失败仅记日志不抛异常，保证 Redis 已存验证码可用 */
    private void sendEmail(String to, String code) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(to);
            msg.setSubject("【商城】邮箱验证码");
            msg.setText("您正在登录商城，验证码为：" + code + "，5 分钟内有效。如非本人操作请忽略本邮件。");
            mailSender.send(msg);
            log.info("验证码邮件已发送至 {}", to);
        } catch (Exception e) {
            // 邮件发送失败不阻断流程，验证码已在 Redis 中，可走控制台查看
            log.warn("邮件发送失败（验证码={}，收件人={}）：{}", code, to, e.getMessage());
        }
    }

    public boolean verifyCode(String target, String type, String code) {
        String fullKey = CODE_PREFIX + type + ":" + target;
        String stored = get(fullKey);
        if (stored == null) return false;
        delete(fullKey);
        return stored.equals(code);
    }

    public String peekCode(String target, String type) {
        return get(CODE_PREFIX + type + ":" + target);
    }

    private void save(String key, String value, long minutes) {
        memoryStore.put(key, value);
        memoryExpire.put(key, System.currentTimeMillis() + minutes * 60_000);
        if (redis == null) return;
        try {
            redis.opsForValue().set(key, value, minutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis 不可用，验证码已写入内存: {}", e.getMessage());
        }
    }

    private String get(String key) {
        expireMemory(key);
        String local = memoryStore.get(key);
        if (local != null) return local;
        if (redis == null) return null;
        try {
            return redis.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 读取失败: {}", e.getMessage());
            return null;
        }
    }

    private void delete(String key) {
        memoryStore.remove(key);
        memoryExpire.remove(key);
        if (redis == null) return;
        try {
            redis.delete(key);
        } catch (Exception ignored) {
        }
    }

    private void expireMemory(String key) {
        Long expireAt = memoryExpire.get(key);
        if (expireAt != null && expireAt < System.currentTimeMillis()) {
            memoryStore.remove(key);
            memoryExpire.remove(key);
        }
    }

}
