package com.company.daizhang.module.system.security.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.module.system.security.entity.LoginAttempt;
import com.company.daizhang.module.system.security.mapper.LoginAttemptMapper;
import com.company.daizhang.module.system.security.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 登录尝试锁定服务实现 (P4.3)
 * <p>
 * 基于 login_attempt 表统计时间窗口内的失败次数。锁定窗口 = lock-duration-minutes,
 * 失败阈值 = login-fail-threshold。成功登录后清除该用户名的全部失败记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    /** 登录失败阈值(默认 5 次) */
    @Value("${app.security.login-fail-threshold:5}")
    private int failThreshold;

    /** 锁定时长(分钟,默认 15) */
    @Value("${app.security.lock-duration-minutes:15}")
    private int lockDurationMinutes;

    private final LoginAttemptMapper loginAttemptMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFailure(String username, String ip) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        // 大小写归一化,防止大小写变体绕过锁定计数
        username = username.toLowerCase(Locale.ROOT);
        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setAttemptTime(LocalDateTime.now());
        attempt.setIp(ip);
        attempt.setSuccess(0);
        loginAttemptMapper.insert(attempt);

        int remaining = getRemainingAttempts(username);
        if (remaining <= 0) {
            log.warn("用户 {} 登录失败次数达到阈值 {} 次,已锁定 {} 分钟", username, failThreshold, lockDurationMinutes);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSuccess(String username) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        // 大小写归一化,与 recordFailure/isLocked 使用同一 key
        username = username.toLowerCase(Locale.ROOT);
        // 成功登录后清除该用户名的全部失败记录
        loginAttemptMapper.delete(
                new LambdaQueryWrapper<LoginAttempt>()
                        .eq(LoginAttempt::getUsername, username)
                        .eq(LoginAttempt::getSuccess, 0));
    }

    @Override
    public boolean isLocked(String username) {
        if (StrUtil.isBlank(username)) {
            return false;
        }
        // 大小写归一化,与 recordFailure 使用同一 key
        username = username.toLowerCase(Locale.ROOT);
        return countRecentFailures(username) >= failThreshold;
    }

    @Override
    public int getRemainingAttempts(String username) {
        if (StrUtil.isBlank(username)) {
            return failThreshold;
        }
        // 大小写归一化,与 recordFailure/isLocked 使用同一 key
        username = username.toLowerCase(Locale.ROOT);
        int failures = countRecentFailures(username);
        int remaining = failThreshold - failures;
        return Math.max(remaining, 0);
    }

    /**
     * 统计锁定窗口内的失败次数。
     */
    private int countRecentFailures(String username) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(lockDurationMinutes);
        Long count = loginAttemptMapper.selectCount(
                new LambdaQueryWrapper<LoginAttempt>()
                        .eq(LoginAttempt::getUsername, username)
                        .eq(LoginAttempt::getSuccess, 0)
                        .ge(LoginAttempt::getAttemptTime, since));
        return count == null ? 0 : count.intValue();
    }
}
