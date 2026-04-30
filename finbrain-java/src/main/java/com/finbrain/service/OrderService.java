package com.finbrain.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.OrderDTO;
import com.finbrain.entity.ProductOrder;
import com.finbrain.vo.OrderVO;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    OrderVO createOrder(Long userId, OrderDTO dto);

    void cancelOrder(Long userId, Long orderId);

    void redeemOrder(Long userId, Long orderId);

    Page<OrderVO> getOrders(Long userId, Integer pageNum, Integer pageSize);

    List<ProductOrder> getOrdersByUserId(Long userId);

    ProductOrder getByOrderNo(String orderNo);
}
