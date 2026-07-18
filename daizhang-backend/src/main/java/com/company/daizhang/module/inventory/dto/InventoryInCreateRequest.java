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
public class InventoryInCreateRequest {
    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;
    @NotNull(message = "入库类型不能为空")
    private Integer inType;
    @NotNull(message = "入库日期不能为空")
    private LocalDate inDate;
    private String supplier;
    private String remark;
    @NotEmpty(message = "入库明细不能为空")
    @Valid
    private List<InDetailDTO> details;

    @Data
    public static class InDetailDTO {
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
