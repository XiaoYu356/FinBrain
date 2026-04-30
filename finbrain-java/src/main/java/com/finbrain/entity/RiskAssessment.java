package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("risk_assessment")
public class RiskAssessment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer score;

    private String riskLevel;

    private String answers;

    private LocalDateTime assessmentTime;
}
