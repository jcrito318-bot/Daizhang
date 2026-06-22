package com.company.daizhang.module.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 科目创建请求
 */
@Data
public class SubjectCreateRequest {
    
    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;
    
    @NotBlank(message = "科目编码不能为空")
    private String subjectCode;
    
    @NotBlank(message = "科目名称不能为空")
    private String subjectName;
    
    private String category;
    
    private Integer subjectType;
    
    private Integer balanceDirection;
    
    private Long parentId;
    
    private Integer auxiliaryAccounting;
    
    private Integer isCash;
    
    private Integer isBank;
    
    private Integer isCurrent;
}
