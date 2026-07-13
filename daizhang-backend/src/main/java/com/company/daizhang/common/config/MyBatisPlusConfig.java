package com.company.daizhang.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus配置
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件（使用H2数据库方言）
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.H2);
        // 限制单页最大500条,防止客户端传入超大pageSize导致OOM
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
