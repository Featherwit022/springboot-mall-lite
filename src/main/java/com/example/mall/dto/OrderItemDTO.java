package com.example.mall.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 价格和库存一律由后端查，绝不信前端传来的金额
public class OrderItemDTO {
    @NotNull(message = "productId 不能为空")
    private Long productId;
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少 1")
    private Integer quantity;







    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
