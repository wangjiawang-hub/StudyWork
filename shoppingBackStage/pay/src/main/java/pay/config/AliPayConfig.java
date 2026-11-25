package pay.config;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AliPayConfig {
    private String appId;
    private String appPrivateKey;
    private String alipayPublicKey;
    private String notifyUrl;
    /** 沙箱网关完整地址 */
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    /** 支付完成后同步跳转（浏览器） */
    private String returnUrl = "http://127.0.0.1:5173/pay/result";

    @PostConstruct
    public void init() {
        Config config = new Config();
        config.protocol = "https";
        config.gatewayHost = resolveGatewayHost();
        config.signType = "RSA2";
        config.appId = this.appId;
        config.merchantPrivateKey = this.appPrivateKey;
        config.alipayPublicKey = this.alipayPublicKey;
        config.notifyUrl = this.notifyUrl;
        Factory.setOptions(config);
        System.out.println("=======支付宝SDK初始化成功，网关: " + config.gatewayHost + " =======");
    }

    public String resolveGatewayHost() {
        if (!StringUtils.hasText(gatewayUrl)) {
            return "openapi-sandbox.dl.alipaydev.com";
        }
        String url = gatewayUrl.trim();
        if (url.startsWith("https://")) {
            url = url.substring(8);
        } else if (url.startsWith("http://")) {
            url = url.substring(7);
        }
        int slash = url.indexOf('/');
        return slash > 0 ? url.substring(0, slash) : url;
    }

    public String resolveGatewayFullUrl() {
        if (StringUtils.hasText(gatewayUrl) && gatewayUrl.contains("gateway.do")) {
            return gatewayUrl.trim();
        }
        return "https://" + resolveGatewayHost() + "/gateway.do";
    }
}
