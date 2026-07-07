package com.company.daizhang.module.accountset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.entity.UserAccountSet;
import com.company.daizhang.module.accountset.mapper.AccountSetMapper;
import com.company.daizhang.module.accountset.mapper.UserAccountSetMapper;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.accountset.vo.UserAccountSetVO;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 账套数据级授权服务实现(IDOR越权治理)
 * <p>
 * 设计要点:
 * 1. 超级管理员基于 ROLE_ADMIN 角色判定(由 UserDetailsServiceImpl 通过 sys_user_role + sys_role 加载),
 *    对所有账套有完全访问权(兜底,保证系统初始化时不会锁死)
 * 2. 普通用户须在 sys_user_account_set 表中存在关联记录才可访问对应账套
 * 3. 创建账套时自动绑定 OWNER 关系(AccountSetServiceImpl.createAccountSet 调用 bindOwner)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSetAccessServiceImpl implements AccountSetAccessService {

    private final UserAccountSetMapper userAccountSetMapper;
    private final AccountSetMapper accountSetMapper;
    private final SysUserMapper sysUserMapper;

    /** 合法的账套角色类型 */
    private static final Set<String> VALID_ROLE_TYPES =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("OWNER", "ACCOUNTANT", "VIEWER")));

    /**
     * 判断当前登录用户是否为超级管理员。
     * 基于 SecurityContext 中的 ROLE_ADMIN 角色判定,而非硬编码用户ID,
     * 避免数据库重置后首位非admin用户(id=1)绕过IDOR校验。
     */
    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

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
        if (isSuperAdmin()) {
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
        if (isSuperAdmin()) {
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
        if (isSuperAdmin()) {
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignAccountSet(Long userId, Long accountSetId, String roleType) {
        // 1. 权限校验:仅账套OWNER或管理员可操作
        checkOwner(accountSetId);
        // 2. 参数校验
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        if (roleType == null || !VALID_ROLE_TYPES.contains(roleType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "角色类型不合法,必须为 OWNER/ACCOUNTANT/VIEWER");
        }
        // 3. 校验用户和账套存在
        if (sysUserMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        if (accountSetMapper.selectById(accountSetId) == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_SET_NOT_FOUND, "账套不存在");
        }
        // 4. 已存在关系则更新角色,不存在则新增
        LambdaQueryWrapper<UserAccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSet::getUserId, userId)
               .eq(UserAccountSet::getAccountSetId, accountSetId);
        UserAccountSet existing = userAccountSetMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setRoleType(roleType);
            userAccountSetMapper.updateById(existing);
            log.info("更新账套访问关系: 账套ID={}, 用户ID={}, 角色={}", accountSetId, userId, roleType);
        } else {
            UserAccountSet rel = new UserAccountSet();
            rel.setUserId(userId);
            rel.setAccountSetId(accountSetId);
            rel.setRoleType(roleType);
            userAccountSetMapper.insert(rel);
            log.info("分配账套访问权限: 账套ID={}, 用户ID={}, 角色={}", accountSetId, userId, roleType);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAccountSet(Long userId, Long accountSetId) {
        // 1. 权限校验:仅账套OWNER或管理员可操作
        checkOwner(accountSetId);
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        // 2. 不允许移除OWNER关系,防止账套无主
        LambdaQueryWrapper<UserAccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSet::getUserId, userId)
               .eq(UserAccountSet::getAccountSetId, accountSetId);
        UserAccountSet existing = userAccountSetMapper.selectOne(wrapper);
        if (existing == null) {
            return;
        }
        if ("OWNER".equals(existing.getRoleType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能移除账套所有者,请先转移所有权");
        }
        userAccountSetMapper.deleteById(existing.getId());
        log.info("移除账套访问权限: 账套ID={}, 用户ID={}", accountSetId, userId);
    }

    @Override
    public List<UserAccountSetVO> listAccountSetUsers(Long accountSetId) {
        // 需对该账套有访问权
        checkAccess(accountSetId);
        if (accountSetId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账套ID不能为空");
        }
        // 查询账套下所有用户关系
        LambdaQueryWrapper<UserAccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSet::getAccountSetId, accountSetId);
        List<UserAccountSet> relations = userAccountSetMapper.selectList(wrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查询用户信息组装VO
        Set<Long> userIds = relations.stream().map(UserAccountSet::getUserId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        return relations.stream().map(rel -> {
            UserAccountSetVO vo = new UserAccountSetVO();
            vo.setId(rel.getId());
            vo.setUserId(rel.getUserId());
            vo.setAccountSetId(rel.getAccountSetId());
            vo.setRoleType(rel.getRoleType());
            SysUser user = userMap.get(rel.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<UserAccountSetVO> listUserAccountSets(Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        // 管理员可查任意用户,普通用户只能查自己
        if (!isSuperAdmin() && !currentUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能查询自己的账套权限");
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        // 查询用户所有账套关系
        LambdaQueryWrapper<UserAccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSet::getUserId, userId);
        List<UserAccountSet> relations = userAccountSetMapper.selectList(wrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量查询账套信息组装VO
        Set<Long> accountSetIds = relations.stream().map(UserAccountSet::getAccountSetId).collect(Collectors.toSet());
        Map<Long, AccountSet> accountSetMap = accountSetMapper.selectBatchIds(accountSetIds).stream()
                .collect(Collectors.toMap(AccountSet::getId, a -> a, (a, b) -> a));
        return relations.stream().map(rel -> {
            UserAccountSetVO vo = new UserAccountSetVO();
            vo.setId(rel.getId());
            vo.setUserId(rel.getUserId());
            vo.setAccountSetId(rel.getAccountSetId());
            vo.setRoleType(rel.getRoleType());
            AccountSet accountSet = accountSetMap.get(rel.getAccountSetId());
            if (accountSet != null) {
                vo.setAccountSetName(accountSet.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private boolean hasRelation(Long userId, Long accountSetId) {
        LambdaQueryWrapper<UserAccountSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAccountSet::getUserId, userId)
               .eq(UserAccountSet::getAccountSetId, accountSetId);
        return userAccountSetMapper.selectCount(wrapper) > 0;
    }
}
