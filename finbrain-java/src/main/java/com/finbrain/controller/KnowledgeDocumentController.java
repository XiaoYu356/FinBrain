package com.finbrain.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.annotation.RequireAdmin;
import com.finbrain.entity.KnowledgeDocument;
import com.finbrain.service.KnowledgeDocumentService;
import com.finbrain.service.KnowledgeService;
import com.finbrain.utils.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "知识库文档管理")
@RestController
@RequestMapping("/admin/knowledge")
@RequiredArgsConstructor
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;
    private final KnowledgeService knowledgeService;

    @Operation(summary = "上传文档")
    @PostMapping("/upload")
    @RequireAdmin
    public Result<KnowledgeDocument> uploadDocument(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        KnowledgeDocument document = documentService.uploadDocument(file, userId);
        return Result.success(document);
    }

    @Operation(summary = "批量上传文档")
    @PostMapping("/upload/batch")
    @RequireAdmin
    public Result<List<KnowledgeDocument>> uploadDocuments(@RequestParam("files") MultipartFile[] files) {
        Long userId = getCurrentUserId();
        List<KnowledgeDocument> documents = documentService.uploadDocuments(files, userId);
        return Result.success(documents);
    }

    @Operation(summary = "获取文档列表")
    @GetMapping("/documents")
    @RequireAdmin
    public Result<Page<KnowledgeDocument>> getDocuments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = getCurrentUserId();
        return Result.success(documentService.getDocumentList(pageNum, pageSize, userId));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/documents/{id}")
    @RequireAdmin
    public Result<Void> deleteDocument(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        documentService.deleteDocument(id, userId);
        return Result.success();
    }

    @Operation(summary = "批量删除文档")
    @DeleteMapping("/documents/batch")
    @RequireAdmin
    public Result<Void> deleteDocuments(@RequestBody List<Long> ids) {
        Long userId = getCurrentUserId();
        documentService.deleteDocuments(ids, userId);
        return Result.success();
    }

    @Operation(summary = "重新处理文档")
    @PostMapping("/documents/{id}/process")
    @RequireAdmin
    public Result<Void> processDocument(@PathVariable Long id) {
        documentService.processDocument(id);
        return Result.success();
    }

    @Operation(summary = "检索文档")
    @PostMapping("/search")
    @RequireAdmin
    public Result<List<Map<String, Object>>> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {
        return Result.success(knowledgeService.searchDocuments(query, topK));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong((String) authentication.getPrincipal());
    }
}
