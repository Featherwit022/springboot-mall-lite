package com.example.mall.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class OrderCreateDTO {
    @NotNull(message = "userId 不能为空")
    private Long userId;
    @NotEmpty(message = "订单明细不能为空")
    @Valid
    private List<OrderItemDTO> items;
    @Size(max = 64, message = "幂等键最长 64 字符")
    private String idempotentKey;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

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
