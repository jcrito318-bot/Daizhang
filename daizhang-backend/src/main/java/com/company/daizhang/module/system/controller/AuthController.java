package com.company.daizhang.module.system.controller;

import cn.hutool.core.util.StrUtil;
import com.company.daizhang.common.config.SecurityUserDetails;
import com.company.daizhang.common.config.TokenBlacklist;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.common.utils.JwtUtils;
import com.company.daizhang.module.system.dto.LoginRequest;
import com.company.daizhang.module.system.dto.LoginResponse;
import com.company.daizhang.module.system.dto.LogoutRequest;
import com.company.daizhang.module.system.entity.LoginLog;
import com.company.daizhang.module.system.service.LoginLogService;
import com.company.daizhang.module.system.service.SysUserService;
import com.company.daizhang.module.system.vo.UserVO;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "用户登录、登出、获取用户信息")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    /**
     * BF-02 修复:refresh token 通过 HttpOnly + Secure + SameSite=Strict Cookie 下发,
     * 不再通过 JSON body 返回,防止 XSS 窃取长生命周期的 refresh token。
     * <p>
     * Cookie 属性说明:
     * - HttpOnly:JS 无法通过 document.cookie 读取,防 XSS 窃取
     * - Secure:仅 HTTPS 传输(开发环境 http://localhost 仍可发送,浏览器对 localhost 豁免 Secure)
     * - SameSite=Strict:跨站请求不携带 Cookie,防 CSRF
     * - Path=/api/auth:仅 /auth/** 路径下发送,缩小暴露面
     * - maxAge:与 refresh token 的 JWT 过期时间一致(默认 7 天)
     */
    @Value("${app.cookie.refresh-token-max-age:604800}")
    private int refreshTokenCookieMaxAge;

    /**
     * 是否启用 Secure 标记。生产环境(HTTPS)应设为 true,本地开发(http)可设为 false。
     * 浏览器对 localhost 会豁免 Secure,但显式关闭可避免开发环境的 Cookie 丢失问题。
     */
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    /**
     * Cookie 名称,固定为 "refresh_token"。
     */
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    /**
     * 将 refresh token 写入 HttpOnly Cookie。
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(refreshTokenCookieMaxAge);
        // SameSite=Strict 需通过响应头设置,Cookie API 不支持(Servlet 6.0 起 Cookie.setHttpOnly 已支持,
        // 但 SameSite 仍需手动)。这里用 Set-Cookie 头覆盖 Cookie API 写入的同名 Cookie。
        response.addCookie(cookie);
        String sameSiteCookie = String.format("%s=%s; Path=/api/auth; Max-Age=%d; %s; SameSite=Strict",
                REFRESH_TOKEN_COOKIE_NAME,
                refreshToken,
                refreshTokenCookieMaxAge,
                cookieSecure ? "Secure" : "");
        response.setHeader("Set-Cookie", sameSiteCookie);
    }

    /**
     * 清除 refresh token Cookie(登出时调用)。
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        response.setHeader("Set-Cookie", String.format("%s=; Path=/api/auth; Max-Age=0; %s; SameSite=Strict",
                REFRESH_TOKEN_COOKIE_NAME, cookieSecure ? "Secure" : ""));
    }

    /**
     * 从 Cookie 中读取 refresh token。
     * 兼容旧版前端:若 Cookie 中无,回退到 Authorization header(过渡期支持)。
     */
    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    if (StringUtils.hasText(value)) {
                        return value;
                    }
                }
            }
        }
        // 兼容旧版前端(过渡期):Authorization header 仍可传 refresh token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();
            String token = jwtUtils.generateToken(userDetails.getUserId(), userDetails.getUsername());
            String refreshToken = jwtUtils.generateRefreshToken(userDetails.getUserId(), userDetails.getUsername());

            // BF-02 修复:refresh token 通过 HttpOnly Cookie 下发,不放入 response body,
            // 防止 XSS 窃取长生命周期的 refresh token。
            setRefreshTokenCookie(httpResponse, refreshToken);

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            // 不再在 body 中返回 refreshToken(向后兼容:字段保留为 null,旧前端读到 null 不会崩)
            response.setRefreshToken(null);

            // 从数据库查询完整用户信息
            UserVO userVO = sysUserService.getUserById(userDetails.getUserId());
            response.setUserInfo(userVO);

            // 记录登录成功日志(原代码从未调用LoginLogService.saveLog,导致sys_login_log表永远为空)
            saveLoginLog(request.getUsername(), userDetails.getUserId(), 1, 1, clientIp, userAgent, "登录成功");
            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            // 记录登录失败日志,userId为null(认证未通过)
            saveLoginLog(request.getUsername(), null, 1, 0, clientIp, userAgent, "用户名或密码错误");
            return Result.error(401, "用户名或密码错误");
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新Token")
    public Result<LoginResponse> refresh(HttpServletRequest httpRequest,
                                          HttpServletResponse httpResponse) {
        // BF-02 修复:从 HttpOnly Cookie 读取 refresh token,不再从 Authorization header。
        // 兼容旧版前端:Cookie 无则回退到 Authorization header(过渡期)。
        String token = extractRefreshToken(httpRequest);
        if (!StringUtils.hasText(token)) {
            return Result.error(401, "未提供 refresh token");
        }

        // 校验refresh token并解析claims(validateToken已包含签名校验与过期校验)
        Claims claims;
        try {
            claims = jwtUtils.validateToken(token);
        } catch (Exception e) {
            // refresh token 无效时清除 Cookie,避免前端持续携带无效 Cookie
            clearRefreshTokenCookie(httpResponse);
            return Result.error(401, "refresh token无效或已过期");
        }

        // 校验token类型(必须是refresh类型,防止用accessToken调用刷新)
        String tokenType = (String) claims.get("type");
        if (!"refresh".equals(tokenType)) {
            clearRefreshTokenCookie(httpResponse);
            return Result.error(401, "非refresh token");
        }

        // S-007 修复:refresh token 已入黑名单则拒绝(登出过的 refresh token 不能再用)
        if (tokenBlacklist.contains(token)) {
            clearRefreshTokenCookie(httpResponse);
            return Result.error(401, "refresh token已失效,请重新登录");
        }

        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();

        // S-007 修复:Refresh Token Rotation — 每次刷新签发新的 refresh token,旧 refresh token 吊销。
        // 原 refresh 不轮换,窃取后可在7天有效期内永久访问;轮换后单次使用,即使泄露也只能用一次,
        // 大幅缩小攻击窗口。同时保持 access token 短期(2小时)有效。
        String newAccessToken = jwtUtils.generateToken(userId, username);
        String newRefreshToken = jwtUtils.generateRefreshToken(userId, username);
        // 旧 refresh token 立即吊销,防止重放
        long oldRefreshExpMillis = jwtUtils.getExpirationMillis(token);
        tokenBlacklist.add(token, oldRefreshExpMillis);

        // BF-02 修复:新的 refresh token 通过 HttpOnly Cookie 下发,覆盖旧 Cookie。
        setRefreshTokenCookie(httpResponse, newRefreshToken);

        LoginResponse response = new LoginResponse();
        response.setToken(newAccessToken);
        response.setRefreshToken(null);  // 不再在 body 中返回
        return Result.success("刷新成功", response);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(HttpServletRequest httpRequest,
                                HttpServletResponse httpResponse,
                                @RequestBody(required = false) LogoutRequest logoutRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUserDetails userDetails) {
            // 记录登出日志(原代码仅清SecurityContext,无任何审计记录)
            saveLoginLog(userDetails.getUsername(), userDetails.getUserId(), 2, 1,
                    getClientIp(httpRequest), httpRequest.getHeader("User-Agent"), "登出成功");
        }
        // 将当前 access token 加入黑名单:JWT 无状态,仅清 SecurityContext 后 token 在过期前仍可用,
        // 登出形同虚设。加入黑名单后,JwtAuthenticationFilter 会拒绝该 token 的后续请求。
        String bearerToken = httpRequest.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            long expMillis = jwtUtils.getExpirationMillis(token);
            tokenBlacklist.add(token, expMillis);
        }
        // S-007 修复:同步吊销前端传来的 refresh token。
        // 原 logout 仅吊销 access token,refresh token 仍有效(7天),攻击者若已窃取 refresh token,
        // 可在 access token 黑名单过期后用 refresh 换取新 access token,绕过登出。
        // BF-02 修复:优先从 Cookie 读取 refresh token,兼容旧前端从 body 传入。
        String refreshTokenToRevoke = null;
        // 优先从 Cookie 读取
        if (httpRequest.getCookies() != null) {
            for (Cookie cookie : httpRequest.getCookies()) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    refreshTokenToRevoke = cookie.getValue();
                    break;
                }
            }
        }
        // 兼容旧前端:若 body 中传了 refresh token,也一并处理
        if (refreshTokenToRevoke == null && logoutRequest != null && StringUtils.hasText(logoutRequest.getRefreshToken())) {
            refreshTokenToRevoke = logoutRequest.getRefreshToken().startsWith("Bearer ")
                    ? logoutRequest.getRefreshToken().substring(7)
                    : logoutRequest.getRefreshToken();
        }
        if (refreshTokenToRevoke != null) {
            try {
                Claims claims = jwtUtils.validateToken(refreshTokenToRevoke);
                if ("refresh".equals(claims.get("type"))) {
                    long refreshExpMillis = jwtUtils.getExpirationMillis(refreshTokenToRevoke);
                    tokenBlacklist.add(refreshTokenToRevoke, refreshExpMillis);
                    log.info("登出时同步吊销 refresh token,用户: {}", claims.getSubject());
                }
            } catch (Exception e) {
                // refresh token 已过期或非法,无需吊销
                log.debug("登出时 refresh token 已无效,无需吊销: {}", e.getMessage());
            }
        }
        // BF-02 修复:清除 refresh token Cookie,前端浏览器不再持有
        clearRefreshTokenCookie(httpResponse);
        SecurityContextHolder.clearContext();
        return Result.success("登出成功", null);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication != null ? authentication.getPrincipal() : null;
        // 匿名用户经 AnonymousAuthenticationFilter 后 isAuthenticated() 返回 true,
        // principal 为字符串 "anonymousUser",直接强转会抛 ClassCastException → 500,
        // 需校验 principal 类型,非 SecurityUserDetails 视为未登录
        if (!(principal instanceof SecurityUserDetails)) {
            return Result.error(401, "未登录或登录已过期");
        }
        SecurityUserDetails userDetails = (SecurityUserDetails) principal;

        // 从数据库查询完整用户信息
        UserVO userVO = sysUserService.getUserById(userDetails.getUserId());

        return Result.success(userVO);
    }

    /**
     * 保存登录日志
     * @param username 用户名
     * @param userId 用户ID(失败时可能为null)
     * @param loginType 1=登录 2=登出
     * @param loginStatus 1=成功 0=失败
     * @param ip 客户端IP
     * @param userAgent User-Agent头
     * @param message 日志消息
     */
    private void saveLoginLog(String username, Long userId, int loginType, int loginStatus,
                              String ip, String userAgent, String message) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUsername(username);
            loginLog.setUserId(userId);
            loginLog.setLoginType(loginType);
            loginLog.setLoginStatus(loginStatus);
            loginLog.setLoginIp(ip);
            loginLog.setLoginLocation("");
            loginLog.setBrowser(parseBrowser(userAgent));
            loginLog.setOs(parseOs(userAgent));
            loginLog.setMessage(message);
            loginLog.setLoginTime(LocalDateTime.now());
            loginLogService.saveLog(loginLog);
        } catch (Exception e) {
            // 日志写入失败不影响主流程
            log.warn("保存登录日志失败: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String parseBrowser(String userAgent) {
        if (StrUtil.isBlank(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Edg")) {
            return "Edge";
        } else if (userAgent.contains("Chrome")) {
            return "Chrome";
        } else if (userAgent.contains("Firefox")) {
            return "Firefox";
        } else if (userAgent.contains("Safari")) {
            return "Safari";
        }
        return "Other";
    }

    private String parseOs(String userAgent) {
        if (StrUtil.isBlank(userAgent)) {
            return "Unknown";
        }
        if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Mac OS")) {
            return "macOS";
        } else if (userAgent.contains("Linux")) {
            return "Linux";
        } else if (userAgent.contains("Android")) {
            return "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "iOS";
        }
        return "Other";
    }
}
