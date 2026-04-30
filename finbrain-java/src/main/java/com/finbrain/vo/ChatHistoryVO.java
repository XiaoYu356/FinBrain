package com.finbrain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatHistoryVO {

    private Long id;

    private String sessionId;

    private String role;

    private String content;

    private String intent;

    private LocalDateTime createTime;
}
