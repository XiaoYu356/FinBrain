package com.finbrain.service;

import com.finbrain.entity.ChatHistory;
import com.finbrain.vo.ChatHistoryVO;

import java.util.List;

public interface ChatHistoryService {

    void saveMessage(Long userId, String sessionId, String role, String content, String intent, String toolCalls);

    List<ChatHistoryVO> getHistoryBySessionId(Long userId, String sessionId);

    List<String> getSessionIds(Long userId);
}
