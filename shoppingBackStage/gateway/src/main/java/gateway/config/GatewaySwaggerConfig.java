package gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Spring Cloud Gateway 上 Swagger UI 需配合 springdoc.enable-native-support=true。
 * 补充入口重定向，避免部分版本 /swagger-ui.html 跳转失败。
 */
@Configuration
public class GatewaySwaggerConfig {

    @Bean
    public RouterFunction<ServerResponse> swaggerUiRedirectRouter() {
        return route(GET("/swagger-ui.html"),
                req -> ServerResponse.status(HttpStatus.FOUND)
                        .location(URI.create("/webjars/swagger-ui/index.html"))
                        .build())
                .and(route(GET("/swagger-ui"),
                        req -> ServerResponse.status(HttpStatus.FOUND)
                                .location(URI.create("/webjars/swagger-ui/index.html"))
                                .build()));
    }
}
