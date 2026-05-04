package com.finbrain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ImportResultVO {

    private String importNo;

    private String importType;

    private String fileName;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private String status;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long duration;

    private List<ImportErrorVO> errors;

    @Data
    public static class ImportErrorVO {
        private int rowNum;
        private String rowData;
        private String errorMessage;
    }
}
