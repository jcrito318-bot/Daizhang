package com.company.daizhang.module.accountset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.accountset.dto.SubjectBalanceRequest;
import com.company.daizhang.module.accountset.entity.SubjectBalance;
import com.company.daizhang.module.accountset.vo.SubjectBalanceVO;

import java.util.List;
import java.util.Map;

/**
 * 科目期初余额服务接口
 */
public interface SubjectBalanceService extends IService<SubjectBalance> {

    /**
     * 按账套和年度查询期初余额
     */
    List<SubjectBalanceVO> listByAccountSetAndYear(Long accountSetId, Integer year);

    /**
     * 批量保存期初余额
     */
    void saveBatch(Long accountSetId, Integer year, List<SubjectBalanceRequest> requests);

    /**
     * 试算平衡（返回借方合计、贷方合计、是否平衡）
     */
    Map<String, Object> trialBalance(Long accountSetId, Integer year);
}
