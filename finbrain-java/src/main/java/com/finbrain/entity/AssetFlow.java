package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("asset_flow")
public class AssetFlow implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String flowNo;

    private String flowType;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private BigDecimal frozenBefore;

    private BigDecimal frozenAfter;

    private BigDecimal totalAssetBefore;

    private BigDecimal totalAssetAfter;

    private Long relatedId;

    private String relatedType;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
