package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatisticsVO {

    private Long totalUsers;

    private Long todayNewUsers;

    private Long totalOrders;

    private BigDecimal totalOrderAmount;

    private BigDecimal totalAssets;

    private BigDecimal totalProfit;

    private Long todayOrders;

    private BigDecimal todayOrderAmount;
}
