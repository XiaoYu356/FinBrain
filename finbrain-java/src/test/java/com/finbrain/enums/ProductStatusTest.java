package com.finbrain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("产品状态枚举测试")
class ProductStatusTest {

    @Test
    @DisplayName("根据code获取状态-正常情况")
    void fromCode_success() {
        ProductStatus status = ProductStatus.fromCode(0);
        assertEquals(ProductStatus.DRAFT, status);
        
        status = ProductStatus.fromCode(1);
        assertEquals(ProductStatus.FUNDRAISING, status);
        
        status = ProductStatus.fromCode(2);
        assertEquals(ProductStatus.OPERATING, status);
    }

    @Test
    @DisplayName("根据code获取状态-无效code返回null")
    void fromCode_invalidCode() {
        ProductStatus status = ProductStatus.fromCode(99);
        assertNull(status);
        
        status = ProductStatus.fromCode(null);
        assertNull(status);
    }

    @Test
    @DisplayName("状态流转-草稿可以发布")
    void canTransit_draftToFundraising() {
        assertTrue(ProductStatus.canTransit(ProductStatus.DRAFT, ProductStatus.FUNDRAISING));
    }

    @Test
    @DisplayName("状态流转-募集期可以进入存续期")
    void canTransit_fundraisingToOperating() {
        assertTrue(ProductStatus.canTransit(ProductStatus.FUNDRAISING, ProductStatus.OPERATING));
    }

    @Test
    @DisplayName("状态流转-募集期可以下架")
    void canTransit_fundraisingToDelisted() {
        assertTrue(ProductStatus.canTransit(ProductStatus.FUNDRAISING, ProductStatus.DELISTED));
    }

    @Test
    @DisplayName("状态流转-存续期可以到期")
    void canTransit_operatingToMatured() {
        assertTrue(ProductStatus.canTransit(ProductStatus.OPERATING, ProductStatus.MATURED));
    }

    @Test
    @DisplayName("状态流转-已到期不能流转")
    void canTransit_maturedCannotTransit() {
        assertFalse(ProductStatus.canTransit(ProductStatus.MATURED, ProductStatus.OPERATING));
        assertFalse(ProductStatus.canTransit(ProductStatus.MATURED, ProductStatus.DELISTED));
    }

    @Test
    @DisplayName("状态流转-已下架不能流转")
    void canTransit_delistedCannotTransit() {
        assertFalse(ProductStatus.canTransit(ProductStatus.DELISTED, ProductStatus.OPERATING));
    }

    @Test
    @DisplayName("状态流转-非法流转")
    void canTransit_invalidTransition() {
        assertFalse(ProductStatus.canTransit(ProductStatus.DRAFT, ProductStatus.MATURED));
        assertFalse(ProductStatus.canTransit(ProductStatus.DRAFT, ProductStatus.OPERATING));
    }

    @Test
    @DisplayName("状态流转-null参数")
    void canTransit_nullParams() {
        assertFalse(ProductStatus.canTransit(null, ProductStatus.FUNDRAISING));
        assertFalse(ProductStatus.canTransit(ProductStatus.DRAFT, null));
        assertFalse(ProductStatus.canTransit(null, null));
    }
}
