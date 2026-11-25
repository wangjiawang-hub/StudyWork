package gateway.filter;

import org.springframework.util.AntPathMatcher;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import gateway.config.IgnoreWhiteProperties;
import gateway.config.ShoppingAuthProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import utils.JwtUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@Component
/**
 * 网关全局鉴权：白名单放行 → 校验 JWT →（可选）Redis 比对 uuid → 注入 uid/role 头转发下游。
 * 下游微服务通过 @RequestHeader("uid") 获取当前用户，不再自行解析 Token。
 */
public class AuthFilter implements GlobalFilter, Ordered {
    @Autowired
    private IgnoreWhiteProperties ignoreWhite;
    @Autowired
    private ShoppingAuthProperties shoppingAuthProperties;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();
        String url = request.getURI().getPath();
        String upgrade = request.getHeaders().getFirst("Upgrade");
        if ("websocket".equalsIgnoreCase(upgrade)) {
            String wsToken = request.getQueryParams().getFirst("token");
            if (wsToken == null || wsToken.isBlank()) {
                return unauthorizedResponse(exchange, HttpStatus.UNAUTHORIZED, "ws token为空");
            }
            try {
                Map<String,Object> claims = JwtUtil.parseToken(wsToken);
                mutate.header("uid", claims.get("id").toString());
                if (claims.get("role") != null) mutate.header("role", claims.get("role").toString());
                removeHeader(mutate, "from-source");
                return chain.filter(exchange.mutate().request(mutate.build()).build());
            } catch (Exception e) {
                return unauthorizedResponse(exchange, HttpStatus.UNAUTHORIZED, "ws token无效");
            }
        }
        // 白名单：无 token 直接放行；有 token 时尝试解析并注入 uid（支持公开接口个性化推荐）
        if (matches(url, ignoreWhite.getEffectiveWhites())) {
            String token = request.getHeaders().getFirst("Authorization");
            if (token != null && !token.isBlank()) {
                tryInjectUserHeaders(mutate, token);
            }
            removeHeader(mutate, "from-source");
            return chain.filter(exchange.mutate().request(mutate.build()).build());
        }
        String token = request.getHeaders().getFirst("Authorization");
        if(token==null)
        {
            return unauthorizedResponse(exchange, HttpStatus.UNAUTHORIZED, "令牌不能为空");
        }
        try {
            injectUserFromToken(mutate, token);
            removeHeader(mutate, "from-source");
            return chain.filter(exchange.mutate().request(mutate.build()).build());
        }catch (TokenExpiredException e)
        {
            return unauthorizedResponse(exchange, HttpStatus.UNAUTHORIZED, "token过期");
        }catch (JWTVerificationException e) {
            // 处理其他JWT验证失败的情况，比如签名无效
            return unauthorizedResponse(exchange, HttpStatus.UNAUTHORIZED, "token验证失败");
        } catch (Exception e) {
            // 捕捉所有其他异常
            return unauthorizedResponse(exchange, HttpStatus.UNAUTHORIZED, "无效token");
        }

    }

    @Override
    public int getOrder() {
        return -100;
    }

    public  boolean matches(String str, List<String> strs)
    {
        if (str.isEmpty() || strs.isEmpty())
        {
            return false;
        }
        AntPathMatcher matcher = new AntPathMatcher();
        for (String pattern : strs)
        {
            if (matcher.match(pattern, str))
            {
                return true;
            }
        }
        return false;
    }
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, HttpStatus status, String errorMessage) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(errorMessage.getBytes())));
    }

    private void addHeader(ServerHttpRequest.Builder mutate, String name, Object value)
    {
        if (value == null)
        {
            return;
        }
        String valueStr = value.toString();
        String valueEncode = null;
        try {
            valueEncode = URLEncoder.encode(valueStr, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        mutate.header(name, valueEncode);

    }

    private void removeHeader(ServerHttpRequest.Builder mutate, String name)
    {
        mutate.headers(httpHeaders -> httpHeaders.remove(name)).build();
    }

    /** 白名单等场景：token 无效时静默忽略，不阻断请求 */
    private void tryInjectUserHeaders(ServerHttpRequest.Builder mutate, String token) {
        try {
            injectUserFromToken(mutate, token);
        } catch (Exception ignored) {
            // 公开接口携带过期/无效 token 仍允许访问
        }
    }

    private void injectUserFromToken(ServerHttpRequest.Builder mutate, String token) {
        Map<String, Object> claims = JwtUtil.parseToken(token);
        String userId = claims.get("id").toString();

        if (!shoppingAuthProperties.isJwtOnly()) {
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(userId);
            if (redisToken == null) {
                throw new JWTVerificationException("无效token");
            }
            Map<String, Object> redisClaims = JwtUtil.parseToken(redisToken);
            String redisUUID = redisClaims.get("uuid").toString();
            String uuid = claims.get("uuid").toString();
            if (!redisUUID.equals(uuid)) {
                throw new JWTVerificationException("过期token");
            }
        }

        addHeader(mutate, "uid", userId);
        Object role = claims.get("role");
        if (role != null) {
            addHeader(mutate, "role", role.toString());
        }
    }

}