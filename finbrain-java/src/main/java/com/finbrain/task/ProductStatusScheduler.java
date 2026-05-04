package com.finbrain.task;

import com.finbrain.entity.FinancialProduct;
import com.finbrain.enums.ProductStatus;
import com.finbrain.service.ProductLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStatusScheduler {

    private final ProductLifecycleService productLifecycleService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void processStatusTransition() {
        log.info("========== 产品状态流转定时任务开始 ==========");
        long startTime = System.currentTimeMillis();
        
        try {
            productLifecycleService.processStatusTransition();
        } catch (Exception e) {
            log.error("产品状态流转定时任务执行失败", e);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("========== 产品状态流转定时任务结束, 耗时: {}ms ==========", duration);
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkExpiringProducts() {
        log.info("========== 检查即将到期产品 ==========");
        
        try {
            List<FinancialProduct> expiringIn7Days = productLifecycleService.getExpiringProducts(7);
            List<FinancialProduct> expiringIn3Days = productLifecycleService.getExpiringProducts(3);
            List<FinancialProduct> expiringIn1Day = productLifecycleService.getExpiringProducts(1);
            
            log.info("7天内到期产品数量: {}", expiringIn7Days.size());
            log.info("3天内到期产品数量: {}", expiringIn3Days.size());
            log.info("1天内到期产品数量: {}", expiringIn1Day.size());
            
            if (!expiringIn1Day.isEmpty()) {
                log.warn("以下产品即将在1天内到期:");
                expiringIn1Day.forEach(p -> log.warn("  - [{}] {} (到期日: {})", 
                        p.getProductCode(), p.getProductName(), p.getMaturityDate()));
            }
        } catch (Exception e) {
            log.error("检查即将到期产品失败", e);
        }
    }

    @Scheduled(cron = "0 30 * * * ?")
    public void logProductStatistics() {
        try {
            long draftCount = productLifecycleService.countProductsByStatus(ProductStatus.DRAFT);
            long fundraisingCount = productLifecycleService.countProductsByStatus(ProductStatus.FUNDRAISING);
            long operatingCount = productLifecycleService.countProductsByStatus(ProductStatus.OPERATING);
            long maturedCount = productLifecycleService.countProductsByStatus(ProductStatus.MATURED);
            long delistedCount = productLifecycleService.countProductsByStatus(ProductStatus.DELISTED);
            
            log.info("产品状态统计 - 草稿: {}, 募集期: {}, 存续期: {}, 已到期: {}, 已下架: {}", 
                    draftCount, fundraisingCount, operatingCount, maturedCount, delistedCount);
        } catch (Exception e) {
            log.error("统计产品状态失败", e);
        }
    }
}
