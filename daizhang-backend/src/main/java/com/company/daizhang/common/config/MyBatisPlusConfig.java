package com.company.daizhang.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.company.daizhang.common.crypto.mybatis.EncryptedStringTypeHandler;
import com.company.daizhang.common.crypto.mybatis.MaskTypeHandler;
import com.company.daizhang.common.crypto.util.AesGcmEncryptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus配置
 * <p>
 * P4.1: 加密 TypeHandler 静态字段初始化。
 * <p>
 * {@link EncryptedStringTypeHandler} 与 {@link MaskTypeHandler} 不能使用 {@code @Component} 注册为 Spring Bean,
 * 否则 MyBatis-Spring 3.x 的 {@code TypeHandlerRegistry.register(TypeHandler)} 会通过反射解析
 * {@code BaseTypeHandler<String>} 的泛型,将它们注册为 {@code String.class} 的默认 TypeHandler,
 * 导致 username / password / ip 等普通 String 字段被错误加解密。
 * <p>
 * 此处通过 {@code TypeHandlerStaticInitializer} Bean 在 Spring 启动时显式注入静态 encryptor,
 * 让显式标注 {@code @TableField(typeHandler = EncryptTypeHandler.class)} 的字段在 MyBatis
 * 通过反射 newInstance 创建 TypeHandler 实例时,能正确使用注入的 encryptor。
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

    /**
     * TypeHandler 静态 encryptor 初始化器。
     * <p>
     * 不返回 TypeHandler Bean(避免被 MyBatis-Spring 注册为默认),
     * 仅在初始化时调用静态 setter 注入 encryptor。
     * 返回的 Object 不被任何 TypeHandler 注册逻辑识别为 TypeHandler Bean。
     */
    @Bean
    public Object typeHandlerStaticInitializer(AesGcmEncryptor encryptor) {
        EncryptedStringTypeHandler.setEncryptorStatic(encryptor);
        MaskTypeHandler.setEncryptorStatic(encryptor);
        return new Object();
    }
}
