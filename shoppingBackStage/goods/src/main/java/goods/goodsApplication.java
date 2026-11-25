package goods;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@org.mybatis.spring.annotation.MapperScan("goods.mapper")
public class goodsApplication {
    public static void main(String[] args) {
        SpringApplication.run(goodsApplication.class,args);
    }
}
