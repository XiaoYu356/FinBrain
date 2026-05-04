package com.finbrain.service.impl;

import com.finbrain.entity.FinancialProduct;
import com.finbrain.entity.ProductLifecycleLog;
import com.finbrain.entity.User;
import com.finbrain.enums.ProductStatus;
import com.finbrain.mapper.FinancialProductMapper;
import com.finbrain.mapper.ProductLifecycleLogMapper;
import com.finbrain.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("产品生命周期服务测试")
class ProductLifecycleServiceImplTest {

    @Mock
    private FinancialProductMapper productMapper;

    @Mock
    private ProductLifecycleLogMapper lifecycleLogMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ProductLifecycleServiceImpl productLifecycleService;

    private FinancialProduct product;

    @BeforeEach
    void setUp() {
        product = new FinancialProduct();
        product.setId(1L);
        product.setProductCode("FB001");
        product.setProductName("测试产品");
        product.setStatus(ProductStatus.FUNDRAISING.getCode());
        product.setStartDate(LocalDate.now().minusDays(10));
        product.setEndDate(LocalDate.now().minusDays(1));
        product.setMaturityDate(LocalDate.now().plusDays(80));
    }

    @Test
    @DisplayName("统计各状态产品数量")
    void countProductsByStatus_success() {
        when(productMapper.selectCount(any())).thenReturn(5L);

        long count = productLifecycleService.countProductsByStatus(ProductStatus.FUNDRAISING);

        assertEquals(5L, count);
        verify(productMapper, times(1)).selectCount(any());
    }

    @Test
    @DisplayName("获取即将到期产品-7天内")
    void getExpiringProducts_7days() {
        FinancialProduct expiringProduct = new FinancialProduct();
        expiringProduct.setId(1L);
        expiringProduct.setStatus(ProductStatus.OPERATING.getCode());
        expiringProduct.setMaturityDate(LocalDate.now().plusDays(5));

        when(productMapper.selectList(any())).thenReturn(Arrays.asList(expiringProduct));

        List<FinancialProduct> result = productLifecycleService.getExpiringProducts(7);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取即将到期产品-无到期产品")
    void getExpiringProducts_empty() {
        when(productMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<FinancialProduct> result = productLifecycleService.getExpiringProducts(7);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("获取生命周期日志")
    void getLifecycleLogs_success() {
        ProductLifecycleLog log = new ProductLifecycleLog();
        log.setId(1L);
        log.setProductId(1L);
        log.setFromStatus(ProductStatus.DRAFT.getCode());
        log.setToStatus(ProductStatus.FUNDRAISING.getCode());

        when(lifecycleLogMapper.selectList(any())).thenReturn(Arrays.asList(log));

        List<ProductLifecycleLog> result = productLifecycleService.getLifecycleLogs(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取需要状态流转的产品")
    void getProductsNeedingTransition_success() {
        FinancialProduct fundraisingProduct = new FinancialProduct();
        fundraisingProduct.setId(1L);
        fundraisingProduct.setStatus(ProductStatus.FUNDRAISING.getCode());
        fundraisingProduct.setEndDate(LocalDate.now().minusDays(1));

        FinancialProduct maturedProduct = new FinancialProduct();
        maturedProduct.setId(2L);
        maturedProduct.setStatus(ProductStatus.OPERATING.getCode());
        maturedProduct.setMaturityDate(LocalDate.now().minusDays(1));

        when(productMapper.selectList(any())).thenReturn(Arrays.asList(fundraisingProduct), Arrays.asList(maturedProduct));

        List<FinancialProduct> result = productLifecycleService.getProductsNeedingTransition();

        assertNotNull(result);
    }
}
