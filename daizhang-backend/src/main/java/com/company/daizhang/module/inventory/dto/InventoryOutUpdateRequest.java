package com.company.daizhang.module.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InventoryOutUpdateRequest {
    @NotNull(message = "ID不能为空")
    private Long id;
    @NotNull(message = "出库类型不能为空")
    private Integer outType;
    @NotNull(message = "出库日期不能为空")
    private LocalDate outDate;
    private String customer;
    private String remark;
    @NotEmpty(message = "出库明细不能为空")
    @Valid
    private List<InventoryOutCreateRequest.OutDetailDTO> details;
}
