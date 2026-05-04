package com.finbrain.service;

import com.finbrain.dto.NavSyncDTO;
import com.finbrain.entity.NavHistory;
import com.finbrain.vo.NavHistoryVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface NavSyncService {

    void syncDailyNav();

    void syncNavByProductCode(String productCode);

    void syncHistoryNav(String productCode, LocalDate startDate, LocalDate endDate);

    List<NavHistory> getNavHistoryByProductId(Long productId);

    NavHistory getLatestNav(Long productId);

    BigDecimal calculateNavChange(Long productId, int days);

    List<NavSyncDTO> fetchExternalNavData();
}
