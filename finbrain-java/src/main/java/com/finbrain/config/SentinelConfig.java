package com.finbrain.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
public class SentinelConfig {

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule loginRule = new FlowRule();
        loginRule.setResource("auth:login");
        loginRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        loginRule.setCount(5);
        rules.add(loginRule);

        FlowRule registerRule = new FlowRule();
        registerRule.setResource("auth:register");
        registerRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        registerRule.setCount(3);
        rules.add(registerRule);

        FlowRule aiChatRule = new FlowRule();
        aiChatRule.setResource("ai:chat");
        aiChatRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        aiChatRule.setCount(20);
        rules.add(aiChatRule);

        FlowRule createOrderRule = new FlowRule();
        createOrderRule.setResource("order:create");
        createOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        createOrderRule.setCount(10);
        rules.add(createOrderRule);

        FlowRule productSearchRule = new FlowRule();
        productSearchRule.setResource("product:search");
        productSearchRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        productSearchRule.setCount(60);
        rules.add(productSearchRule);

        FlowRule productDetailRule = new FlowRule();
        productDetailRule.setResource("product:detail");
        productDetailRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        productDetailRule.setCount(100);
        rules.add(productDetailRule);

        FlowRule importRule = new FlowRule();
        importRule.setResource("data:import");
        importRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        importRule.setCount(5);
        rules.add(importRule);

        FlowRule globalRule = new FlowRule();
        globalRule.setResource("global");
        globalRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        globalRule.setCount(1000);
        rules.add(globalRule);

        FlowRuleManager.loadRules(rules);
        log.info("Sentinel flow rules initialized: {} rules", rules.size());
    }

    public void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        DegradeRule aiChatDegradeRule = new DegradeRule("ai:chat");
        aiChatDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        aiChatDegradeRule.setCount(5000);
        aiChatDegradeRule.setSlowRatioThreshold(0.5);
        aiChatDegradeRule.setMinRequestAmount(10);
        aiChatDegradeRule.setStatIntervalMs(10000);
        aiChatDegradeRule.setTimeWindow(30);
        rules.add(aiChatDegradeRule);

        DegradeRule orderDegradeRule = new DegradeRule("order:create");
        orderDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        orderDegradeRule.setCount(0.3);
        orderDegradeRule.setMinRequestAmount(10);
        orderDegradeRule.setStatIntervalMs(10000);
        orderDegradeRule.setTimeWindow(30);
        rules.add(orderDegradeRule);

        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel degrade rules initialized: {} rules", rules.size());
    }
}
