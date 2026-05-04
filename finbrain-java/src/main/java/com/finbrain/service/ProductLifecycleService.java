package com.finbrain.service;

import com.finbrain.dto.AdminProductDTO;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.entity.ProductLifecycleLog;
import com.finbrain.enums.ProductStatus;

import java.util.List;

public interface ProductLifecycleService {

    void createProduct(AdminProductDTO dto, Long operatorId);

    void updateProduct(Long productId, AdminProductDTO dto, Long operatorId);

    void publishProduct(Long productId, Long operatorId);

    void delistProduct(Long productId, String reason, Long operatorId);

    void processStatusTransition();

    List<ProductLifecycleLog> getLifecycleLogs(Long productId);

    List<FinancialProduct> getProductsNeedingTransition();

    int batchUpdateStatus(List<Long> productIds, ProductStatus newStatus, String triggerType, String reason);

    long countProductsByStatus(ProductStatus status);

    List<FinancialProduct> getExpiringProducts(int days);
}
