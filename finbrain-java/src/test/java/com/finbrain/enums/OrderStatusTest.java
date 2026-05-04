package com.finbrain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("订单状态枚举测试")
class OrderStatusTest {

    @Test
    @DisplayName("根据code获取状态-正常情况")
    void fromCode_success() {
        OrderStatus status = OrderStatus.fromCode(0);
        assertEquals(OrderStatus.PENDING, status);
        
        status = OrderStatus.fromCode(1);
        assertEquals(OrderStatus.CONFIRMED, status);
        
        status = OrderStatus.fromCode(2);
        assertEquals(OrderStatus.CANCELLED, status);
    }

    @Test
    @DisplayName("根据code获取状态-无效code返回null")
    void fromCode_invalidCode() {
        OrderStatus status = OrderStatus.fromCode(99);
        assertNull(status);
    }

    @Test
    @DisplayName("状态流转-待确认可以确认")
    void canTransit_pendingToConfirmed() {
        assertTrue(OrderStatus.canTransit(OrderStatus.PENDING, OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("状态流转-待确认可以取消")
    void canTransit_pendingToCancelled() {
        assertTrue(OrderStatus.canTransit(OrderStatus.PENDING, OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("状态流转-已确认可以赎回")
    void canTransit_confirmedToRedeemed() {
        assertTrue(OrderStatus.canTransit(OrderStatus.CONFIRMED, OrderStatus.REDEEMED));
    }

    @Test
    @DisplayName("状态流转-已确认可以到期")
    void canTransit_confirmedToMatured() {
        assertTrue(OrderStatus.canTransit(OrderStatus.CONFIRMED, OrderStatus.MATURED));
    }

    @Test
    @DisplayName("状态流转-已到期可以结算")
    void canTransit_maturedToSettled() {
        assertTrue(OrderStatus.canTransit(OrderStatus.MATURED, OrderStatus.SETTLED));
    }

    @Test
    @DisplayName("状态流转-已取消不能流转")
    void canTransit_cancelledCannotTransit() {
        assertFalse(OrderStatus.canTransit(OrderStatus.CANCELLED, OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.canTransit(OrderStatus.CANCELLED, OrderStatus.REDEEMED));
    }

    @Test
    @DisplayName("状态流转-已结算不能流转")
    void canTransit_settledCannotTransit() {
        assertFalse(OrderStatus.canTransit(OrderStatus.SETTLED, OrderStatus.REDEEMED));
    }

    @Test
    @DisplayName("是否可取消-待确认可取消")
    void canCancel_pending() {
        assertTrue(OrderStatus.canCancel(OrderStatus.PENDING));
    }

    @Test
    @DisplayName("是否可取消-已确认可取消")
    void canCancel_confirmed() {
        assertTrue(OrderStatus.canCancel(OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("是否可取消-其他状态不可取消")
    void canCancel_otherStatus() {
        assertFalse(OrderStatus.canCancel(OrderStatus.REDEEMED));
        assertFalse(OrderStatus.canCancel(OrderStatus.MATURED));
        assertFalse(OrderStatus.canCancel(OrderStatus.SETTLED));
        assertFalse(OrderStatus.canCancel(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("是否可赎回-已确认可赎回")
    void canRedeem_confirmed() {
        assertTrue(OrderStatus.canRedeem(OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("是否可赎回-其他状态不可赎回")
    void canRedeem_otherStatus() {
        assertFalse(OrderStatus.canRedeem(OrderStatus.PENDING));
        assertFalse(OrderStatus.canRedeem(OrderStatus.REDEEMED));
        assertFalse(OrderStatus.canRedeem(OrderStatus.MATURED));
    }
}
