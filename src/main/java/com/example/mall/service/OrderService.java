package com.example.mall.service;

import com.example.mall.dto.OrderItemDTO;

import java.util.List;

public interface OrderService {
    String createOrder(Long userId, List<OrderItemDTO> items,String idempotentKey);
    String findOrderNo(Long userId, String idempotentKey);
}
