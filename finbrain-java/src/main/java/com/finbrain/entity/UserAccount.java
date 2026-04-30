package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_account")
public class UserAccount implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private BigDecimal totalAsset;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private BigDecimal totalProfit;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
