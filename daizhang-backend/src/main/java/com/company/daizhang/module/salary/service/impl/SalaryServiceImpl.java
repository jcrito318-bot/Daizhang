package com.company.daizhang.module.salary.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.salary.dto.*;
import com.company.daizhang.module.salary.entity.Employee;
import com.company.daizhang.module.salary.entity.SalaryFormula;
import com.company.daizhang.module.salary.entity.SalaryItem;
import com.company.daizhang.module.salary.entity.SalarySheet;
import com.company.daizhang.module.salary.mapper.EmployeeMapper;
import com.company.daizhang.module.salary.mapper.SalaryItemMapper;
import com.company.daizhang.module.salary.mapper.SalarySheetMapper;
import com.company.daizhang.module.salary.service.SalaryFormulaService;
import com.company.daizhang.module.salary.service.SalaryService;
import com.company.daizhang.module.salary.service.SocialSecurityConfigService;
import com.company.daizhang.module.salary.service.SpecialDeductionService;
import com.company.daizhang.module.salary.util.SalaryExportUtil;
import com.company.daizhang.module.salary.vo.EmployeeVO;
import com.company.daizhang.module.salary.vo.SalaryItemVO;
import com.company.daizhang.module.salary.vo.SalarySheetVO;
import com.company.daizhang.module.salary.vo.SocialSecurityCalculationVO;
import com.company.daizhang.module.salary.vo.SocialSecurityConfigVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherDetailRequest;
import com.company.daizhang.module.voucher.entity.VoucherWord;
import com.company.daizhang.module.voucher.mapper.VoucherWordMapper;
import com.company.daizhang.module.voucher.service.VoucherService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 薪资服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryServiceImpl extends ServiceImpl<SalarySheetMapper, SalarySheet> implements SalaryService {

    private final EmployeeMapper employeeMapper;
    private final SalaryItemMapper salaryItemMapper;
    private final SalarySheetMapper salarySheetMapper;
    private final AccountSetAccessService accountSetAccessService;
    private final SocialSecurityConfigService socialSecurityConfigService;
    private final SpecialDeductionService specialDeductionService;
    private final SubjectMapper subjectMapper;
    private final SysUserMapper sysUserMapper;
    private final VoucherService voucherService;
    private final VoucherWordMapper voucherWordMapper;
    private final SalaryExportUtil salaryExportUtil;
    private final SalaryFormulaService salaryFormulaService;

    // ==================== 员工管理 ====================

    @Override
    public PageResult<EmployeeVO> pageEmployees(EmployeeQueryRequest request) {
        Page<Employee> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getAccountSetId, request.getAccountSetId())
               .like(request.getEmployeeCode() != null, Employee::getEmployeeCode, request.getEmployeeCode())
               .like(request.getEmployeeName() != null, Employee::getEmployeeName, request.getEmployeeName())
               .like(request.getDepartment() != null, Employee::getDepartment, request.getDepartment())
               .eq(request.getStatus() != null, Employee::getStatus, request.getStatus())
               .orderByDesc(Employee::getCreateTime);

        Page<Employee> result = employeeMapper.selectPage(page, wrapper);

        List<EmployeeVO> voList = result.getRecords().stream()
                .map(this::convertEmployeeToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public EmployeeVO getEmployeeById(Long id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "员工不存在");
        }
        // IDOR治理:校验当前用户对该员工所属账套的访问权
        accountSetAccessService.checkAccess(employee.getAccountSetId());
        return convertEmployeeToVO(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEmployee(EmployeeCreateRequest request) {
        // 检查员工编号是否已存在
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getAccountSetId, request.getAccountSetId())
               .eq(Employee::getEmployeeCode, request.getEmployeeCode());
        Long count = employeeMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "员工编号已存在");
        }

        Employee employee = new Employee();
        BeanUtil.copyProperties(request, employee);
        if (employee.getStatus() == null) {
            employee.setStatus(1); // 默认在职
        }
        employeeMapper.insert(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(Long id, EmployeeUpdateRequest request) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "员工不存在");
        }
        // IDOR治理:校验当前用户对该员工所属账套的所有者权限
        accountSetAccessService.checkOwner(employee.getAccountSetId());

        // 如果修改了员工编号，检查是否与其他员工重复
        if (request.getEmployeeCode() != null && !request.getEmployeeCode().equals(employee.getEmployeeCode())) {
            LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Employee::getAccountSetId, employee.getAccountSetId())
                   .eq(Employee::getEmployeeCode, request.getEmployeeCode())
                   .ne(Employee::getId, id);
            Long count = employeeMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "员工编号已存在");
            }
        }

        BeanUtil.copyProperties(request, employee);
        employeeMapper.updateById(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmployee(Long id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "员工不存在");
        }
        // IDOR治理:校验当前用户对该员工所属账套的所有者权限
        accountSetAccessService.checkOwner(employee.getAccountSetId());

        // 检查是否有薪资记录
        LambdaQueryWrapper<SalarySheet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalarySheet::getEmployeeId, id);
        Long salaryCount = salarySheetMapper.selectCount(wrapper);
        if (salaryCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该员工已有薪资记录，无法删除");
        }

        employeeMapper.deleteById(id);
    }

    // ==================== 薪资项目管理 ====================

    @Override
    public PageResult<SalaryItemVO> pageSalaryItems(SalaryItemQueryRequest request) {
        Page<SalaryItem> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SalaryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalaryItem::getAccountSetId, request.getAccountSetId())
               .like(request.getItemName() != null, SalaryItem::getItemName, request.getItemName())
               .like(request.getItemCode() != null, SalaryItem::getItemCode, request.getItemCode())
               .eq(request.getItemType() != null, SalaryItem::getItemType, request.getItemType())
               .orderByDesc(SalaryItem::getCreateTime);

        Page<SalaryItem> result = salaryItemMapper.selectPage(page, wrapper);

        List<SalaryItemVO> voList = result.getRecords().stream()
                .map(this::convertSalaryItemToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public SalaryItemVO getSalaryItemById(Long id) {
        SalaryItem salaryItem = salaryItemMapper.selectById(id);
        if (salaryItem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资项目不存在");
        }
        // IDOR治理:校验当前用户对该薪资项目所属账套的访问权
        accountSetAccessService.checkAccess(salaryItem.getAccountSetId());
        return convertSalaryItemToVO(salaryItem);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSalaryItem(SalaryItemCreateRequest request) {
        // 检查项目编码是否已存在
        LambdaQueryWrapper<SalaryItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalaryItem::getAccountSetId, request.getAccountSetId())
               .eq(SalaryItem::getItemCode, request.getItemCode());
        Long count = salaryItemMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "薪资项目编码已存在");
        }

        SalaryItem salaryItem = new SalaryItem();
        BeanUtil.copyProperties(request, salaryItem);
        salaryItemMapper.insert(salaryItem);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalaryItem(Long id, SalaryItemUpdateRequest request) {
        SalaryItem salaryItem = salaryItemMapper.selectById(id);
        if (salaryItem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资项目不存在");
        }
        // IDOR治理:校验当前用户对该薪资项目所属账套的所有者权限
        accountSetAccessService.checkOwner(salaryItem.getAccountSetId());

        // 如果修改了项目编码，检查是否与其他项目重复
        if (request.getItemCode() != null && !request.getItemCode().equals(salaryItem.getItemCode())) {
            LambdaQueryWrapper<SalaryItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SalaryItem::getAccountSetId, salaryItem.getAccountSetId())
                   .eq(SalaryItem::getItemCode, request.getItemCode())
                   .ne(SalaryItem::getId, id);
            Long count = salaryItemMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "薪资项目编码已存在");
            }
        }

        BeanUtil.copyProperties(request, salaryItem);
        salaryItemMapper.updateById(salaryItem);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSalaryItem(Long id) {
        SalaryItem salaryItem = salaryItemMapper.selectById(id);
        if (salaryItem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资项目不存在");
        }
        // IDOR治理:校验当前用户对该薪资项目所属账套的所有者权限
        accountSetAccessService.checkOwner(salaryItem.getAccountSetId());
        salaryItemMapper.deleteById(id);
    }

    // ==================== 薪资表管理 ====================

    @Override
    public PageResult<SalarySheetVO> pageSalarySheets(SalarySheetQueryRequest request) {
        Page<SalarySheet> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<SalarySheet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalarySheet::getAccountSetId, request.getAccountSetId())
               .eq(request.getYear() != null, SalarySheet::getYear, request.getYear())
               .eq(request.getMonth() != null, SalarySheet::getMonth, request.getMonth())
               .eq(request.getEmployeeId() != null, SalarySheet::getEmployeeId, request.getEmployeeId())
               .like(request.getEmployeeName() != null, SalarySheet::getEmployeeName, request.getEmployeeName())
               .eq(request.getStatus() != null, SalarySheet::getStatus, request.getStatus())
               .orderByDesc(SalarySheet::getCreateTime);

        Page<SalarySheet> result = salarySheetMapper.selectPage(page, wrapper);

        List<SalarySheetVO> voList = result.getRecords().stream()
                .map(this::convertSalarySheetToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public SalarySheetVO getSalarySheetById(Long id) {
        SalarySheet salarySheet = salarySheetMapper.selectById(id);
        if (salarySheet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资表不存在");
        }
        // IDOR治理:校验当前用户对该薪资表所属账套的访问权
        accountSetAccessService.checkAccess(salarySheet.getAccountSetId());
        return convertSalarySheetToVO(salarySheet);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createSalarySheet(SalarySheetCreateRequest request) {
        // 检查员工是否存在
        Employee employee = employeeMapper.selectById(request.getEmployeeId());
        if (employee == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "员工不存在");
        }

        // 检查该员工该月份是否已有薪资记录
        LambdaQueryWrapper<SalarySheet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalarySheet::getAccountSetId, request.getAccountSetId())
               .eq(SalarySheet::getYear, request.getYear())
               .eq(SalarySheet::getMonth, request.getMonth())
               .eq(SalarySheet::getEmployeeId, request.getEmployeeId());
        Long count = salarySheetMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该员工该月份已有薪资记录");
        }

        SalarySheet salarySheet = new SalarySheet();
        BeanUtil.copyProperties(request, salarySheet);
        salarySheet.setEmployeeName(employee.getEmployeeName());
        salarySheet.setStatus(0); // 草稿状态

        // 计算应纳税所得额和个税
        calculateTaxableIncomeAndTax(salarySheet);

        salarySheetMapper.insert(salarySheet);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSalarySheet(Long id, SalarySheetUpdateRequest request) {
        SalarySheet salarySheet = salarySheetMapper.selectById(id);
        if (salarySheet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资表不存在");
        }
        // IDOR治理:校验当前用户对该薪资表所属账套的所有者权限
        accountSetAccessService.checkOwner(salarySheet.getAccountSetId());

        // 已确认或已发放的不能修改
        if (salarySheet.getStatus() != null && salarySheet.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "已确认或已发放的薪资不能修改");
        }

        BeanUtil.copyProperties(request, salarySheet);

        // 重新计算应纳税所得额和个税
        calculateTaxableIncomeAndTax(salarySheet);

        salarySheetMapper.updateById(salarySheet);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSalarySheet(Long id) {
        SalarySheet salarySheet = salarySheetMapper.selectById(id);
        if (salarySheet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资表不存在");
        }
        // IDOR治理:校验当前用户对该薪资表所属账套的所有者权限
        accountSetAccessService.checkOwner(salarySheet.getAccountSetId());

        // 只有草稿状态才能删除
        if (salarySheet.getStatus() != null && salarySheet.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "只能删除草稿状态的薪资记录");
        }

        salarySheetMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSalarySheet(Long id) {
        SalarySheet salarySheet = salarySheetMapper.selectById(id);
        if (salarySheet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资表不存在");
        }
        // IDOR治理:校验当前用户对该薪资表所属账套的所有者权限(确认)
        accountSetAccessService.checkOwner(salarySheet.getAccountSetId());

        if (salarySheet.getStatus() == null || salarySheet.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "只能确认草稿状态的薪资记录");
        }

        salarySheet.setStatus(1); // 已确认
        salarySheetMapper.updateById(salarySheet);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySalarySheet(Long id) {
        SalarySheet salarySheet = salarySheetMapper.selectById(id);
        if (salarySheet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资表不存在");
        }
        // IDOR治理:校验当前用户对该薪资表所属账套的所有者权限(发放)
        accountSetAccessService.checkOwner(salarySheet.getAccountSetId());

        if (salarySheet.getStatus() == null || salarySheet.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "只能发放已确认状态的薪资记录");
        }

        salarySheet.setStatus(2); // 已发放
        salarySheetMapper.updateById(salarySheet);
    }

    // ==================== 薪资计算 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateSalary(SalaryCalculateRequest request) {
        // 查询该账套该月份的所有在职员工
        LambdaQueryWrapper<Employee> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(Employee::getAccountSetId, request.getAccountSetId())
                  .eq(Employee::getStatus, 1); // 在职
        List<Employee> employees = employeeMapper.selectList(empWrapper);

        if (employees.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "没有在职员工");
        }

        // 查询该账套的所有薪资项目
        LambdaQueryWrapper<SalaryItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(SalaryItem::getAccountSetId, request.getAccountSetId());
        List<SalaryItem> salaryItems = salaryItemMapper.selectList(itemWrapper);

        // 为每个员工生成薪资记录
        for (Employee employee : employees) {
            // 检查是否已存在该月份的薪资记录
            LambdaQueryWrapper<SalarySheet> sheetWrapper = new LambdaQueryWrapper<>();
            sheetWrapper.eq(SalarySheet::getAccountSetId, request.getAccountSetId())
                        .eq(SalarySheet::getYear, request.getYear())
                        .eq(SalarySheet::getMonth, request.getMonth())
                        .eq(SalarySheet::getEmployeeId, employee.getId());
            Long count = salarySheetMapper.selectCount(sheetWrapper);

            if (count > 0) {
                continue; // 已存在则跳过
            }

            SalarySheet salarySheet = new SalarySheet();
            salarySheet.setAccountSetId(request.getAccountSetId());
            salarySheet.setYear(request.getYear());
            salarySheet.setMonth(request.getMonth());
            salarySheet.setEmployeeId(employee.getId());
            salarySheet.setEmployeeName(employee.getEmployeeName());
            salarySheet.setStatus(0); // 草稿

            // 根据薪资项目计算各项金额
            calculateSalaryItems(salarySheet, salaryItems);

            // 计算应纳税所得额和个税
            calculateTaxableIncomeAndTax(salarySheet, request.getThreshold());

            salarySheetMapper.insert(salarySheet);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateSalaryVoucher(SalaryVoucherGenerateRequest request) {
        // 查询该月份的已确认薪资记录
        LambdaQueryWrapper<SalarySheet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalarySheet::getAccountSetId, request.getAccountSetId())
               .eq(SalarySheet::getYear, request.getYear())
               .eq(SalarySheet::getMonth, request.getMonth())
               .eq(SalarySheet::getStatus, 1); // 已确认
        List<SalarySheet> salarySheets = salarySheetMapper.selectList(wrapper);

        if (salarySheets.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "没有已确认的薪资记录");
        }

        // 汇总各项金额
        BigDecimal totalBaseSalary = BigDecimal.ZERO;
        BigDecimal totalAllowance = BigDecimal.ZERO;
        BigDecimal totalBonus = BigDecimal.ZERO;
        BigDecimal totalDeduction = BigDecimal.ZERO;
        BigDecimal totalSocialSecurity = BigDecimal.ZERO;
        BigDecimal totalHousingFund = BigDecimal.ZERO;
        BigDecimal totalIncomeTax = BigDecimal.ZERO;
        BigDecimal totalNetSalary = BigDecimal.ZERO;

        for (SalarySheet sheet : salarySheets) {
            totalBaseSalary = totalBaseSalary.add(sheet.getBaseSalary() != null ? sheet.getBaseSalary() : BigDecimal.ZERO);
            totalAllowance = totalAllowance.add(sheet.getAllowance() != null ? sheet.getAllowance() : BigDecimal.ZERO);
            totalBonus = totalBonus.add(sheet.getBonus() != null ? sheet.getBonus() : BigDecimal.ZERO);
            totalDeduction = totalDeduction.add(sheet.getDeduction() != null ? sheet.getDeduction() : BigDecimal.ZERO);
            totalSocialSecurity = totalSocialSecurity.add(sheet.getSocialSecurity() != null ? sheet.getSocialSecurity() : BigDecimal.ZERO);
            totalHousingFund = totalHousingFund.add(sheet.getHousingFund() != null ? sheet.getHousingFund() : BigDecimal.ZERO);
            totalIncomeTax = totalIncomeTax.add(sheet.getIncomeTax() != null ? sheet.getIncomeTax() : BigDecimal.ZERO);
            totalNetSalary = totalNetSalary.add(sheet.getNetSalary() != null ? sheet.getNetSalary() : BigDecimal.ZERO);
        }

        // 获取科目信息
        Subject payableSubject = subjectMapper.selectById(request.getPayableSubjectId());
        Subject bankSubject = subjectMapper.selectById(request.getBankSubjectId());

        if (payableSubject == null || bankSubject == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "科目不存在");
        }

        // 创建凭证
        VoucherCreateRequest voucherRequest = new VoucherCreateRequest();
        voucherRequest.setAccountSetId(request.getAccountSetId());
        voucherRequest.setVoucherDate(LocalDate.of(request.getYear(), request.getMonth(), 1));
        voucherRequest.setYear(request.getYear());
        voucherRequest.setMonth(request.getMonth());
        voucherRequest.setAttachmentCount(0);

        // 获取凭证字
        LambdaQueryWrapper<VoucherWord> wordWrapper = new LambdaQueryWrapper<>();
        wordWrapper.eq(VoucherWord::getAccountSetId, request.getAccountSetId())
                   .last("LIMIT 1");
        VoucherWord voucherWord = voucherWordMapper.selectOne(wordWrapper);
        if (voucherWord != null) {
            voucherRequest.setVoucherWordId(voucherWord.getId());
        }

        List<VoucherDetailRequest> details = new ArrayList<>();
        int lineNo = 1;

        BigDecimal totalGrossSalary = totalBaseSalary.add(totalAllowance).add(totalBonus).subtract(totalDeduction);

        // 费用科目(计提工资借方):优先用传入的expenseSubjectId,否则查找5602管理费用
        Subject expenseSubject = null;
        if (request.getExpenseSubjectId() != null) {
            expenseSubject = subjectMapper.selectById(request.getExpenseSubjectId());
        }
        if (expenseSubject == null) {
            // 查找编码5602(管理费用)作为默认费用科目
            LambdaQueryWrapper<Subject> expenseWrapper = new LambdaQueryWrapper<>();
            expenseWrapper.eq(Subject::getAccountSetId, request.getAccountSetId())
                          .eq(Subject::getCode, "5602")
                          .last("LIMIT 1");
            expenseSubject = subjectMapper.selectOne(expenseWrapper);
        }

        // === 计提分录 ===
        // 借：管理费用/费用科目 (应发工资总额)
        if (expenseSubject != null) {
            VoucherDetailRequest debitExpense = new VoucherDetailRequest();
            debitExpense.setLineNo(lineNo++);
            debitExpense.setSummary("计提" + request.getYear() + "年" + request.getMonth() + "月工资");
            debitExpense.setSubjectId(expenseSubject.getId());
            debitExpense.setSubjectCode(expenseSubject.getCode());
            debitExpense.setSubjectName(expenseSubject.getName());
            debitExpense.setDebit(totalGrossSalary);
            debitExpense.setCredit(BigDecimal.ZERO);
            debitExpense.setSortOrder(1);
            details.add(debitExpense);
        }

        // 贷：应付职工薪酬 (应发工资总额) —— 负债科目在贷方,原代码错误地放在借方
        VoucherDetailRequest creditPayable = new VoucherDetailRequest();
        creditPayable.setLineNo(lineNo++);
        creditPayable.setSummary("计提" + request.getYear() + "年" + request.getMonth() + "月工资");
        creditPayable.setSubjectId(payableSubject.getId());
        creditPayable.setSubjectCode(payableSubject.getCode());
        creditPayable.setSubjectName(payableSubject.getName());
        creditPayable.setDebit(BigDecimal.ZERO);
        creditPayable.setCredit(totalGrossSalary);
        creditPayable.setSortOrder(2);
        details.add(creditPayable);

        // === 发放分录 ===
        // 借：应付职工薪酬 (应发工资总额,核销计提的负债)
        VoucherDetailRequest debitPayable = new VoucherDetailRequest();
        debitPayable.setLineNo(lineNo++);
        debitPayable.setSummary("发放" + request.getYear() + "年" + request.getMonth() + "月工资");
        debitPayable.setSubjectId(payableSubject.getId());
        debitPayable.setSubjectCode(payableSubject.getCode());
        debitPayable.setSubjectName(payableSubject.getName());
        debitPayable.setDebit(totalGrossSalary);
        debitPayable.setCredit(BigDecimal.ZERO);
        debitPayable.setSortOrder(3);
        details.add(debitPayable);

        // 贷：银行存款 (实发工资)
        VoucherDetailRequest creditBank = new VoucherDetailRequest();
        creditBank.setLineNo(lineNo++);
        creditBank.setSummary("发放" + request.getYear() + "年" + request.getMonth() + "月工资");
        creditBank.setSubjectId(bankSubject.getId());
        creditBank.setSubjectCode(bankSubject.getCode());
        creditBank.setSubjectName(bankSubject.getName());
        creditBank.setDebit(BigDecimal.ZERO);
        creditBank.setCredit(totalNetSalary);
        creditBank.setSortOrder(4);
        details.add(creditBank);

        // 贷：其他应付款-社保 (个人部分)
        if (totalSocialSecurity.compareTo(BigDecimal.ZERO) > 0) {
            if (request.getSocialSecuritySubjectId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "社保金额大于0但未指定社保科目");
            }
            Subject socialSecuritySubject = subjectMapper.selectById(request.getSocialSecuritySubjectId());
            if (socialSecuritySubject == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "社保科目不存在");
            }
            VoucherDetailRequest creditSocialSecurity = new VoucherDetailRequest();
            creditSocialSecurity.setLineNo(lineNo++);
            creditSocialSecurity.setSummary("代扣社保");
            creditSocialSecurity.setSubjectId(socialSecuritySubject.getId());
            creditSocialSecurity.setSubjectCode(socialSecuritySubject.getCode());
            creditSocialSecurity.setSubjectName(socialSecuritySubject.getName());
            creditSocialSecurity.setDebit(BigDecimal.ZERO);
            creditSocialSecurity.setCredit(totalSocialSecurity);
            creditSocialSecurity.setSortOrder(5);
            details.add(creditSocialSecurity);
        }

        // 贷：其他应付款-公积金 (个人部分)
        if (totalHousingFund.compareTo(BigDecimal.ZERO) > 0) {
            if (request.getHousingFundSubjectId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "公积金金额大于0但未指定公积金科目");
            }
            Subject housingFundSubject = subjectMapper.selectById(request.getHousingFundSubjectId());
            if (housingFundSubject == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "公积金科目不存在");
            }
            VoucherDetailRequest creditHousingFund = new VoucherDetailRequest();
            creditHousingFund.setLineNo(lineNo++);
            creditHousingFund.setSummary("代扣公积金");
            creditHousingFund.setSubjectId(housingFundSubject.getId());
            creditHousingFund.setSubjectCode(housingFundSubject.getCode());
            creditHousingFund.setSubjectName(housingFundSubject.getName());
            creditHousingFund.setDebit(BigDecimal.ZERO);
            creditHousingFund.setCredit(totalHousingFund);
            creditHousingFund.setSortOrder(6);
            details.add(creditHousingFund);
        }

        // 贷：应交税费-个人所得税
        if (totalIncomeTax.compareTo(BigDecimal.ZERO) > 0) {
            if (request.getIncomeTaxSubjectId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "个税金额大于0但未指定个税科目");
            }
            Subject incomeTaxSubject = subjectMapper.selectById(request.getIncomeTaxSubjectId());
            if (incomeTaxSubject == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "个税科目不存在");
            }
            VoucherDetailRequest creditIncomeTax = new VoucherDetailRequest();
            creditIncomeTax.setLineNo(lineNo++);
            creditIncomeTax.setSummary("代扣个人所得税");
            creditIncomeTax.setSubjectId(incomeTaxSubject.getId());
            creditIncomeTax.setSubjectCode(incomeTaxSubject.getCode());
            creditIncomeTax.setSubjectName(incomeTaxSubject.getName());
            creditIncomeTax.setDebit(BigDecimal.ZERO);
            creditIncomeTax.setCredit(totalIncomeTax);
            creditIncomeTax.setSortOrder(7);
            details.add(creditIncomeTax);
        }

        voucherRequest.setDetails(details);
        voucherService.createVoucher(voucherRequest);

        // 更新薪资记录状态为已发放
        for (SalarySheet sheet : salarySheets) {
            sheet.setStatus(2); // 已发放
            salarySheetMapper.updateById(sheet);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 计算应纳税所得额和个人所得税
     */
    private void calculateTaxableIncomeAndTax(SalarySheet salarySheet) {
        calculateTaxableIncomeAndTax(salarySheet, new BigDecimal("5000"));
    }

    /**
     * 计算应纳税所得额和个人所得税
     */
    private void calculateTaxableIncomeAndTax(SalarySheet salarySheet, BigDecimal threshold) {
        if (threshold == null) {
            threshold = new BigDecimal("5000");
        }

        // 应发工资 = 基本工资 + 津贴补贴 + 奖金 - 扣款
        BigDecimal grossSalary = BigDecimal.ZERO;
        grossSalary = grossSalary.add(salarySheet.getBaseSalary() != null ? salarySheet.getBaseSalary() : BigDecimal.ZERO);
        grossSalary = grossSalary.add(salarySheet.getAllowance() != null ? salarySheet.getAllowance() : BigDecimal.ZERO);
        grossSalary = grossSalary.add(salarySheet.getBonus() != null ? salarySheet.getBonus() : BigDecimal.ZERO);
        grossSalary = grossSalary.subtract(salarySheet.getDeduction() != null ? salarySheet.getDeduction() : BigDecimal.ZERO);

        // 应纳税所得额 = 应发工资 - 社保 - 公积金 - 起征点 - 专项附加扣除
        BigDecimal socialSecurity = salarySheet.getSocialSecurity() != null ? salarySheet.getSocialSecurity() : BigDecimal.ZERO;
        BigDecimal housingFund = salarySheet.getHousingFund() != null ? salarySheet.getHousingFund() : BigDecimal.ZERO;

        // 专项附加扣除:调用专项扣除服务获取当月可扣除总额,可能返回null,用0兜底
        BigDecimal specialDeduction = BigDecimal.ZERO;
        if (salarySheet.getEmployeeId() != null && salarySheet.getYear() != null && salarySheet.getMonth() != null) {
            BigDecimal monthlyDeduction = specialDeductionService.calculateMonthlyDeduction(
                    salarySheet.getEmployeeId(), salarySheet.getYear(), salarySheet.getMonth());
            if (monthlyDeduction != null) {
                specialDeduction = monthlyDeduction;
            }
        }

        BigDecimal taxableIncome = grossSalary.subtract(socialSecurity).subtract(housingFund)
                .subtract(threshold).subtract(specialDeduction);

        if (taxableIncome.compareTo(BigDecimal.ZERO) < 0) {
            taxableIncome = BigDecimal.ZERO;
        }

        salarySheet.setTaxableIncome(taxableIncome);

        // 计算个人所得税（使用累计预扣预缴法,符合《个人所得税法》2019年起规定）
        BigDecimal incomeTax = calculateIncomeTaxByCumulative(
                salarySheet.getEmployeeId(), salarySheet.getYear(), salarySheet.getMonth(), taxableIncome);
        salarySheet.setIncomeTax(incomeTax);

        // 实发工资 = 应发工资 - 社保 - 公积金 - 个人所得税
        BigDecimal netSalary = grossSalary.subtract(socialSecurity).subtract(housingFund).subtract(incomeTax);
        salarySheet.setNetSalary(netSalary);
    }

    /**
     * 计算个人所得税（月度七级超额累进税率）
     * <p>注意:自2019年起工资薪金个税应按累计预扣预缴法计算,本方法保留仅供兼容,
     * 实际计算流程已改用 {@link #calculateIncomeTaxByCumulative}。</p>
     */
    private BigDecimal calculateIncomeTax(BigDecimal monthlyTaxableIncome) {
        if (monthlyTaxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal tax;
        // 税率表（月度）
        if (monthlyTaxableIncome.compareTo(new BigDecimal("3000")) <= 0) {
            tax = monthlyTaxableIncome.multiply(new BigDecimal("0.03"));
        } else if (monthlyTaxableIncome.compareTo(new BigDecimal("12000")) <= 0) {
            tax = monthlyTaxableIncome.multiply(new BigDecimal("0.10")).subtract(new BigDecimal("210"));
        } else if (monthlyTaxableIncome.compareTo(new BigDecimal("25000")) <= 0) {
            tax = monthlyTaxableIncome.multiply(new BigDecimal("0.20")).subtract(new BigDecimal("1410"));
        } else if (monthlyTaxableIncome.compareTo(new BigDecimal("35000")) <= 0) {
            tax = monthlyTaxableIncome.multiply(new BigDecimal("0.25")).subtract(new BigDecimal("2660"));
        } else if (monthlyTaxableIncome.compareTo(new BigDecimal("55000")) <= 0) {
            tax = monthlyTaxableIncome.multiply(new BigDecimal("0.30")).subtract(new BigDecimal("4410"));
        } else if (monthlyTaxableIncome.compareTo(new BigDecimal("80000")) <= 0) {
            tax = monthlyTaxableIncome.multiply(new BigDecimal("0.35")).subtract(new BigDecimal("7160"));
        } else {
            tax = monthlyTaxableIncome.multiply(new BigDecimal("0.45")).subtract(new BigDecimal("15160"));
        }

        return tax.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 按累计预扣预缴法计算本月应扣个人所得税
     * <p>公式:
     * <pre>
     * 累计应纳税所得额 = 截至上月累计应纳税所得额 + 本月应纳税所得额
     * 累计应预扣预缴税额 = 累计应纳税所得额 × 预扣率 - 速算扣除数
     * 本月应预扣预缴税额 = 累计应预扣预缴税额 - 截至上月已预扣预缴税额(不能为负)
     * </pre></p>
     *
     * @param employeeId                员工ID,用于查询本年度历史工资记录
     * @param year                      年度
     * @param month                     月份
     * @param currentMonthTaxableIncome 本月应纳税所得额(已扣除5000起征点及专项附加扣除等)
     * @return 本月应预扣预缴税额
     */
    private BigDecimal calculateIncomeTaxByCumulative(Long employeeId, Integer year, Integer month,
                                                     BigDecimal currentMonthTaxableIncome) {
        if (currentMonthTaxableIncome == null || currentMonthTaxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            currentMonthTaxableIncome = BigDecimal.ZERO;
        }

        // 查询本年度截至上月已确认/已发放的工资表,累加应纳税所得额和已预扣税额
        BigDecimal yearToDateTaxableIncome = BigDecimal.ZERO;
        BigDecimal yearToDateWithheldTax = BigDecimal.ZERO;
        if (employeeId != null && year != null && month != null && month > 1) {
            LambdaQueryWrapper<SalarySheet> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SalarySheet::getEmployeeId, employeeId)
                   .eq(SalarySheet::getYear, year)
                   .lt(SalarySheet::getMonth, month)
                   .in(SalarySheet::getStatus, 1, 2); // 已确认或已发放
            List<SalarySheet> historySheets = salarySheetMapper.selectList(wrapper);
            for (SalarySheet hs : historySheets) {
                if (hs.getTaxableIncome() != null) {
                    yearToDateTaxableIncome = yearToDateTaxableIncome.add(hs.getTaxableIncome());
                }
                if (hs.getIncomeTax() != null) {
                    yearToDateWithheldTax = yearToDateWithheldTax.add(hs.getIncomeTax());
                }
            }
        }

        // 累计应纳税所得额 = 截至上月累计 + 本月
        BigDecimal cumulativeTaxableIncome = yearToDateTaxableIncome.add(currentMonthTaxableIncome);
        if (cumulativeTaxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 按年度累计预扣率表计算累计应预扣预缴税额
        BigDecimal cumulativeTax = calculateCumulativeTaxByAnnualTable(cumulativeTaxableIncome);

        // 本月应预扣预缴税额 = 累计应预扣 - 已预扣(不能为负,避免累计已预扣超过应预扣时产生负税)
        BigDecimal currentMonthTax = cumulativeTax.subtract(yearToDateWithheldTax);
        if (currentMonthTax.compareTo(BigDecimal.ZERO) < 0) {
            currentMonthTax = BigDecimal.ZERO;
        }

        return currentMonthTax.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 按年度累计预扣率表(居民个人工资薪金)计算累计应预扣预缴税额
     */
    private BigDecimal calculateCumulativeTaxByAnnualTable(BigDecimal cumulativeTaxableIncome) {
        BigDecimal tax;
        if (cumulativeTaxableIncome.compareTo(new BigDecimal("36000")) <= 0) {
            tax = cumulativeTaxableIncome.multiply(new BigDecimal("0.03"));
        } else if (cumulativeTaxableIncome.compareTo(new BigDecimal("144000")) <= 0) {
            tax = cumulativeTaxableIncome.multiply(new BigDecimal("0.10")).subtract(new BigDecimal("2520"));
        } else if (cumulativeTaxableIncome.compareTo(new BigDecimal("300000")) <= 0) {
            tax = cumulativeTaxableIncome.multiply(new BigDecimal("0.20")).subtract(new BigDecimal("16920"));
        } else if (cumulativeTaxableIncome.compareTo(new BigDecimal("420000")) <= 0) {
            tax = cumulativeTaxableIncome.multiply(new BigDecimal("0.25")).subtract(new BigDecimal("31920"));
        } else if (cumulativeTaxableIncome.compareTo(new BigDecimal("660000")) <= 0) {
            tax = cumulativeTaxableIncome.multiply(new BigDecimal("0.30")).subtract(new BigDecimal("52920"));
        } else if (cumulativeTaxableIncome.compareTo(new BigDecimal("960000")) <= 0) {
            tax = cumulativeTaxableIncome.multiply(new BigDecimal("0.35")).subtract(new BigDecimal("85920"));
        } else {
            tax = cumulativeTaxableIncome.multiply(new BigDecimal("0.45")).subtract(new BigDecimal("181920"));
        }

        if (tax.compareTo(BigDecimal.ZERO) < 0) {
            tax = BigDecimal.ZERO;
        }
        return tax;
    }

    /**
     * 根据薪资项目计算各项金额
     * <p>基本工资取自员工档案,社保公积金按配置计算,津贴/奖金/扣款等浮动项
     * 通过薪资公式(SalaryFormula)动态求值。</p>
     */
    private void calculateSalaryItems(SalarySheet salarySheet, List<SalaryItem> salaryItems) {
        // 基本工资从员工档案取
        Employee employee = employeeMapper.selectById(salarySheet.getEmployeeId());
        BigDecimal baseSalary = BigDecimal.ZERO;
        if (employee != null && employee.getBaseSalary() != null) {
            baseSalary = employee.getBaseSalary();
        }
        salarySheet.setBaseSalary(baseSalary);

        // 津贴/奖金/扣款默认0,后续若有公式则覆盖
        if (salarySheet.getAllowance() == null) {
            salarySheet.setAllowance(BigDecimal.ZERO);
        }
        if (salarySheet.getBonus() == null) {
            salarySheet.setBonus(BigDecimal.ZERO);
        }
        if (salarySheet.getDeduction() == null) {
            salarySheet.setDeduction(BigDecimal.ZERO);
        }

        // 社保及公积金:根据社保公积金配置按缴费基数计算个人部分
        BigDecimal socialSecurity = BigDecimal.ZERO;
        BigDecimal housingFund = BigDecimal.ZERO;
        if (salarySheet.getAccountSetId() != null && salarySheet.getYear() != null) {
            // 先校验配置是否存在,缺失时按0处理,避免批量计算整体失败
            SocialSecurityConfigVO config = socialSecurityConfigService.getConfig(
                    salarySheet.getAccountSetId(), salarySheet.getYear());
            if (config != null) {
                SocialSecurityCalculationVO calc = socialSecurityConfigService.calculate(
                        salarySheet.getAccountSetId(), salarySheet.getYear(), baseSalary);
                BigDecimal employeeTotal = calc.getEmployeeTotal() != null ? calc.getEmployeeTotal() : BigDecimal.ZERO;
                BigDecimal housingFundEmployee = calc.getHousingFundEmployee() != null ? calc.getHousingFundEmployee() : BigDecimal.ZERO;
                // 个人部分合计包含公积金个人部分,社保单独取差额
                socialSecurity = employeeTotal.subtract(housingFundEmployee);
                housingFund = housingFundEmployee;
            }
        }
        salarySheet.setSocialSecurity(socialSecurity);
        salarySheet.setHousingFund(housingFund);

        // 按薪资项目公式动态计算:查询该账套启用的公式并求值
        evaluateSalaryFormulas(salarySheet, salaryItems, baseSalary, socialSecurity, housingFund);
    }

    /**
     * 查询账套启用的薪资公式并按优先级逐个求值,将结果回写到工资表对应字段
     *
     * @param salarySheet     工资表
     * @param salaryItems     账套下启用的薪资项目
     * @param baseSalary      已计算的基本工资
     * @param socialSecurity  已计算的社保个人部分
     * @param housingFund     已计算的公积金个人部分
     */
    private void evaluateSalaryFormulas(SalarySheet salarySheet, List<SalaryItem> salaryItems,
                                        BigDecimal baseSalary, BigDecimal socialSecurity, BigDecimal housingFund) {
        if (salarySheet.getAccountSetId() == null) {
            return;
        }

        // 查询该账套启用的薪资公式,按优先级升序排序(优先级小的先算,支持公式链式引用)
        LambdaQueryWrapper<SalaryFormula> formulaWrapper = new LambdaQueryWrapper<>();
        formulaWrapper.eq(SalaryFormula::getAccountSetId, salarySheet.getAccountSetId())
                      .eq(SalaryFormula::getStatus, 1)
                      .orderByAsc(SalaryFormula::getPriority);
        List<SalaryFormula> formulas = salaryFormulaService.list(formulaWrapper);
        if (formulas.isEmpty()) {
            return;
        }

        // 收集已配置薪资项目编码集合,公式目标项必须命中已配置项目才处理
        Set<String> itemCodeSet = salaryItems.stream()
                .map(SalaryItem::getItemCode)
                .filter(c -> c != null && !c.trim().isEmpty())
                .collect(Collectors.toSet());

        // 构建公式上下文:包含当前已知的薪资项值(同时提供下划线和驼峰两种命名,兼容不同公式写法)
        Map<String, BigDecimal> context = new HashMap<>();
        context.put("base_salary", baseSalary);
        context.put("baseSalary", baseSalary);
        context.put("allowance", salarySheet.getAllowance());
        context.put("bonus", salarySheet.getBonus());
        context.put("deduction", salarySheet.getDeduction());
        context.put("social_security", socialSecurity);
        context.put("socialSecurity", socialSecurity);
        context.put("housing_fund", housingFund);
        context.put("housingFund", housingFund);

        // 按优先级逐个求值,结果回写上下文以支持公式链式引用
        for (SalaryFormula formula : formulas) {
            String targetItem = formula.getTargetItem();
            String expression = formula.getFormulaExpression();
            if (targetItem == null || targetItem.trim().isEmpty()
                    || expression == null || expression.trim().isEmpty()) {
                continue;
            }
            // 仅处理目标项为已配置薪资项目的公式
            if (!itemCodeSet.contains(targetItem)) {
                continue;
            }
            try {
                BigDecimal result = salaryFormulaService.evaluateFormula(expression, context);
                if (result == null) {
                    result = BigDecimal.ZERO;
                }
                // 回写上下文(同时写原编码和驼峰形式,方便后续公式引用)
                context.put(targetItem, result);
                context.put(toCamelCase(targetItem), result);
                // 回写到工资表对应字段
                applyFormulaResultToSheet(salarySheet, targetItem, result);
            } catch (Exception e) {
                // 公式计算异常(如除零、变量未定义)时记日志并跳过,不中断整体计算
                log.warn("薪资公式计算失败,公式名称:{},目标项:{},表达式:{},错误:{}",
                        formula.getFormulaName(), targetItem, expression, e.getMessage());
            }
        }
    }

    /**
     * 将公式计算结果回写到工资表对应字段
     * 支持下划线和驼峰命名的项目编码
     */
    private void applyFormulaResultToSheet(SalarySheet salarySheet, String itemCode, BigDecimal value) {
        if (itemCode == null || value == null) {
            return;
        }
        String code = itemCode.trim();
        switch (code) {
            case "base_salary":
            case "baseSalary":
                salarySheet.setBaseSalary(value);
                break;
            case "allowance":
                salarySheet.setAllowance(value);
                break;
            case "bonus":
                salarySheet.setBonus(value);
                break;
            case "deduction":
                salarySheet.setDeduction(value);
                break;
            case "social_security":
            case "socialSecurity":
                salarySheet.setSocialSecurity(value);
                break;
            case "housing_fund":
            case "housingFund":
                salarySheet.setHousingFund(value);
                break;
            default:
                // 未知薪资项(如个税、实发工资等由后续流程计算)不回写
                break;
        }
    }

    /**
     * 将下划线命名转换为驼峰命名(如 base_salary → baseSalary)
     */
    private String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                upperNext = true;
            } else if (upperNext) {
                sb.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 员工实体转VO
     */
    private EmployeeVO convertEmployeeToVO(Employee employee) {
        EmployeeVO vo = new EmployeeVO();
        BeanUtil.copyProperties(employee, vo);

        // 查询创建人名称
        if (employee.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(employee.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }

        return vo;
    }

    /**
     * 薪资项目实体转VO
     */
    private SalaryItemVO convertSalaryItemToVO(SalaryItem salaryItem) {
        SalaryItemVO vo = new SalaryItemVO();
        BeanUtil.copyProperties(salaryItem, vo);

        // 查询创建人名称
        if (salaryItem.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(salaryItem.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }

        return vo;
    }

    /**
     * 薪资表实体转VO
     */
    private SalarySheetVO convertSalarySheetToVO(SalarySheet salarySheet) {
        SalarySheetVO vo = new SalarySheetVO();
        BeanUtil.copyProperties(salarySheet, vo);

        // 查询创建人名称
        if (salarySheet.getCreateBy() != null) {
            SysUser createUser = sysUserMapper.selectById(salarySheet.getCreateBy());
            if (createUser != null) {
                vo.setCreateByName(createUser.getRealName() != null ? createUser.getRealName() : createUser.getUsername());
            }
        }

        return vo;
    }

    // ==================== 薪资导出 ====================

    @Override
    public void exportBankDisbursementFile(Long accountSetId, Integer year, Integer month, HttpServletResponse response) {
        List<SalarySheet> sheets = listSalarySheetsByPeriod(accountSetId, year, month);
        Map<Long, Employee> employeeMap = loadEmployeeMap(accountSetId);
        salaryExportUtil.exportBankDisbursementFile(sheets, employeeMap, year, month, response);
    }

    @Override
    public void exportPayslips(Long accountSetId, Integer year, Integer month, HttpServletResponse response) {
        List<SalarySheet> sheets = listSalarySheetsByPeriod(accountSetId, year, month);
        Map<Long, Employee> employeeMap = loadEmployeeMap(accountSetId);
        salaryExportUtil.exportPayslips(sheets, employeeMap, year, month, response);
    }

    /**
     * 查询某期间全部薪资表
     */
    private List<SalarySheet> listSalarySheetsByPeriod(Long accountSetId, Integer year, Integer month) {
        if (accountSetId == null || year == null || month == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "账套ID、年、月不能为空");
        }
        LambdaQueryWrapper<SalarySheet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalarySheet::getAccountSetId, accountSetId)
                .eq(SalarySheet::getYear, year)
                .eq(SalarySheet::getMonth, month)
                .orderByAsc(SalarySheet::getEmployeeId);
        List<SalarySheet> sheets = salarySheetMapper.selectList(wrapper);
        if (sheets.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "该期间无薪资数据");
        }
        return sheets;
    }

    /**
     * 加载账套下全部员工映射
     */
    private Map<Long, Employee> loadEmployeeMap(Long accountSetId) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getAccountSetId, accountSetId);
        List<Employee> employees = employeeMapper.selectList(wrapper);
        return employees.stream().collect(Collectors.toMap(Employee::getId, e -> e, (x, y) -> x));
    }
}
