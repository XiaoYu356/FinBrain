package com.finbrain.service;

import com.finbrain.entity.KnowledgeDocument;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeDocumentService {
    
    KnowledgeDocument uploadDocument(MultipartFile file, Long userId);
    
    void processDocument(Long documentId);
    
    void deleteDocument(Long id, Long userId);
    
    Page<KnowledgeDocument> getDocumentList(int pageNum, int pageSize, Long userId);
    
    KnowledgeDocument getDocumentById(Long id);
}
