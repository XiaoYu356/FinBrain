package com.finbrain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchDTO {

    private String productType;

    private BigDecimal minRate;

    private BigDecimal maxRate;

    private Integer minTerm;

    private Integer maxTerm;

    private Integer saleStatus;

    private String keyword;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
