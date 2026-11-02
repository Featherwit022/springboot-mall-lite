package com.example.mall.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductCreateDTO {
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 120, message = "商品名最长 120 字")
    private String name;
    @NotNull(message = "必须指定分类")
    private Long categoryId;
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负")
    private Integer stock;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}
