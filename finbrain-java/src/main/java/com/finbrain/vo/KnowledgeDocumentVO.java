package com.finbrain.vo;

import lombok.Data;

@Data
public class KnowledgeDocumentVO {
    private Long id;
    private String content;
    private String contentPreview;
    private String metadata;
}
