package com.example.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mall.common.BusinessException;
import com.example.mall.config.MallProperties;
import com.example.mall.dto.OrderItemDTO;
import com.example.mall.entity.Order;
import com.example.mall.entity.OrderItem;
import com.example.mall.entity.Product;
import com.example.mall.mapper.OrderItemMapper;
import com.example.mall.mapper.OrderMapper;
import com.example.mall.mapper.ProductMapper;
import com.example.mall.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderServiceImpl implements OrderService {
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final MallProperties mallProperties;
    public OrderServiceImpl(ProductMapper productMapper,
                            OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            MallProperties mallProperties) {
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.mallProperties = mallProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(Long userId, List<OrderItemDTO> items, String idempotentKey) {
        if (idempotentKey != null && !idempotentKey.isEmpty()) {
            Order exist = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                    .eq(Order::getUserId, userId)
                    .eq(Order::getIdempotentKey, idempotentKey));
            if (exist != null) {
                return exist.getOrderNo();
            }
        }
        if(items==null||items.isEmpty()){
            throw new BusinessException("订单不能为空");
        }
        if(items.size()>mallProperties.getOrder().getMaxItems()){
            throw new BusinessException("单笔最多 " + mallProperties.getOrder().getMaxItems() + " 件");
        }
        BigDecimal total=BigDecimal.ZERO;
        List<OrderItem> rows = new ArrayList<>();
        for(OrderItemDTO item:items){
            Product p=productMapper.selectByIdForUpdate(item.getProductId());
            if(p==null||p.getStatus()!=1){
                throw new BusinessException("商品不存在或已下架: " + item.getProductId());
            }
            int affected =productMapper.deductStock(item.getProductId(),item.getQuantity());
            if(affected ==0){
                throw new BusinessException("库存不足: " + p.getName());
            }
            total=total.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            OrderItem row =new OrderItem();
            row.setProductId(p.getId());
            row.setPrice(p.getPrice());
            row.setProductName(p.getName());
            row.setQuantity(item.getQuantity());
            rows.add(row);
        }
        Order order=new Order();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setStatus(0);
        order.setIdempotentKey(idempotentKey);
        orderMapper.insert(order);
        for (OrderItem row : rows) {
            row.setOrderId(order.getId());                 // 用回填的 id 关联
            orderItemMapper.insert(row);
        }
        return  order.getOrderNo();
    }
    @Override
    public String findOrderNo(Long userId, String idempotentKey) {
        if (idempotentKey == null || idempotentKey.isEmpty()) return null;
        Order o = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getIdempotentKey, idempotentKey));
        return o == null ? null : o.getOrderNo();
    }
    private static final DateTimeFormatter ORDER_NO_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private String nextOrderNo(){
        return LocalDateTime.now().format(ORDER_NO_TIME)
                +String.format("%04d", ThreadLocalRandom.current().nextInt(10000));

    }
}

