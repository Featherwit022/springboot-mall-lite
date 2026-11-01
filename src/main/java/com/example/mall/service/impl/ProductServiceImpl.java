package com.example.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mall.common.BusinessException;
import com.example.mall.dto.ProductCreateDTO;
import com.example.mall.entity.Category;
import com.example.mall.entity.Product;
import com.example.mall.mapper.CategoryMapper;
import com.example.mall.mapper.ProductMapper;
import com.example.mall.service.ProductService;
import com.example.mall.vo.ProductVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    public ProductServiceImpl(ProductMapper productMapper, CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
    }


    @Override
    public IPage<ProductVO> page(long current, long size, Long categoryId, String keyword) {
        return productMapper.selectPageWithCategory(
                new Page<>(current, size), keyword, categoryId);
    }


    @Override
    public Long create(ProductCreateDTO dto) {
        Product product=new Product();
        product.setCategoryId(dto.getCategoryId());
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());
        product.setStatus(1);
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

    @Override
    public ProductVO getByVOId(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在：" + id);
        }
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);
        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            productVO.setCategoryName(category.getName());
        }
        return productVO;
    }
}
