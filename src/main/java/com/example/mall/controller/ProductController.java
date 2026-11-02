package com.example.mall.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.mall.annotation.Public;
import com.example.mall.annotation.RequireRole;
import com.example.mall.common.Result;
import com.example.mall.dto.ProductCreateDTO;
import com.example.mall.dto.ProductQueryDTO;
import com.example.mall.security.Role;
import com.example.mall.service.ProductService;
import com.example.mall.vo.ProductVO;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    @Public
    @GetMapping
    public Result<IPage<ProductVO>> page(@Valid ProductQueryDTO query) {
        return Result.ok(productService.page(query.getPage(), query.getSize(),
                query.getCategoryId(), query.getKeyword()));
    }

    /**
     * 查不到时由 Service 抛 BusinessException(NOT_FOUND)，这里不再判 null，
     * 否则"Service 抛异常"和"Controller 返回 4004"两套语义只能生效一套。
     */
    @Public
    @GetMapping("/{id}")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.ok(productService.getByVOId(id));
    }
    @RequireRole(Role.ADMIN)
    @PostMapping
    public Result<Long> create(@RequestBody @Valid ProductCreateDTO dto) {
        return Result.ok(productService.create(dto));
    }
    @RequireRole(Role.ADMIN)
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid ProductCreateDTO dto) {
        productService.update(id, dto);
        return Result.ok();
    }
    @RequireRole(Role.ADMIN)
    @PatchMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        productService.remove(id);
        return Result.ok();
    }
}
