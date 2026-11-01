package com.example.mall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.mall.dto.ProductCreateDTO;
import com.example.mall.entity.Product;
import com.example.mall.vo.ProductVO;

public interface ProductService {

    IPage<ProductVO> page(long current, long size, Long categoryId, String keyword);

    Product getById(Long id);

    Long create(ProductCreateDTO dto);

    void update(Long id, ProductCreateDTO dto);

    void remove(Long id);
}
