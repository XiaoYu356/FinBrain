package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetVO {

    private Long userId;

    private BigDecimal totalAsset;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private BigDecimal totalProfit;

    private BigDecimal todayProfit;

    private BigDecimal totalInvest;
}
