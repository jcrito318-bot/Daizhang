package com.company.daizhang.module.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.salary.dto.*;
import com.company.daizhang.module.salary.entity.Employee;
import com.company.daizhang.module.salary.entity.SalaryItem;
import com.company.daizhang.module.salary.entity.SalarySheet;
import com.company.daizhang.module.salary.vo.EmployeeVO;
import com.company.daizhang.module.salary.vo.SalaryItemVO;
import com.company.daizhang.module.salary.vo.SalarySheetVO;

/**
 * 薪资服务接口
 */
public interface SalaryService extends IService<SalarySheet> {

    // ==================== 员工管理 ====================

    /**
     * 分页查询员工
     */
    PageResult<EmployeeVO> pageEmployees(EmployeeQueryRequest request);

    /**
     * 根据ID查询员工
     */
    EmployeeVO getEmployeeById(Long id);

    /**
     * 创建员工
     */
    void createEmployee(EmployeeCreateRequest request);

    /**
     * 更新员工
     */
    void updateEmployee(Long id, EmployeeUpdateRequest request);

    /**
     * 删除员工
     */
    void deleteEmployee(Long id);

    // ==================== 薪资项目管理 ====================

    /**
     * 分页查询薪资项目
     */
    PageResult<SalaryItemVO> pageSalaryItems(SalaryItemQueryRequest request);

    /**
     * 根据ID查询薪资项目
     */
    SalaryItemVO getSalaryItemById(Long id);

    /**
     * 创建薪资项目
     */
    void createSalaryItem(SalaryItemCreateRequest request);

    /**
     * 更新薪资项目
     */
    void updateSalaryItem(Long id, SalaryItemUpdateRequest request);

    /**
     * 删除薪资项目
     */
    void deleteSalaryItem(Long id);

    // ==================== 薪资表管理 ====================

    /**
     * 分页查询薪资表
     */
    PageResult<SalarySheetVO> pageSalarySheets(SalarySheetQueryRequest request);

    /**
     * 根据ID查询薪资表
     */
    SalarySheetVO getSalarySheetById(Long id);

    /**
     * 创建薪资表
     */
    void createSalarySheet(SalarySheetCreateRequest request);

    /**
     * 更新薪资表
     */
    void updateSalarySheet(Long id, SalarySheetUpdateRequest request);

    /**
     * 删除薪资表
     */
    void deleteSalarySheet(Long id);

    /**
     * 确认薪资表
     */
    void confirmSalarySheet(Long id);

    /**
     * 发放薪资
     */
    void paySalarySheet(Long id);

    // ==================== 薪资计算 ====================

    /**
     * 批量计算薪资
     */
    void calculateSalary(SalaryCalculateRequest request);

    /**
     * 生成薪资凭证
     */
    void generateSalaryVoucher(SalaryVoucherGenerateRequest request);
}
