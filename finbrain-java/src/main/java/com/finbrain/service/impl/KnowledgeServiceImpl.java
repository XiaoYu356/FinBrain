package com.finbrain.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.finbrain.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    @Override
    public Map<String, Object> getDocuments(int page, int pageSize) {
        String url = aiServiceUrl + "/rag/documents?page=" + page + "&page_size=" + pageSize;
        
        try {
            HttpResponse response = HttpRequest.get(url)
                    .timeout(30000)
                    .execute();
            
            if (!response.isOk()) {
                log.error("获取文档列表失败: status={}", response.getStatus());
                return Map.of("total", 0, "documents", Collections.emptyList());
            }
            
            JSONObject result = JSONUtil.parseObj(response.body());
            if (result.getBool("success", false)) {
                return result.getJSONObject("data").toBean(Map.class);
            }
            
            return Map.of("total", 0, "documents", Collections.emptyList());
        } catch (Exception e) {
            log.error("获取文档列表异常: {}", e.getMessage(), e);
            return Map.of("total", 0, "documents", Collections.emptyList());
        }
    }

    @Override
    public void addDocument(String content, Map<String, Object> metadata) {
        String url = aiServiceUrl + "/rag/documents";
        
        JSONObject requestBody = new JSONObject();
        requestBody.set("content", content);
        requestBody.set("metadata", metadata != null ? metadata : Collections.emptyMap());
        
        try {
            HttpResponse response = HttpRequest.post(url)
                    .body(requestBody.toString())
                    .contentType("application/json")
                    .timeout(60000)
                    .execute();
            
            if (!response.isOk()) {
                log.error("添加文档失败: status={}", response.getStatus());
                throw new RuntimeException("添加文档失败");
            }
            
            log.info("添加文档成功: {}", content.substring(0, Math.min(50, content.length())));
        } catch (Exception e) {
            log.error("添加文档异常: {}", e.getMessage(), e);
            throw new RuntimeException("添加文档失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteDocument(Long docId) {
        String url = aiServiceUrl + "/rag/documents/" + docId;
        
        try {
            HttpResponse response = HttpRequest.delete(url)
                    .timeout(30000)
                    .execute();
            
            if (!response.isOk()) {
                log.error("删除文档失败: status={}", response.getStatus());
                throw new RuntimeException("删除文档失败");
            }
            
            log.info("删除文档成功: {}", docId);
        } catch (Exception e) {
            log.error("删除文档异常: {}", e.getMessage(), e);
            throw new RuntimeException("删除文档失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> searchDocuments(String query, int topK) {
        String url = aiServiceUrl + "/rag/search";
        
        JSONObject requestBody = new JSONObject();
        requestBody.set("query", query);
        requestBody.set("top_k", topK);
        
        try {
            HttpResponse response = HttpRequest.post(url)
                    .body(requestBody.toString())
                    .contentType("application/json")
                    .timeout(30000)
                    .execute();
            
            if (!response.isOk()) {
                log.error("检索文档失败: status={}", response.getStatus());
                return Collections.emptyList();
            }
            
            JSONObject result = JSONUtil.parseObj(response.body());
            if (result.getBool("success", false)) {
                JSONArray dataArray = result.getJSONArray("data");
                List<Map<String, Object>> documents = new ArrayList<>();
                for (int i = 0; i < dataArray.size(); i++) {
                    documents.add(dataArray.getJSONObject(i).toBean(Map.class));
                }
                return documents;
            }
            
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("检索文档异常: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
