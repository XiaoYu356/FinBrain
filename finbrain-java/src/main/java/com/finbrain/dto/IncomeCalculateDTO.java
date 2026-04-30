package com.finbrain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class IncomeCalculateDTO {

    private BigDecimal amount;

    private BigDecimal annualRate;

    private Integer termDays;

    private String investType;

    private Integer investPeriod;
}
