package com.finbrain.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finbrain.config.MinioConfig;
import com.finbrain.entity.KnowledgeDocument;
import com.finbrain.mapper.KnowledgeDocumentMapper;
import com.finbrain.service.KnowledgeDocumentService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final KnowledgeDocumentMapper documentMapper;

    @Value("${ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    @Override
    public KnowledgeDocument uploadDocument(MultipartFile file, Long userId) {
        try {
            String originalName = file.getOriginalFilename();
            String fileType = getFileExtension(originalName);
            String fileName = UUID.randomUUID().toString() + "." + fileType;
            String objectName = "documents/" + fileName;

            ensureBucketExists();

            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            KnowledgeDocument document = new KnowledgeDocument();
            document.setFileName(fileName);
            document.setOriginalName(originalName);
            document.setFileType(fileType);
            document.setFileSize(file.getSize());
            document.setObjectName(objectName);
            document.setBucketName(minioConfig.getBucketName());
            document.setStatus("pending");
            document.setCreatedBy(userId);
            document.setCreateTime(LocalDateTime.now());
            
            documentMapper.insert(document);
            
            log.info("文档上传成功: {}", originalName);
            
            processDocumentAsync(document.getId());
            
            return document;
        } catch (Exception e) {
            log.error("文档上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档上传失败: " + e.getMessage());
        }
    }

    @Override
    public void processDocument(Long documentId) {
        KnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            return;
        }

        document.setStatus("processing");
        documentMapper.updateById(document);

        try {
            String url = aiServiceUrl + "/rag/process-document";
            JSONObject requestBody = new JSONObject();
            requestBody.set("document_id", documentId);
            requestBody.set("object_name", document.getObjectName());
            requestBody.set("bucket_name", document.getBucketName());
            requestBody.set("file_type", document.getFileType());

            HttpResponse response = HttpRequest.post(url)
                    .body(requestBody.toString())
                    .contentType("application/json")
                    .timeout(300000)
                    .execute();

            if (response.isOk()) {
                JSONObject result = JSONUtil.parseObj(response.body());
                if (result.getBool("success", false)) {
                    document.setStatus("completed");
                    document.setChunkCount(result.getInt("chunk_count", 0));
                    document.setProcessTime(LocalDateTime.now());
                    log.info("文档处理完成: {}", document.getOriginalName());
                } else {
                    document.setStatus("failed");
                    document.setErrorMessage(result.getStr("message", "处理失败"));
                }
            } else {
                document.setStatus("failed");
                document.setErrorMessage("AI服务调用失败");
            }
        } catch (Exception e) {
            document.setStatus("failed");
            document.setErrorMessage(e.getMessage());
            log.error("文档处理失败: {}", e.getMessage(), e);
        }

        documentMapper.updateById(document);
    }

    private void processDocumentAsync(Long documentId) {
        new Thread(() -> processDocument(documentId)).start();
    }

    @Override
    public void deleteDocument(Long id, Long userId) {
        KnowledgeDocument document = documentMapper.selectById(id);
        if (document == null) {
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(document.getBucketName())
                            .object(document.getObjectName())
                            .build()
            );
            
            String url = aiServiceUrl + "/rag/delete-document";
            JSONObject requestBody = new JSONObject();
            requestBody.set("document_id", id);

            try {
                HttpRequest.post(url)
                        .body(requestBody.toString())
                        .contentType("application/json")
                        .timeout(30000)
                        .execute();
            } catch (Exception e) {
                log.warn("删除向量文档失败: {}", e.getMessage());
            }

            documentMapper.deleteById(id);
            log.info("文档删除成功: {}", document.getOriginalName());
        } catch (Exception e) {
            log.error("文档删除失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档删除失败: " + e.getMessage());
        }
    }

    @Override
    public Page<KnowledgeDocument> getDocumentList(int pageNum, int pageSize, Long userId) {
        return documentMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<KnowledgeDocument>()
                        .eq(KnowledgeDocument::getCreatedBy, userId)
                        .orderByDesc(KnowledgeDocument::getCreateTime)
        );
    }

    @Override
    public KnowledgeDocument getDocumentById(Long id) {
        return documentMapper.selectById(id);
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "txt";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
