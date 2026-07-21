package com.company.daizhang.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Token 黑名单 Redis 实现骨架(预留扩展点)。
 *
 * <p>BV-06 修复:为集群部署预留 Redis 实现。本类不引入 spring-boot-starter-data-redis 依赖
 * (保持项目轻量),仅提供实现骨架与切换说明。实际启用步骤:</p>
 *
 * <ol>
 *   <li>在 pom.xml 中添加:
 *       <pre>{@code
 *       <dependency>
 *           <groupId>org.springframework.boot</groupId>
 *           <artifactId>spring-boot-starter-data-redis</artifactId>
 *       </dependency>
 *       }</pre>
 *   </li>
 *   <li>在 application.yml 中配置:
 *       <pre>{@code
 *       spring:
 *         data:
 *           redis:
 *             host: 127.0.0.1
 *             port: 6379
 *             password: ${REDIS_PASSWORD:}
 *             database: 0
 *       app:
 *         token-blacklist:
 *           type: redis
 *       }</pre>
 *   </li>
 *   <li>取消下方代码注释,注入 StringRedisTemplate 实现真正的 Redis 存取。
 *       TTL 取 token 剩余有效期(秒),过期后 Redis 自动清理,无需定时任务。</li>
 * </ol>
 *
 * <p>注意:本类当前为骨架(无实际 Redis 操作),仅在 {@code app.token-blacklist.type=redis}
 * 时被装配,但因无 StringRedisTemplate 依赖,启动时会因找不到 Bean 而失败(此为预期行为,
 * 提示用户按上述步骤引入依赖)。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.token-blacklist.type", havingValue = "redis")
public class RedisTokenBlacklist implements TokenBlacklist {

    // 启用 Redis 实现时取消注释并注入:
    // private final StringRedisTemplate redisTemplate;
    //
    // private static final String KEY_PREFIX = "token:blacklist:";
    //
    // public RedisTokenBlacklist(StringRedisTemplate redisTemplate) {
    //     this.redisTemplate = redisTemplate;
    //     log.info("启用 Redis Token 黑名单实现(集群部署适用)");
    // }

    @Override
    public void add(String token, long expirationMillis) {
        // 启用时实现:
        // if (token == null || token.isEmpty()) return;
        // long now = System.currentTimeMillis();
        // long ttlSeconds = expirationMillis > now
        //     ? (expirationMillis - now) / 1000
        //     : 24L * 3600;  // 兜底 24 小时
        // if (ttlSeconds <= 0) return;
        // redisTemplate.opsForValue().set(KEY_PREFIX + token, "1",
        //     ttlSeconds, TimeUnit.SECONDS);
        // log.debug("token 已加入 Redis 黑名单,TTL={}s", ttlSeconds);
        throw new UnsupportedOperationException(
                "Redis Token 黑名单未启用,请在 pom.xml 引入 spring-boot-starter-data-redis "
                        + "并在 application.yml 配置 app.token-blacklist.type=redis + spring.data.redis.*");
    }

    @Override
    public boolean contains(String token) {
        // 启用时实现:
        // if (token == null || token.isEmpty()) return false;
        // Boolean exists = redisTemplate.hasKey(KEY_PREFIX + token);
        // return Boolean.TRUE.equals(exists);
        throw new UnsupportedOperationException(
                "Redis Token 黑名单未启用,请在 pom.xml 引入 spring-boot-starter-data-redis "
                        + "并在 application.yml 配置 app.token-blacklist.type=redis + spring.data.redis.*");
    }
}
