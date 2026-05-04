package com.finbrain.task;

import com.finbrain.service.OrderStateMachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusScheduler {

    private final OrderStateMachineService orderStateMachineService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void processExpiredOrders() {
        log.info("========== 订单超时检查定时任务开始 ==========");
        long startTime = System.currentTimeMillis();
        
        try {
            orderStateMachineService.processExpiredOrders();
        } catch (Exception e) {
            log.error("订单超时检查定时任务执行失败", e);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("========== 订单超时检查定时任务结束, 耗时: {}ms ==========", duration);
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void processMaturedOrders() {
        log.info("========== 订单到期结算定时任务开始 ==========");
        long startTime = System.currentTimeMillis();
        
        try {
            orderStateMachineService.processMaturedOrders();
        } catch (Exception e) {
            log.error("订单到期结算定时任务执行失败", e);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("========== 订单到期结算定时任务结束, 耗时: {}ms ==========", duration);
    }

    @Scheduled(cron = "0 30 * * * ?")
    public void logOrderStatistics() {
        try {
            long expiredCount = orderStateMachineService.getExpiredOrders().size();
            long maturedCount = orderStateMachineService.getMaturedOrders().size();
            
            log.info("订单状态统计 - 待处理过期订单: {}, 待处理到期订单: {}", expiredCount, maturedCount);
        } catch (Exception e) {
            log.error("统计订单状态失败", e);
        }
    }
}
