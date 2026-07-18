package com.company.daizhang.common.config;

import com.company.daizhang.common.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (StringUtils.hasText(token) && !jwtUtils.isTokenExpired(token)) {
            // B-022 修复:拒绝 refresh token 用于业务接口访问。
            // refresh token 有效期长达7天,若被当作 access token 使用,等同于变相延长 access token 有效期,
            // 丢失"短 access + 长 refresh"分层失效的安全收益。refresh token 仅可用于 /auth/refresh 端点。
            if (jwtUtils.isRefreshToken(token)) {
                log.warn("refresh token 被用于业务接口访问,已拒绝: {}", request.getRequestURI());
            } else if (tokenBlacklist.contains(token)) {
                // 黑名单校验:登出时 token 会被加入黑名单,此处拒绝已登出的 token
                // (JWT 无状态,否则登出后 token 在过期前仍可正常访问所有受保护接口)
                log.warn("token 已登出(在黑名单中),访问被拒绝");
            } else {
                try {
                    String username = jwtUtils.getUsername(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 校验用户状态:被禁用/删除的用户即使持有有效token也不允许访问
                    // (JWT无状态,需在filter层主动校验isEnabled,否则禁用用户的token仍可用)
                    if (!userDetails.isEnabled()) {
                        log.warn("用户{}已被禁用,token访问被拒绝", username);
                    } else {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (Exception e) {
                    log.error("JWT认证失败: {}", e.getMessage());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
