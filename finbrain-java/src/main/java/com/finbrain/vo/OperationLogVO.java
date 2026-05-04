package com.finbrain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {

    private Long id;

    private String traceId;

    private String module;

    private String operation;

    private String description;

    private String method;

    private String requestUrl;

    private String requestMethod;

    private String ip;

    private Long userId;

    private String username;

    private String status;

    private String errorMsg;

    private Long duration;

    private LocalDateTime createTime;
}
