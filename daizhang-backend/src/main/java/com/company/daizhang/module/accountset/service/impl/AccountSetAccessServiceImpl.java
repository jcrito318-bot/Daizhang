package com.company.daizhang.module.accountset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.entity.UserAccountSet;
import com.company.daizhang.module.accountset.mapper.UserAccountSetMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 账套数据级授权服务实现(IDOR越权治理)
 * <p>
 * 设计要点:
 * 1. admin(id=1)为超级管理员,对所有账套有完全访问权(兜底,保证系统初始化时不会锁死)
 * 2. 普通用户须在 sys_user_account_set 表中存在关联记录才可访问对应账套
 * 3. 创建账套时自动绑定 OWNER 关系(AccountSetServiceImpl.createAccountSet 调用 bindOwner)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSetAccessServiceImpl implements AccountSetAccessService {

    private final UserAccountSetMapper userAccountSetMapper;

    /** 超级管理员用户ID(id=1的admin用户),对所有账套有完全访问权 */
    private static final Long SUPER_ADMIN_USER_ID = 1L;

    @Override
    public void checkAccess(Long accountSetId) {
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // 超级管理员放行
        if (SUPER_ADMIN_USER_ID.equals(userId)) {
            return;
        }
        // 普通用户校验关联记录
        if (!hasRelation(userId, accountSetId)) {
            log.warn("IDOR越权拦截: 用户{}尝试访问无权限的账套{}", userId, accountSetId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该账套");
        }
    }

    @Override
    public void checkOwner(Long accountSetId) {
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // 超级管理员放行
        if (SUPER_ADMIN_USER_ID.equals(userId)) {
            return;
        }
        // 校验OWNER关系
        LambdaQueryWrapper<UserAccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSet::getUserId, userId)
               .eq(UserAccountSet::getAccountSetId, accountSetId)
               .eq(UserAccountSet::getRoleType, "OWNER");
        if (userAccountSetMapper.selectCount(wrapper) == 0) {
            log.warn("IDOR越权拦截: 用户{}尝试以非所有者身份操作账套{}", userId, accountSetId);
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅账套所有者可执行此操作");
        }
    }

    @Override
    public Set<Long> listAccessibleAccountSetIds() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Collections.emptySet();
        }
        // 超级管理员可访问全部账套(返回null表示不限制,调用方需特殊处理)
        if (SUPER_ADMIN_USER_ID.equals(userId)) {
            return null;
        }
        LambdaQueryWrapper<UserAccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSet::getUserId, userId);
        return userAccountSetMapper.selectList(wrapper).stream()
                .map(UserAccountSet::getAccountSetId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindOwner(Long accountSetId, Long userId) {
        if (accountSetId == null || userId == null) {
            return;
        }
        // 幂等:已存在则跳过
        LambdaQueryWrapper<UserAccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSet::getUserId, userId)
               .eq(UserAccountSet::getAccountSetId, accountSetId);
        if (userAccountSetMapper.selectCount(wrapper) > 0) {
            return;
        }
        UserAccountSet rel = new UserAccountSet();
        rel.setUserId(userId);
        rel.setAccountSetId(accountSetId);
        rel.setRoleType("OWNER");
        userAccountSetMapper.insert(rel);
        log.info("绑定账套所有者关系: 账套ID={}, 用户ID={}", accountSetId, userId);
    }

    private boolean hasRelation(Long userId, Long accountSetId) {
        LambdaQueryWrapper<UserAccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSet::getUserId, userId)
               .eq(UserAccountSet::getAccountSetId, accountSetId);
        return userAccountSetMapper.selectCount(wrapper) > 0;
    }
}
