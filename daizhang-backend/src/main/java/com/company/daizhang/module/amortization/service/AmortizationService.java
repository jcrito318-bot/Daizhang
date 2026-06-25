package com.company.daizhang.module.amortization.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.amortization.dto.AmortizationRequest;
import com.company.daizhang.module.amortization.vo.AmortizationVO;

import java.util.List;

/**
 * 长期待摊费用服务接口
 */
public interface AmortizationService {

    /**
     * 分页查询长期待摊费用
     */
    PageResult<AmortizationVO> pageAmortizations(Long accountSetId, String amortizationName, Integer status, int pageNum, int pageSize);

    /**
     * 根据ID查询长期待摊费用
     */
    AmortizationVO getAmortizationById(Long id);

    /**
     * 创建长期待摊费用（自动计算月摊销额和剩余待摊）
     */
    void createAmortization(AmortizationRequest request);

    /**
     * 更新长期待摊费用
     */
    void updateAmortization(Long id, AmortizationRequest request);

    /**
     * 删除长期待摊费用
     */
    void deleteAmortization(Long id);

    /**
     * 执行月摊销（增加已摊销额，减少剩余待摊，满了改状态为已摊完）
     */
    void amortize(Long id, Integer year, Integer month);

    /**
     * 批量摊销所有摊销中的费用
     */
    void batchAmortize(Long accountSetId, Integer year, Integer month);

    /**
     * 生成长期待摊费用摊销凭证
     * 借：管理费用/销售费用（根据费用类型）
     * 贷：长期待摊费用（科目ID取自Amortization.subjectId）
     *
     * @param id    摊销记录ID
     * @param year  年度
     * @param month 月份
     * @return 生成的凭证ID
     */
    Long generateAmortizationVoucher(Long id, Integer year, Integer month);

    /**
     * 批量生成摊销凭证（针对该账套该期间所有摊销中的费用）
     *
     * @return 生成的凭证ID列表
     */
    List<Long> batchGenerateAmortizationVouchers(Long accountSetId, Integer year, Integer month);
}
