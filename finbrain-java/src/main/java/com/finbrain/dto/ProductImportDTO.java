package com.finbrain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductImportDTO {

    private String productCode;

    private String productName;

    private String productType;

    private BigDecimal annualReturnRate;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private Integer termDays;

    private Integer riskLevel;

    private String issuer;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate maturityDate;

    private String description;

    private int rowNum;

    private boolean valid = true;

    private String errorMessage;
}
