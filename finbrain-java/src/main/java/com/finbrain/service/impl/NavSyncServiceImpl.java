package com.finbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finbrain.client.FundDataClient;
import com.finbrain.dto.NavSyncDTO;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.entity.NavHistory;
import com.finbrain.mapper.FinancialProductMapper;
import com.finbrain.mapper.NavHistoryMapper;
import com.finbrain.service.NavSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NavSyncServiceImpl implements NavSyncService {

    private final FundDataClient fundDataClient;
    private final NavHistoryMapper navHistoryMapper;
    private final FinancialProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncDailyNav() {
        log.info("========== 开始同步每日净值 ==========");
        
        List<NavSyncDTO> navList = fundDataClient.fetchDailyNav();
        
        int successCount = 0;
        int failCount = 0;
        
        for (NavSyncDTO dto : navList) {
            try {
                FinancialProduct product = productMapper.selectOne(
                        new LambdaQueryWrapper<FinancialProduct>()
                                .eq(FinancialProduct::getProductCode, dto.getProductCode())
                );
                
                if (product == null) {
                    log.warn("产品不存在: {}", dto.getProductCode());
                    failCount++;
                    continue;
                }
                
                NavHistory existing = navHistoryMapper.selectOne(
                        new LambdaQueryWrapper<NavHistory>()
                                .eq(NavHistory::getProductId, product.getId())
                                .eq(NavHistory::getNavDate, dto.getNavDate())
                );
                
                if (existing != null) {
                    existing.setNav(dto.getNav());
                    existing.setAccumulatedNav(dto.getAccumulatedNav());
                    existing.setDailyReturnRate(dto.getDailyReturnRate());
                    existing.setDataSource(dto.getDataSource());
                    navHistoryMapper.updateById(existing);
                    log.debug("更新净值: productCode={}, nav={}", dto.getProductCode(), dto.getNav());
                } else {
                    NavHistory navHistory = new NavHistory();
                    navHistory.setProductId(product.getId());
                    navHistory.setProductCode(dto.getProductCode());
                    navHistory.setProductName(product.getProductName());
                    navHistory.setNav(dto.getNav());
                    navHistory.setAccumulatedNav(dto.getAccumulatedNav());
                    navHistory.setNavDate(dto.getNavDate());
                    navHistory.setDailyReturnRate(dto.getDailyReturnRate());
                    navHistory.setDataSource(dto.getDataSource());
                    navHistoryMapper.insert(navHistory);
                    log.debug("新增净值: productCode={}, nav={}", dto.getProductCode(), dto.getNav());
                }
                
                successCount++;
                
            } catch (Exception e) {
                log.error("同步净值失败: productCode={}", dto.getProductCode(), e);
                failCount++;
            }
        }
        
        log.info("========== 净值同步完成: 成功={}, 失败={} ==========", successCount, failCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncNavByProductCode(String productCode) {
        log.info("开始同步产品 {} 的净值", productCode);
        
        NavSyncDTO dto = fundDataClient.fetchNavByProductCode(productCode);
        
        FinancialProduct product = productMapper.selectOne(
                new LambdaQueryWrapper<FinancialProduct>()
                        .eq(FinancialProduct::getProductCode, productCode)
        );
        
        if (product == null) {
            log.warn("产品不存在: {}", productCode);
            return;
        }
        
        NavHistory navHistory = new NavHistory();
        navHistory.setProductId(product.getId());
        navHistory.setProductCode(productCode);
        navHistory.setProductName(product.getProductName());
        navHistory.setNav(dto.getNav());
        navHistory.setAccumulatedNav(dto.getAccumulatedNav());
        navHistory.setNavDate(dto.getNavDate());
        navHistory.setDailyReturnRate(dto.getDailyReturnRate());
        navHistory.setDataSource(dto.getDataSource());
        navHistoryMapper.insert(navHistory);
        
        log.info("产品 {} 净值同步完成: nav={}", productCode, dto.getNav());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncHistoryNav(String productCode, LocalDate startDate, LocalDate endDate) {
        log.info("开始同步产品 {} 从 {} 到 {} 的历史净值", productCode, startDate, endDate);
        
        List<NavSyncDTO> navList = fundDataClient.fetchHistoryNav(productCode, startDate, endDate);
        
        FinancialProduct product = productMapper.selectOne(
                new LambdaQueryWrapper<FinancialProduct>()
                        .eq(FinancialProduct::getProductCode, productCode)
        );
        
        if (product == null) {
            log.warn("产品不存在: {}", productCode);
            return;
        }
        
        int count = 0;
        for (NavSyncDTO dto : navList) {
            NavHistory existing = navHistoryMapper.selectOne(
                    new LambdaQueryWrapper<NavHistory>()
                            .eq(NavHistory::getProductId, product.getId())
                            .eq(NavHistory::getNavDate, dto.getNavDate())
            );
            
            if (existing == null) {
                NavHistory navHistory = new NavHistory();
                navHistory.setProductId(product.getId());
                navHistory.setProductCode(productCode);
                navHistory.setProductName(product.getProductName());
                navHistory.setNav(dto.getNav());
                navHistory.setAccumulatedNav(dto.getAccumulatedNav());
                navHistory.setNavDate(dto.getNavDate());
                navHistory.setDailyReturnRate(dto.getDailyReturnRate());
                navHistory.setDataSource(dto.getDataSource());
                navHistoryMapper.insert(navHistory);
                count++;
            }
        }
        
        log.info("产品 {} 历史净值同步完成，新增 {} 条记录", productCode, count);
    }

    @Override
    public List<NavHistory> getNavHistoryByProductId(Long productId) {
        return navHistoryMapper.selectList(
                new LambdaQueryWrapper<NavHistory>()
                        .eq(NavHistory::getProductId, productId)
                        .orderByDesc(NavHistory::getNavDate)
        );
    }

    @Override
    public NavHistory getLatestNav(Long productId) {
        return navHistoryMapper.selectOne(
                new LambdaQueryWrapper<NavHistory>()
                        .eq(NavHistory::getProductId, productId)
                        .orderByDesc(NavHistory::getNavDate)
                        .last("LIMIT 1")
        );
    }

    @Override
    public BigDecimal calculateNavChange(Long productId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        NavHistory latestNav = getLatestNav(productId);
        if (latestNav == null) {
            return BigDecimal.ZERO;
        }
        
        NavHistory startNav = navHistoryMapper.selectOne(
                new LambdaQueryWrapper<NavHistory>()
                        .eq(NavHistory::getProductId, productId)
                        .le(NavHistory::getNavDate, startDate)
                        .orderByDesc(NavHistory::getNavDate)
                        .last("LIMIT 1")
        );
        
        if (startNav == null) {
            return BigDecimal.ZERO;
        }
        
        return latestNav.getNav().subtract(startNav.getNav())
                .divide(startNav.getNav(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    @Override
    public List<NavSyncDTO> fetchExternalNavData() {
        return fundDataClient.fetchDailyNav();
    }
}
