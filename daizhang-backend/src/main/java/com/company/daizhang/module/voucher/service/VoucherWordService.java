package com.company.daizhang.module.voucher.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.voucher.entity.VoucherWord;

import java.util.List;

/**
 * 凭证字服务接口
 */
public interface VoucherWordService extends IService<VoucherWord> {

    /**
     * 根据账套ID查询凭证字列表
     */
    List<VoucherWord> listByAccountSetId(Long accountSetId);
}
