package com.company.daizhang.module.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InventoryInCreateRequest {
    private Long accountSetId;
    private Integer inType;
    private LocalDate inDate;
    private String supplier;
    private String remark;
    private List<InDetailDTO> details;

    @Data
    public static class InDetailDTO {
        private Long itemId;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private String remark;
    }
}
