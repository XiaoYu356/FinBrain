package com.finbrain.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.finbrain.dto.ChangePasswordDTO;
import com.finbrain.dto.LoginDTO;
import com.finbrain.dto.RegisterDTO;
import com.finbrain.dto.UpdateProfileDTO;
import com.finbrain.handler.SentinelBlockHandler;
import com.finbrain.service.AuthService;
import com.finbrain.utils.Result;
import com.finbrain.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @SentinelResource(value = "auth:login", blockHandler = "loginBlockHandler", blockHandlerClass = SentinelBlockHandler.class)
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    @SentinelResource(value = "auth:register", blockHandler = "registerBlockHandler", blockHandlerClass = SentinelBlockHandler.class)
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success();
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    @SentinelResource(value = "auth:refresh", blockHandler = "refreshTokenBlockHandler", blockHandlerClass = SentinelBlockHandler.class)
    public Result<Map<String, Object>> refreshToken(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Result.success(authService.refreshToken(token));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    @SentinelResource(value = "auth:info")
    public Result<UserVO> getCurrentUserInfo() {
        return Result.success(authService.getCurrentUserInfo());
    }

    @Operation(summary = "修改密码")
    @PutMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success();
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UpdateProfileDTO dto) {
        authService.updateProfile(dto);
        return Result.success();
    }
}
