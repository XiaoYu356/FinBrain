package com.finbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.annotation.RequireAdmin;
import com.finbrain.entity.DataImportLog;
import com.finbrain.service.DataImportService;
import com.finbrain.utils.Result;
import com.finbrain.vo.ImportResultVO;
import com.finbrain.vo.NavHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "数据导入管理")
@RestController
@RequestMapping("/admin/import")
@RequiredArgsConstructor
public class DataImportController {

    private final DataImportService dataImportService;

    @Operation(summary = "导入产品数据")
    @PostMapping("/products")
    @RequireAdmin
    public Result<ImportResultVO> importProducts(@RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong((String) authentication.getPrincipal());
        
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return Result.error("只支持Excel文件格式(.xlsx, .xls)");
        }
        
        return Result.success(dataImportService.importProducts(file, currentUserId));
    }

    @Operation(summary = "导入净值数据")
    @PostMapping("/nav")
    @RequireAdmin
    public Result<ImportResultVO> importNavData(@RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.parseLong((String) authentication.getPrincipal());
        
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            return Result.error("只支持Excel文件格式(.xlsx, .xls)");
        }
        
        return Result.success(dataImportService.importNavData(file, currentUserId));
    }

    @Operation(summary = "获取导入日志列表")
    @GetMapping("/logs")
    @RequireAdmin
    public Result<List<DataImportLog>> getImportLogs(
            @RequestParam(required = false) String importType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(dataImportService.getImportLogs(importType, pageNum, pageSize));
    }

    @Operation(summary = "获取导入日志详情")
    @GetMapping("/logs/{id}")
    @RequireAdmin
    public Result<DataImportLog> getImportLogById(@PathVariable Long id) {
        return Result.success(dataImportService.getImportLogById(id));
    }

    @Operation(summary = "获取产品净值历史")
    @GetMapping("/nav-history/{productId}")
    @RequireAdmin
    public Result<List<NavHistoryVO>> getNavHistory(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "30") Integer pageSize) {
        return Result.success(dataImportService.getNavHistory(productId, pageNum, pageSize));
    }
}
