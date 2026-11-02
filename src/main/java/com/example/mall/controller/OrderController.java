package com.example.mall.controller;

import com.example.mall.common.BusinessException;
import com.example.mall.common.Result;
import com.example.mall.dto.OrderCreateDTO;
import com.example.mall.security.LoginUser;
import com.example.mall.security.UserContext;
import com.example.mall.service.OrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 这里的 catch 是"恢复"而不是"翻译"，所以不放进 GlobalExceptionHandler：
     * 幂等命中时这是一次成功请求，应该返回上一次的订单号，而不是失败响应。
     * 注意：catch 必须在事务方法之外（这里是 Controller），此时事务已整体回滚。
     */
    @PostMapping
    public Result<String> create(@RequestBody @Valid OrderCreateDTO dto) {
        Long userId = UserContext.require().getUserId();
        try {
            return Result.ok(orderService.createOrder(
                    userId, dto.getItems(), dto.getIdempotentKey()));
        } catch (DuplicateKeyException e) {
            String cause = String.valueOf(e.getMostSpecificCause().getMessage());

            // 21 位单号撞车（同一毫秒 + 同一随机数），不能再"装作成功"，否则前端拿到 null 单号
            if (cause.contains("uk_order_no")) {
                throw new BusinessException("订单号生成冲突，请重试");
            }

            // uk_user_idem：另一笔同 key 的请求先成功，本笔已回滚（库存也回滚了）
            String orderNo = orderService.findOrderNo(userId, dto.getIdempotentKey());
            if (orderNo == null) {
                throw new BusinessException("订单正在处理中，请稍后查询订单列表");
            }
            return Result.ok(orderNo);
        }
    }
}
