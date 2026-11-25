package gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "security.ignore")
public class IgnoreWhiteProperties {

    private static final List<String> DEFAULT_WHITES = Arrays.asList(
            "/auth/login",
            "/auth/admin/login",
            "/auth/register",
            "/auth/captcha",
            "/auth/sendCode",
            "/auth/login/code",
            "/goods/**",
            "/admin/config/public",
            "/user/review/list",
            "/alipay/notify",
            "/ai/chat",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
           // "/ws/**",
           //  "/ws/chat"
    );

    private List<String> whites = new ArrayList<>();

    /** 合并默认白名单，避免 Nacos 覆盖后商品等公开接口被拦截 */
    public List<String> getEffectiveWhites() {
        Set<String> merged = new LinkedHashSet<>(DEFAULT_WHITES);
        if (whites != null) {
            merged.addAll(whites);
        }
        return new ArrayList<>(merged);
    }

    public List<String> getWhites() {
        return whites;
    }

    public void setWhites(List<String> whites) {
        this.whites = whites;
    }
}
