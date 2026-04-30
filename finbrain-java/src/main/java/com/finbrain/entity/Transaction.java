package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transaction")
public class Transaction implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String transactionNo;

    private Long userId;

    private Long orderId;

    private String type;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String remark;

    private LocalDateTime createTime;
}
