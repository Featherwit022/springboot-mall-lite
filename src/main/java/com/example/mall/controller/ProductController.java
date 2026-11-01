package com.example.mall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.mall.common.Result;
import com.example.mall.common.ResultCode;
import com.example.mall.dto.ProductCreateDTO;
import com.example.mall.dto.ProductQueryDTO;
import com.example.mall.entity.Product;
import com.example.mall.service.ProductService;
import com.example.mall.vo.ProductVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
@Validated
@RestController
@RequestMapping("/api/products")

public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    @GetMapping
    public Result<IPage<ProductVO>> page(@Valid ProductQueryDTO query) {
        return Result.ok(productService.page(query.getPage(), query.getSize(), query.getCategoryId(), query.getKeyword()));
    }
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        ProductVO vo = productService.getByVOId(id);
        return vo==null?Result.fail(ResultCode.NOT_FOUND):Result.ok(vo);
    }
    @PostMapping
    public Result<Long> create(@RequestBody @Valid ProductCreateDTO dto){
        return Result.ok(productService.create(dto));
    }
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody ProductCreateDTO dto) {
        productService.update(id, dto);
        return Result.ok();
    }

    @PatchMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        productService.remove(id);
        return Result.ok();
    }

}
