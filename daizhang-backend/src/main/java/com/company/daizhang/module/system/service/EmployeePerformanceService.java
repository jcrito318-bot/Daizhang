package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.entity.EmployeePerformance;
import com.company.daizhang.module.system.vo.EmployeePerformanceVO;

/**
 * 员工绩效服务接口
 */
public interface EmployeePerformanceService extends IService<EmployeePerformance> {

    /**
     * 分页查询绩效
     */
    PageResult<EmployeePerformanceVO> pagePerformances(Long userId, Integer year, Integer month, int pageNum, int pageSize);

    /**
     * 查询某员工某月绩效
     */
    EmployeePerformanceVO getPerformance(Long userId, Integer year, Integer month);

    /**
     * 自动生成绩效（统计各员工的凭证数、审核数等）
     */
    void generatePerformance(Integer year, Integer month);
}
