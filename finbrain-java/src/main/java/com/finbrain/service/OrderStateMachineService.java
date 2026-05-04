package com.finbrain.service;

import com.finbrain.dto.OrderDTO;
import com.finbrain.entity.ProductOrder;
import com.finbrain.enums.OrderStatus;
import com.finbrain.vo.OrderVO;

import java.math.BigDecimal;
import java.util.List;

public interface OrderStateMachineService {

    OrderVO createOrder(Long userId, OrderDTO dto);

    void confirmOrder(Long orderId, Long operatorId);

    void cancelOrder(Long userId, Long orderId, String reason);

    void redeemOrder(Long userId, Long orderId);

    void settleOrder(Long orderId);

    void processExpiredOrders();

    void processMaturedOrders();

    List<ProductOrder> getExpiredOrders();

    List<ProductOrder> getMaturedOrders();

    BigDecimal calculateExpectedIncome(ProductOrder order);

    BigDecimal calculateActualIncome(ProductOrder order);

    void updateOrderIncome(Long orderId);

    boolean canTransition(Long orderId, OrderStatus targetStatus);

    OrderStatus getCurrentStatus(Long orderId);
}
