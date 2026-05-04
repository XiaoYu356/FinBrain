package com.finbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.annotation.RequireAdmin;
import com.finbrain.service.AuditService;
import com.finbrain.utils.Result;
import com.finbrain.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "审计日志管理")
@RestController
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @Operation(summary = "获取操作日志列表")
    @GetMapping("/logs")
    @RequireAdmin
    public Result<Page<OperationLogVO>> getLogList(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(auditService.getLogList(module, operation, userId, startTime, endTime, pageNum, pageSize));
    }

    @Operation(summary = "获取操作日志详情")
    @GetMapping("/logs/{id}")
    @RequireAdmin
    public Result<OperationLogVO> getLogById(@PathVariable Long id) {
        return Result.success(auditService.getLogById(id));
    }

    @Operation(summary = "获取所有模块")
    @GetMapping("/modules")
    @RequireAdmin
    public Result<List<String>> getModules() {
        return Result.success(auditService.getModules());
    }

    @Operation(summary = "获取模块下的操作类型")
    @GetMapping("/operations")
    @RequireAdmin
    public Result<List<String>> getOperationsByModule(@RequestParam String module) {
        return Result.success(auditService.getOperationsByModule(module));
    }

    @Operation(summary = "统计用户操作次数")
    @GetMapping("/count/{userId}")
    @RequireAdmin
    public Result<Long> countByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.success(auditService.countByUserId(userId, startTime, endTime));
    }

    @Operation(summary = "清理旧日志")
    @DeleteMapping("/clean")
    @RequireAdmin
    public Result<Void> cleanOldLogs(@RequestParam(defaultValue = "90") Integer days) {
        auditService.cleanOldLogs(days);
        return Result.success();
    }
}
