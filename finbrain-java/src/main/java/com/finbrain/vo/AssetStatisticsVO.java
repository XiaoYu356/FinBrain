package com.finbrain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class AssetStatisticsVO {

    private Long userId;

    private BigDecimal totalAsset;

    private BigDecimal availableBalance;

    private BigDecimal frozenBalance;

    private BigDecimal holdingAmount;

    private BigDecimal totalProfit;

    private BigDecimal todayProfit;

    private BigDecimal yesterdayProfit;

    private BigDecimal weekProfit;

    private BigDecimal monthProfit;

    private BigDecimal yearProfit;

    private BigDecimal annualizedReturnRate;

    private Integer totalOrderCount;

    private Integer holdingOrderCount;

    private List<AssetTrendVO> assetTrend;

    private List<ProfitTrendVO> profitTrend;

    @Data
    public static class AssetTrendVO {
        private LocalDate date;
        private BigDecimal totalAsset;
        private BigDecimal profit;
    }

    @Data
    public static class ProfitTrendVO {
        private LocalDate date;
        private BigDecimal dailyProfit;
        private BigDecimal cumulativeProfit;
    }
}
