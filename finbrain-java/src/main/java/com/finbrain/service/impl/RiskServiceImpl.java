package com.finbrain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finbrain.dto.RiskDTO;
import com.finbrain.entity.RiskAssessment;
import com.finbrain.entity.User;
import com.finbrain.mapper.RiskAssessmentMapper;
import com.finbrain.mapper.UserMapper;
import com.finbrain.service.RiskService;
import com.finbrain.vo.RiskAssessmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService {

    private final RiskAssessmentMapper riskAssessmentMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public RiskAssessmentVO submitAssessment(Long userId, RiskDTO dto) {
        int score = calculateScore(dto);
        String riskLevel = calculateRiskLevel(score);
        
        RiskAssessment assessment = new RiskAssessment();
        assessment.setUserId(userId);
        assessment.setScore(score);
        assessment.setRiskLevel(riskLevel);
        assessment.setAnswers(JSONUtil.toJsonStr(dto.getAnswers()));
        assessment.setAssessmentTime(LocalDateTime.now());
        riskAssessmentMapper.insert(assessment);
        
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setRiskLevel(riskLevel);
            userMapper.updateById(user);
        }
        
        RiskAssessmentVO vo = new RiskAssessmentVO();
        BeanUtil.copyProperties(assessment, vo);
        vo.setRiskLevelDesc(getRiskLevelDesc(riskLevel));
        return vo;
    }

    @Override
    public RiskAssessmentVO getLatestAssessment(Long userId) {
        RiskAssessment assessment = riskAssessmentMapper.selectOne(
                new LambdaQueryWrapper<RiskAssessment>()
                        .eq(RiskAssessment::getUserId, userId)
                        .orderByDesc(RiskAssessment::getAssessmentTime)
                        .last("LIMIT 1")
        );
        
        if (assessment == null) {
            return null;
        }
        
        RiskAssessmentVO vo = new RiskAssessmentVO();
        BeanUtil.copyProperties(assessment, vo);
        vo.setRiskLevelDesc(getRiskLevelDesc(assessment.getRiskLevel()));
        return vo;
    }

    @Override
    public String calculateRiskLevel(Integer score) {
        if (score >= 80) {
            return "C5";
        } else if (score >= 60) {
            return "C4";
        } else if (score >= 40) {
            return "C3";
        } else if (score >= 20) {
            return "C2";
        } else {
            return "C1";
        }
    }

    private int calculateScore(RiskDTO dto) {
        int score = 0;
        
        for (RiskDTO.AnswerItem item : dto.getAnswers()) {
            if (item.getAnswer() == null) continue;
            
            switch (item.getQuestion()) {
                case "您的年龄范围是？":
                    if (item.getAnswer().contains("30-40")) score += 15;
                    else if (item.getAnswer().contains("40-50")) score += 10;
                    else if (item.getAnswer().contains("50以上")) score += 5;
                    else score += 20;
                    break;
                case "您的投资经验如何？":
                    if (item.getAnswer().contains("5年以上")) score += 25;
                    else if (item.getAnswer().contains("3-5年")) score += 20;
                    else if (item.getAnswer().contains("1-3年")) score += 15;
                    else score += 5;
                    break;
                case "您能接受的最大亏损比例是？":
                    if (item.getAnswer().contains("50%")) score += 25;
                    else if (item.getAnswer().contains("30%")) score += 20;
                    else if (item.getAnswer().contains("20%")) score += 15;
                    else if (item.getAnswer().contains("10%")) score += 10;
                    else score += 5;
                    break;
                case "您的年收入水平是？":
                    if (item.getAnswer().contains("50万")) score += 20;
                    else if (item.getAnswer().contains("30万")) score += 15;
                    else if (item.getAnswer().contains("10万")) score += 10;
                    else score += 5;
                    break;
                case "您的投资目标是？":
                    if (item.getAnswer().contains("高收益")) score += 20;
                    else if (item.getAnswer().contains("平衡")) score += 15;
                    else score += 10;
                    break;
                default:
                    score += 10;
            }
        }
        
        return Math.min(score, 100);
    }

    private String getRiskLevelDesc(String riskLevel) {
        switch (riskLevel) {
            case "C1": return "保守型";
            case "C2": return "谨慎型";
            case "C3": return "稳健型";
            case "C4": return "进取型";
            case "C5": return "激进型";
            default: return "未知";
        }
    }
}
