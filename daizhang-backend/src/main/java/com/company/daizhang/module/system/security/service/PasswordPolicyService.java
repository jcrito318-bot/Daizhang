package com.company.daizhang.module.system.security.service;

/**
 * 密码策略服务 (P4.3)
 * <p>
 * 校验密码强度,维护密码历史,防止弱密码与密码复用。
 */
public interface PasswordPolicyService {

    /**
     * 校验密码强度(不涉及历史)。
     * <ul>
     *     <li>长度 >= 8</li>
     *     <li>必须包含大写字母 + 小写字母 + 数字</li>
     *     <li>不能是常见弱密码(123456/admin123/password 等黑名单)</li>
     *     <li>不能与用户名相同</li>
     * </ul>
     *
     * @param plainPassword 明文密码
     * @param username      用户名(用于"不能与用户名相同"校验),可为空
     * @throws com.company.daizhang.common.exception.BusinessException 校验不通过
     */
    void validatePassword(String plainPassword, String username);

    /**
     * 校验密码强度 + 密码历史(不能与最近 N 次历史密码相同)。
     *
     * @param userId        用户ID
     * @param plainPassword 明文密码
     * @param username      用户名
     * @throws com.company.daizhang.common.exception.BusinessException 校验不通过
     */
    void validatePasswordWithHistory(Long userId, String plainPassword, String username);

    /**
     * 记录一条密码历史(改密时调用)。
     *
     * @param userId       用户ID
     * @param passwordHash 旧密码 hash(BCrypt)
     */
    void recordPasswordHistory(Long userId, String passwordHash);

    /**
     * 判断密码是否已过期(距上次改密超过配置天数)。
     *
     * @param userId 用户ID
     * @return true=已过期,需强制改密
     */
    boolean isPasswordExpired(Long userId);
}
