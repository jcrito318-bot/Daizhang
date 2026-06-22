package com.company.daizhang.module.customer.dto;

import lombok.Data;

/**
 * 服务合同查询请求
 */
@Data
public class ContractQueryRequest {

    private String contractNo;

    private String contractName;

    private Long customerId;

    private String contractType;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
