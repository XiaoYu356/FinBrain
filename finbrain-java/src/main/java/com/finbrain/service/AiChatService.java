package com.finbrain.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finbrain.entity.ChatSession;
import com.finbrain.entity.User;
import com.finbrain.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AuthService authService;
    private final ChatSessionMapper chatSessionMapper;

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    public Map<String, Object> chat(String message, String sessionId) {
        User user = authService.getCurrentUser();
        Long userId = user.getId();

        ensureSession(userId, sessionId, message);

        String url = aiServiceUrl + "/chat";
        JSONObject requestBody = new JSONObject();
        requestBody.set("message", message);
        requestBody.set("user_id", userId);
        requestBody.set("session_id", sessionId);

        try {
            log.info("调用AI服务: url={}, message={}", url, message);
            HttpResponse response = HttpRequest.post(url)
                    .body(requestBody.toString())
                    .contentType("application/json")
                    .timeout(120000)
                    .setConnectionTimeout(30000)
                    .setReadTimeout(120000)
                    .execute();

            log.info("AI服务响应: status={}", response.getStatus());
            
            if (!response.isOk()) {
                log.error("AI 服务请求失败, status={}, body={}", response.getStatus(), response.body());
                String fallbackResponse = "抱歉，AI服务暂时不可用，请稍后再试。";
                return Map.of("response", fallbackResponse, "intent", "", "tool_used", "");
            }

            String responseBody = response.body();
            log.info("AI服务响应体长度: {}", responseBody.length());
            log.info("AI服务响应体前200字符: {}", responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody);
            
            JSONObject result = JSONUtil.parseObj(responseBody);
            log.info("解析JSON成功, keys: {}", result.keySet());
            
            String aiResponse = result.getStr("response", "抱歉，我无法处理您的请求。");
            String intent = result.getStr("intent");
            String toolUsed = result.getStr("tool_used");
            
            log.info("AI回复长度: {}", aiResponse != null ? aiResponse.length() : 0);

            return Map.of(
                    "response", aiResponse,
                    "intent", intent != null ? intent : "",
                    "tool_used", toolUsed != null ? toolUsed : ""
            );
        } catch (Exception e) {
            log.error("调用AI服务异常: {}", e.getMessage(), e);
            String fallbackResponse = "抱歉，AI服务暂时不可用，请稍后再试。";
            return Map.of("response", fallbackResponse, "intent", "", "tool_used", "");
        }
    }

    private void ensureSession(Long userId, String sessionId, String firstMessage) {
        ChatSession existing = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>().eq(ChatSession::getSessionId, sessionId)
        );

        String title = firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;

        if (existing == null) {
            ChatSession session = new ChatSession();
            session.setSessionId(sessionId);
            session.setUserId(userId);
            session.setTitle(title);
            chatSessionMapper.insert(session);
            log.info("创建新会话: {}, 标题: {}", sessionId, title);
        } else if ("新会话".equals(existing.getTitle())) {
            existing.setTitle(title);
            existing.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(existing);
            log.info("更新会话标题: {} -> {}", sessionId, title);
        } else {
            existing.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(existing);
        }
    }

    public void deleteSessionMemory(Long userId, String sessionId) {
        try {
            String url = aiServiceUrl + "/session?user_id=" + userId + "&session_id=" + sessionId;
            log.info("调用AI服务删除会话记忆: url={}", url);
            
            HttpResponse response = HttpRequest.delete(url)
                    .timeout(10000)
                    .execute();
            
            if (response.isOk()) {
                log.info("AI服务会话记忆删除成功: sessionId={}", sessionId);
            } else {
                log.warn("AI服务会话记忆删除失败: status={}", response.getStatus());
            }
        } catch (Exception e) {
            log.error("调用AI服务删除会话记忆异常: {}", e.getMessage());
        }
    }
}
