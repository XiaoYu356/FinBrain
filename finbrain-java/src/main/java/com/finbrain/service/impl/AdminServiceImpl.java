package com.finbrain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.dto.AdminProductDTO;
import com.finbrain.entity.FinancialProduct;
import com.finbrain.entity.ProductLifecycleLog;
import com.finbrain.entity.ProductOrder;
import com.finbrain.entity.User;
import com.finbrain.entity.UserAccount;
import com.finbrain.enums.ProductStatus;
import com.finbrain.exception.BusinessException;
import com.finbrain.mapper.FinancialProductMapper;
import com.finbrain.mapper.ProductLifecycleLogMapper;
import com.finbrain.mapper.ProductOrderMapper;
import com.finbrain.mapper.UserAccountMapper;
import com.finbrain.mapper.UserMapper;
import com.finbrain.service.AdminService;
import com.finbrain.vo.AdminAssetVO;
import com.finbrain.vo.AdminOrderVO;
import com.finbrain.vo.AdminUserVO;
import com.finbrain.vo.ProductVO;
import com.finbrain.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final UserAccountMapper userAccountMapper;
    private final FinancialProductMapper productMapper;
    private final ProductOrderMapper orderMapper;
    private final ProductLifecycleLogMapper lifecycleLogMapper;

    @Override
    public Page<AdminUserVO> getUserList(Integer pageNum, Integer pageSize, String keyword) {
        Page<User> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getPhone, keyword);
        }
        wrapper.orderByDesc(User::getCreateTime);
        
        Page<User> userPage = userMapper.selectPage(page, wrapper);
        
        Page<AdminUserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        voPage.setRecords(userPage.getRecords().stream().map(user -> {
            AdminUserVO vo = new AdminUserVO();
            BeanUtil.copyProperties(user, vo);
            
            UserAccount account = userAccountMapper.selectOne(
                    new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, user.getId())
            );
            if (account != null) {
                vo.setTotalAsset(account.getTotalAsset());
                vo.setAvailableBalance(account.getAvailableBalance());
                vo.setTotalProfit(account.getTotalProfit());
            }
            return vo;
        }).collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public AdminUserVO getUserDetail(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        AdminUserVO vo = new AdminUserVO();
        BeanUtil.copyProperties(user, vo);
        
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, id)
        );
        if (account != null) {
            vo.setTotalAsset(account.getTotalAsset());
            vo.setAvailableBalance(account.getAvailableBalance());
            vo.setTotalProfit(account.getTotalProfit());
        }
        
        return vo;
    }

    @Override
    public void updateUserStatus(Long currentUserId, Long id, Integer status) {
        if (currentUserId.equals(id)) {
            throw new BusinessException("不能禁用自己");
        }
        
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        user.setStatus(status);
        userMapper.updateById(user);
        log.info("管理员更新用户 {} 状态为 {}", id, status);
    }

    @Override
    public Page<ProductVO> getProductList(Integer pageNum, Integer pageSize, Integer status) {
        Page<FinancialProduct> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<FinancialProduct> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(FinancialProduct::getStatus, status);
        }
        wrapper.orderByDesc(FinancialProduct::getCreateTime);
        
        Page<FinancialProduct> productPage = productMapper.selectPage(page, wrapper);
        
        Page<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream().map(product -> {
            ProductVO vo = new ProductVO();
            BeanUtil.copyProperties(product, vo);
            
            ProductStatus productStatus = ProductStatus.fromCode(product.getStatus());
            if (productStatus != null) {
                vo.setStatusName(productStatus.getDesc());
            }
            
            return vo;
        }).collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public FinancialProduct getProductById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    @Transactional
    public void addProduct(AdminProductDTO dto) {
        FinancialProduct product = new FinancialProduct();
        BeanUtil.copyProperties(dto, product);
        product.setSaleStatus(1);
        product.setStatus(ProductStatus.DRAFT.getCode());
        productMapper.insert(product);
        log.info("管理员添加产品: {}", product.getProductName());
    }

    @Override
    @Transactional
    public void updateProduct(Long id, AdminProductDTO dto) {
        FinancialProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        
        BeanUtil.copyProperties(dto, product);
        productMapper.updateById(product);
        log.info("管理员更新产品: {}", product.getProductName());
    }

    @Override
    @Transactional
    public void updateProductSaleStatus(Long id, Integer saleStatus) {
        FinancialProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        
        product.setSaleStatus(saleStatus);
        productMapper.updateById(product);
        log.info("管理员更新产品 {} 销售状态为 {}", id, saleStatus);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        FinancialProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("产品不存在");
        }
        
        Long orderCount = orderMapper.selectCount(
                new LambdaQueryWrapper<ProductOrder>().eq(ProductOrder::getProductId, id)
        );
        if (orderCount > 0) {
            throw new BusinessException("该产品存在关联订单，无法删除。请先下架产品。");
        }
        
        lifecycleLogMapper.delete(
                new LambdaQueryWrapper<ProductLifecycleLog>().eq(ProductLifecycleLog::getProductId, id)
        );
        
        productMapper.deleteById(id);
        log.info("管理员删除产品: {}", product.getProductName());
    }

    @Override
    public Page<AdminOrderVO> getOrderList(Integer pageNum, Integer pageSize, Long userId, Integer status) {
        Page<ProductOrder> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<ProductOrder> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(ProductOrder::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(ProductOrder::getStatus, status);
        }
        wrapper.orderByDesc(ProductOrder::getOrderTime);
        
        Page<ProductOrder> orderPage = orderMapper.selectPage(page, wrapper);
        
        Page<AdminOrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords().stream().map(order -> {
            AdminOrderVO vo = new AdminOrderVO();
            BeanUtil.copyProperties(order, vo);
            
            User user = userMapper.selectById(order.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
            }
            
            FinancialProduct product = productMapper.selectById(order.getProductId());
            if (product != null) {
                vo.setProductName(product.getProductName());
                vo.setProductCode(product.getProductCode());
            }
            
            vo.setStatusText(getStatusText(order.getStatus()));
            return vo;
        }).collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public AdminOrderVO getOrderDetail(Long id) {
        ProductOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        AdminOrderVO vo = new AdminOrderVO();
        BeanUtil.copyProperties(order, vo);
        
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
        }
        
        FinancialProduct product = productMapper.selectById(order.getProductId());
        if (product != null) {
            vo.setProductName(product.getProductName());
            vo.setProductCode(product.getProductCode());
        }
        
        vo.setStatusText(getStatusText(order.getStatus()));
        return vo;
    }

    @Override
    public Page<AdminAssetVO> getAssetList(Integer pageNum, Integer pageSize, String keyword) {
        Page<UserAccount> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<UserAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(UserAccount::getUpdateTime);
        
        Page<UserAccount> accountPage = userAccountMapper.selectPage(page, wrapper);
        
        Page<AdminAssetVO> voPage = new Page<>(accountPage.getCurrent(), accountPage.getSize(), accountPage.getTotal());
        voPage.setRecords(accountPage.getRecords().stream().map(account -> {
            AdminAssetVO vo = new AdminAssetVO();
            vo.setUserId(account.getUserId());
            vo.setTotalAsset(account.getTotalAsset());
            vo.setAvailableBalance(account.getAvailableBalance());
            vo.setFrozenBalance(account.getFrozenBalance());
            vo.setTotalProfit(account.getTotalProfit());
            
            User user = userMapper.selectById(account.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
                vo.setPhone(user.getPhone());
                vo.setEmail(user.getEmail());
                vo.setRiskLevel(user.getRiskLevel());
                vo.setStatus(user.getStatus());
                
                if (StringUtils.hasText(keyword)) {
                    if (!user.getUsername().contains(keyword) && 
                        !user.getRealName().contains(keyword) &&
                        !user.getPhone().contains(keyword)) {
                        return null;
                    }
                }
            }
            return vo;
        }).filter(vo -> vo != null).collect(Collectors.toList()));
        
        return voPage;
    }

    @Override
    public StatisticsVO getStatistics() {
        StatisticsVO vo = new StatisticsVO();
        
        Long totalUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)
        );
        vo.setTotalUsers(totalUsers);
        
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .ge(User::getCreateTime, todayStart)
        );
        vo.setTodayNewUsers(todayNewUsers);
        
        Long totalOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<ProductOrder>().in(ProductOrder::getStatus, 1, 3)
        );
        vo.setTotalOrders(totalOrders);
        
        List<ProductOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<ProductOrder>().eq(ProductOrder::getStatus, 1)
        );
        BigDecimal totalOrderAmount = orders.stream()
                .map(ProductOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalOrderAmount(totalOrderAmount);
        
        List<UserAccount> accounts = userAccountMapper.selectList(null);
        BigDecimal totalAssets = accounts.stream()
                .map(UserAccount::getTotalAsset)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalAssets(totalAssets);
        
        BigDecimal totalProfit = accounts.stream()
                .map(UserAccount::getTotalProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalProfit(totalProfit);
        
        Long todayOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<ProductOrder>()
                        .ge(ProductOrder::getOrderTime, todayStart)
        );
        vo.setTodayOrders(todayOrders);
        
        List<ProductOrder> todayOrderList = orderMapper.selectList(
                new LambdaQueryWrapper<ProductOrder>()
                        .ge(ProductOrder::getOrderTime, todayStart)
                        .eq(ProductOrder::getStatus, 1)
        );
        BigDecimal todayOrderAmount = todayOrderList.stream()
                .map(ProductOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTodayOrderAmount(todayOrderAmount);
        
        return vo;
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
