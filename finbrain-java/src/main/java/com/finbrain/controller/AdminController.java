package com.finbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.annotation.RequireAdmin;
import com.finbrain.dto.AdminProductDTO;
import com.finbrain.service.AdminService;
import com.finbrain.utils.Result;
import com.finbrain.vo.AdminAssetVO;
import com.finbrain.vo.AdminOrderVO;
import com.finbrain.vo.AdminUserVO;
import com.finbrain.vo.ProductVO;
import com.finbrain.vo.StatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

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
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminService.getProductList(pageNum, pageSize));
    }

    @Operation(summary = "添加产品")
    @PostMapping("/products")
    @RequireAdmin
    public Result<Void> addProduct(@RequestBody AdminProductDTO dto) {
        adminService.addProduct(dto);
        return Result.success();
    }

    @Operation(summary = "更新产品")
    @PutMapping("/products/{id}")
    @RequireAdmin
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody AdminProductDTO dto) {
        adminService.updateProduct(id, dto);
        return Result.success();
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
