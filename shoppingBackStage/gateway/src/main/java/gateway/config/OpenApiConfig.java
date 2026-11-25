package gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI shoppingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("购物商城 API")
                        .description("购物商城微服务 API 文档。Swagger UI 右上角可切换各服务；/api-docs/{服务名} 为聚合入口。")
                        .version("1.0"));
    }
}
