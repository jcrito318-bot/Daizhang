package com.company.daizhang.common.utils;

import com.company.daizhang.common.config.SecurityUserDetails;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类
 */
public class SecurityUtils {

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUserDetails) {
            return ((SecurityUserDetails) principal).getUserId();
        }
        return null;
    }

    /**
     * 获取当前登录用户ID(必须已登录)。
     * <p>
     * 与 {@link #getCurrentUserId()} 不同,本方法在未登录或principal非SecurityUserDetails时
     * 直接抛出 {@link BusinessException}(被全局异常处理器捕获返回401),
     * 避免调用方(如创建账套后绑定OWNER)因拿到null而静默跳过关键绑定逻辑。
     */
    public static Long getCurrentUserIdRequired() {
        Authentication authentication = getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof SecurityUserDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }
        return ((SecurityUserDetails) principal).getUserId();
    }

    public static String getCurrentUsername() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof SecurityUserDetails) {
            return ((SecurityUserDetails) principal).getUsername();
        }
        return null;
    }
}
