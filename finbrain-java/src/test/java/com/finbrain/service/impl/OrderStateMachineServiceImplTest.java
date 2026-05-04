package com.finbrain.service.impl;

import com.finbrain.dto.OrderDTO;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.entity.UserAccount;
import com.finbrain.enums.OrderStatus;
import com.finbrain.enums.ProductStatus;
import com.finbrain.mapper.*;
import com.finbrain.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("订单状态机服务测试")
class OrderStateMachineServiceImplTest {

    @Mock
    private ProductOrderMapper orderMapper;

    @Mock
    private OrderStatusLogMapper statusLogMapper;

    @Mock
    private FinancialProductMapper productMapper;

    @Mock
    private UserAccountMapper userAccountMapper;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private OrderStateMachineServiceImpl orderStateMachineService;

    private FinancialProduct product;
    private UserAccount account;
    private OrderDTO orderDTO;

    @BeforeEach
    void setUp() {
        product = new FinancialProduct();
        product.setId(1L);
        product.setProductCode("FB001");
        product.setProductName("测试产品");
        product.setAnnualReturnRate(new BigDecimal("5.00"));
        product.setMinAmount(new BigDecimal("10000"));
        product.setMaxAmount(new BigDecimal("100000"));
        product.setTermDays(90);
        product.setStatus(ProductStatus.FUNDRAISING.getCode());
        product.setMaturityDate(LocalDate.now().plusDays(90));

        account = new UserAccount();
        account.setUserId(1L);
        account.setTotalAsset(new BigDecimal("100000"));
        account.setAvailableBalance(new BigDecimal("50000"));
        account.setFrozenBalance(BigDecimal.ZERO);
        account.setTotalProfit(BigDecimal.ZERO);

        orderDTO = new OrderDTO();
        orderDTO.setProductId(1L);
        orderDTO.setAmount(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("创建订单-成功")
    void createOrder_success() {
        when(productMapper.selectById(1L)).thenReturn(product);
        when(userAccountMapper.selectOne(any())).thenReturn(account);
        when(orderMapper.insert(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, com.finbrain.entity.ProductOrder.class).setId(1L);
            return 1;
        });
        when(userAccountMapper.updateById(any())).thenReturn(1);
        when(transactionMapper.insert(any())).thenReturn(1);
        when(statusLogMapper.insert(any())).thenReturn(1);

        OrderVO result = orderStateMachineService.createOrder(1L, orderDTO);

        assertNotNull(result);
        assertEquals(OrderStatus.PENDING.getCode(), result.getStatus());
        verify(orderMapper, times(1)).insert(any());
        verify(userAccountMapper, times(1)).updateById(any());
    }

    @Test
    @DisplayName("创建订单-产品不存在")
    void createOrder_productNotFound() {
        when(productMapper.selectById(1L)).thenReturn(null);

        assertThrows(com.finbrain.exception.BusinessException.class, () -> {
            orderStateMachineService.createOrder(1L, orderDTO);
        });
    }

    @Test
    @DisplayName("创建订单-余额不足")
    void createOrder_insufficientBalance() {
        account.setAvailableBalance(new BigDecimal("5000"));
        when(productMapper.selectById(1L)).thenReturn(product);
        when(userAccountMapper.selectOne(any())).thenReturn(account);

        assertThrows(com.finbrain.exception.BusinessException.class, () -> {
            orderStateMachineService.createOrder(1L, orderDTO);
        });
    }

    @Test
    @DisplayName("创建订单-金额低于起购金额")
    void createOrder_belowMinAmount() {
        orderDTO.setAmount(new BigDecimal("5000"));
        when(productMapper.selectById(1L)).thenReturn(product);

        assertThrows(com.finbrain.exception.BusinessException.class, () -> {
            orderStateMachineService.createOrder(1L, orderDTO);
        });
    }

    @Test
    @DisplayName("计算预期收益-正常情况")
    void calculateExpectedIncome_success() {
        com.finbrain.entity.ProductOrder order = new com.finbrain.entity.ProductOrder();
        order.setAmount(new BigDecimal("10000"));
        order.setAnnualReturnRate(new BigDecimal("5.00"));
        order.setTermDays(90);

        BigDecimal income = orderStateMachineService.calculateExpectedIncome(order);

        assertNotNull(income);
        assertTrue(income.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("计算预期收益-空参数")
    void calculateExpectedIncome_nullParams() {
        BigDecimal income = orderStateMachineService.calculateExpectedIncome(null);
        assertEquals(BigDecimal.ZERO, income);

        com.finbrain.entity.ProductOrder order = new com.finbrain.entity.ProductOrder();
        income = orderStateMachineService.calculateExpectedIncome(order);
        assertEquals(BigDecimal.ZERO, income);
    }
}
