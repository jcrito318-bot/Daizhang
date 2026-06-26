package com.company.daizhang.module.inventory.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InventoryInUpdateRequest {
    private Long id;
    private Integer inType;
    private LocalDate inDate;
    private String supplier;
    private String remark;
    private List<InventoryInCreateRequest.InDetailDTO> details;
}
