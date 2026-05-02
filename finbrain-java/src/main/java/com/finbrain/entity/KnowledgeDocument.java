package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_document")
public class KnowledgeDocument implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String originalName;

    private String fileType;

    private Long fileSize;

    private String objectName;

    private String bucketName;

    private String status;

    private Integer chunkCount;

    private String errorMessage;

    private Long createdBy;

    private LocalDateTime createTime;

    private LocalDateTime processTime;
}
