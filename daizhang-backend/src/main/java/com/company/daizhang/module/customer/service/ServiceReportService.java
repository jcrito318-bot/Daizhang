package com.company.daizhang.module.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.customer.dto.ServiceReportRequest;
import com.company.daizhang.module.customer.entity.ServiceReport;
import com.company.daizhang.module.customer.vo.ServiceReportVO;

/**
 * 客户服务报告服务接口
 */
public interface ServiceReportService extends IService<ServiceReport> {

    /**
     * 分页查询报告
     */
    PageResult<ServiceReportVO> pageReports(Long accountSetId, Long customerId, Integer reportYear,
                                            Integer reportMonth, int pageNum, int pageSize);

    /**
     * 根据ID查询报告
     */
    ServiceReportVO getReportById(Long id);

    /**
     * 创建报告
     */
    void createReport(ServiceReportRequest request);

    /**
     * 更新报告
     */
    void updateReport(Long id, ServiceReportRequest request);

    /**
     * 删除报告
     */
    void deleteReport(Long id);

    /**
     * 发布报告
     */
    void publishReport(Long id);

    /**
     * 自动生成报告（查询财务数据填充）
     */
    ServiceReportVO generateReport(Long accountSetId, Long customerId, Integer year, Integer month);
}
