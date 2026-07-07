package com.company.daizhang.module.accountset.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.dto.AccountSetCreateRequest;
import com.company.daizhang.module.accountset.dto.AccountSetQueryRequest;
import com.company.daizhang.module.accountset.dto.AccountSetUpdateRequest;
import com.company.daizhang.module.accountset.entity.AccountPeriod;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.mapper.AccountPeriodMapper;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.accountset.service.AccountSetService;
import com.company.daizhang.module.accountset.vo.AccountSetVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.subject.service.SubjectService;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherWord;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import com.company.daizhang.module.voucher.mapper.VoucherWordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 账套服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSetServiceImpl extends ServiceImpl<AccountSetMapper, AccountSet> implements AccountSetService {
    
    private final AccountPeriodMapper periodMapper;
    private final SubjectMapper subjectMapper;
    private final SubjectService subjectService;
    private final VoucherMapper voucherMapper;
    private final VoucherWordMapper voucherWordMapper;
    private final AccountSetAccessService accountSetAccessService;
    
    @Override
    public PageResult<AccountSetVO> pageAccountSets(AccountSetQueryRequest request) {
        Page<AccountSet> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<AccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(request.getCode()), AccountSet::getCode, request.getCode())
               .like(StrUtil.isNotBlank(request.getName()), AccountSet::getName, request.getName())
               .like(StrUtil.isNotBlank(request.getCompanyName()), AccountSet::getCompanyName, request.getCompanyName())
               .eq(request.getStatus() != null, AccountSet::getStatus, request.getStatus())
               .orderByDesc(AccountSet::getCreateTime);

        // IDOR治理:仅返回当前用户有权限访问的账套(超级管理员返回null表示不限制)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds != null) {
            if (accessibleIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L, request.getPageNum(), request.getPageSize());
            }
            wrapper.in(AccountSet::getId, accessibleIds);
        }

        Page<AccountSet> result = this.page(page, wrapper);

        List<AccountSetVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<AccountSetVO> listAllAccountSets() {
        // IDOR治理:仅返回当前用户有权限访问的账套(超级管理员返回null表示不限制)
        Set<Long> accessibleIds = accountSetAccessService.listAccessibleAccountSetIds();
        if (accessibleIds != null && accessibleIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<AccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AccountSet::getCreateTime);
        if (accessibleIds != null) {
            wrapper.in(AccountSet::getId, accessibleIds);
        }

        List<AccountSet> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public AccountSetVO getAccountSetById(Long id) {
        AccountSet accountSet = this.getById(id);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该账套的访问权
        accountSetAccessService.checkAccess(id);
        return convertToVO(accountSet);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAccountSet(AccountSetCreateRequest request) {
        // 业务校验：账套编码不能为空
        if (StrUtil.isBlank(request.getCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套编码不能为空");
        }
        // 业务校验：账套名称不能为空
        if (StrUtil.isBlank(request.getName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套名称不能为空");
        }
        // 业务校验：启用年度必须合理（1900-2099）
        if (request.getStartYear() == null || request.getStartYear() < 1900 || request.getStartYear() > 2099) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "启用年度格式不正确");
        }
        // 业务校验：启用月份必须在1-12之间
        if (request.getStartMonth() == null || request.getStartMonth() < 1 || request.getStartMonth() > 12) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "启用月份必须在1-12之间");
        }
        
        // 检查编码是否已存在
        LambdaQueryWrapper<AccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AccountSet::getCode, request.getCode());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_CODE_DUPLICATE);
        }
        
        AccountSet accountSet = new AccountSet();
        BeanUtil.copyProperties(request, accountSet);
        if (accountSet.getAccountingStandard() == null) {
            accountSet.setAccountingStandard("小企业会计准则");
        }
        if (accountSet.getCurrencyCode() == null) {
            accountSet.setCurrencyCode("CNY");
        }
        if (accountSet.getStatus() == null) {
            accountSet.setStatus(1);
        }
        this.save(accountSet);

        // IDOR治理:创建账套时自动绑定创建者为OWNER
        accountSetAccessService.bindOwner(accountSet.getId(), SecurityUtils.getCurrentUserId());

        log.info("创建账套成功，账套编码: {}, 账套名称: {}", accountSet.getCode(), accountSet.getName());

        // 自动初始化账套
        initAccountSet(accountSet.getId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccountSet(Long id, AccountSetUpdateRequest request) {
        AccountSet accountSet = this.getById(id);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_NOT_FOUND);
        }
        // IDOR治理:校验当前用户对该账套的访问权
        accountSetAccessService.checkAccess(id);

        // 排除status:账套启用/停用须走专用方法(含业务校验),不可通过通用更新绕过
        BeanUtil.copyProperties(request, accountSet, "status");
        this.updateById(accountSet);

        log.info("更新账套成功，账套ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableAccountSet(Long id) {
        AccountSet accountSet = this.getById(id);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_NOT_FOUND);
        }
        // IDOR治理:启用账套须所有者权限
        accountSetAccessService.checkOwner(id);
        accountSet.setStatus(1);
        this.updateById(accountSet);
        log.info("启用账套成功，账套ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableAccountSet(Long id) {
        AccountSet accountSet = this.getById(id);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_NOT_FOUND);
        }
        // IDOR治理:停用账套须所有者权限
        accountSetAccessService.checkOwner(id);

        // 业务校验：账套存在未结账业务(未结账期间或未审核/未过账凭证)时不允许停用,避免遗漏在途业务
        LambdaQueryWrapper<AccountPeriod> periodWrapper = new LambdaQueryWrapper<>();
        periodWrapper.eq(AccountPeriod::getAccountSetId, id)
                .eq(AccountPeriod::getStatus, 0); // 未结账
        if (periodMapper.selectCount(periodWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套存在未结账业务，无法停用");
        }

        // 校验是否存在未审核(0)/已审核未过账(1)的凭证(已过账=2,已作废不在此范围)
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, id)
                .in(Voucher::getStatus, 0, 1);
        if (voucherMapper.selectCount(voucherWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套存在未结账业务，无法停用");
        }

        accountSet.setStatus(0);
        this.updateById(accountSet);
        log.info("停用账套成功，账套ID: {}", id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountSet(Long id) {
        AccountSet accountSet = this.getById(id);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_NOT_FOUND);
        }
        // IDOR治理:删除账套须所有者权限
        accountSetAccessService.checkOwner(id);

        // 业务校验：检查账套下是否存在凭证，存在则不允许删除
        LambdaQueryWrapper<Voucher> voucherWrapper = new LambdaQueryWrapper<>();
        voucherWrapper.eq(Voucher::getAccountSetId, id);
        if (voucherMapper.selectCount(voucherWrapper) > 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_HAS_VOUCHERS);
        }
        
        this.removeById(id);
        
        // 删除会计期间
        LambdaQueryWrapper<AccountPeriod> periodWrapper = new LambdaQueryWrapper<>();
        periodWrapper.eq(AccountPeriod::getAccountSetId, id);
        periodMapper.delete(periodWrapper);
        
        // 删除科目
        LambdaQueryWrapper<Subject> subjectWrapper = new LambdaQueryWrapper<>();
        subjectWrapper.eq(Subject::getAccountSetId, id);
        subjectMapper.delete(subjectWrapper);
        
        log.info("删除账套成功，账套ID: {}, 账套编码: {}", id, accountSet.getCode());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initAccountSet(Long id) {
        AccountSet accountSet = this.getById(id);
        if (accountSet == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_NOT_FOUND);
        }
        // IDOR治理:初始化账套须所有者权限
        accountSetAccessService.checkOwner(id);

        // 业务校验：检查是否已经初始化过（是否已有会计期间）
        LambdaQueryWrapper<AccountPeriod> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(AccountPeriod::getAccountSetId, id);
        if (periodMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_INITIALIZED);
        }
        
        // 创建12个会计期间
        for (int month = 1; month <= 12; month++) {
            AccountPeriod period = new AccountPeriod();
            period.setAccountSetId(id);
            period.setYear(accountSet.getStartYear());
            period.setMonth(month);
            period.setStartDate(LocalDate.of(accountSet.getStartYear(), month, 1));
            period.setEndDate(period.getStartDate().plusMonths(1).minusDays(1));
            period.setStatus(0); // 未结账
            periodMapper.insert(period);
        }
        
        // 导入默认科目
        subjectService.initDefaultSubjects(id, accountSet.getAccountingStandard());

        // 复制默认凭证字模板（account_set_id=0 为系统默认模板）
        LambdaQueryWrapper<VoucherWord> wordWrapper = new LambdaQueryWrapper<>();
        wordWrapper.eq(VoucherWord::getAccountSetId, 0L);
        List<VoucherWord> templateWords = voucherWordMapper.selectList(wordWrapper);
        for (VoucherWord template : templateWords) {
            VoucherWord word = new VoucherWord();
            word.setAccountSetId(id);
            word.setName(template.getName());
            word.setCode(template.getCode());
            word.setSortOrder(template.getSortOrder());
            word.setStatus(1);
            voucherWordMapper.insert(word);
        }

        log.info("初始化账套成功，账套ID: {}, 启用年度: {}", id, accountSet.getStartYear());
    }
    
    private AccountSetVO convertToVO(AccountSet accountSet) {
        AccountSetVO vo = new AccountSetVO();
        BeanUtil.copyProperties(accountSet, vo);
        return vo;
    }
}
