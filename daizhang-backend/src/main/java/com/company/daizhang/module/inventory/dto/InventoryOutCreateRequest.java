package com.company.daizhang.module.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InventoryOutCreateRequest {
    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;
    @NotNull(message = "出库类型不能为空")
    private Integer outType;
    @NotNull(message = "出库日期不能为空")
    private LocalDate outDate;
    private String customer;
    private String remark;
    @NotEmpty(message = "出库明细不能为空")
    @Valid
    private List<OutDetailDTO> details;

    @Data
    public static class OutDetailDTO {
        @NotNull(message = "商品ID不能为空")
        private Long itemId;
        @NotNull(message = "数量不能为空")
        @DecimalMin(value = "0", message = "数量不能为负数")
        @Digits(integer = 15, fraction = 4, message = "数量精度超出范围")
        private BigDecimal quantity;
        @NotNull(message = "单价不能为空")
        @DecimalMin(value = "0", message = "单价不能为负数")
        @Digits(integer = 15, fraction = 4, message = "单价精度超出范围")
        private BigDecimal unitPrice;
        private String remark;
    }
}
