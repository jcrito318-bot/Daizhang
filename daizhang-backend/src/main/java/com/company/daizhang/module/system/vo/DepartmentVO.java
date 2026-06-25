package com.company.daizhang.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门视图对象（含children列表，树形）
 */
@Data
public class DepartmentVO {

    private Long id;

    private Long parentId;

    private String deptCode;

    private String deptName;

    private Long managerId;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<DepartmentVO> children;
}
