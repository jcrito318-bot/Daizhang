package com.company.daizhang.module.voucher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.module.voucher.entity.VoucherWord;
import com.company.daizhang.module.voucher.mapper.VoucherWordMapper;
import com.company.daizhang.module.voucher.service.VoucherWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 凭证字服务实现
 */
@Service
@RequiredArgsConstructor
public class VoucherWordServiceImpl extends ServiceImpl<VoucherWordMapper, VoucherWord> implements VoucherWordService {

    @Override
    public List<VoucherWord> listByAccountSetId(Long accountSetId) {
        LambdaQueryWrapper<VoucherWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoucherWord::getAccountSetId, accountSetId)
               .orderByAsc(VoucherWord::getSortOrder);
        return this.list(wrapper);
    }
}
