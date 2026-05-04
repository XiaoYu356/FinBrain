package com.finbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.OrderDTO;
import com.finbrain.enums.OrderStatus;
import com.finbrain.service.AuthService;
import com.finbrain.service.OrderService;
import com.finbrain.service.OrderStateMachineService;
import com.finbrain.utils.Result;
import com.finbrain.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderStateMachineService orderStateMachineService;
    private final AuthService authService;

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderDTO dto) {
        Long userId = authService.getCurrentUser().getId();
        return Result.success(orderStateMachineService.createOrder(userId, dto));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/cancel/{orderId}")
    public Result<Void> cancelOrder(@PathVariable Long orderId, @RequestParam(required = false) String reason) {
        Long userId = authService.getCurrentUser().getId();
        orderStateMachineService.cancelOrder(userId, orderId, reason);
        return Result.success();
    }

    @Operation(summary = "赎回订单")
    @PostMapping("/redeem/{orderId}")
    public Result<Void> redeemOrder(@PathVariable Long orderId) {
        Long userId = authService.getCurrentUser().getId();
        orderStateMachineService.redeemOrder(userId, orderId);
        return Result.success();
    }

    @Operation(summary = "获取订单列表")
    @GetMapping("/list")
    public Result<Page<OrderVO>> getOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = authService.getCurrentUser().getId();
        Page<OrderVO> result = orderService.getOrders(userId, pageNum, pageSize);
        
        result.getRecords().forEach(vo -> {
            OrderStatus orderStatus = OrderStatus.fromCode(vo.getStatus());
            vo.setCanCancel(OrderStatus.canCancel(orderStatus));
            vo.setCanRedeem(OrderStatus.canRedeem(orderStatus));
            
            if (vo.getMaturityDate() != null) {
                long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), vo.getMaturityDate());
                vo.setRemainingDays((int) Math.max(0, remainingDays));
            }
        });
        
        return Result.success(result);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderVO> getOrderById(@PathVariable Long orderId) {
        Long userId = authService.getCurrentUser().getId();
        Page<OrderVO> orders = orderService.getOrders(userId, 1, 100);
        
        OrderVO vo = orders.getRecords().stream()
                .filter(o -> o.getId().equals(orderId))
                .findFirst()
                .orElse(null);
        
        if (vo != null) {
            OrderStatus orderStatus = OrderStatus.fromCode(vo.getStatus());
            vo.setCanCancel(OrderStatus.canCancel(orderStatus));
            vo.setCanRedeem(OrderStatus.canRedeem(orderStatus));
            
            if (vo.getMaturityDate() != null) {
                long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), vo.getMaturityDate());
                vo.setRemainingDays((int) Math.max(0, remainingDays));
            }
        }
        
        return Result.success(vo);
    }
}
