package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("product_lifecycle_log")
public class ProductLifecycleLog implements Serializable {

    @TableId(type = IdType.AUTO)
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

    private Long operatorId;

    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
