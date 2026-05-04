package com.finbrain.service;

import com.finbrain.entity.AssetDetail;
import com.finbrain.entity.AssetFlow;
import com.finbrain.vo.AssetStatisticsVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AssetStatisticsService {

    AssetStatisticsVO getAssetStatistics(Long userId);

    List<AssetDetail> getAssetTrend(Long userId, Integer days);

    List<AssetFlow> getAssetFlows(Long userId, String flowType, Integer pageNum, Integer pageSize);

    void recordAssetFlow(Long userId, String flowType, BigDecimal amount, 
                         BigDecimal balanceBefore, BigDecimal balanceAfter,
                         BigDecimal frozenBefore, BigDecimal frozenAfter,
                         BigDecimal totalAssetBefore, BigDecimal totalAssetAfter,
                         Long relatedId, String relatedType, String remark);

    void generateDailyStatistics(Long userId);

    void generateAllDailyStatistics();

    BigDecimal calculateDailyProfit(Long userId, LocalDate date);

    BigDecimal calculateHoldingAmount(Long userId);

    int countHoldingOrders(Long userId);
}
