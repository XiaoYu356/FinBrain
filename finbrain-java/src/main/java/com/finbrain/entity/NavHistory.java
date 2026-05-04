package com.finbrain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("nav_history")
public class NavHistory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String productCode;

    private String productName;

    private BigDecimal nav;

    private BigDecimal accumulatedNav;

    private LocalDate navDate;

    private BigDecimal dailyReturnRate;

    private String dataSource;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
