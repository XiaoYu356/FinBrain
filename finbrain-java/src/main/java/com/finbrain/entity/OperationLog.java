package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;

    private String module;

    private String operation;

    private String description;

    private String method;

    private String requestUrl;

    private String requestMethod;

    private String requestParams;

    private String responseData;

    private String ip;

    private String userAgent;

    private Long userId;

    private String username;

    private String status;

    private String errorMsg;

    private Long duration;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
