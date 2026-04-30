package com.finbrain.controller;

import com.finbrain.dto.RiskDTO;
import com.finbrain.service.AuthService;
import com.finbrain.service.RiskService;
import com.finbrain.utils.Result;
import com.finbrain.vo.RiskAssessmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "风险测评")
@RestController
@RequestMapping("/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;
    private final AuthService authService;

    @Operation(summary = "获取测评问卷")
    @GetMapping("/questions")
    public Result<List<Map<String, Object>>> getQuestions() {
        List<Map<String, Object>> questions = new ArrayList<>();
        
        Map<String, Object> q1 = new HashMap<>();
        q1.put("id", 1);
        q1.put("question", "您的年龄范围是？");
        q1.put("options", List.of("18-30岁", "30-40岁", "40-50岁", "50岁以上"));
        questions.add(q1);
        
        Map<String, Object> q2 = new HashMap<>();
        q2.put("id", 2);
        q2.put("question", "您的投资经验如何？");
        q2.put("options", List.of("无经验", "1年以下", "1-3年", "3-5年", "5年以上"));
        questions.add(q2);
        
        Map<String, Object> q3 = new HashMap<>();
        q3.put("id", 3);
        q3.put("question", "您能接受的最大亏损比例是？");
        q3.put("options", List.of("0%", "5%", "10%", "20%", "30%", "50%以上"));
        questions.add(q3);
        
        Map<String, Object> q4 = new HashMap<>();
        q4.put("id", 4);
        q4.put("question", "您的年收入水平是？");
        q4.put("options", List.of("10万以下", "10-30万", "30-50万", "50万以上"));
        questions.add(q4);
        
        Map<String, Object> q5 = new HashMap<>();
        q5.put("id", 5);
        q5.put("question", "您的投资目标是？");
        q5.put("options", List.of("本金安全", "稳健增值", "平衡收益", "追求高收益"));
        questions.add(q5);
        
        return Result.success(questions);
    }

    @Operation(summary = "提交测评答案")
    @PostMapping("/submit")
    public Result<RiskAssessmentVO> submitAssessment(@Valid @RequestBody RiskDTO dto) {
        Long userId = authService.getCurrentUser().getId();
        return Result.success(riskService.submitAssessment(userId, dto));
    }

    @Operation(summary = "获取最新测评结果")
    @GetMapping("/result")
    public Result<RiskAssessmentVO> getLatestAssessment() {
        Long userId = authService.getCurrentUser().getId();
        return Result.success(riskService.getLatestAssessment(userId));
    }
}
