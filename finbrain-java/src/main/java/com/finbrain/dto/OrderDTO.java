package com.finbrain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderDTO {

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @NotNull(message = "申购金额不能为空")
    @DecimalMin(value = "1.00", message = "申购金额必须大于0")
    private BigDecimal amount;
}
