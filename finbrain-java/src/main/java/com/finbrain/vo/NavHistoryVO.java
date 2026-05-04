package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class NavHistoryVO {

    private Long id;

    private Long productId;

    private String productCode;

    private String productName;

    private BigDecimal nav;

    private BigDecimal accumulatedNav;

    private LocalDate navDate;

    private BigDecimal dailyReturnRate;

    private String dataSource;

    private LocalDateTime createTime;
}
