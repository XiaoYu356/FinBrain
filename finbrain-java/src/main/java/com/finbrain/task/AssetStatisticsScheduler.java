package com.finbrain.task;

import com.finbrain.service.AssetStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssetStatisticsScheduler {

    private final AssetStatisticsService assetStatisticsService;

    @Scheduled(cron = "0 5 0 * * ?")
    public void generateDailyStatistics() {
        log.info("========== 开始生成日资产统计 ==========");
        long startTime = System.currentTimeMillis();
        
        try {
            assetStatisticsService.generateAllDailyStatistics();
        } catch (Exception e) {
            log.error("生成日资产统计失败", e);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("========== 日资产统计生成完成, 耗时: {}ms ==========", duration);
    }
}
