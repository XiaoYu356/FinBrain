package com.finbrain.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.finbrain.utils.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class SentinelBlockHandler {

    public static Result<Map<String, Object>> loginBlockHandler(BlockException e) {
        log.warn("Login blocked by Sentinel: {}", e.getMessage());
        return Result.error(429, "登录请求过于频繁，请1分钟后再试");
    }

    public static Result<Void> registerBlockHandler(BlockException e) {
        log.warn("Register blocked by Sentinel: {}", e.getMessage());
        return Result.error(429, "注册请求过于频繁，请稍后再试");
    }

    public static Result<Map<String, Object>> refreshTokenBlockHandler(BlockException e) {
        log.warn("Refresh token blocked by Sentinel: {}", e.getMessage());
        return Result.error(429, "Token刷新过于频繁，请稍后再试");
    }

    public static Result<Map<String, Object>> chatBlockHandler(BlockException e) {
        log.warn("AI chat blocked by Sentinel: {}", e.getMessage());
        return Result.error(429, "AI对话请求过于频繁，请1分钟后再试");
    }

    public static Result<Map<String, Object>> createOrderBlockHandler(BlockException e) {
        log.warn("Create order blocked by Sentinel: {}", e.getMessage());
        return Result.error(429, "订单创建过于频繁，请1分钟后再试");
    }

    public static Result<Void> cancelOrderBlockHandler(BlockException e) {
        log.warn("Cancel order blocked by Sentinel: {}", e.getMessage());
        return Result.error(429, "订单操作过于频繁，请稍后再试");
    }

    public static Result<Void> redeemOrderBlockHandler(BlockException e) {
        log.warn("Redeem order blocked by Sentinel: {}", e.getMessage());
        return Result.error(429, "订单操作过于频繁，请稍后再试");
    }

    public static Result<Map<String, Object>> searchProductsBlockHandler(BlockException e) {
        log.warn("Search products blocked by Sentinel: {}", e.getMessage());
        return Result.error(429, "搜索请求过于频繁，请稍后再试");
    }

    public static Result<Map<String, Object>> importProductsBlockHandler(BlockException e) {
        log.warn("Import products blocked by Sentinel: {}", e.getMessage());
        return Result.error(429, "导入操作过于频繁，请稍后再试");
    }

    public static Result<Map<String, Object>> importNavBlockHandler(BlockException e) {
        log.warn("Import nav blocked by Sentinel: {}", e.getMessage());
        return Result.error(429, "导入操作过于频繁，请稍后再试");
    }
}
