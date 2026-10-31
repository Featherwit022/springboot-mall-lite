package com.example.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.BusinessException;
import com.example.mall.dto.ProductCreateDTO;
import com.example.mall.entity.Product;
import com.example.mall.mapper.ProductMapper;
import com.example.mall.service.ProductService;
import com.example.mall.vo.ProductVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }


    @Override
    public IPage<ProductVO> page(long current, long size, Long categoryId, String keyword) {
        return productMapper.selectPageWithCategory(
                new Page<>(current, size), keyword, categoryId);
    }

    @Override
    public Product getById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public Long create(ProductCreateDTO dto) {
        Product product=new Product();
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setStatus(1);
        product.setCreateTime(LocalDateTime.now());
        int rows=productMapper.insert(product);
        return rows==0?null:product.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductCreateDTO dto) {
        Product product=new Product();
        product.setId(id);
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        int rows=productMapper.updateById(product);
        if(rows==0){
            throw new BusinessException("商品不存在或未变更: " + id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(0);                  // 0 = 下架
        int rows = productMapper.updateById(product);
        if (rows == 0) {
            throw new BusinessException("商品不存在: " + id);
        }
    }
}
