package com.finbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.OrderDTO;
import com.finbrain.service.AuthService;
import com.finbrain.service.OrderService;
import com.finbrain.utils.Result;
import com.finbrain.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AuthService authService;

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderDTO dto) {
        Long userId = authService.getCurrentUser().getId();
        return Result.success(orderService.createOrder(userId, dto));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/cancel/{orderId}")
    public Result<Void> cancelOrder(@PathVariable Long orderId) {
        Long userId = authService.getCurrentUser().getId();
        orderService.cancelOrder(userId, orderId);
        return Result.success();
    }

    @Operation(summary = "赎回订单")
    @PostMapping("/redeem/{orderId}")
    public Result<Void> redeemOrder(@PathVariable Long orderId) {
        Long userId = authService.getCurrentUser().getId();
        orderService.redeemOrder(userId, orderId);
        return Result.success();
    }

    @Operation(summary = "获取订单列表")
    @GetMapping("/list")
    public Result<Page<OrderVO>> getOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = authService.getCurrentUser().getId();
        return Result.success(orderService.getOrders(userId, pageNum, pageSize));
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderVO> getOrderById(@PathVariable Long orderId) {
        Long userId = authService.getCurrentUser().getId();
        return Result.success(orderService.getOrders(userId, 1, 1).getRecords().stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElse(null));
    }
}
