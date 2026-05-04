package com.finbrain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.finbrain.dto.OrderDTO;
import com.finbrain.entity.*;
import com.finbrain.enums.OrderStatus;
import com.finbrain.enums.ProductStatus;
import com.finbrain.exception.BusinessException;
import com.finbrain.mapper.*;
import com.finbrain.service.AssetStatisticsService;
import com.finbrain.service.OrderStateMachineService;
import com.finbrain.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStateMachineServiceImpl implements OrderStateMachineService {

    private final ProductOrderMapper orderMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final FinancialProductMapper productMapper;
    private final UserAccountMapper userAccountMapper;
    private final TransactionMapper transactionMapper;
    private final UserMapper userMapper;
    private final AssetStatisticsService assetStatisticsService;

    private static final int ORDER_EXPIRE_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, OrderDTO dto) {
        FinancialProduct product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        
        ProductStatus productStatus = ProductStatus.fromCode(product.getStatus());
        if (productStatus != ProductStatus.FUNDRAISING && productStatus != ProductStatus.OPERATING) {
            throw new BusinessException("产品不在可购买状态");
        }
        
        if (dto.getAmount().compareTo(product.getMinAmount()) < 0) {
            throw new BusinessException("申购金额低于最低起购金额 " + product.getMinAmount());
        }
        
        if (product.getMaxAmount() != null && dto.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException("申购金额超过最大购买金额 " + product.getMaxAmount());
        }
        
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId)
        );
        if (account == null) {
            throw new BusinessException("账户不存在");
        }
        
        if (account.getAvailableBalance().compareTo(dto.getAmount()) < 0) {
            throw new BusinessException("余额不足，当前可用余额: " + account.getAvailableBalance());
        }
        
        String orderNo = generateOrderNo();
        
        ProductOrder order = new ProductOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(dto.getProductId());
        order.setAmount(dto.getAmount());
        order.setShares(dto.getAmount());
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setAnnualReturnRate(product.getAnnualReturnRate());
        order.setTermDays(product.getTermDays());
        order.setOrderTime(LocalDateTime.now());
        order.setExpireTime(LocalDateTime.now().plusMinutes(ORDER_EXPIRE_MINUTES));
        
        if (product.getMaturityDate() != null) {
            order.setMaturityDate(product.getMaturityDate());
        } else {
            order.setMaturityDate(LocalDate.now().plusDays(product.getTermDays()));
        }
        
        order.setExpectedIncome(calculateExpectedIncome(order));
        
        orderMapper.insert(order);
        
        BigDecimal balanceBefore = account.getAvailableBalance();
        BigDecimal frozenBefore = account.getFrozenBalance();
        BigDecimal totalAssetBefore = account.getTotalAsset();
        BigDecimal balanceAfter = balanceBefore.subtract(dto.getAmount());
        BigDecimal frozenAfter = frozenBefore.add(dto.getAmount());
        
        account.setAvailableBalance(balanceAfter);
        account.setFrozenBalance(frozenAfter);
        userAccountMapper.updateById(account);
        
        createTransaction(userId, order.getId(), "buy", dto.getAmount(), balanceBefore, balanceAfter, 
                "申购" + product.getProductName() + "，订单号: " + orderNo);
        
        assetStatisticsService.recordAssetFlow(userId, "freeze", dto.getAmount(),
                balanceBefore, balanceAfter, frozenBefore, frozenAfter,
                totalAssetBefore, totalAssetBefore,
                order.getId(), "order", "申购冻结: " + product.getProductName());
        
        logStatusChange(order, null, OrderStatus.PENDING, "SYSTEM", "创建订单", userId);
        
        log.info("创建订单成功: orderNo={}, userId={}, amount={}", orderNo, userId, dto.getAmount());
        
        return buildOrderVO(order, product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long orderId, Long operatorId) {
        ProductOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (!OrderStatus.canTransit(currentStatus, OrderStatus.CONFIRMED)) {
            throw new BusinessException("订单状态不允许确认，当前状态: " + currentStatus.getDesc());
        }
        
        FinancialProduct product = productMapper.selectById(order.getProductId());
        
        order.setStatus(OrderStatus.CONFIRMED.getCode());
        order.setConfirmTime(LocalDateTime.now());
        order.setSuccessTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, order.getUserId())
        );
        
        BigDecimal frozenBefore = account.getFrozenBalance();
        BigDecimal totalAssetBefore = account.getTotalAsset();
        BigDecimal frozenAfter = frozenBefore.subtract(order.getAmount());
        BigDecimal totalAssetAfter = totalAssetBefore.subtract(order.getAmount());
        
        account.setFrozenBalance(frozenAfter);
        account.setTotalAsset(totalAssetAfter);
        userAccountMapper.updateById(account);
        
        assetStatisticsService.recordAssetFlow(order.getUserId(), "invest", order.getAmount(),
                account.getAvailableBalance(), account.getAvailableBalance(),
                frozenBefore, frozenAfter,
                totalAssetBefore, totalAssetAfter,
                order.getId(), "order", "确认投资: " + product.getProductName());
        
        logStatusChange(order, currentStatus, OrderStatus.CONFIRMED, "MANUAL", "确认订单", operatorId);
        
        log.info("确认订单成功: orderId={}, operatorId={}", orderId, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId, String reason) {
        ProductOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (!OrderStatus.canCancel(currentStatus)) {
            throw new BusinessException("订单状态不允许取消，当前状态: " + currentStatus.getDesc());
        }
        
        FinancialProduct product = productMapper.selectById(order.getProductId());
        
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason != null ? reason : "用户主动取消");
        orderMapper.updateById(order);
        
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId)
        );
        
        BigDecimal balanceBefore = account.getAvailableBalance();
        BigDecimal frozenBefore = account.getFrozenBalance();
        BigDecimal totalAssetBefore = account.getTotalAsset();
        BigDecimal balanceAfter;
        BigDecimal frozenAfter;
        BigDecimal totalAssetAfter;
        String flowType;
        
        if (currentStatus == OrderStatus.PENDING) {
            balanceAfter = balanceBefore.add(order.getAmount());
            frozenAfter = frozenBefore.subtract(order.getAmount());
            totalAssetAfter = totalAssetBefore;
            flowType = "unfreeze";
            account.setAvailableBalance(balanceAfter);
            account.setFrozenBalance(frozenAfter);
        } else {
            balanceAfter = balanceBefore.add(order.getAmount());
            frozenAfter = frozenBefore;
            totalAssetAfter = totalAssetBefore.add(order.getAmount());
            flowType = "refund";
            account.setAvailableBalance(balanceAfter);
            account.setTotalAsset(totalAssetAfter);
        }
        
        userAccountMapper.updateById(account);
        
        createTransaction(userId, orderId, "refund", order.getAmount(), balanceBefore, balanceAfter, 
                "取消订单退款: " + order.getOrderNo());
        
        assetStatisticsService.recordAssetFlow(userId, flowType, order.getAmount(),
                balanceBefore, balanceAfter, frozenBefore, frozenAfter,
                totalAssetBefore, totalAssetAfter,
                orderId, "order", "取消订单退款: " + product.getProductName());
        
        logStatusChange(order, currentStatus, OrderStatus.CANCELLED, "MANUAL", reason != null ? reason : "用户取消", userId);
        
        log.info("取消订单成功: orderId={}, userId={}, reason={}", orderId, userId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void redeemOrder(Long userId, Long orderId) {
        ProductOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (!OrderStatus.canRedeem(currentStatus)) {
            throw new BusinessException("订单状态不允许赎回，当前状态: " + currentStatus.getDesc());
        }
        
        FinancialProduct product = productMapper.selectById(order.getProductId());
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        
        BigDecimal actualIncome = calculateActualIncome(order);
        BigDecimal redeemAmount = order.getAmount().add(actualIncome);
        
        order.setStatus(OrderStatus.REDEEMED.getCode());
        order.setActualIncome(actualIncome);
        order.setSuccessTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId)
        );
        
        BigDecimal balanceBefore = account.getAvailableBalance();
        BigDecimal totalAssetBefore = account.getTotalAsset();
        BigDecimal totalProfitBefore = account.getTotalProfit();
        BigDecimal balanceAfter = balanceBefore.add(redeemAmount);
        BigDecimal totalAssetAfter = totalAssetBefore.add(redeemAmount);
        BigDecimal totalProfitAfter = totalProfitBefore.add(actualIncome);
        
        account.setAvailableBalance(balanceAfter);
        account.setTotalAsset(totalAssetAfter);
        account.setTotalProfit(totalProfitAfter);
        userAccountMapper.updateById(account);
        
        createTransaction(userId, orderId, "redeem", redeemAmount, balanceBefore, balanceAfter, 
                "赎回" + product.getProductName() + "，收益: " + actualIncome);
        
        assetStatisticsService.recordAssetFlow(userId, "redeem", redeemAmount,
                balanceBefore, balanceAfter, BigDecimal.ZERO, BigDecimal.ZERO,
                totalAssetBefore, totalAssetAfter,
                orderId, "order", "赎回: " + product.getProductName() + ", 本金: " + order.getAmount() + ", 收益: " + actualIncome);
        
        if (actualIncome.compareTo(BigDecimal.ZERO) > 0) {
            createTransaction(userId, orderId, "profit", actualIncome, balanceBefore, balanceAfter, 
                    "投资收益: " + product.getProductName());
            
            assetStatisticsService.recordAssetFlow(userId, "profit", actualIncome,
                    balanceBefore, balanceAfter, BigDecimal.ZERO, BigDecimal.ZERO,
                    totalAssetBefore, totalAssetAfter,
                    orderId, "order", "投资收益: " + product.getProductName());
        }
        
        logStatusChange(order, currentStatus, OrderStatus.REDEEMED, "MANUAL", "用户赎回", userId);
        
        log.info("赎回订单成功: orderId={}, userId={}, redeemAmount={}, income={}", orderId, userId, redeemAmount, actualIncome);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleOrder(Long orderId) {
        ProductOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        if (currentStatus != OrderStatus.MATURED) {
            throw new BusinessException("只有已到期订单可以结算");
        }
        
        FinancialProduct product = productMapper.selectById(order.getProductId());
        
        BigDecimal actualIncome = calculateActualIncome(order);
        BigDecimal settleAmount = order.getAmount().add(actualIncome);
        
        order.setStatus(OrderStatus.SETTLED.getCode());
        order.setActualIncome(actualIncome);
        order.setSettleTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, order.getUserId())
        );
        
        BigDecimal balanceBefore = account.getAvailableBalance();
        BigDecimal totalAssetBefore = account.getTotalAsset();
        BigDecimal totalProfitBefore = account.getTotalProfit();
        BigDecimal balanceAfter = balanceBefore.add(settleAmount);
        BigDecimal totalAssetAfter = totalAssetBefore.add(settleAmount);
        BigDecimal totalProfitAfter = totalProfitBefore.add(actualIncome);
        
        account.setAvailableBalance(balanceAfter);
        account.setTotalAsset(totalAssetAfter);
        account.setTotalProfit(totalProfitAfter);
        userAccountMapper.updateById(account);
        
        createTransaction(order.getUserId(), orderId, "settle", settleAmount, balanceBefore, balanceAfter, 
                "产品到期结算: " + product.getProductName());
        
        assetStatisticsService.recordAssetFlow(order.getUserId(), "settle", settleAmount,
                balanceBefore, balanceAfter, BigDecimal.ZERO, BigDecimal.ZERO,
                totalAssetBefore, totalAssetAfter,
                orderId, "order", "到期结算: " + product.getProductName() + ", 本金: " + order.getAmount() + ", 收益: " + actualIncome);
        
        if (actualIncome.compareTo(BigDecimal.ZERO) > 0) {
            assetStatisticsService.recordAssetFlow(order.getUserId(), "profit", actualIncome,
                    balanceBefore, balanceAfter, BigDecimal.ZERO, BigDecimal.ZERO,
                    totalAssetBefore, totalAssetAfter,
                    orderId, "order", "到期收益: " + product.getProductName());
        }
        
        logStatusChange(order, currentStatus, OrderStatus.SETTLED, "AUTO", "到期自动结算", null);
        
        log.info("结算订单成功: orderId={}, settleAmount={}, income={}", orderId, settleAmount, actualIncome);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processExpiredOrders() {
        log.info("开始处理过期订单...");
        
        LambdaQueryWrapper<ProductOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductOrder::getStatus, OrderStatus.PENDING.getCode())
                .lt(ProductOrder::getExpireTime, LocalDateTime.now());
        
        List<ProductOrder> expiredOrders = orderMapper.selectList(wrapper);
        
        int count = 0;
        for (ProductOrder order : expiredOrders) {
            try {
                FinancialProduct product = productMapper.selectById(order.getProductId());
                
                order.setStatus(OrderStatus.CANCELLED.getCode());
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("订单超时自动取消");
                orderMapper.updateById(order);
                
                UserAccount account = userAccountMapper.selectOne(
                        new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, order.getUserId())
                );
                
                BigDecimal balanceBefore = account.getAvailableBalance();
                BigDecimal frozenBefore = account.getFrozenBalance();
                BigDecimal totalAssetBefore = account.getTotalAsset();
                BigDecimal balanceAfter = balanceBefore.add(order.getAmount());
                BigDecimal frozenAfter = frozenBefore.subtract(order.getAmount());
                
                account.setAvailableBalance(balanceAfter);
                account.setFrozenBalance(frozenAfter);
                userAccountMapper.updateById(account);
                
                createTransaction(order.getUserId(), order.getId(), "refund", order.getAmount(), 
                        balanceBefore, balanceAfter, "订单超时自动取消退款");
                
                assetStatisticsService.recordAssetFlow(order.getUserId(), "unfreeze", order.getAmount(),
                        balanceBefore, balanceAfter, frozenBefore, frozenAfter,
                        totalAssetBefore, totalAssetBefore,
                        order.getId(), "order", "订单超时自动取消退款: " + product.getProductName());
                
                logStatusChange(order, OrderStatus.PENDING, OrderStatus.CANCELLED, "AUTO", "订单超时自动取消", null);
                
                count++;
            } catch (Exception e) {
                log.error("处理过期订单失败: orderId={}", order.getId(), e);
            }
        }
        
        log.info("处理过期订单完成: 共处理 {} 个订单", count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processMaturedOrders() {
        log.info("开始处理到期订单...");
        
        LambdaQueryWrapper<ProductOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductOrder::getStatus, OrderStatus.CONFIRMED.getCode())
                .le(ProductOrder::getMaturityDate, LocalDate.now());
        
        List<ProductOrder> maturedOrders = orderMapper.selectList(wrapper);
        
        int count = 0;
        for (ProductOrder order : maturedOrders) {
            try {
                order.setStatus(OrderStatus.MATURED.getCode());
                orderMapper.updateById(order);
                
                logStatusChange(order, OrderStatus.CONFIRMED, OrderStatus.MATURED, "AUTO", "产品到期", null);
                
                settleOrder(order.getId());
                
                count++;
            } catch (Exception e) {
                log.error("处理到期订单失败: orderId={}", order.getId(), e);
            }
        }
        
        log.info("处理到期订单完成: 共处理 {} 个订单", count);
    }

    @Override
    public List<ProductOrder> getExpiredOrders() {
        LambdaQueryWrapper<ProductOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductOrder::getStatus, OrderStatus.PENDING.getCode())
                .lt(ProductOrder::getExpireTime, LocalDateTime.now());
        return orderMapper.selectList(wrapper);
    }

    @Override
    public List<ProductOrder> getMaturedOrders() {
        LambdaQueryWrapper<ProductOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductOrder::getStatus, OrderStatus.CONFIRMED.getCode())
                .le(ProductOrder::getMaturityDate, LocalDate.now());
        return orderMapper.selectList(wrapper);
    }

    @Override
    public BigDecimal calculateExpectedIncome(ProductOrder order) {
        if (order == null || order.getAmount() == null || order.getAnnualReturnRate() == null || order.getTermDays() == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal dailyRate = order.getAnnualReturnRate()
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        
        return order.getAmount()
                .multiply(dailyRate)
                .multiply(BigDecimal.valueOf(order.getTermDays()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateActualIncome(ProductOrder order) {
        if (order == null || order.getAmount() == null || order.getAnnualReturnRate() == null) {
            return BigDecimal.ZERO;
        }
        
        LocalDate startDate = order.getConfirmTime() != null ? 
                order.getConfirmTime().toLocalDate() : order.getOrderTime().toLocalDate();
        LocalDate endDate = LocalDate.now();
        
        long holdingDays = Math.max(0, ChronoUnit.DAYS.between(startDate, endDate));
        
        BigDecimal dailyRate = order.getAnnualReturnRate()
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        
        return order.getAmount()
                .multiply(dailyRate)
                .multiply(BigDecimal.valueOf(holdingDays))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderIncome(Long orderId) {
        ProductOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        
        BigDecimal expectedIncome = calculateExpectedIncome(order);
        order.setExpectedIncome(expectedIncome);
        orderMapper.updateById(order);
    }

    @Override
    public boolean canTransition(Long orderId, OrderStatus targetStatus) {
        ProductOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        
        OrderStatus currentStatus = OrderStatus.fromCode(order.getStatus());
        return OrderStatus.canTransit(currentStatus, targetStatus);
    }

    @Override
    public OrderStatus getCurrentStatus(Long orderId) {
        ProductOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return null;
        }
        return OrderStatus.fromCode(order.getStatus());
    }

    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD" + dateStr + IdUtil.randomUUID().substring(0, 6).toUpperCase();
    }

    private void logStatusChange(ProductOrder order, OrderStatus fromStatus, OrderStatus toStatus, 
                                  String triggerType, String reason, Long operatorId) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setOrderNo(order.getOrderNo());
        log.setUserId(order.getUserId());
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
        
        statusLogMapper.insert(log);
    }

    private void createTransaction(Long userId, Long orderId, String type, BigDecimal amount, 
                                    BigDecimal balanceBefore, BigDecimal balanceAfter, String remark) {
        Transaction transaction = new Transaction();
        transaction.setTransactionNo("TXN" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000)));
        transaction.setUserId(userId);
        transaction.setOrderId(orderId);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRemark(remark);
        transaction.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(transaction);
    }

    private OrderVO buildOrderVO(ProductOrder order, FinancialProduct product) {
        OrderVO vo = new OrderVO();
        BeanUtil.copyProperties(order, vo);
        
        if (product != null) {
            vo.setProductName(product.getProductName());
            vo.setProductCode(product.getProductCode());
            vo.setProductType(product.getProductType());
            vo.setAnnualReturnRate(product.getAnnualReturnRate());
        }
        
        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        if (status != null) {
            vo.setStatusText(status.getDesc());
        }
        
        return vo;
    }
}
