package com.finbrain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finbrain.dto.LoginDTO;
import com.finbrain.dto.RegisterDTO;
import com.finbrain.entity.User;
import com.finbrain.entity.UserAccount;
import com.finbrain.exception.BusinessException;
import com.finbrain.mapper.UserAccountMapper;
import com.finbrain.mapper.UserMapper;
import com.finbrain.service.AuthService;
import com.finbrain.utils.JwtUtil;
import com.finbrain.utils.RedisUtil;
import com.finbrain.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        String tokenKey = "token:user:" + user.getId();
        redisUtil.set(tokenKey, token, 24, TimeUnit.HOURS);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return result;
    }

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRiskLevel("C1");
        user.setStatus(1);
        user.setRole("user");
        userMapper.insert(user);
        
        UserAccount account = new UserAccount();
        account.setUserId(user.getId());
        account.setTotalAsset(BigDecimal.ZERO);
        account.setAvailableBalance(BigDecimal.ZERO);
        account.setFrozenBalance(BigDecimal.ZERO);
        account.setTotalProfit(BigDecimal.ZERO);
        userAccountMapper.insert(account);
    }

    @Override
    public Map<String, Object> refreshToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("Token无效");
        }
        
        String userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        
        String newToken = jwtUtil.generateToken(Long.parseLong(userId), username);
        
        String tokenKey = "token:user:" + userId;
        redisUtil.set(tokenKey, newToken, 24, TimeUnit.HOURS);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        return result;
    }

    @Override
    public void logout(String token) {
        if (StrUtil.isNotBlank(token)) {
            String userId = jwtUtil.getUserIdFromToken(token);
            String tokenKey = "token:user:" + userId;
            redisUtil.delete(tokenKey);
            
            String blacklistKey = "token:blacklist:" + token;
            redisUtil.set(blacklistKey, "1", 24, TimeUnit.HOURS);
        }
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        String userId = (String) authentication.getPrincipal();
        User user = userMapper.selectById(Long.parseLong(userId));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public UserVO getCurrentUserInfo() {
        User user = getCurrentUser();
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }
}
