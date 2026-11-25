package model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Product {
    private Long id;
    private String name;
    private Integer categoryId;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private String image;
    private String images;
    private String description;
    private Integer sales;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
