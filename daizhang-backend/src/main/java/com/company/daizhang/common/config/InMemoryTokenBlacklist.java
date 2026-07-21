package com.company.daizhang.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT token 黑名单(内存版,默认实现)。
 *
 * <p>用于解决 JWT 无状态登出无效问题:登出时将 token 加入黑名单,
 * {@code JwtAuthenticationFilter} 在校验 token 时拒绝黑名单中的 token。</p>
 *
 * <p>BV-06 修复:从原 {@code TokenBlacklist} 具体类抽象为接口实现,通过条件装配支持多种后端存储。
 * 默认启用内存实现({@code app.token-blacklist.type=memory} 或未配置时),适用于单实例部署。
 * 集群部署需切换为 Redis 实现(见 {@link TokenBlacklist} 接口文档)。</p>
 *
 * <p>限制说明:
 * <ul>
 *   <li>内存实现,仅适用于单实例部署(本项目使用 H2 嵌入式数据库,本身即为单实例)。
 *       多实例部署需替换为 Redis 等共享存储,并以 token 的 jti+TTL 为键。</li>
 *   <li>重启后黑名单丢失,但被登出的 token 仍受 expiration 限制,风险窗口有限。</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.token-blacklist.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenBlacklist implements TokenBlacklist {

    /**
     * key: token字符串
     * value: 过期时间戳(毫秒),过期后自动清理
     */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void add(String token, long expirationMillis) {
        if (token == null || token.isEmpty()) {
            return;
        }
        // 若 token 已过期,无需加入黑名单
        long now = System.currentTimeMillis();
        if (expirationMillis > 0 && expirationMillis <= now) {
            return;
        }
        // 未提供过期时间时,兜底保留 24 小时
        long ttl = expirationMillis > 0 ? expirationMillis : now + 24L * 3600 * 1000;
        blacklist.put(token, ttl);
        log.info("token 已加入黑名单(将于 {} 过期),当前黑名单大小: {}", ttl, blacklist.size());
    }

    @Override
    public boolean contains(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        Long exp = blacklist.get(token);
        if (exp == null) {
            return false;
        }
        // 惰性清理:若已过期,移除并返回 false
        if (exp <= System.currentTimeMillis()) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    /**
     * 定时清理过期条目,每 10 分钟一次,防止内存无限增长。
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();
        Iterator<Map.Entry<String, Long>> it = blacklist.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() <= now) {
                it.remove();
            }
        }
        int removed = before - blacklist.size();
        if (removed > 0) {
            log.info("清理过期黑名单 token {} 条,剩余: {}", removed, blacklist.size());
        }
    }
}
