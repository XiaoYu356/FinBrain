package com.finbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finbrain.entity.UserAccount;
import com.finbrain.exception.BusinessException;
import com.finbrain.mapper.UserAccountMapper;
import com.finbrain.service.AccountService;
import com.finbrain.vo.AssetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserAccountMapper userAccountMapper;

    @Override
    public UserAccount getUserAccount(Long userId) {
        return userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId)
        );
    }

    @Override
    public AssetVO getAssetInfo(Long userId) {
        UserAccount account = getUserAccount(userId);
        if (account == null) {
            throw new BusinessException("账户不存在");
        }
        
        AssetVO vo = new AssetVO();
        vo.setUserId(userId);
        vo.setTotalAsset(account.getTotalAsset());
        vo.setAvailableBalance(account.getAvailableBalance());
        vo.setFrozenBalance(account.getFrozenBalance());
        vo.setTotalProfit(account.getTotalProfit());
        vo.setTodayProfit(BigDecimal.ZERO);
        vo.setTotalInvest(account.getTotalAsset().subtract(account.getTotalProfit()));
        
        return vo;
    }

    @Override
    public void updateBalance(Long userId, BigDecimal amount, String type) {
        UserAccount account = getUserAccount(userId);
        if (account == null) {
            throw new BusinessException("账户不存在");
        }
        
        BigDecimal balanceBefore = account.getAvailableBalance();
        BigDecimal balanceAfter;
        
        switch (type) {
            case "buy":
                if (balanceBefore.compareTo(amount) < 0) {
                    throw new BusinessException("余额不足");
                }
                balanceAfter = balanceBefore.subtract(amount);
                account.setAvailableBalance(balanceAfter);
                account.setFrozenBalance(account.getFrozenBalance().add(amount));
                break;
            case "redeem":
                balanceAfter = balanceBefore.add(amount);
                account.setAvailableBalance(balanceAfter);
                account.setFrozenBalance(account.getFrozenBalance().subtract(amount));
                break;
            case "profit":
                balanceAfter = balanceBefore.add(amount);
                account.setAvailableBalance(balanceAfter);
                account.setTotalAsset(account.getTotalAsset().add(amount));
                account.setTotalProfit(account.getTotalProfit().add(amount));
                break;
            default:
                throw new BusinessException("未知交易类型");
        }
        
        userAccountMapper.updateById(account);
    }

    @Override
    public void recharge(Long userId, BigDecimal amount) {
        UserAccount account = getUserAccount(userId);
        if (account == null) {
            throw new BusinessException("账户不存在");
        }
        
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setTotalAsset(account.getTotalAsset().add(amount));
        
        userAccountMapper.updateById(account);
        log.info("用户 {} 充值成功，金额: {}", userId, amount);
    }
}
