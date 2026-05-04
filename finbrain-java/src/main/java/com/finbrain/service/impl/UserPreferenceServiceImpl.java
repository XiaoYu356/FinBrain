package com.finbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finbrain.entity.UserPreference;
import com.finbrain.mapper.UserPreferenceMapper;
import com.finbrain.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceMapper userPreferenceMapper;

    @Override
    @Transactional
    public void setPreference(Long userId, String key, String value) {
        UserPreference existing = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
                        .eq(UserPreference::getPreferenceKey, key)
        );

        if (existing != null) {
            existing.setPreferenceValue(value);
            userPreferenceMapper.updateById(existing);
        } else {
            UserPreference preference = new UserPreference();
            preference.setUserId(userId);
            preference.setPreferenceKey(key);
            preference.setPreferenceValue(value);
            userPreferenceMapper.insert(preference);
        }
        
        log.info("用户偏好已保存: userId={}, key={}, value={}", userId, key, value);
    }

    @Override
    public String getPreference(Long userId, String key) {
        UserPreference preference = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
                        .eq(UserPreference::getPreferenceKey, key)
        );
        return preference != null ? preference.getPreferenceValue() : null;
    }

    @Override
    public Map<String, String> getAllPreferences(Long userId) {
        List<UserPreference> preferences = userPreferenceMapper.selectList(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId)
        );

        Map<String, String> result = new HashMap<>();
        for (UserPreference pref : preferences) {
            result.put(pref.getPreferenceKey(), pref.getPreferenceValue());
        }
        return result;
    }

    @Override
    @Transactional
    public void setPreferences(Long userId, Map<String, String> preferences) {
        for (Map.Entry<String, String> entry : preferences.entrySet()) {
            setPreference(userId, entry.getKey(), entry.getValue());
        }
    }
}
