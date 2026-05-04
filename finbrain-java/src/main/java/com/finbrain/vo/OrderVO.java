package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private BigDecimal expectedIncome;

    private BigDecimal actualIncome;

    private Integer termDays;

    private LocalDate maturityDate;

    private LocalDateTime expireTime;

    private LocalDateTime orderTime;

    private LocalDateTime confirmTime;

    private LocalDateTime successTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private LocalDateTime settleTime;

    private Integer remainingDays;

    private Boolean canCancel;

    private Boolean canRedeem;
}
