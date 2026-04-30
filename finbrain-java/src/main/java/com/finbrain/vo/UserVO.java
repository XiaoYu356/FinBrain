package com.finbrain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private String phone;

    private String email;

    private String riskLevel;

    private Integer status;

    private LocalDateTime createTime;
}
