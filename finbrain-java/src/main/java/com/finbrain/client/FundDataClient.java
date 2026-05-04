package com.finbrain.client;

import com.finbrain.dto.NavSyncDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class FundDataClient {

    private final Random random = new Random();

    public List<NavSyncDTO> fetchDailyNav() {
        log.info("模拟从外部数据源获取每日净值数据...");
        
        List<NavSyncDTO> result = new ArrayList<>();
        
        result.add(createNavData("FB001", "稳健增值计划A", new BigDecimal("1.0234")));
        result.add(createNavData("FB002", "平衡成长计划B", new BigDecimal("1.0567")));
        result.add(createNavData("FB003", "进取收益计划C", new BigDecimal("1.0892")));
        result.add(createNavData("FB004", "灵活理财宝", new BigDecimal("1.0028")));
        
        log.info("模拟获取净值数据完成，共 {} 条", result.size());
        return result;
    }

    public NavSyncDTO fetchNavByProductCode(String productCode) {
        log.info("模拟获取产品 {} 的净值数据", productCode);
        
        BigDecimal baseNav = new BigDecimal("1.0" + (random.nextInt(20) + 10));
        return createNavData(productCode, "产品" + productCode, baseNav);
    }

    public List<NavSyncDTO> fetchHistoryNav(String productCode, LocalDate startDate, LocalDate endDate) {
        log.info("模拟获取产品 {} 从 {} 到 {} 的历史净值", productCode, startDate, endDate);
        
        List<NavSyncDTO> result = new ArrayList<>();
        BigDecimal nav = new BigDecimal("1.0000");
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            BigDecimal dailyChange = new BigDecimal(random.nextDouble() * 0.02 - 0.01)
                    .setScale(4, RoundingMode.HALF_UP);
            nav = nav.add(dailyChange).max(new BigDecimal("0.5"));
            
            NavSyncDTO dto = new NavSyncDTO();
            dto.setProductCode(productCode);
            dto.setNav(nav);
            dto.setAccumulatedNav(nav);
            dto.setNavDate(current);
            dto.setDailyReturnRate(dailyChange.multiply(new BigDecimal("100")));
            dto.setDataSource("API");
            
            result.add(dto);
            current = current.plusDays(1);
        }
        
        return result;
    }

    private NavSyncDTO createNavData(String productCode, String productName, BigDecimal baseNav) {
        BigDecimal dailyChange = new BigDecimal(random.nextDouble() * 0.02 - 0.01)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal nav = baseNav.add(dailyChange).setScale(4, RoundingMode.HALF_UP);
        
        NavSyncDTO dto = new NavSyncDTO();
        dto.setProductCode(productCode);
        dto.setProductName(productName);
        dto.setNav(nav);
        dto.setAccumulatedNav(nav);
        dto.setNavDate(LocalDate.now());
        dto.setDailyReturnRate(dailyChange.multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_UP));
        dto.setDataSource("API");
        
        return dto;
    }
}
