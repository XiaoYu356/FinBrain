package com.finbrain.config;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.finbrain.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
public class SentinelExceptionHandler {

    @ExceptionHandler(BlockException.class)
    @ResponseBody
    public Result<Void> handleBlockException(BlockException e) {
        log.warn("Sentinel block exception: {}", e.getMessage());
        
        String message = "系统繁忙，请稍后再试";
        
        if (e.getMessage() != null) {
            if (e.getMessage().contains("auth:login")) {
                message = "登录请求过于频繁，请1分钟后再试";
            } else if (e.getMessage().contains("auth:register")) {
                message = "注册请求过于频繁，请稍后再试";
            } else if (e.getMessage().contains("ai:chat")) {
                message = "AI对话请求过于频繁，请1分钟后再试";
            } else if (e.getMessage().contains("order:create")) {
                message = "订单创建过于频繁，请1分钟后再试";
            } else if (e.getMessage().contains("product:search")) {
                message = "搜索请求过于频繁，请稍后再试";
            } else if (e.getMessage().contains("data:import")) {
                message = "导入操作过于频繁，请稍后再试";
            }
        }
        
        return Result.error(429, message);
    }
}
