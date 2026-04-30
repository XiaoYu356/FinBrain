package com.finbrain.service;

import com.finbrain.dto.RiskDTO;
import com.finbrain.entity.RiskAssessment;
import com.finbrain.vo.RiskAssessmentVO;

public interface RiskService {

    RiskAssessmentVO submitAssessment(Long userId, RiskDTO dto);

    RiskAssessmentVO getLatestAssessment(Long userId);

    String calculateRiskLevel(Integer score);
}
