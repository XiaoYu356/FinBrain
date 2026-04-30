package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminOrderVO {

    private Long id;

    private String orderNo;

    private Long userId;

    private String username;

    private Long productId;

    private String productName;

    private String productCode;

    private BigDecimal amount;

    private BigDecimal shares;

    private Integer status;

    private String statusText;

    private LocalDateTime orderTime;

    private LocalDateTime successTime;
}
