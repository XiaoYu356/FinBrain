package com.finbrain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminProductDTO {

    private String productCode;

    private String productName;

    private String productType;

    private BigDecimal annualReturnRate;

    private BigDecimal minAmount;

    private Integer termDays;

    private String description;

    private String instructionUrl;
}
