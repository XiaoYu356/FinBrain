package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_preference")
public class UserPreference implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String preferenceKey;

    private String preferenceValue;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
