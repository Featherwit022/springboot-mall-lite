package com.example.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.mall.entity.Product;
import com.example.mall.vo.ProductVO;
import org.apache.ibatis.annotations.Param;

public interface ProductMapper extends BaseMapper<Product> {
    IPage<ProductVO> selectPageWithCategory(IPage<ProductVO> page,
                                            @Param("keyword") String keyword,
                                            @Param("categoryId") Long categoryId);

    int deductStock(@Param("id") Long id, @Param("qty") Integer qty);
    Product selectByIdForUpdate(@Param("id") Long id);
}
