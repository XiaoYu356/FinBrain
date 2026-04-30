package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long productId;

    private String productName;

    private String productCode;

    private String productType;

    private BigDecimal annualReturnRate;

    private BigDecimal amount;

    private BigDecimal shares;

    private Integer status;

    private String statusText;

    private LocalDateTime orderTime;

    private LocalDateTime successTime;
}
