package com.example.mall.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class ProductQueryDTO {
    private String keyword;
    private Long categoryId;
    @Min(value = 1, message = "page 从 1 开始")
    private Integer page = 1;
    @Min(value = 1, message = "size 最小 1")
    @Max(value = 100, message = "size 最大 100")
    private Integer size = 10;




    // getter / setter 省略

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = (page == null) ? 1 : page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = (size == null) ? 10 : size;
    }
}