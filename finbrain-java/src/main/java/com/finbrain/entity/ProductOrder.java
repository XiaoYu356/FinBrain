package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("product_order")
public class ProductOrder implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long productId;

    private BigDecimal amount;

    private BigDecimal shares;

    private Integer status;

    private BigDecimal expectedIncome;

    private BigDecimal actualIncome;

    private BigDecimal annualReturnRate;

    private Integer termDays;

    private LocalDate maturityDate;

    private LocalDateTime expireTime;

    private LocalDateTime orderTime;

    private LocalDateTime confirmTime;

    private LocalDateTime successTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private LocalDateTime settleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
