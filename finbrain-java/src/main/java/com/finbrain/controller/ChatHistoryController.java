package com.finbrain.controller;

import com.finbrain.service.AuthService;
import com.finbrain.service.ChatHistoryService;
import com.finbrain.utils.Result;
import com.finbrain.vo.ChatHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "对话历史")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;
    private final AuthService authService;

    @Operation(summary = "获取会话列表")
    @GetMapping("/sessions")
    public Result<List<String>> getSessions() {
        Long userId = authService.getCurrentUser().getId();
        return Result.success(chatHistoryService.getSessionIds(userId));
    }

    @Operation(summary = "获取会话历史")
    @GetMapping("/history/{sessionId}")
    public Result<List<ChatHistoryVO>> getHistory(@PathVariable String sessionId) {
        Long userId = authService.getCurrentUser().getId();
        return Result.success(chatHistoryService.getHistoryBySessionId(userId, sessionId));
    }
}
