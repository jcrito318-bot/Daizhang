package com.company.daizhang.common.config;

/**
 * Token 黑名单抽象接口。
 *
 * <p>用于解决 JWT 无状态登出无效问题:登出时将 token 加入黑名单,
 * {@code JwtAuthenticationFilter} 在校验 token 时拒绝黑名单中的 token。</p>
 *
 * <p>BV-06 修复:抽象为接口,通过条件装配支持多种实现:
 * <ul>
 *   <li>{@link InMemoryTokenBlacklist} — 默认实现,基于 {@link java.util.concurrent.ConcurrentHashMap},
 *       适用于单实例部署(本项目默认使用 H2 嵌入式数据库,本身即为单实例)。</li>
 *   <li>Redis 实现(预留扩展点)— 集群部署时通过 {@code spring.data.redis.*} 配置启用,
 *       以 token 的 jti+TTL 为键,实现跨实例共享黑名单。
 *       引入 Redis 依赖后,新增 {@code @ConditionalOnProperty(name = "app.token-blacklist.type", havingValue = "redis")}
 *       的实现类即可自动切换。</li>
 * </ul>
 *
 * <p>切换方式:在 application.yml 中配置 {@code app.token-blacklist.type=memory|redis},
 * 默认 memory。Redis 实现需额外引入 spring-boot-starter-data-redis 依赖。</p>
 */
public interface TokenBlacklist {

    /**
     * 将 token 加入黑名单。
     *
     * @param token            JWT token 字符串
     * @param expirationMillis token 的过期时间戳(毫秒),过期后自动清理。
     *                         传 0 或负数时,实现可自行兜底(如默认保留 24 小时)。
     */
    void add(String token, long expirationMillis);

    /**
     * 判断 token 是否在黑名单中。
     *
     * @param token JWT token 字符串
     * @return true 表示在黑名单中(应拒绝该 token);false 表示不在(可继续校验)
     */
    boolean contains(String token);
}
