package com.finbrain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RiskAssessmentVO {

    private Long id;

    private Integer score;

    private String riskLevel;

    private String riskLevelDesc;

    private String answers;

    private LocalDateTime assessmentTime;
}
