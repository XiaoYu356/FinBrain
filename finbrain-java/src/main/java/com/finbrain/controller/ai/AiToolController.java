package com.finbrain.controller.ai;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.IncomeCalculateDTO;
import com.finbrain.dto.OrderDTO;
import com.finbrain.dto.ProductSearchDTO;
import com.finbrain.dto.RiskDTO;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.service.AccountService;
import com.finbrain.service.OrderService;
import com.finbrain.service.ProductService;
import com.finbrain.service.RiskService;
import com.finbrain.service.UserPreferenceService;
import com.finbrain.utils.Result;
import com.finbrain.vo.AssetVO;
import com.finbrain.vo.OrderVO;
import com.finbrain.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "AI工具接口")
@RestController
@RequestMapping("/ai-tools")
@RequiredArgsConstructor
public class AiToolController {

    private final ProductService productService;
    private final AccountService accountService;
    private final OrderService orderService;
    private final RiskService riskService;
    private final UserPreferenceService userPreferenceService;

    @Operation(summary = "搜索理财产品")
    @PostMapping("/search-products")
    @SentinelResource(value = "ai-tools:search-products")
    public Result<List<Map<String, Object>>> searchProducts(
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) Double minRate,
            @RequestParam(required = false) Double maxRate,
            @RequestParam(required = false) Integer minTerm,
            @RequestParam(required = false) Integer maxTerm) {
        
        List<FinancialProduct> products = productService.searchByParams(
                productType, minRate, maxRate, minTerm, maxTerm
        );
        
        List<Map<String, Object>> result = products.stream().map(product -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", product.getId());
            map.put("productCode", product.getProductCode());
            map.put("productName", product.getProductName());
            map.put("productType", product.getProductType());
            map.put("annualReturnRate", product.getAnnualReturnRate());
            map.put("minAmount", product.getMinAmount());
            map.put("termDays", product.getTermDays());
            map.put("description", product.getDescription());
            return map;
        }).collect(Collectors.toList());
        
        return Result.success(result);
    }

    @Operation(summary = "计算收益")
    @PostMapping("/calculate-income")
    public Result<Map<String, Object>> calculateIncome(@RequestBody IncomeCalculateDTO dto) {
        Map<String, Object> result = new HashMap<>();
        
        BigDecimal amount = dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO;
        BigDecimal annualRate = dto.getAnnualRate() != null ? dto.getAnnualRate() : BigDecimal.ZERO;
        Integer termDays = dto.getTermDays() != null ? dto.getTermDays() : 0;
        String investType = dto.getInvestType() != null ? dto.getInvestType() : "one_time";
        Integer investPeriod = dto.getInvestPeriod() != null ? dto.getInvestPeriod() : 1;
        
        BigDecimal totalIncome;
        BigDecimal totalPrincipal;
        
        if ("one_time".equals(investType)) {
            BigDecimal dailyRate = annualRate.divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
            totalIncome = amount.multiply(dailyRate).multiply(BigDecimal.valueOf(termDays));
            totalPrincipal = amount;
        } else {
            BigDecimal dailyRate = annualRate.divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);
            BigDecimal periodIncome = amount.multiply(dailyRate).multiply(BigDecimal.valueOf(termDays));
            totalIncome = periodIncome.multiply(BigDecimal.valueOf(investPeriod));
            totalPrincipal = amount.multiply(BigDecimal.valueOf(investPeriod));
        }
        
        result.put("totalPrincipal", totalPrincipal.setScale(2, RoundingMode.HALF_UP));
        result.put("totalIncome", totalIncome.setScale(2, RoundingMode.HALF_UP));
        result.put("annualRate", annualRate);
        result.put("termDays", termDays);
        result.put("investType", investType);
        
        return Result.success(result);
    }

    @Operation(summary = "获取用户资产")
    @GetMapping("/user-asset/{userId}")
    @SentinelResource(value = "ai-tools:user-asset")
    public Result<Map<String, Object>> getUserAsset(@PathVariable Long userId) {
        AssetVO asset = accountService.getAssetInfo(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", asset.getUserId());
        result.put("totalAsset", asset.getTotalAsset());
        result.put("availableBalance", asset.getAvailableBalance());
        result.put("frozenBalance", asset.getFrozenBalance());
        result.put("totalProfit", asset.getTotalProfit());
        
        return Result.success(result);
    }

    @Operation(summary = "创建订单")
    @PostMapping("/create-order")
    @SentinelResource(value = "ai-tools:create-order")
    public Result<Map<String, Object>> createOrder(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam BigDecimal amount) {
        
        OrderDTO dto = new OrderDTO();
        dto.setProductId(productId);
        dto.setAmount(amount);
        
        try {
            OrderVO order = orderService.createOrder(userId, dto);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("orderId", order.getId());
            result.put("orderNo", order.getOrderNo());
            result.put("amount", order.getAmount());
            result.put("status", order.getStatus());
            result.put("message", "订单创建成功");
            
            return Result.success(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return Result.success(result);
        }
    }

    @Operation(summary = "查询用户订单")
    @GetMapping("/orders/{userId}")
    @SentinelResource(value = "ai-tools:orders")
    public Result<List<Map<String, Object>>> getOrders(@PathVariable Long userId) {
        Page<OrderVO> orderPage = orderService.getOrders(userId, 1, 100);
        
        List<Map<String, Object>> result = orderPage.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("productId", order.getProductId());
            map.put("productName", order.getProductName());
            map.put("amount", order.getAmount());
            map.put("shares", order.getShares());
            map.put("status", order.getStatus());
            map.put("statusText", order.getStatusText());
            map.put("orderTime", order.getOrderTime());
            return map;
        }).collect(Collectors.toList());
        
        return Result.success(result);
    }

    @Operation(summary = "提交风险测评")
    @PostMapping("/risk-assessment")
    @SentinelResource(value = "ai-tools:risk-assessment")
    public Result<Map<String, Object>> submitRiskAssessment(
            @RequestParam Long userId,
            @RequestBody RiskDTO dto) {
        
        try {
            var assessment = riskService.submitAssessment(userId, dto);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("score", assessment.getScore());
            result.put("riskLevel", assessment.getRiskLevel());
            result.put("riskLevelDesc", assessment.getRiskLevelDesc());
            result.put("message", "风险测评提交成功");
            
            return Result.success(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return Result.success(result);
        }
    }

    @Operation(summary = "获取用户风险等级")
    @GetMapping("/risk-level/{userId}")
    public Result<Map<String, Object>> getUserRiskLevel(@PathVariable Long userId) {
        var assessment = riskService.getLatestAssessment(userId);
        
        Map<String, Object> result = new HashMap<>();
        if (assessment == null) {
            result.put("hasAssessment", false);
            result.put("message", "用户尚未完成风险测评");
        } else {
            result.put("hasAssessment", true);
            result.put("score", assessment.getScore());
            result.put("riskLevel", assessment.getRiskLevel());
            result.put("riskLevelDesc", assessment.getRiskLevelDesc());
            result.put("assessmentTime", assessment.getAssessmentTime());
        }
        
        return Result.success(result);
    }

    @Operation(summary = "获取用户偏好")
    @GetMapping("/user-preferences/{userId}")
    public Result<Map<String, Object>> getUserPreferences(@PathVariable Long userId) {
        Map<String, String> preferences = userPreferenceService.getAllPreferences(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("preferences", preferences);
        
        return Result.success(result);
    }

    @Operation(summary = "保存用户偏好")
    @PostMapping("/user-preferences/{userId}")
    public Result<Map<String, Object>> saveUserPreferences(
            @PathVariable Long userId,
            @RequestBody Map<String, String> preferences) {
        
        try {
            userPreferenceService.setPreferences(userId, preferences);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "用户偏好保存成功");
            
            return Result.success(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return Result.success(result);
        }
    }
}
