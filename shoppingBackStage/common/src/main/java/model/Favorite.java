package model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Favorite {
    private Long id;
    private Long userId;
    private Long productId;
    private Date createTime;
    private String productName;
    private String productImage;
    private BigDecimal price;
}
