package com.company.daizhang.module.document.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.document.dto.InputInvoiceRequest;
import com.company.daizhang.module.document.dto.InvoiceQueryRequest;
import com.company.daizhang.module.document.dto.OutputInvoiceRequest;
import com.company.daizhang.module.document.vo.InputInvoiceVO;
import com.company.daizhang.module.document.vo.InvoiceStatisticsVO;
import com.company.daizhang.module.document.vo.OutputInvoiceVO;

/**
 * 发票服务接口
 */
public interface InvoiceService {

    /**
     * 分页查询进项发票
     */
    PageResult<InputInvoiceVO> pageInputInvoices(InvoiceQueryRequest request);

    /**
     * 根据ID查询进项发票
     */
    InputInvoiceVO getInputInvoiceById(Long id);

    /**
     * 创建进项发票
     */
    void createInputInvoice(InputInvoiceRequest request);

    /**
     * 更新进项发票
     */
    void updateInputInvoice(Long id, InputInvoiceRequest request);

    /**
     * 删除进项发票
     */
    void deleteInputInvoice(Long id);

    /**
     * 认证进项发票
     */
    void authenticateInputInvoice(Long id);

    /**
     * 分页查询销项发票
     */
    PageResult<OutputInvoiceVO> pageOutputInvoices(InvoiceQueryRequest request);

    /**
     * 根据ID查询销项发票
     */
    OutputInvoiceVO getOutputInvoiceById(Long id);

    /**
     * 创建销项发票
     */
    void createOutputInvoice(OutputInvoiceRequest request);

    /**
     * 更新销项发票
     */
    void updateOutputInvoice(Long id, OutputInvoiceRequest request);

    /**
     * 删除销项发票
     */
    void deleteOutputInvoice(Long id);

    /**
     * 作废销项发票
     */
    void voidOutputInvoice(Long id);

    /**
     * 发票统计
     */
    InvoiceStatisticsVO getInvoiceStatistics(Long accountSetId, Integer year, Integer month);
}
