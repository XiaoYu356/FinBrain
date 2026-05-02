package com.finbrain.controller.ai;

import com.finbrain.service.ChatHistoryService;
import com.finbrain.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "AI工具接口")
@RestController
@RequestMapping("/ai-tools")
@RequiredArgsConstructor
public class AiHistoryController {

    private final ChatHistoryService chatHistoryService;

    @Operation(summary = "获取会话历史(内部调用)")
    @GetMapping("/chat-history/{sessionId}")
    public Result<List<?>> getChatHistory(
            @PathVariable String sessionId,
            @RequestParam Long userId) {
        return Result.success(chatHistoryService.getHistoryBySessionId(userId, sessionId));
    }

    @Operation(summary = "保存聊天消息(内部调用)")
    @PostMapping("/chat-message")
    public Result<Void> saveChatMessage(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String sessionId = (String) request.get("sessionId");
        String role = (String) request.get("role");
        String content = (String) request.get("content");
        
        chatHistoryService.saveMessage(userId, sessionId, role, content, null, null);
        return Result.success();
    }

    @Operation(summary = "批量保存聊天消息(内部调用)")
    @PostMapping("/chat-messages")
    public Result<Void> saveChatMessages(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String sessionId = (String) request.get("sessionId");
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
        
        if (messages != null) {
            for (Map<String, String> msg : messages) {
                String role = msg.get("role");
                String content = msg.get("content");
                chatHistoryService.saveMessage(userId, sessionId, role, content, null, null);
            }
        }
        return Result.success();
    }
}
