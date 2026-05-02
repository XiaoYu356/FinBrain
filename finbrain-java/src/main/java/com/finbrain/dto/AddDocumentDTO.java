package com.finbrain.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AddDocumentDTO {
    private String content;
    private Map<String, Object> metadata;
}
