package com.example.mall.service;

import com.example.mall.common.BusinessException;
import com.example.mall.dto.OrderItemDTO;
import com.example.mall.entity.Product;
import com.example.mall.mapper.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional(rollbackFor = Exception.class)       // ← 关键：测试方法结束后自动回滚，不污染真实数据库
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductMapper productMapper;

    @Test
    @DisplayName("库存不足时，订单不应写入，库存不应变化")
    void 库存不足应整体回滚() {
        // given
        Product before = productMapper.selectById(1L);
        assertNotNull(before, "测试数据缺失：product id=1 不存在");
        int stockBefore = before.getStock();

        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(1L);
        item.setQuantity(stockBefore + 1000);        // 明显超过库存

        // when
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(1L, List.of(item), null));

        // then
        assertTrue(ex.getMessage().contains("库存不足"), "异常信息应指明库存不足");
        assertEquals(stockBefore, productMapper.selectById(1L).getStock(),
                "事务应回滚，库存必须保持原值");
    }
    @Test
    @DisplayName("同一幂等键重复下单，只产生一个订单")
    void 幂等键应只落一单() {
        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(1L);
        item.setQuantity(1);

        String key = "test-idem-" + System.currentTimeMillis();

        String first = orderService.createOrder(1L, List.of(item), key);
        int stockAfterFirst = productMapper.selectById(1L).getStock();

        String second = orderService.createOrder(1L, List.of(item), key);
        int stockAfterSecond = productMapper.selectById(1L).getStock();

        assertEquals(first, second, "同一 key 应返回同一个单号");
        assertEquals(stockAfterFirst, stockAfterSecond, "第二次不应重复扣库存");
    }
}