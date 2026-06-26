package com.company.daizhang.module.inventory.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class InventoryOutUpdateRequest {
    private Long id;
    private Integer outType;
    private LocalDate outDate;
    private String customer;
    private String remark;
    private List<InventoryOutCreateRequest.OutDetailDTO> details;
}
