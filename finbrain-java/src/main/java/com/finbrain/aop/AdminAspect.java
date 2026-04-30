package com.finbrain.aop;

import com.finbrain.annotation.RequireAdmin;
import com.finbrain.entity.User;
import com.finbrain.exception.BusinessException;
import com.finbrain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminAspect {

    private final UserMapper userMapper;

    @Before("@annotation(requireAdmin)")
    public void checkAdmin(RequireAdmin requireAdmin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        
        String userId = (String) authentication.getPrincipal();
        User user = userMapper.selectById(Long.parseLong(userId));
        
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        if (!"admin".equals(user.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }
    }
}
