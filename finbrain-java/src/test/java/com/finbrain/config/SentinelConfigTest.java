package com.finbrain.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;

class SentinelConfigTest {

    private SentinelConfig sentinelConfig;

    @BeforeEach
    void setUp() {
        sentinelConfig = new SentinelConfig();
    }

    @Test
    void testSentinelResourceAspectBeanCreation() {
        assertNotNull(sentinelConfig.sentinelResourceAspect());
    }

    @Test
    void testInitFlowRules() {
        try (MockedStatic<FlowRuleManager> mockedStatic = Mockito.mockStatic(FlowRuleManager.class)) {
            mockedStatic.when(() -> FlowRuleManager.loadRules(anyList()))
                .thenAnswer(invocation -> {
                    List<FlowRule> rules = invocation.getArgument(0);
                    assertTrue(rules.size() > 0);
                    
                    boolean hasLoginRule = rules.stream()
                        .anyMatch(rule -> "auth:login".equals(rule.getResource()));
                    assertTrue(hasLoginRule);
                    
                    boolean hasAiChatRule = rules.stream()
                        .anyMatch(rule -> "ai:chat".equals(rule.getResource()));
                    assertTrue(hasAiChatRule);
                    
                    return null;
                });

            sentinelConfig.initFlowRules();
            
            mockedStatic.verify(() -> FlowRuleManager.loadRules(anyList()), Mockito.times(1));
        }
    }

    @Test
    void testInitDegradeRules() {
        try (MockedStatic<com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager> mockedStatic = 
                Mockito.mockStatic(com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager.class)) {
            
            mockedStatic.when(() -> com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager.loadRules(anyList()))
                .thenAnswer(invocation -> {
                    List<com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule> rules = invocation.getArgument(0);
                    assertTrue(rules.size() > 0);
                    
                    boolean hasAiChatRule = rules.stream()
                        .anyMatch(rule -> "ai:chat".equals(rule.getResource()));
                    assertTrue(hasAiChatRule);
                    
                    return null;
                });

            sentinelConfig.initDegradeRules();
            
            mockedStatic.verify(() -> 
                com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager.loadRules(anyList()), 
                Mockito.times(1));
        }
    }
}
