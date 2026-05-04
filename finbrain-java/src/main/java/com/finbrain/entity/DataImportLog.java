package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("data_import_log")
public class DataImportLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String importNo;

    private String importType;

    private String fileName;

    private String originalName;

    private Long fileSize;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private String status;

    private String errorMessage;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
