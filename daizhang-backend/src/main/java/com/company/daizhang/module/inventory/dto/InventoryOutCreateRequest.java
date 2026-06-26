package com.company.daizhang.module.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InventoryOutCreateRequest {
    private Long accountSetId;
    private Integer outType;
    private LocalDate outDate;
    private String customer;
    private String remark;
    private List<OutDetailDTO> details;

    @Data
    public static class OutDetailDTO {
        private Long itemId;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private String remark;
    }
}
