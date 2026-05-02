package com.finbrain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finbrain.entity.ChatSession;
import com.finbrain.mapper.ChatSessionMapper;
import com.finbrain.service.AiChatService;
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
    private final ChatSessionMapper chatSessionMapper;
    private final AiChatService aiChatService;

    @Operation(summary = "获取会话列表")
    @GetMapping("/sessions")
    public Result<List<ChatSession>> getSessions() {
        Long userId = authService.getCurrentUser().getId();
        List<ChatSession> sessions = chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdateTime)
        );
        return Result.success(sessions);
    }

    @Operation(summary = "获取会话历史")
    @GetMapping("/history/{sessionId}")
    public Result<List<ChatHistoryVO>> getHistory(@PathVariable String sessionId) {
        Long userId = authService.getCurrentUser().getId();
        return Result.success(chatHistoryService.getHistoryBySessionId(userId, sessionId));
    }

    @Operation(summary = "创建会话")
    @PostMapping("/sessions")
    public Result<ChatSession> createSession() {
        Long userId = authService.getCurrentUser().getId();
        
        ChatSession session = new ChatSession();
        session.setSessionId(generateSessionId());
        session.setUserId(userId);
        session.setTitle("新会话");
        chatSessionMapper.insert(session);
        
        return Result.success(session);
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        Long userId = authService.getCurrentUser().getId();
        
        chatSessionMapper.delete(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId)
                        .eq(ChatSession::getUserId, userId)
        );
        chatHistoryService.deleteBySessionId(userId, sessionId);
        
        aiChatService.deleteSessionMemory(userId, sessionId);
        
        return Result.success();
    }

    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + 
               java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 9);
    }
}
