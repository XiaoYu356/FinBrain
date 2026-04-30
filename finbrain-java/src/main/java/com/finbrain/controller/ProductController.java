package com.finbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.ProductSearchDTO;
import com.finbrain.service.ProductService;
import com.finbrain.utils.Result;
import com.finbrain.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "产品管理")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "搜索产品列表")
    @PostMapping("/search")
    public Result<Page<ProductVO>> searchProducts(@RequestBody ProductSearchDTO dto) {
        return Result.success(productService.searchProducts(dto));
    }

    @Operation(summary = "获取产品详情")
    @GetMapping("/{id}")
    public Result<ProductVO> getProductById(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }

    @Operation(summary = "获取在售产品列表")
    @GetMapping("/list")
    public Result<Page<ProductVO>> getProductList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        ProductSearchDTO dto = new ProductSearchDTO();
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        dto.setSaleStatus(1);
        return Result.success(productService.searchProducts(dto));
    }
}
