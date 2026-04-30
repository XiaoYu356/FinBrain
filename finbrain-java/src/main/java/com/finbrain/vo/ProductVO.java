package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVO {

    private Long id;

    private String productCode;

    private String productName;

    private String productType;

    private BigDecimal annualReturnRate;

    private BigDecimal minAmount;

    private Integer termDays;

    private Integer saleStatus;

    private String description;

    private String instructionUrl;
}
