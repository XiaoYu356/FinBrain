package com.finbrain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.finbrain.dto.AdminProductDTO;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.entity.ProductLifecycleLog;
import com.finbrain.entity.User;
import com.finbrain.enums.ProductStatus;
import com.finbrain.exception.BusinessException;
import com.finbrain.mapper.FinancialProductMapper;
import com.finbrain.mapper.ProductLifecycleLogMapper;
import com.finbrain.mapper.UserMapper;
import com.finbrain.service.ProductLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductLifecycleServiceImpl implements ProductLifecycleService {

    private final FinancialProductMapper productMapper;
    private final ProductLifecycleLogMapper lifecycleLogMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProduct(AdminProductDTO dto, Long operatorId) {
        FinancialProduct product = new FinancialProduct();
        BeanUtil.copyProperties(dto, product);
        product.setProductCode(generateProductCode());
        product.setStatus(ProductStatus.DRAFT.getCode());
        product.setSaleStatus(0);
        productMapper.insert(product);
        
        logLifecycle(product, null, ProductStatus.DRAFT, "MANUAL", "创建产品", operatorId);
        log.info("创建产品成功: productId={}, productCode={}", product.getId(), product.getProductCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Long productId, AdminProductDTO dto, Long operatorId) {
        FinancialProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        
        ProductStatus currentStatus = ProductStatus.fromCode(product.getStatus());
        if (currentStatus != ProductStatus.DRAFT) {
            throw new BusinessException("只有草稿状态的产品可以修改");
        }
        
        BeanUtil.copyProperties(dto, product);
        productMapper.updateById(product);
        log.info("更新产品成功: productId={}", productId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishProduct(Long productId, Long operatorId) {
        FinancialProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        
        ProductStatus currentStatus = ProductStatus.fromCode(product.getStatus());
        if (currentStatus != ProductStatus.DRAFT) {
            throw new BusinessException("只有草稿状态的产品可以发布");
        }
        
        if (product.getStartDate() == null || product.getEndDate() == null) {
            throw new BusinessException("请设置产品募集开始时间和结束时间");
        }
        
        if (product.getStartDate().isAfter(product.getEndDate())) {
            throw new BusinessException("募集开始时间不能晚于结束时间");
        }
        
        ProductStatus newStatus = ProductStatus.FUNDRAISING;
        product.setStatus(newStatus.getCode());
        product.setSaleStatus(1);
        productMapper.updateById(product);
        
        logLifecycle(product, currentStatus, newStatus, "MANUAL", "发布产品", operatorId);
        log.info("发布产品成功: productId={}, status={}", productId, newStatus.getDesc());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delistProduct(Long productId, String reason, Long operatorId) {
        FinancialProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        
        ProductStatus currentStatus = ProductStatus.fromCode(product.getStatus());
        if (!ProductStatus.canTransit(currentStatus, ProductStatus.DELISTED)) {
            throw new BusinessException("当前状态不允许下架");
        }
        
        ProductStatus newStatus = ProductStatus.DELISTED;
        product.setStatus(newStatus.getCode());
        product.setSaleStatus(0);
        productMapper.updateById(product);
        
        logLifecycle(product, currentStatus, newStatus, "MANUAL", reason, operatorId);
        log.info("下架产品成功: productId={}, reason={}", productId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processStatusTransition() {
        log.info("开始执行产品状态流转定时任务...");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        int fundraisingToOperating = 0;
        int operatingToMatured = 0;
        
        LambdaQueryWrapper<FinancialProduct> fundraisingWrapper = new LambdaQueryWrapper<>();
        fundraisingWrapper.eq(FinancialProduct::getStatus, ProductStatus.FUNDRAISING.getCode())
                .le(FinancialProduct::getEndDate, today);
        List<FinancialProduct> fundraisingProducts = productMapper.selectList(fundraisingWrapper);
        
        for (FinancialProduct product : fundraisingProducts) {
            ProductStatus fromStatus = ProductStatus.FUNDRAISING;
            ProductStatus toStatus = ProductStatus.OPERATING;
            
            product.setStatus(toStatus.getCode());
            product.setSaleStatus(1);
            productMapper.updateById(product);
            
            logLifecycle(product, fromStatus, toStatus, "AUTO", "募集期结束，自动进入存续期", null);
            fundraisingToOperating++;
        }
        
        LambdaQueryWrapper<FinancialProduct> maturedWrapper = new LambdaQueryWrapper<>();
        maturedWrapper.eq(FinancialProduct::getStatus, ProductStatus.OPERATING.getCode());
        List<FinancialProduct> operatingProducts = productMapper.selectList(maturedWrapper);
        
        for (FinancialProduct product : operatingProducts) {
            if (product.getMaturityDate() != null && !product.getMaturityDate().isAfter(today)) {
                ProductStatus fromStatus = ProductStatus.OPERATING;
                ProductStatus toStatus = ProductStatus.MATURED;
                
                product.setStatus(toStatus.getCode());
                product.setSaleStatus(0);
                productMapper.updateById(product);
                
                logLifecycle(product, fromStatus, toStatus, "AUTO", "产品到期，自动标记为已到期", null);
                operatingToMatured++;
            }
        }
        
        log.info("产品状态流转完成: 募集期→存续期={}个, 存续期→已到期={}个", fundraisingToOperating, operatingToMatured);
    }

    @Override
    public List<ProductLifecycleLog> getLifecycleLogs(Long productId) {
        LambdaQueryWrapper<ProductLifecycleLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductLifecycleLog::getProductId, productId)
                .orderByDesc(ProductLifecycleLog::getCreateTime);
        return lifecycleLogMapper.selectList(wrapper);
    }

    @Override
    public List<FinancialProduct> getProductsNeedingTransition() {
        List<FinancialProduct> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        LambdaQueryWrapper<FinancialProduct> fundraisingWrapper = new LambdaQueryWrapper<>();
        fundraisingWrapper.eq(FinancialProduct::getStatus, ProductStatus.FUNDRAISING.getCode())
                .le(FinancialProduct::getEndDate, today);
        result.addAll(productMapper.selectList(fundraisingWrapper));
        
        LambdaQueryWrapper<FinancialProduct> operatingWrapper = new LambdaQueryWrapper<>();
        operatingWrapper.eq(FinancialProduct::getStatus, ProductStatus.OPERATING.getCode());
        List<FinancialProduct> operatingProducts = productMapper.selectList(operatingWrapper);
        
        for (FinancialProduct product : operatingProducts) {
            if (product.getMaturityDate() != null && !product.getMaturityDate().isAfter(today)) {
                result.add(product);
            }
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(List<Long> productIds, ProductStatus newStatus, String triggerType, String reason) {
        if (productIds == null || productIds.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (Long productId : productIds) {
            FinancialProduct product = productMapper.selectById(productId);
            if (product != null) {
                ProductStatus fromStatus = ProductStatus.fromCode(product.getStatus());
                if (ProductStatus.canTransit(fromStatus, newStatus)) {
                    product.setStatus(newStatus.getCode());
                    product.setSaleStatus(newStatus == ProductStatus.FUNDRAISING || newStatus == ProductStatus.OPERATING ? 1 : 0);
                    productMapper.updateById(product);
                    logLifecycle(product, fromStatus, newStatus, triggerType, reason, null);
                    count++;
                }
            }
        }
        
        log.info("批量更新产品状态: count={}, newStatus={}", count, newStatus.getDesc());
        return count;
    }

    @Override
    public long countProductsByStatus(ProductStatus status) {
        LambdaQueryWrapper<FinancialProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinancialProduct::getStatus, status.getCode());
        return productMapper.selectCount(wrapper);
    }

    @Override
    public List<FinancialProduct> getExpiringProducts(int days) {
        LocalDate targetDate = LocalDate.now().plusDays(days);
        
        LambdaQueryWrapper<FinancialProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FinancialProduct::getStatus, ProductStatus.OPERATING.getCode())
                .le(FinancialProduct::getMaturityDate, targetDate)
                .ge(FinancialProduct::getMaturityDate, LocalDate.now());
        
        return productMapper.selectList(wrapper);
    }

    private void logLifecycle(FinancialProduct product, ProductStatus fromStatus, ProductStatus toStatus, 
                              String triggerType, String reason, Long operatorId) {
        ProductLifecycleLog log = new ProductLifecycleLog();
        log.setProductId(product.getId());
        log.setProductCode(product.getProductCode());
        log.setProductName(product.getProductName());
        log.setFromStatus(fromStatus != null ? fromStatus.getCode() : null);
        log.setFromStatusName(fromStatus != null ? fromStatus.getDesc() : null);
        log.setToStatus(toStatus.getCode());
        log.setToStatusName(toStatus.getDesc());
        log.setTriggerType(triggerType);
        log.setTriggerReason(reason);
        log.setOperatorId(operatorId);
        
        if (operatorId != null) {
            User operator = userMapper.selectById(operatorId);
            if (operator != null) {
                log.setOperatorName(operator.getRealName() != null ? operator.getRealName() : operator.getUsername());
            }
        }
        
        lifecycleLogMapper.insert(log);
    }

    private String generateProductCode() {
        return "FB" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
