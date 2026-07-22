package com.company.daizhang.module.system.security.service;

/**
 * 登录尝试锁定服务 (P4.3)
 * <p>
 * 记录登录失败尝试,达到阈值(默认 5 次/15 分钟)后锁定账户。
 */
public interface LoginAttemptService {

    /**
     * 记录一次失败登录尝试。
     *
     * @param username 用户名
     * @param ip       客户端IP
     */
    void recordFailure(String username, String ip);

    /**
     * 记录一次成功登录,清除该用户名的失败记录。
     *
     * @param username 用户名
     */
    void recordSuccess(String username);

    /**
     * 检查用户是否被锁定(15 分钟内失败 >= 阈值)。
     *
     * @param username 用户名
     * @return true=已锁定
     */
    boolean isLocked(String username);

    /**
     * 获取剩余尝试次数(达到阈值前还能尝试几次)。
     *
     * @param username 用户名
     * @return 剩余次数,>=0
     */
    int getRemainingAttempts(String username);
}
