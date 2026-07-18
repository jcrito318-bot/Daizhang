package com.company.daizhang.module.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InventoryInUpdateRequest {
    @NotNull(message = "ID不能为空")
    private Long id;
    @NotNull(message = "入库类型不能为空")
    private Integer inType;
    @NotNull(message = "入库日期不能为空")
    private LocalDate inDate;
    private String supplier;
    private String remark;
    @NotEmpty(message = "入库明细不能为空")
    @Valid
    private List<InventoryInCreateRequest.InDetailDTO> details;
}
