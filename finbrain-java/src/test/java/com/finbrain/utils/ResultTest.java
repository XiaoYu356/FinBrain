package com.finbrain.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("统一响应结果测试")
class ResultTest {

    @Test
    @DisplayName("成功响应-无数据")
    void success_noData() {
        Result<Void> result = Result.success();
        
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("成功响应-有数据")
    void success_withData() {
        String data = "test data";
        Result<String> result = Result.success(data);
        
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals(data, result.getData());
    }

    @Test
    @DisplayName("失败响应-默认错误码")
    void error_defaultCode() {
        String message = "操作失败";
        Result<Void> result = Result.error(message);
        
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals(message, result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("失败响应-自定义错误码")
    void error_customCode() {
        String message = "未授权";
        Result<Void> result = Result.error(401, message);
        
        assertNotNull(result);
        assertEquals(401, result.getCode());
        assertEquals(message, result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("成功响应-自定义消息")
    void success_customMessage() {
        String message = "操作成功";
        String data = "test data";
        Result<String> result = Result.success(message, data);
        
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals(message, result.getMessage());
        assertEquals(data, result.getData());
    }
}
