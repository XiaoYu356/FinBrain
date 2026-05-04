package com.finbrain.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.finbrain.handler.SentinelBlockHandler;
import com.finbrain.service.AiChatService;
import com.finbrain.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI对话")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @Operation(summary = "发送聊天消息")
    @PostMapping("/chat")
    @SentinelResource(value = "ai:chat", blockHandler = "chatBlockHandler", blockHandlerClass = SentinelBlockHandler.class)
    public Result<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.get("sessionId");
        
        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息不能为空");
        }
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Result.error("会话ID不能为空");
        }
        
        Map<String, Object> result = aiChatService.chat(message, sessionId);
        return Result.success(result);
    }
}
