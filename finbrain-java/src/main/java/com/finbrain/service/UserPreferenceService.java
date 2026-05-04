package com.finbrain.service;

import java.util.Map;

public interface UserPreferenceService {

    void setPreference(Long userId, String key, String value);

    String getPreference(Long userId, String key);

    Map<String, String> getAllPreferences(Long userId);

    void setPreferences(Long userId, Map<String, String> preferences);
}
