package com.company.daizhang.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * <p>
 * 启用 {@link EnableAsync},并提供两个专用线程池:
 * <ul>
 *   <li>{@code preferenceAsyncExecutor}:用于账套访问记录等"可失败、不阻塞主流程"的辅助写入。</li>
 *   <li>{@code backupAsyncExecutor}:用于数据库备份任务(P3.3),单线程串行执行,
 *       避免并发备份争抢数据库资源;队列容量较大以容纳定时备份堆积。</li>
 * </ul>
 * <p>
 * 拒绝策略采用 CallerRunsPolicy:线程池满时退化为调用线程同步执行,
 * 保证记录不丢失,而非直接丢弃任务。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("preferenceAsyncExecutor")
    public Executor preferenceAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("pref-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }

    /**
     * 备份专用线程池 (P3.3)。
     * <p>
     * 设计要点:
     * <ul>
     *   <li>corePoolSize=1:备份任务串行执行,避免并发备份对 H2 数据库产生锁竞争</li>
     *   <li>maxPoolSize=1:严格串行,即使队列积压也不并发</li>
     *   <li>queueCapacity=50:容忍短时积压(如定时备份+手动备份同时触发)</li>
     *   <li>CallerRunsPolicy:队列满时退化为调用线程同步执行,保证备份任务不丢失</li>
     *   <li>waitForTasksToCompleteOnShutdown + 60s 等待:应用关闭时给备份任务足够时间完成,
     *       避免备份文件损坏</li>
     * </ul>
     */
    @Bean("backupAsyncExecutor")
    public Executor backupAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("backup-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
