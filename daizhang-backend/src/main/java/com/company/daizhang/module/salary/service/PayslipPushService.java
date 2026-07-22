package com.company.daizhang.module.salary.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.salary.vo.PayslipPushRecordVO;
import com.company.daizhang.module.salary.vo.PayslipPushResultVO;

/**
 * 工资条推送服务接口
 */
public interface PayslipPushService {

    /**
     * 批量生成工资条PDF并记录推送
     * <p>根据传入的薪资表ID定位薪资期间(账套+年+月),为该期间所有员工的薪资记录
     * 生成工资条PDF并记录推送状态。</p>
     *
     * @param salarySheetId 薪资表ID(用于定位薪资期间)
     * @return 推送结果汇总
     */
    PayslipPushResultVO batchPushPayslip(Long salarySheetId);

    /**
     * 查询推送记录
     * <p>根据传入的薪资表ID定位薪资期间,返回该期间所有员工的推送记录。</p>
     *
     * @param salarySheetId 薪资表ID(用于定位薪资期间)
     * @param page          页码
     * @param size          每页条数
     * @return 推送记录分页结果
     */
    PageResult<PayslipPushRecordVO> pagePushRecords(Long salarySheetId, int page, int size);
}
