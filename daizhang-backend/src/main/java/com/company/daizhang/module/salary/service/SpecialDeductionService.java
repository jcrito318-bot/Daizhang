package com.company.daizhang.module.salary.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.salary.dto.SpecialDeductionQueryRequest;
import com.company.daizhang.module.salary.dto.SpecialDeductionRequest;
import com.company.daizhang.module.salary.vo.SpecialDeductionVO;

import java.math.BigDecimal;

/**
 * 个税专项附加扣除服务接口
 */
public interface SpecialDeductionService {

    /**
     * 分页查询专项附加扣除
     */
    PageResult<SpecialDeductionVO> pageDeductions(SpecialDeductionQueryRequest request);

    /**
     * 根据ID查询专项附加扣除
     */
    SpecialDeductionVO getDeductionById(Long id);

    /**
     * 创建专项附加扣除
     */
    void createDeduction(SpecialDeductionRequest request);

    /**
     * 更新专项附加扣除
     */
    void updateDeduction(Long id, SpecialDeductionRequest request);

    /**
     * 删除专项附加扣除
     */
    void deleteDeduction(Long id);

    /**
     * 计算员工某月专项附加扣除总额
     *
     * @param employeeId 员工ID
     * @param year       年度
     * @param month      月份
     * @return 该员工当月专项附加扣除合计金额
     */
    BigDecimal calculateMonthlyDeduction(Long employeeId, Integer year, Integer month);
}
