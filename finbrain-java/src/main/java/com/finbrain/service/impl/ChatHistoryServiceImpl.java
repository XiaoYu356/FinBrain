package com.finbrain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finbrain.entity.ChatHistory;
import com.finbrain.mapper.ChatHistoryMapper;
import com.finbrain.service.ChatHistoryService;
import com.finbrain.vo.ChatHistoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatHistoryMapper chatHistoryMapper;

    @Override
    public void saveMessage(Long userId, String sessionId, String role, String content, String intent, String toolCalls) {
        ChatHistory history = new ChatHistory();
        history.setUserId(userId);
        history.setSessionId(sessionId);
        history.setRole(role);
        history.setContent(content);
        history.setIntent(intent);
        if (toolCalls != null && !toolCalls.isEmpty()) {
            if (toolCalls.trim().startsWith("[") || toolCalls.trim().startsWith("{")) {
                history.setToolCalls(toolCalls);
            } else {
                history.setToolCalls(JSONUtil.createArray().put(toolCalls).toString());
            }
        }
        history.setCreateTime(LocalDateTime.now());
        chatHistoryMapper.insert(history);
    }

    @Override
    public List<ChatHistoryVO> getHistoryBySessionId(Long userId, String sessionId) {
        List<ChatHistory> histories = chatHistoryMapper.selectList(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .eq(ChatHistory::getSessionId, sessionId)
                        .orderByAsc(ChatHistory::getCreateTime)
        );
        
        return histories.stream().map(history -> {
            ChatHistoryVO vo = new ChatHistoryVO();
            BeanUtil.copyProperties(history, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getSessionIds(Long userId) {
        return chatHistoryMapper.selectSessionIdsByUserId(userId);
    }

    @Override
    public void deleteBySessionId(Long userId, String sessionId) {
        chatHistoryMapper.delete(
                new LambdaQueryWrapper<ChatHistory>()
                        .eq(ChatHistory::getUserId, userId)
                        .eq(ChatHistory::getSessionId, sessionId)
        );
    }
}
