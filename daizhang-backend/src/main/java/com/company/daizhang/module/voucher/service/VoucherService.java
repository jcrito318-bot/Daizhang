package com.company.daizhang.module.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherQueryRequest;
import com.company.daizhang.module.voucher.dto.VoucherUpdateRequest;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.vo.VoucherVO;

import java.util.List;

/**
 * 凭证服务接口
 */
public interface VoucherService extends IService<Voucher> {

    /**
     * 分页查询凭证
     */
    PageResult<VoucherVO> pageVouchers(VoucherQueryRequest request);

    /**
     * 根据ID查询凭证
     */
    VoucherVO getVoucherById(Long id);

    /**
     * 创建凭证
     */
    void createVoucher(VoucherCreateRequest request);

    /**
     * 更新凭证
     */
    void updateVoucher(Long id, VoucherUpdateRequest request);

    /**
     * 删除凭证
     */
    void deleteVoucher(Long id);

    /**
     * 审核凭证
     */
    void auditVoucher(Long id);

    /**
     * 反审核凭证
     */
    void unauditVoucher(Long id);

    /**
     * 批量审核凭证
     *
     * @param ids 凭证ID列表
     * @return 成功审核的数量
     */
    int batchAuditVoucher(List<Long> ids);

    /**
     * 批量反审核凭证
     *
     * @param ids 凭证ID列表
     * @return 成功反审核的数量
     */
    int batchUnauditVoucher(List<Long> ids);

    /**
     * 过账凭证
     */
    void postVoucher(Long id);

    /**
     * 凭证整理（断号重编）
     * 重新生成该账套该期间所有凭证的连续凭证号
     *
     * @param accountSetId 账套ID
     * @param year         年度
     * @param month        月份
     */
    void rearrangeVoucherNo(Long accountSetId, Integer year, Integer month);

    /**
     * 复制凭证（生成新的未审核凭证，复制明细，金额相同）
     *
     * @param id 原凭证ID
     * @return 新凭证ID
     */
    Long copyVoucher(Long id);

    /**
     * 红冲凭证（生成一张金额取负的红字凭证，用于冲销原凭证）
     * 红冲凭证在原凭证所在期间生成。
     *
     * @param id 原凭证ID
     * @return 新凭证ID
     */
    Long reverseVoucher(Long id);

    /**
     * 红冲凭证（支持跨期红冲）。
     * 当原凭证所在期间已结账时，可指定 targetYear/targetMonth 在当前期间生成红冲凭证。
     *
     * @param id           原凭证ID
     * @param targetYear   红冲凭证目标年度，为null时取原凭证年度
     * @param targetMonth  红冲凭证目标月份，为null时取原凭证月份
     * @return 新凭证ID
     */
    Long reverseVoucher(Long id, Integer targetYear, Integer targetMonth);

    /**
     * 保存草稿（创建凭证但status=0, draftStatus=1，不校验借贷平衡）
     *
     * @param request 凭证创建请求
     * @return 草稿凭证ID
     */
    Long saveDraft(VoucherCreateRequest request);

    /**
     * 提交草稿（校验借贷平衡，draftStatus=0，转为正常凭证）
     *
     * @param id 草稿凭证ID
     */
    void submitDraft(Long id);
}
