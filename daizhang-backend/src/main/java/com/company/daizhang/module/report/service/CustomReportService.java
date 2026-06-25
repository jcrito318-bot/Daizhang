package com.company.daizhang.module.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.report.dto.CustomReportRequest;
import com.company.daizhang.module.report.entity.CustomReport;
import com.company.daizhang.module.report.vo.CustomReportDataVO;
import com.company.daizhang.module.report.vo.CustomReportVO;

/**
 * 自定义报表服务接口
 */
public interface CustomReportService extends IService<CustomReport> {

    /**
     * 分页查询自定义报表
     */
    PageResult<CustomReportVO> pageReports(String reportName, int pageNum, int pageSize);

    /**
     * 根据ID查询自定义报表(含明细)
     */
    CustomReportVO getReportById(Long id);

    /**
     * 创建自定义报表
     */
    void createReport(CustomReportRequest request);

    /**
     * 更新自定义报表
     */
    void updateReport(Long id, CustomReportRequest request);

    /**
     * 删除自定义报表
     */
    void deleteReport(Long id);

    /**
     * 执行报表取数
     */
    CustomReportDataVO executeReport(Long id, Long accountSetId, Integer year, Integer month);
}
