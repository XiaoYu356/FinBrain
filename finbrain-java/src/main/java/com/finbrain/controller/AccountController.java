package com.finbrain.controller;

import com.finbrain.service.AccountService;
import com.finbrain.service.AuthService;
import com.finbrain.utils.Result;
import com.finbrain.vo.AssetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "账户管理")
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AuthService authService;

    @Operation(summary = "获取用户资产信息")
    @GetMapping("/asset")
    public Result<AssetVO> getAssetInfo(@RequestParam Long userId) {
        return Result.success(accountService.getAssetInfo(userId));
    }

    @Operation(summary = "获取当前用户资产")
    @GetMapping("/my-asset")
    public Result<AssetVO> getMyAssetInfo() {
        Long userId = authService.getCurrentUser().getId();
        return Result.success(accountService.getAssetInfo(userId));
    }

    @Operation(summary = "充值")
    @PostMapping("/recharge")
    public Result<Map<String, Object>> recharge(@RequestParam BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.error("充值金额必须大于0");
        }
        Long userId = authService.getCurrentUser().getId();
        accountService.recharge(userId, amount);
        return Result.success("充值成功", Map.of("amount", amount));
    }
}
