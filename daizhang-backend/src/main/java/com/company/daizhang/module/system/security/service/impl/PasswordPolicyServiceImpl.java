package com.company.daizhang.module.system.security.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.system.security.entity.PasswordHistory;
import com.company.daizhang.module.system.security.mapper.PasswordHistoryMapper;
import com.company.daizhang.module.system.security.service.PasswordPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 密码策略服务实现 (P4.3)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordPolicyServiceImpl implements PasswordPolicyService {

    /** 大写字母 */
    private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
    /** 小写字母 */
    private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
    /** 数字 */
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");

    /** 常见弱密码黑名单(小写比对) */
    private static final Set<String> WEAK_PASSWORDS = new HashSet<>();

    static {
        // 常见弱密码黑名单
        String[] weak = {
                "123456", "12345678", "123456789", "1234567890", "111111", "000000",
                "password", "password1", "password123", "admin", "admin123", "administrator",
                "root", "toor", "qwerty", "qwerty123", "abc123", "letmein", "welcome",
                "welcome1", "monkey", "dragon", "master", "login", "passw0rd",
                "iloveyou", "sunshine", "princess", "football", "baseball",
                "1q2w3e4r", "1qaz2wsx", "qazwsx", "zxcvbnm", "asdfghjkl"
        };
        for (String w : weak) {
            WEAK_PASSWORDS.add(w);
        }
    }

    /** 密码策略开关 */
    @Value("${app.security.password-policy-enabled:true}")
    private boolean passwordPolicyEnabled;

    /** 密码历史校验深度(不能与最近 N 次相同) */
    @Value("${app.security.password-history-depth:5}")
    private int passwordHistoryDepth;

    /** 密码过期天数(0 表示不强制) */
    @Value("${app.security.password-expire-days:90}")
    private int passwordExpireDays;

    private final PasswordHistoryMapper passwordHistoryMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void validatePassword(String plainPassword, String username) {
        if (!passwordPolicyEnabled) {
            return;
        }
        if (StrUtil.isBlank(plainPassword)) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_TOO_SHORT);
        }
        // 长度 >= 8
        if (plainPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码长度不能少于8位");
        }
        // 必须包含大写字母 + 小写字母 + 数字
        if (!UPPER.matcher(plainPassword).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码必须包含大写字母");
        }
        if (!LOWER.matcher(plainPassword).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码必须包含小写字母");
        }
        if (!DIGIT.matcher(plainPassword).matches()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码必须包含数字");
        }
        // 不能是常见弱密码
        if (WEAK_PASSWORDS.contains(plainPassword.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码过于简单,不能使用常见弱密码");
        }
        // 不能与用户名相同
        if (StrUtil.isNotBlank(username) && plainPassword.equalsIgnoreCase(username)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能与用户名相同");
        }
    }

    @Override
    public void validatePasswordWithHistory(Long userId, String plainPassword, String username) {
        // 先校验强度
        validatePassword(plainPassword, username);
        if (!passwordPolicyEnabled || userId == null) {
            return;
        }
        // 新密码不能与最近 N 次历史密码相同
        List<PasswordHistory> histories = passwordHistoryMapper.selectList(
                new LambdaQueryWrapper<PasswordHistory>()
                        .eq(PasswordHistory::getUserId, userId)
                        .orderByDesc(PasswordHistory::getCreateTime)
                        .last("LIMIT " + passwordHistoryDepth));
        for (PasswordHistory h : histories) {
            if (passwordEncoder.matches(plainPassword, h.getPasswordHash())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "新密码不能与最近" + passwordHistoryDepth + "次使用过的密码相同");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPasswordHistory(Long userId, String passwordHash) {
        if (userId == null || StrUtil.isBlank(passwordHash)) {
            return;
        }
        PasswordHistory history = new PasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        history.setCreateTime(LocalDateTime.now());
        passwordHistoryMapper.insert(history);

        // 清理超出历史深度的旧记录(保留最近 N 条)
        cleanupExcessHistory(userId);
    }

    /**
     * 清理超出保留深度的历史密码记录,仅保留最近 passwordHistoryDepth 条。
     */
    private void cleanupExcessHistory(Long userId) {
        List<PasswordHistory> all = passwordHistoryMapper.selectList(
                new LambdaQueryWrapper<PasswordHistory>()
                        .eq(PasswordHistory::getUserId, userId)
                        .orderByDesc(PasswordHistory::getCreateTime));
        if (all.size() <= passwordHistoryDepth) {
            return;
        }
        // 删除超出部分(最旧的)
        List<PasswordHistory> toDelete = all.subList(passwordHistoryDepth, all.size());
        for (PasswordHistory h : toDelete) {
            passwordHistoryMapper.deleteById(h.getId());
        }
    }

    @Override
    public boolean isPasswordExpired(Long userId) {
        if (!passwordPolicyEnabled || passwordExpireDays <= 0 || userId == null) {
            return false;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        LocalDateTime changedAt = user.getPasswordChangedAt();
        if (changedAt == null) {
            // 无改密记录(老用户),视为未过期(避免一上线就强制全员改密)
            return false;
        }
        long days = ChronoUnit.DAYS.between(changedAt, LocalDateTime.now());
        return days > passwordExpireDays;
    }
}
