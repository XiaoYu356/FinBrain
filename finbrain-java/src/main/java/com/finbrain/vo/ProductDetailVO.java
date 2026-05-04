package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductDetailVO {

    private Long id;

    private String productCode;

    private String productName;

    private String productType;

    private BigDecimal annualReturnRate;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private Integer termDays;

    private Integer riskLevel;

    private String issuer;

    private Integer status;

    private String statusName;

    private Integer saleStatus;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate maturityDate;

    private String description;

    private String instructionUrl;

    private Integer remainingDays;

    private Boolean canPurchase;

    private Boolean canRedeem;
}
