package com.finbrain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.OrderDTO;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.entity.ProductOrder;
import com.finbrain.entity.Transaction;
import com.finbrain.entity.UserAccount;
import com.finbrain.exception.BusinessException;
import com.finbrain.mapper.FinancialProductMapper;
import com.finbrain.mapper.ProductOrderMapper;
import com.finbrain.mapper.TransactionMapper;
import com.finbrain.mapper.UserAccountMapper;
import com.finbrain.service.OrderService;
import com.finbrain.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ProductOrderMapper orderMapper;
    private final FinancialProductMapper productMapper;
    private final UserAccountMapper userAccountMapper;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, OrderDTO dto) {
        FinancialProduct product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        
        if (product.getSaleStatus() != 1) {
            throw new BusinessException("产品不在售");
        }
        
        if (dto.getAmount().compareTo(product.getMinAmount()) < 0) {
            throw new BusinessException("申购金额低于最低起购金额");
        }
        
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId)
        );
        if (account == null) {
            throw new BusinessException("账户不存在");
        }
        
        if (account.getAvailableBalance().compareTo(dto.getAmount()) < 0) {
            throw new BusinessException("余额不足");
        }
        
        String orderNo = generateOrderNo();
        
        ProductOrder order = new ProductOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(dto.getProductId());
        order.setAmount(dto.getAmount());
        order.setShares(dto.getAmount());
        order.setStatus(1);
        order.setOrderTime(LocalDateTime.now());
        order.setSuccessTime(LocalDateTime.now());
        orderMapper.insert(order);
        
        BigDecimal balanceBefore = account.getAvailableBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(dto.getAmount());
        account.setAvailableBalance(balanceAfter);
        userAccountMapper.updateById(account);
        
        Transaction transaction = new Transaction();
        transaction.setTransactionNo("TXN" + System.currentTimeMillis());
        transaction.setUserId(userId);
        transaction.setOrderId(order.getId());
        transaction.setType("buy");
        transaction.setAmount(dto.getAmount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark("申购" + product.getProductName());
        transaction.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(transaction);
        
        OrderVO vo = new OrderVO();
        BeanUtil.copyProperties(order, vo);
        vo.setProductName(product.getProductName());
        vo.setProductCode(product.getProductCode());
        vo.setStatusText(getStatusText(order.getStatus()));
        return vo;
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        ProductOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许取消");
        }
        
        order.setStatus(2);
        orderMapper.updateById(order);
        
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId)
        );
        
        BigDecimal balanceBefore = account.getAvailableBalance();
        BigDecimal balanceAfter = balanceBefore.add(order.getAmount());
        account.setAvailableBalance(balanceAfter);
        account.setFrozenBalance(account.getFrozenBalance().subtract(order.getAmount()));
        userAccountMapper.updateById(account);
        
        Transaction transaction = new Transaction();
        transaction.setTransactionNo("TXN" + System.currentTimeMillis());
        transaction.setUserId(userId);
        transaction.setOrderId(orderId);
        transaction.setType("redeem");
        transaction.setAmount(order.getAmount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark("取消订单退款");
        transaction.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(transaction);
    }

    @Override
    public Page<OrderVO> getOrders(Long userId, Integer pageNum, Integer pageSize) {
        Page<ProductOrder> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<ProductOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductOrder::getUserId, userId);
        wrapper.orderByDesc(ProductOrder::getOrderTime);
        
        Page<ProductOrder> orderPage = orderMapper.selectPage(page, wrapper);
        
        Page<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtil.copyProperties(order, vo);
            
            FinancialProduct product = productMapper.selectById(order.getProductId());
            if (product != null) {
                vo.setProductName(product.getProductName());
                vo.setProductCode(product.getProductCode());
                vo.setProductType(product.getProductType());
                vo.setAnnualReturnRate(product.getAnnualReturnRate());
            }
            
            vo.setStatusText(getStatusText(order.getStatus()));
            return vo;
        }).collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public List<ProductOrder> getOrdersByUserId(Long userId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<ProductOrder>()
                        .eq(ProductOrder::getUserId, userId)
                        .orderByDesc(ProductOrder::getOrderTime)
        );
    }

    @Override
    public ProductOrder getByOrderNo(String orderNo) {
        return orderMapper.selectOne(
                new LambdaQueryWrapper<ProductOrder>().eq(ProductOrder::getOrderNo, orderNo)
        );
    }

    @Override
    @Transactional
    public void redeemOrder(Long userId, Long orderId) {
        ProductOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        
        if (order.getStatus() != 1) {
            throw new BusinessException("订单状态不允许赎回");
        }
        
        FinancialProduct product = productMapper.selectById(order.getProductId());
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        
        order.setStatus(3);
        orderMapper.updateById(order);
        
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId)
        );
        
        BigDecimal redeemAmount = order.getAmount();
        BigDecimal balanceBefore = account.getAvailableBalance();
        BigDecimal balanceAfter = balanceBefore.add(redeemAmount);
        account.setAvailableBalance(balanceAfter);
        userAccountMapper.updateById(account);
        
        Transaction transaction = new Transaction();
        transaction.setTransactionNo("TXN" + System.currentTimeMillis());
        transaction.setUserId(userId);
        transaction.setOrderId(orderId);
        transaction.setType("redeem");
        transaction.setAmount(redeemAmount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark("赎回" + product.getProductName());
        transaction.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(transaction);
        
        log.info("用户 {} 赎回订单 {} 成功，金额: {}", userId, orderId, redeemAmount);
    }

    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD" + dateStr + IdUtil.randomUUID().substring(0, 6).toUpperCase();
    }

    private String getStatusText(Integer status) {
        switch (status) {
            case 0: return "待确认";
            case 1: return "已确认";
            case 2: return "已取消";
            case 3: return "已赎回";
            default: return "未知";
        }
    }
}
