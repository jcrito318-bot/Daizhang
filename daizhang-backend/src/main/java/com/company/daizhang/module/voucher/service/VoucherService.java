package com.company.daizhang.module.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.voucher.dto.VoucherCreateRequest;
import com.company.daizhang.module.voucher.dto.VoucherQueryRequest;
import com.company.daizhang.module.voucher.dto.VoucherUpdateRequest;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.vo.VoucherVO;

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
     * 过账凭证
     */
    void postVoucher(Long id);
}
