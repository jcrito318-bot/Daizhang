package com.company.daizhang.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtils {

    /**
     * 已知弱密钥黑名单,启动时拒绝使用,防止生产环境误用默认值
     */
    private static final String[] KNOWN_WEAK_SECRETS = {
            "daizhang-secret-key-must-be-at-least-32-bytes-long",
            "daizhang-system-secret-key-2026-must-be-at-least-256-bits-long"
    };

    /**
     * 不再提供弱默认值。配置缺失时启动直接失败,强制通过环境变量 JWT_SECRET 注入。
     */
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400}")
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800}")
    private Long refreshExpiration;

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("jwt.secret 未配置,请通过环境变量 JWT_SECRET 注入不少于32字节的随机密钥");
        }
        // 长度不足属于配置错误,直接拒绝启动
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret 长度不足32字节(HS256要求),当前长度: "
                    + secret.getBytes(StandardCharsets.UTF_8).length);
        }
        // 已知弱密钥(代码库默认值)直接拒绝启动,防止生产环境误用公开弱密钥导致JWT可被任意伪造
        for (String weak : KNOWN_WEAK_SECRETS) {
            if (weak.equals(secret)) {
                log.warn("==========================================================");
                log.warn("警告: jwt.secret 使用了已知弱密钥(代码库默认值),存在JWT伪造风险!");
                log.warn("请通过环境变量 JWT_SECRET 注入随机密钥(>=32字节)。");
                log.warn("==========================================================");
                throw new IllegalStateException("jwt.secret 使用了已知弱密钥，请通过环境变量 JWT_SECRET 设置随机密钥(>=32字节)");
            }
        }
        log.info("JWT密钥校验通过(长度{}字节)", secret.getBytes(StandardCharsets.UTF_8).length);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成refresh token,有效期由 jwt.refresh-expiration 配置(默认7天)。
     * claims 中加 type=refresh 以与 accessToken 区分。
     */
    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        Claims claims = validateToken(token);
        return claims.get("userId", Long.class);
    }

    public String getUsername(String token) {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 获取token的过期时间戳(毫秒),用于黑名单TTL设置。
     * @return 过期时间戳;解析失败返回0
     */
    public long getExpirationMillis(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().getTime();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * B-022 修复:判断 token 是否为 refresh token。
     * refresh token 在 claims 中带 type=refresh 标识,有效期长达7天;
     * 若被当作 access token 用于业务接口访问,等同于变相延长 access token 有效期,
     * 丢失了"短 access token + 长 refresh token"分层失效的安全收益。
     *
     * @param token JWT
     * @return true=refresh token,false=access token 或解析失败
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }
}

