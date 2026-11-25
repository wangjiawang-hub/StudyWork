package order.service;

import order.mapper.ConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Component
public class MallConfigReader {
    private static final String KEY_FREE_SHIPPING = "free_shipping_amount";
    private static final String KEY_SHIPPING_FEE = "shipping_fee";

    @Autowired
    private ConfigMapper configMapper;

    public BigDecimal freeShippingAmount() {
        return getDecimal(KEY_FREE_SHIPPING, new BigDecimal("99"));
    }

    public BigDecimal shippingFee() {
        return getDecimal(KEY_SHIPPING_FEE, new BigDecimal("10"));
    }

    public BigDecimal calcFreight(BigDecimal merchandiseTotal) {
        if (merchandiseTotal.compareTo(freeShippingAmount()) >= 0) {
            return BigDecimal.ZERO;//免邮
        }
        return shippingFee();
    }

    private BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String value = configMapper.getConfigValue(key);
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
