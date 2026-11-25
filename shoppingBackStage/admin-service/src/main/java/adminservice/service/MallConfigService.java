package adminservice.service;

import adminservice.mapper.AdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class MallConfigService {
    private static final String KEY_SITE_NAME = "site_name";
    private static final String KEY_FREE_SHIPPING = "free_shipping_amount";
    private static final String KEY_SHIPPING_FEE = "shipping_fee";

    @Autowired
    private AdminMapper adminMapper;

    public Map<String, Object> publicConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("siteName", getString(KEY_SITE_NAME, "购物商城"));
        map.put("freeShippingAmount", getDecimal(KEY_FREE_SHIPPING, new BigDecimal("99")));
        map.put("shippingFee", getDecimal(KEY_SHIPPING_FEE, new BigDecimal("10")));
        return map;
    }

    public String getString(String key, String defaultValue) {
        String value = adminMapper.getConfigValue(key);
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String value = adminMapper.getConfigValue(key);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
