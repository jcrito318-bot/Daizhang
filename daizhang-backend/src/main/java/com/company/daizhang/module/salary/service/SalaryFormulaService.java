package com.company.daizhang.module.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.salary.dto.SalaryFormulaRequest;
import com.company.daizhang.module.salary.entity.SalaryFormula;
import com.company.daizhang.module.salary.vo.SalaryFormulaVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 薪资公式服务接口
 */
public interface SalaryFormulaService extends IService<SalaryFormula> {

    /**
     * 查询公式列表
     */
    List<SalaryFormulaVO> listFormulas(Long accountSetId);

    /**
     * 创建公式
     */
    void createFormula(SalaryFormulaRequest request);

    /**
     * 更新公式
     */
    void updateFormula(Long id, SalaryFormulaRequest request);

    /**
     * 删除公式
     */
    void deleteFormula(Long id);

    /**
     * 公式求值（支持加减乘除、括号和变量替换）
     *
     * @param expression 表达式，如: base_salary * 0.1 + bonus - social_security
     * @param variables   变量名到值的映射
     * @return 计算结果
     */
    BigDecimal evaluateFormula(String expression, Map<String, BigDecimal> variables);
}
