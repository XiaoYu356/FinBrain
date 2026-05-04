package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("financial_product")
public class FinancialProduct implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productCode;

    private String productName;

    private String productType;

    private BigDecimal annualReturnRate;

    private BigDecimal minAmount;

    private Integer termDays;

    private Integer saleStatus;

    private Integer status;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate maturityDate;

    private BigDecimal maxAmount;

    private Integer riskLevel;

    private String issuer;

    private String description;

    private String instructionUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
