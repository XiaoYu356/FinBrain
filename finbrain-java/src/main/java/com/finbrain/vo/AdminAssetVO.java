package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminAssetVO {

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private String riskLevel;

    private Integer status;

    private BigDecimal totalAsset;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private BigDecimal totalProfit;
}
