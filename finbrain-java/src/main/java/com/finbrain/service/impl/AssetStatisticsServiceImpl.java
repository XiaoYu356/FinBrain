package com.finbrain.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finbrain.entity.*;
import com.finbrain.enums.OrderStatus;
import com.finbrain.mapper.*;
import com.finbrain.service.AssetStatisticsService;
import com.finbrain.vo.AssetStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetStatisticsServiceImpl implements AssetStatisticsService {

    private final UserAccountMapper userAccountMapper;
    private final AssetDetailMapper assetDetailMapper;
    private final AssetFlowMapper assetFlowMapper;
    private final ProductOrderMapper orderMapper;

    @Override
    public AssetStatisticsVO getAssetStatistics(Long userId) {
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId)
        );
        
        if (account == null) {
            return null;
        }
        
        AssetStatisticsVO vo = new AssetStatisticsVO();
        vo.setUserId(userId);
        vo.setTotalAsset(account.getTotalAsset());
        vo.setAvailableBalance(account.getAvailableBalance());
        vo.setFrozenBalance(account.getFrozenBalance());
        vo.setTotalProfit(account.getTotalProfit());
        
        BigDecimal holdingAmount = calculateHoldingAmount(userId);
        vo.setHoldingAmount(holdingAmount);
        
        LocalDate today = LocalDate.now();
        BigDecimal todayProfit = calculateDailyProfit(userId, today);
        vo.setTodayProfit(todayProfit != null ? todayProfit : BigDecimal.ZERO);
        
        LocalDate yesterday = today.minusDays(1);
        BigDecimal yesterdayProfit = calculateDailyProfit(userId, yesterday);
        vo.setYesterdayProfit(yesterdayProfit != null ? yesterdayProfit : BigDecimal.ZERO);
        
        BigDecimal weekProfit = calculatePeriodProfit(userId, today.minusDays(7), today);
        vo.setWeekProfit(weekProfit);
        
        BigDecimal monthProfit = calculatePeriodProfit(userId, today.minusMonths(1), today);
        vo.setMonthProfit(monthProfit);
        
        BigDecimal yearProfit = calculatePeriodProfit(userId, today.minusYears(1), today);
        vo.setYearProfit(yearProfit);
        
        if (account.getTotalAsset().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal annualizedRate = account.getTotalProfit()
                    .divide(account.getTotalAsset(), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            vo.setAnnualizedReturnRate(annualizedRate);
        } else {
            vo.setAnnualizedReturnRate(BigDecimal.ZERO);
        }
        
        int holdingOrders = countHoldingOrders(userId);
        vo.setHoldingOrderCount(holdingOrders);
        
        Long totalOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<ProductOrder>().eq(ProductOrder::getUserId, userId)
        );
        vo.setTotalOrderCount(totalOrders.intValue());
        
        List<AssetDetail> trend = getAssetTrend(userId, 30);
        List<AssetStatisticsVO.AssetTrendVO> assetTrend = trend.stream().map(detail -> {
            AssetStatisticsVO.AssetTrendVO trendVO = new AssetStatisticsVO.AssetTrendVO();
            trendVO.setDate(detail.getStatDate());
            trendVO.setTotalAsset(detail.getTotalAsset());
            trendVO.setProfit(detail.getDailyProfit());
            return trendVO;
        }).collect(Collectors.toList());
        vo.setAssetTrend(assetTrend);
        
        return vo;
    }

    @Override
    public List<AssetDetail> getAssetTrend(Long userId, Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        
        LambdaQueryWrapper<AssetDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetDetail::getUserId, userId)
                .between(AssetDetail::getStatDate, startDate, endDate)
                .orderByDesc(AssetDetail::getStatDate);
        
        return assetDetailMapper.selectList(wrapper);
    }

    @Override
    public List<AssetFlow> getAssetFlows(Long userId, String flowType, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AssetFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetFlow::getUserId, userId);
        
        if (flowType != null && !flowType.isEmpty()) {
            wrapper.eq(AssetFlow::getFlowType, flowType);
        }
        
        wrapper.orderByDesc(AssetFlow::getCreateTime);
        
        return assetFlowMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAssetFlow(Long userId, String flowType, BigDecimal amount,
                                 BigDecimal balanceBefore, BigDecimal balanceAfter,
                                 BigDecimal frozenBefore, BigDecimal frozenAfter,
                                 BigDecimal totalAssetBefore, BigDecimal totalAssetAfter,
                                 Long relatedId, String relatedType, String remark) {
        AssetFlow flow = new AssetFlow();
        flow.setUserId(userId);
        flow.setFlowNo("AF" + System.currentTimeMillis() + IdUtil.randomUUID().substring(0, 6).toUpperCase());
        flow.setFlowType(flowType);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setFrozenBefore(frozenBefore);
        flow.setFrozenAfter(frozenAfter);
        flow.setTotalAssetBefore(totalAssetBefore);
        flow.setTotalAssetAfter(totalAssetAfter);
        flow.setRelatedId(relatedId);
        flow.setRelatedType(relatedType);
        flow.setRemark(remark);
        flow.setCreateTime(LocalDateTime.now());
        
        assetFlowMapper.insert(flow);
        
        log.info("记录资产流水: userId={}, flowType={}, amount={}", userId, flowType, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateDailyStatistics(Long userId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        AssetDetail existing = assetDetailMapper.selectOne(
                new LambdaQueryWrapper<AssetDetail>()
                        .eq(AssetDetail::getUserId, userId)
                        .eq(AssetDetail::getStatDate, yesterday)
        );
        
        if (existing != null) {
            log.info("用户 {} 的 {} 资产统计已存在，跳过", userId, yesterday);
            return;
        }
        
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId)
        );
        
        if (account == null) {
            return;
        }
        
        BigDecimal dailyProfit = calculateDailyProfit(userId, yesterday);
        BigDecimal holdingAmount = calculateHoldingAmount(userId);
        
        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<ProductOrder>()
                        .eq(ProductOrder::getUserId, userId)
                        .eq(ProductOrder::getStatus, OrderStatus.CONFIRMED.getCode())
        );
        
        AssetDetail detail = new AssetDetail();
        detail.setUserId(userId);
        detail.setStatDate(yesterday);
        detail.setTotalAsset(account.getTotalAsset());
        detail.setAvailableBalance(account.getAvailableBalance());
        detail.setFrozenBalance(account.getFrozenBalance());
        detail.setHoldingAmount(holdingAmount);
        detail.setDailyProfit(dailyProfit != null ? dailyProfit : BigDecimal.ZERO);
        detail.setTotalProfit(account.getTotalProfit());
        detail.setOrderCount(orderCount.intValue());
        detail.setCreateTime(LocalDateTime.now());
        
        assetDetailMapper.insert(detail);
        
        log.info("生成用户 {} 的 {} 资产统计完成", userId, yesterday);
    }

    @Override
    public void generateAllDailyStatistics() {
        log.info("开始生成所有用户的日资产统计...");
        
        List<UserAccount> accounts = userAccountMapper.selectList(null);
        
        int count = 0;
        for (UserAccount account : accounts) {
            try {
                generateDailyStatistics(account.getUserId());
                count++;
            } catch (Exception e) {
                log.error("生成用户 {} 资产统计失败", account.getUserId(), e);
            }
        }
        
        log.info("生成所有用户的日资产统计完成，共处理 {} 个用户", count);
    }

    @Override
    public BigDecimal calculateDailyProfit(Long userId, LocalDate date) {
        LambdaQueryWrapper<AssetFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetFlow::getUserId, userId)
                .eq(AssetFlow::getFlowType, "profit")
                .ge(AssetFlow::getCreateTime, date.atStartOfDay())
                .lt(AssetFlow::getCreateTime, date.plusDays(1).atStartOfDay());
        
        List<AssetFlow> flows = assetFlowMapper.selectList(wrapper);
        
        return flows.stream()
                .map(AssetFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal calculateHoldingAmount(Long userId) {
        List<ProductOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<ProductOrder>()
                        .eq(ProductOrder::getUserId, userId)
                        .eq(ProductOrder::getStatus, OrderStatus.CONFIRMED.getCode())
        );
        
        return orders.stream()
                .map(ProductOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public int countHoldingOrders(Long userId) {
        Long count = orderMapper.selectCount(
                new LambdaQueryWrapper<ProductOrder>()
                        .eq(ProductOrder::getUserId, userId)
                        .eq(ProductOrder::getStatus, OrderStatus.CONFIRMED.getCode())
        );
        return count.intValue();
    }

    private BigDecimal calculatePeriodProfit(Long userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AssetDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AssetDetail::getUserId, userId)
                .ge(AssetDetail::getStatDate, startDate)
                .le(AssetDetail::getStatDate, endDate);
        
        List<AssetDetail> details = assetDetailMapper.selectList(wrapper);
        
        return details.stream()
                .map(AssetDetail::getDailyProfit)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
