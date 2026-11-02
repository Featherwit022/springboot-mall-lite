package com.example.mall.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class OrderCreateDTO {
    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<OrderItemDTO> items;
    @Size(max = 64, message = "幂等键最长 64 字符")
    private String idempotentKey;

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }

    public String getIdempotentKey() {
        return idempotentKey;
    }

    public void setIdempotentKey(String idempotentKey) {
        this.idempotentKey = idempotentKey;
    }
}
