package gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "shopping.auth")
public class ShoppingAuthProperties {
    /** 开发模式：仅校验 JWT 签名，不依赖 Redis 会话 */
    private boolean jwtOnly = true;

    public boolean isJwtOnly() {
        return jwtOnly;
    }

    public void setJwtOnly(boolean jwtOnly) {
        this.jwtOnly = jwtOnly;
    }
}
