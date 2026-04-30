package com.finbrain.service;

import com.finbrain.entity.UserAccount;
import com.finbrain.vo.AssetVO;

import java.math.BigDecimal;

public interface AccountService {

    UserAccount getUserAccount(Long userId);

    AssetVO getAssetInfo(Long userId);

    void updateBalance(Long userId, BigDecimal amount, String type);

    void recharge(Long userId, BigDecimal amount);
}
