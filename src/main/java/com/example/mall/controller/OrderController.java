package com.example.mall.controller;


import com.example.mall.common.Result;
import com.example.mall.dto.OrderCreateDTO;
import com.example.mall.service.OrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }


    @PostMapping
    public Result<String> create(@RequestBody @Valid OrderCreateDTO dto) {
        try {
            return Result.ok(orderService.createOrder(
                    dto.getUserId(), dto.getItems(), dto.getIdempotentKey()));
        } catch (DuplicateKeyException e) {
            return Result.ok(orderService.findOrderNo(dto.getUserId(), dto.getIdempotentKey()));
        }
    }
}
