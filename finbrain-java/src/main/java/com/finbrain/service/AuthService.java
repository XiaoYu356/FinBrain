package com.finbrain.service;

import com.finbrain.dto.ChangePasswordDTO;
import com.finbrain.dto.LoginDTO;
import com.finbrain.dto.RegisterDTO;
import com.finbrain.dto.UpdateProfileDTO;
import com.finbrain.entity.User;
import com.finbrain.vo.UserVO;

import java.util.Map;

public interface AuthService {

    Map<String, Object> login(LoginDTO dto);

    void register(RegisterDTO dto);

    Map<String, Object> refreshToken(String token);

    void logout(String token);

    User getCurrentUser();

    UserVO getCurrentUserInfo();

    void changePassword(ChangePasswordDTO dto);

    void updateProfile(UpdateProfileDTO dto);
}
