package com.company.daizhang.module.salary.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.salary.dto.SalaryFormulaRequest;
import com.company.daizhang.module.salary.entity.SalaryFormula;
import com.company.daizhang.module.salary.mapper.SalaryFormulaMapper;
import com.company.daizhang.module.salary.service.SalaryFormulaService;
import com.company.daizhang.module.salary.vo.SalaryFormulaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 薪资公式服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryFormulaServiceImpl extends ServiceImpl<SalaryFormulaMapper, SalaryFormula> implements SalaryFormulaService {

    @Override
    public List<SalaryFormulaVO> listFormulas(Long accountSetId) {
        LambdaQueryWrapper<SalaryFormula> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalaryFormula::getAccountSetId, accountSetId)
               .orderByAsc(SalaryFormula::getPriority)
               .orderByDesc(SalaryFormula::getCreateTime);
        List<SalaryFormula> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFormula(SalaryFormulaRequest request) {
        SalaryFormula formula = new SalaryFormula();
        BeanUtil.copyProperties(request, formula);
        if (formula.getPriority() == null) {
            formula.setPriority(0);
        }
        if (formula.getStatus() == null) {
            formula.setStatus(1);
        }
        this.save(formula);
        log.info("创建薪资公式成功，公式名称: {}", request.getFormulaName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFormula(Long id, SalaryFormulaRequest request) {
        SalaryFormula formula = this.getById(id);
        if (formula == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资公式不存在");
        }
        BeanUtil.copyProperties(request, formula);
        this.updateById(formula);
        log.info("更新薪资公式成功，公式ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFormula(Long id) {
        SalaryFormula formula = this.getById(id);
        if (formula == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资公式不存在");
        }
        this.removeById(id);
        log.info("删除薪资公式成功，公式ID: {}", id);
    }

    @Override
    public BigDecimal evaluateFormula(String expression, Map<String, BigDecimal> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        // 替换变量：采用“变量名长度降序 + 两步占位符法”避免子串误匹配。
        // 例如公式“基本工资 + 岗位工资”，若存在变量“工资”，直接 replace 会把
        // “基本工资”/“岗位工资”中的“工资”先替换掉，破坏成“基本<值>”/“岗位<值>”。
        String expr = expression.trim();
        if (variables != null && !variables.isEmpty()) {
            // 1) 按变量名长度降序排序：长变量名先被占位符替换，避免被短变量名（其子串）破坏。
            List<Map.Entry<String, BigDecimal>> sortedEntries = variables.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                    .collect(Collectors.toList());
            // 2) 第一步：把每个变量替换为唯一占位符 #{i}，占位符不会与其他变量名产生子串匹配。
            List<String> values = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> entry : sortedEntries) {
                String placeholder = "#{" + values.size() + "}";
                expr = expr.replace(entry.getKey(), placeholder);
                values.add(entry.getValue().toPlainString());
            }
            // 3) 第二步：把占位符替换为实际值，值中即使包含其他变量名也不会被二次匹配。
            for (int i = 0; i < values.size(); i++) {
                expr = expr.replace("#{" + i + "}", values.get(i));
            }
        }
        // 解析并计算
        try {
            return new FormulaEvaluator(expr).parse();
        } catch (Exception e) {
            log.error("公式求值失败，表达式: {}，错误: {}", expression, e.getMessage());
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "公式表达式格式不正确: " + e.getMessage());
        }
    }

    private SalaryFormulaVO convertToVO(SalaryFormula formula) {
        SalaryFormulaVO vo = new SalaryFormulaVO();
        BeanUtil.copyProperties(formula, vo);
        return vo;
    }

    /**
     * 递归下降解析器，支持加减乘除和括号，使用BigDecimal保证精度
     */
    private static class FormulaEvaluator {
        private final String input;
        private int pos;

        FormulaEvaluator(String input) {
            this.input = input.replaceAll("\\s+", "");
            this.pos = 0;
        }

        BigDecimal parse() {
            BigDecimal result = parseExpression();
            if (pos < input.length()) {
                throw new IllegalArgumentException("无法解析的字符: " + input.charAt(pos));
            }
            return result;
        }

        /**
         * 表达式 = 项 (('+' | '-') 项)*
         */
        private BigDecimal parseExpression() {
            BigDecimal result = parseTerm();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '+') {
                    pos++;
                    result = result.add(parseTerm());
                } else if (op == '-') {
                    pos++;
                    result = result.subtract(parseTerm());
                } else {
                    break;
                }
            }
            return result;
        }

        /**
         * 项 = 因子 (('*' | '/') 因子)*
         */
        private BigDecimal parseTerm() {
            BigDecimal result = parseFactor();
            while (pos < input.length()) {
                char op = input.charAt(pos);
                if (op == '*') {
                    pos++;
                    result = result.multiply(parseFactor());
                } else if (op == '/') {
                    pos++;
                    BigDecimal divisor = parseFactor();
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw new IllegalArgumentException("除数不能为零");
                    }
                    result = result.divide(divisor, 10, RoundingMode.HALF_UP);
                } else {
                    break;
                }
            }
            return result;
        }

        /**
         * 因子 = 数字 | '(' 表达式 ')' | '-' 因子
         */
        private BigDecimal parseFactor() {
            if (pos >= input.length()) {
                throw new IllegalArgumentException("表达式不完整");
            }
            char ch = input.charAt(pos);
            // 处理负号
            if (ch == '-') {
                pos++;
                return parseFactor().negate();
            }
            if (ch == '+') {
                pos++;
                return parseFactor();
            }
            // 处理括号
            if (ch == '(') {
                pos++;
                BigDecimal result = parseExpression();
                if (pos >= input.length() || input.charAt(pos) != ')') {
                    throw new IllegalArgumentException("缺少右括号");
                }
                pos++;
                return result;
            }
            // 处理数字
            if (Character.isDigit(ch) || ch == '.') {
                return parseNumber();
            }
            throw new IllegalArgumentException("无法解析的字符: " + ch);
        }

        /**
         * 解析数字（支持小数）
         */
        private BigDecimal parseNumber() {
            int start = pos;
            while (pos < input.length() && (Character.isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            String numStr = input.substring(start, pos);
            if (numStr.isEmpty()) {
                throw new IllegalArgumentException("数字格式不正确");
            }
            return new BigDecimal(numStr);
        }
    }
}
