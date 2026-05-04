package com.finbrain.task;

import com.finbrain.service.NavSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NavSyncScheduler {

    private final NavSyncService navSyncService;

    @Scheduled(cron = "0 30 9 * * ?")
    public void syncDailyNav() {
        log.info("========== 开始执行每日净值同步定时任务 ==========");
        long startTime = System.currentTimeMillis();
        
        try {
            navSyncService.syncDailyNav();
        } catch (Exception e) {
            log.error("每日净值同步定时任务执行失败", e);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("========== 每日净值同步定时任务完成, 耗时: {}ms ==========", duration);
    }

    @Scheduled(cron = "0 0 15 * * ?")
    public void syncDailyNavAfternoon() {
        log.info("========== 开始执行下午净值同步定时任务 ==========");
        
        try {
            navSyncService.syncDailyNav();
        } catch (Exception e) {
            log.error("下午净值同步定时任务执行失败", e);
        }
    }
}
