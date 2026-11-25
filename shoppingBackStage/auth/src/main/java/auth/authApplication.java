package auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient

@MapperScan("auth.mapper")
@ComponentScan({"auth", "handler", "exception"})

@org.mybatis.spring.annotation.MapperScan("auth.mapper")
public class authApplication {
    public static void main (String[] args) {
        SpringApplication.run(authApplication . class , args ) ;
    }
}