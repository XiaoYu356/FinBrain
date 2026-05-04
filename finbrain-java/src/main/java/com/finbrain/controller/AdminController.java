package com.finbrain.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.annotation.RequireAdmin;
import com.finbrain.dto.AdminProductDTO;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.entity.ProductLifecycleLog;
import com.finbrain.enums.ProductStatus;
import com.finbrain.service.AdminService;
import com.finbrain.service.OrderStateMachineService;
import com.finbrain.service.ProductLifecycleService;
import com.finbrain.utils.Result;
import com.finbrain.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "管理员接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ProductLifecycleService productLifecycleService;
    private final OrderStateMachineService orderStateMachineService;

    @Operation(summary = "获取用户列表")
    @GetMapping("/users")
    @RequireAdmin
    public Result<Page<AdminUserVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getUserList(pageNum, pageSize, keyword));
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/users/{id}")
    @RequireAdmin
    public Result<AdminUserVO> getUserDetail(@PathVariable Long id) {
        return Result.success(adminService.getUserDetail(id));
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/users/{id}/status")
    @RequireAdmin
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong((String) authentication.getPrincipal());
        adminService.updateUserStatus(currentUserId, id, status);
        return Result.success();
    }

    @Operation(summary = "获取产品列表(管理员)")
    @GetMapping("/products")
    @RequireAdmin
    public Result<Page<ProductVO>> getProductList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(adminService.getProductList(pageNum, pageSize, status));
    }

    @Operation(summary = "获取产品详情")
    @GetMapping("/products/{id}")
    @RequireAdmin
    public Result<ProductDetailVO> getProductDetail(@PathVariable Long id) {
        FinancialProduct product = adminService.getProductById(id);
        if (product == null) {
            return Result.error("产品不存在");
        }
        
        ProductDetailVO vo = new ProductDetailVO();
        BeanUtil.copyProperties(product, vo);
        
        ProductStatus productStatus = ProductStatus.fromCode(product.getStatus());
        if (productStatus != null) {
            vo.setStatusName(productStatus.getDesc());
        }
        
        if (product.getMaturityDate() != null) {
            long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), product.getMaturityDate());
            vo.setRemainingDays((int) Math.max(0, remainingDays));
        }
        
        vo.setCanPurchase(product.getStatus() != null && 
                (product.getStatus().equals(ProductStatus.FUNDRAISING.getCode()) || 
                 product.getStatus().equals(ProductStatus.OPERATING.getCode())));
        vo.setCanRedeem(product.getStatus() != null && 
                product.getStatus().equals(ProductStatus.OPERATING.getCode()));
        
        return Result.success(vo);
    }

    @Operation(summary = "创建产品")
    @PostMapping("/products")
    @RequireAdmin
    public Result<Void> createProduct(@RequestBody AdminProductDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong((String) authentication.getPrincipal());
        productLifecycleService.createProduct(dto, currentUserId);
        return Result.success();
    }

    @Operation(summary = "更新产品")
    @PutMapping("/products/{id}")
    @RequireAdmin
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody AdminProductDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong((String) authentication.getPrincipal());
        productLifecycleService.updateProduct(id, dto, currentUserId);
        return Result.success();
    }

    @Operation(summary = "发布产品")
    @PostMapping("/products/{id}/publish")
    @RequireAdmin
    public Result<Void> publishProduct(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong((String) authentication.getPrincipal());
        productLifecycleService.publishProduct(id, currentUserId);
        return Result.success();
    }

    @Operation(summary = "下架产品")
    @PostMapping("/products/{id}/delist")
    @RequireAdmin
    public Result<Void> delistProduct(@PathVariable Long id, @RequestParam(required = false) String reason) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong((String) authentication.getPrincipal());
        productLifecycleService.delistProduct(id, reason != null ? reason : "管理员手动下架", currentUserId);
        return Result.success();
    }

    @Operation(summary = "获取产品生命周期日志")
    @GetMapping("/products/{id}/lifecycle")
    @RequireAdmin
    public Result<List<ProductLifecycleVO>> getProductLifecycle(@PathVariable Long id) {
        List<ProductLifecycleLog> logs = productLifecycleService.getLifecycleLogs(id);
        List<ProductLifecycleVO> voList = logs.stream().map(log -> {
            ProductLifecycleVO vo = new ProductLifecycleVO();
            BeanUtil.copyProperties(log, vo);
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }

    @Operation(summary = "获取产品状态统计")
    @GetMapping("/products/statistics")
    @RequireAdmin
    public Result<Map<String, Long>> getProductStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        statistics.put("draft", productLifecycleService.countProductsByStatus(ProductStatus.DRAFT));
        statistics.put("fundraising", productLifecycleService.countProductsByStatus(ProductStatus.FUNDRAISING));
        statistics.put("operating", productLifecycleService.countProductsByStatus(ProductStatus.OPERATING));
        statistics.put("matured", productLifecycleService.countProductsByStatus(ProductStatus.MATURED));
        statistics.put("delisted", productLifecycleService.countProductsByStatus(ProductStatus.DELISTED));
        return Result.success(statistics);
    }

    @Operation(summary = "获取即将到期产品")
    @GetMapping("/products/expiring")
    @RequireAdmin
    public Result<List<ProductDetailVO>> getExpiringProducts(@RequestParam(defaultValue = "7") Integer days) {
        List<FinancialProduct> products = productLifecycleService.getExpiringProducts(days);
        List<ProductDetailVO> voList = products.stream().map(product -> {
            ProductDetailVO vo = new ProductDetailVO();
            BeanUtil.copyProperties(product, vo);
            ProductStatus status = ProductStatus.fromCode(product.getStatus());
            if (status != null) {
                vo.setStatusName(status.getDesc());
            }
            if (product.getMaturityDate() != null) {
                long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), product.getMaturityDate());
                vo.setRemainingDays((int) Math.max(0, remainingDays));
            }
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }

    @Operation(summary = "更新产品销售状态")
    @PutMapping("/products/{id}/sale-status")
    @RequireAdmin
    public Result<Void> updateProductSaleStatus(@PathVariable Long id, @RequestParam Integer saleStatus) {
        adminService.updateProductSaleStatus(id, saleStatus);
        return Result.success();
    }

    @Operation(summary = "删除产品")
    @DeleteMapping("/products/{id}")
    @RequireAdmin
    public Result<Void> deleteProduct(@PathVariable Long id) {
        adminService.deleteProduct(id);
        return Result.success();
    }

    @Operation(summary = "获取订单列表(管理员)")
    @GetMapping("/orders")
    @RequireAdmin
    public Result<Page<AdminOrderVO>> getOrderList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        return Result.success(adminService.getOrderList(pageNum, pageSize, userId, status));
    }

    @Operation(summary = "获取订单详情(管理员)")
    @GetMapping("/orders/{id}")
    @RequireAdmin
    public Result<AdminOrderVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(adminService.getOrderDetail(id));
    }

    @Operation(summary = "确认订单")
    @PostMapping("/orders/{id}/confirm")
    @RequireAdmin
    public Result<Void> confirmOrder(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong((String) authentication.getPrincipal());
        orderStateMachineService.confirmOrder(id, currentUserId);
        return Result.success();
    }

    @Operation(summary = "获取用户资产列表")
    @GetMapping("/assets")
    @RequireAdmin
    public Result<Page<AdminAssetVO>> getAssetList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminService.getAssetList(pageNum, pageSize, keyword));
    }

    @Operation(summary = "获取统计数据")
    @GetMapping("/statistics")
    @RequireAdmin
    public Result<StatisticsVO> getStatistics() {
        return Result.success(adminService.getStatistics());
    }
}
