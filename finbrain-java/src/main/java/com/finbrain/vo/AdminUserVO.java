package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminUserVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private String riskLevel;

    private Integer status;

    private String role;

    private LocalDateTime createTime;

    private BigDecimal totalAsset;

    private BigDecimal availableBalance;

    private BigDecimal totalProfit;
}
