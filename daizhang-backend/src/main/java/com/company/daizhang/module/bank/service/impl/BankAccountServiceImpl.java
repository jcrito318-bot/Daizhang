package com.company.daizhang.module.bank.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.bank.dto.BankAccountQueryRequest;
import com.company.daizhang.module.bank.dto.BankAccountRequest;
import com.company.daizhang.module.bank.entity.BankAccount;
import com.company.daizhang.module.bank.mapper.BankAccountMapper;
import com.company.daizhang.module.bank.service.BankAccountService;
import com.company.daizhang.module.bank.vo.BankAccountVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 银行账户主数据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountMapper bankAccountMapper;
    private final SubjectMapper subjectMapper;

    @Override
    public PageResult<BankAccountVO> pageBankAccounts(BankAccountQueryRequest request) {
        Page<BankAccount> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getAccountSetId() != null, BankAccount::getAccountSetId, request.getAccountSetId())
               .like(StrUtil.isNotBlank(request.getAccountName()), BankAccount::getAccountName, request.getAccountName())
               .like(StrUtil.isNotBlank(request.getAccountNumber()), BankAccount::getAccountNumber, request.getAccountNumber())
               .like(StrUtil.isNotBlank(request.getBankName()), BankAccount::getBankName, request.getBankName())
               .eq(StrUtil.isNotBlank(request.getAccountType()), BankAccount::getAccountType, request.getAccountType())
               .eq(request.getStatus() != null, BankAccount::getStatus, request.getStatus())
               .orderByDesc(BankAccount::getCreateTime);

        Page<BankAccount> result = bankAccountMapper.selectPage(page, wrapper);

        List<BankAccountVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<BankAccountVO> listByAccountSetId(Long accountSetId) {
        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankAccount::getAccountSetId, accountSetId)
               .orderByDesc(BankAccount::getCreateTime);
        List<BankAccount> list = bankAccountMapper.selectList(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public BankAccountVO getBankAccountById(Long id) {
        BankAccount account = bankAccountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "银行账户不存在");
        }
        return convertToVO(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBankAccount(BankAccountRequest request) {
        // 校验账号在该账套下唯一
        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankAccount::getAccountSetId, request.getAccountSetId())
               .eq(BankAccount::getAccountNumber, request.getAccountNumber());
        Long count = bankAccountMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "银行账号已存在");
        }

        BankAccount account = new BankAccount();
        BeanUtil.copyProperties(request, account);
        if (StrUtil.isBlank(account.getCurrency())) {
            account.setCurrency("CNY");
        }
        if (account.getStatus() == null) {
            account.setStatus(1);
        }
        bankAccountMapper.insert(account);
        log.info("创建银行账户成功，ID: {}, 账户名: {}, 账号: {}",
                account.getId(), account.getAccountName(), account.getAccountNumber());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBankAccount(Long id, BankAccountRequest request) {
        BankAccount account = bankAccountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "银行账户不存在");
        }
        // 账号变更时校验唯一性
        if (!account.getAccountNumber().equals(request.getAccountNumber())) {
            LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BankAccount::getAccountSetId, request.getAccountSetId())
                   .eq(BankAccount::getAccountNumber, request.getAccountNumber())
                   .ne(BankAccount::getId, id);
            Long count = bankAccountMapper.selectCount(wrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "银行账号已存在");
            }
        }
        BeanUtil.copyProperties(request, account);
        bankAccountMapper.updateById(account);
        log.info("更新银行账户成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBankAccount(Long id) {
        BankAccount account = bankAccountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "银行账户不存在");
        }
        bankAccountMapper.deleteById(id);
        log.info("删除银行账户成功，ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        BankAccount account = bankAccountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "银行账户不存在");
        }
        account.setStatus(status);
        bankAccountMapper.updateById(account);
        log.info("更新银行账户状态，ID: {}, status: {}", id, status);
    }

    // ==================== 辅助方法 ====================

    private BankAccountVO convertToVO(BankAccount account) {
        BankAccountVO vo = new BankAccountVO();
        BeanUtil.copyProperties(account, vo);
        // 账户类型描述
        if (StrUtil.isNotBlank(account.getAccountType())) {
            switch (account.getAccountType()) {
                case "CHECKING": vo.setAccountTypeDesc("活期"); break;
                case "DEPOSIT": vo.setAccountTypeDesc("定期"); break;
                case "OTHER": vo.setAccountTypeDesc("其他"); break;
                default: vo.setAccountTypeDesc(account.getAccountType());
            }
        }
        // 状态描述
        if (account.getStatus() != null) {
            vo.setStatusDesc(account.getStatus() == 1 ? "正常" : "停用");
        }
        // 关联科目名称
        if (account.getSubjectId() != null) {
            Subject subject = subjectMapper.selectById(account.getSubjectId());
            if (subject != null) {
                vo.setSubjectName(subject.getName());
            }
        }
        return vo;
    }
}
