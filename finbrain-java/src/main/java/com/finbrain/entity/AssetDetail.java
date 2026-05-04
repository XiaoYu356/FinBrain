package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("asset_detail")
public class AssetDetail implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate statDate;

    private BigDecimal totalAsset;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private BigDecimal holdingAmount;

    private BigDecimal dailyProfit;

    private BigDecimal totalProfit;

    private Integer orderCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
