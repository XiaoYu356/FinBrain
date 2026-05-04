package com.finbrain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductLifecycleVO {

    private Long id;

    private Long productId;

    private String productCode;

    private String productName;

    private Integer fromStatus;

    private String fromStatusName;

    private Integer toStatus;

    private String toStatusName;

    private String triggerType;

    private String triggerReason;

    private String operatorName;

    private LocalDateTime createTime;
}
