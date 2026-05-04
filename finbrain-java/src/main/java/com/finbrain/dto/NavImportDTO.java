package com.finbrain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class NavImportDTO {

    private String productCode;

    private String productName;

    private BigDecimal nav;

    private BigDecimal accumulatedNav;

    private LocalDate navDate;

    private BigDecimal dailyReturnRate;

    private int rowNum;

    private boolean valid = true;

    private String errorMessage;
}
