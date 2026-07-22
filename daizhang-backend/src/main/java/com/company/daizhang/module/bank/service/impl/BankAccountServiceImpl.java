package com.company.daizhang.module.bank.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.crypto.enums.MaskType;
import com.company.daizhang.common.crypto.util.AesGcmEncryptor;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.bank.dto.BankAccountQueryRequest;
import com.company.daizhang.module.bank.dto.BankAccountRequest;
import com.company.daizhang.module.bank.entity.BankAccount;
import com.company.daizhang.module.bank.entity.BankTransaction;
import com.company.daizhang.module.bank.mapper.BankAccountMapper;
import com.company.daizhang.module.bank.mapper.BankTransactionMapper;
import com.company.daizhang.module.bank.service.BankAccountService;
import com.company.daizhang.module.bank.vo.BankAccountVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
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
    private final AccountSetAccessService accountSetAccessService;
    private final BankTransactionMapper bankTransactionMapper;
    private final AesGcmEncryptor encryptor;

    @Override
    public PageResult<BankAccountVO> pageBankAccounts(BankAccountQueryRequest request) {
        Page<BankAccount> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<BankAccount> wrapper = new LambdaQueryWrapper<>();
        // IDOR治理:校验当前用户对该账套的访问权
        applyAccountSetFilter(wrapper, BankAccount::getAccountSetId, request.getAccountSetId());
        wrapper.like(StrUtil.isNotBlank(request.getAccountName()), BankAccount::getAccountName, request.getAccountName())
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
        // IDOR治理:校验当前用户对该账套的访问权
        accountSetAccessService.checkAccess(accountSetId);
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
        accountSetAccessService.checkAccess(account.getAccountSetId());
        return convertToVO(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBankAccount(BankAccountRequest request) {
        accountSetAccessService.checkOwner(request.getAccountSetId());
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
        accountSetAccessService.checkOwner(account.getAccountSetId());
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
        // 排除状态与归属字段:status变更须走updateStatus专用方法,
        // accountSetId不可通过通用更新修改(防止银行账户被移到其他账套)
        BeanUtil.copyProperties(request, account, "id", "accountSetId", "status");
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
        accountSetAccessService.checkOwner(account.getAccountSetId());
        // 校验是否存在交易流水，有流水不允许删除
        LambdaQueryWrapper<BankTransaction> txWrapper = new LambdaQueryWrapper<>();
        txWrapper.eq(BankTransaction::getAccountSetId, account.getAccountSetId())
                 .eq(BankTransaction::getBankAccount, account.getAccountNumber());
        Long txCount = bankTransactionMapper.selectCount(txWrapper);
        if (txCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "该银行账户存在交易流水，无法删除");
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
        accountSetAccessService.checkOwner(account.getAccountSetId());
        account.setStatus(status);
        bankAccountMapper.updateById(account);
        log.info("更新银行账户状态，ID: {}, status: {}", id, status);
    }

    // ==================== 辅助方法 ====================

    private BankAccountVO convertToVO(BankAccount account) {
        BankAccountVO vo = new BankAccountVO();
        BeanUtil.copyProperties(account, vo);
        // P4.1: 银行账号脱敏,对外 API 不返回明文
        vo.setAccountNumber(encryptor.mask(account.getAccountNumber(), MaskType.BANK_ACCOUNT));
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

    /**
     * 分页/列表查询的账套访问过滤(IDOR治理):
     * - accountSetId 非空: checkAccess 校验后按该账套精确过滤
     * - accountSetId 为空: 按当前用户可访问账套集合过滤(超级管理员返回null表示不限制;
     *   空集合表示无权限,注入永不命中条件避免 MyBatis-Plus 对空集合in跳过导致越权)
     */
    private <T> void applyAccountSetFilter(LambdaQueryWrapper<T> wrapper,
                                           SFunction<T, Long> accountSetIdColumn,
                                           Long accountSetId) {
        if (accountSetId != null) {
            accountSetAccessService.checkAccess(accountSetId);
            wrapper.eq(accountSetIdColumn, accountSetId);
            return;
        }
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds == null) {
            return;
        }
        if (accessibleIds.isEmpty()) {
            wrapper.eq(accountSetIdColumn, -1L);
            return;
        }
        wrapper.in(accountSetIdColumn, accessibleIds);
    }
}
