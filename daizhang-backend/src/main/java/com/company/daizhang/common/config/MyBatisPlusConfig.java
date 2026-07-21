package com.company.daizhang.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus配置
 * <p>
 * P4.1: 加密 TypeHandler {@code EncryptedStringTypeHandler} 通过自身 {@code @Component}
 * 注入 Spring 容器,Spring 会调用其 {@code setEncryptor} 方法将 {@link com.company.daizhang.common.crypto.util.AesGcmEncryptor}
 * 注入到静态字段,供 MyBatis-Plus 通过反射创建的 TypeHandler 实例使用。
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件（使用H2数据库方言）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
