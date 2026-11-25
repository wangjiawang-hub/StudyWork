package model;

import lombok.Data;

@Data
public class SkuSpec {
    private Long id;
    private Long skuId;
    private String specName;
    private String specValue;
}
