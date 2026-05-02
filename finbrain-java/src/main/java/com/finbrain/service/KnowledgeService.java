package com.finbrain.service;

import com.finbrain.vo.KnowledgeDocumentVO;
import java.util.List;
import java.util.Map;

public interface KnowledgeService {
    
    Map<String, Object> getDocuments(int page, int pageSize);
    
    void addDocument(String content, Map<String, Object> metadata);
    
    void deleteDocument(Long docId);
    
    List<Map<String, Object>> searchDocuments(String query, int topK);
}
