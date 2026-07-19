package com.company.daizhang.common.config;

import com.company.daizhang.common.utils.JwtUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 单元测试 (BUG-后端 修复验证)
 * <p>
 * 修复点:原 catch (Exception e) 范围过大,会吞噬 NullPointerException 等运行时异常,
 * 隐藏真实代码缺陷。修复后缩小为 JwtException 和 UsernameNotFoundException,
 * 并在异常分支调用 SecurityContextHolder.clearContext()。
 * <p>
 * 测试聚焦验证:
 * 1. JwtException 时 SecurityContext 被清空
 * 2. UsernameNotFoundException 时 SecurityContext 被清空
 * 3. 其他 RuntimeException(如 NPE)不被捕获,向上抛出
 * 4. 有效 token 时正确设置 SecurityContext
 * 5. refresh token 被拒绝(不调 userDetailsService)
 * 6. 黑名单 token 被拒绝
 * 7. 被禁用用户(isEnabled=false)不设置 SecurityContext
 * 8. 无 token 时不触发任何校验
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private TokenBlacklist tokenBlacklist;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        // 每个测试前清空 SecurityContext,避免上一个测试残留
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubBearer(String token) {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
    }

    /**
     * 修复点验证:JwtException 应被 catch 且 SecurityContext 被清空。
     * 修复前 catch(Exception) 不会清空 context,导致可能残留旧的认证信息。
     */
    @Test
    void doFilterInternal_jwtException_shouldClearSecurityContext() throws Exception {
        String token = "invalid.jwt.token";
        stubBearer(token);
        when(jwtUtils.isTokenExpired(token)).thenReturn(false);
        when(jwtUtils.isRefreshToken(token)).thenReturn(false);
        when(tokenBlacklist.contains(token)).thenReturn(false);
        // 模拟 jwtUtils.getUsername 抛 JwtException(签名错误/格式错误等)
        when(jwtUtils.getUsername(token)).thenThrow(new JwtException("signed JWT parse failed"));

        filter.doFilterInternal(request, response, filterChain);

        // 关键断言:SecurityContext 应被清空(没有 authentication)
        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "JwtException 时 SecurityContext 必须被清空");
        // filterChain 应继续执行(让后续 AuthenticationEntryPoint 处理)
        verify(filterChain).doFilter(request, response);
    }

    /**
     * 修复点验证:UsernameNotFoundException(用户已被物理删除)应被 catch 且清空 context。
     */
    @Test
    void doFilterInternal_usernameNotFoundException_shouldClearSecurityContext() throws Exception {
        String token = "valid.token.but.user.deleted";
        stubBearer(token);
        when(jwtUtils.isTokenExpired(token)).thenReturn(false);
        when(jwtUtils.isRefreshToken(token)).thenReturn(false);
        when(tokenBlacklist.contains(token)).thenReturn(false);
        when(jwtUtils.getUsername(token)).thenReturn("ghost_user");
        when(userDetailsService.loadUserByUsername("ghost_user"))
                .thenThrow(new UsernameNotFoundException("用户不存在: ghost_user"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "UsernameNotFoundException 时 SecurityContext 必须被清空");
        verify(filterChain).doFilter(request, response);
    }

    /**
     * 修复点验证:其他 RuntimeException(如 NullPointerException)不被捕获,向上抛出。
     * 修复前 catch(Exception) 会吞噬这类异常,隐藏代码缺陷。
     */
    @Test
    void doFilterInternal_runtimeException_shouldNotBeCaught() {
        String token = "valid.token.but.code.bug";
        stubBearer(token);
        when(jwtUtils.isTokenExpired(token)).thenReturn(false);
        when(jwtUtils.isRefreshToken(token)).thenReturn(false);
        when(tokenBlacklist.contains(token)).thenReturn(false);
        when(jwtUtils.getUsername(token)).thenReturn("alice");
        // 模拟 userDetailsService 内部抛出 NPE(典型代码缺陷)
        when(userDetailsService.loadUserByUsername("alice"))
                .thenThrow(new NullPointerException("mapper null pointer bug"));

        // 应向上抛出,不被 filter 内的 catch 吞噬
        assertThrows(NullPointerException.class, () -> filter.doFilterInternal(request, response, filterChain));
    }

    /**
     * 验证有效 token 时正确设置 SecurityContext。
     */
    @Test
    void doFilterInternal_validToken_shouldSetAuthentication() throws Exception {
        String token = "valid.jwt.token";
        stubBearer(token);
        when(jwtUtils.isTokenExpired(token)).thenReturn(false);
        when(jwtUtils.isRefreshToken(token)).thenReturn(false);
        when(tokenBlacklist.contains(token)).thenReturn(false);
        when(jwtUtils.getUsername(token)).thenReturn("alice");
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "有效 token 时 SecurityContext 必须被设置");
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    /**
     * 验证 refresh token 被拒绝(不调 userDetailsService,不设置 context)。
     * 这是 B-022 修复:refresh token 仅可用于 /auth/refresh 端点。
     */
    @Test
    void doFilterInternal_refreshToken_shouldRejectAndNotSetContext() throws Exception {
        String token = "refresh.token";
        stubBearer(token);
        when(jwtUtils.isTokenExpired(token)).thenReturn(false);
        when(jwtUtils.isRefreshToken(token)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "refresh token 不应设置 SecurityContext");
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    /**
     * 验证黑名单 token(已登出)被拒绝。
     */
    @Test
    void doFilterInternal_blacklistedToken_shouldRejectAndNotSetContext() throws Exception {
        String token = "blacklisted.token";
        stubBearer(token);
        when(jwtUtils.isTokenExpired(token)).thenReturn(false);
        when(jwtUtils.isRefreshToken(token)).thenReturn(false);
        when(tokenBlacklist.contains(token)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "黑名单 token 不应设置 SecurityContext");
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    /**
     * 验证被禁用用户(isEnabled=false)的 token 被拒绝。
     */
    @Test
    void doFilterInternal_disabledUser_shouldNotSetContext() throws Exception {
        String token = "valid.but.disabled.user";
        stubBearer(token);
        when(jwtUtils.isTokenExpired(token)).thenReturn(false);
        when(jwtUtils.isRefreshToken(token)).thenReturn(false);
        when(tokenBlacklist.contains(token)).thenReturn(false);
        when(jwtUtils.getUsername(token)).thenReturn("banned");
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.isEnabled()).thenReturn(false);
        when(userDetailsService.loadUserByUsername("banned")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "被禁用用户不应设置 SecurityContext");
        verify(filterChain).doFilter(request, response);
    }

    /**
     * 验证无 Authorization 头时不触发任何校验。
     */
    @Test
    void doFilterInternal_noAuthHeader_shouldNotInteractWithJwt() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtils);
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    /**
     * 验证 Authorization 头格式不正确(不以 "Bearer " 开头)时也不触发校验。
     */
    @Test
    void doFilterInternal_nonBearerHeader_shouldNotInteractWithJwt() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtUtils);
        verify(filterChain).doFilter(request, response);
    }

    /**
     * 验证 token 已过期时不进入校验分支。
     */
    @Test
    void doFilterInternal_expiredToken_shouldNotSetContext() throws Exception {
        String token = "expired.token";
        stubBearer(token);
        when(jwtUtils.isTokenExpired(token)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }
}
