package com.company.daizhang.common.crypto.mybatis;

/**
 * 通用加密 TypeHandler (P4.1)
 * <p>
 * 继承自 {@link EncryptedStringTypeHandler},复用其静态注入的 {@link com.company.daizhang.common.crypto.util.AesGcmEncryptor}
 * 完成写库加密、读库解密。作为 {@code @TableField(typeHandler = EncryptTypeHandler.class)} 的标准入口,
 * 与 {@link com.company.daizhang.common.annotation.FieldEncrypt} 注解配合使用。
 * <p>
 * 设计说明:
 * MyBatis-Plus 通过反射 {@code newInstance()} 创建 TypeHandler,无法走 Spring 依赖注入。
 * 父类 {@link EncryptedStringTypeHandler} 通过 {@link com.company.daizhang.common.config.MyBatisPlusConfig}
 * 在启动时调用 {@link EncryptedStringTypeHandler#setEncryptorStatic} 注入静态字段,
 * 本子类自动共享该静态字段,无需重复注入。
 * <p>
 * 重要:本 TypeHandler 不使用 {@code @Component} / {@code @MappedTypes(String.class)},
 * 原因详见 {@link EncryptedStringTypeHandler}。仅通过
 * {@code @TableField(typeHandler = EncryptTypeHandler.class)} 显式启用。
 */
public class EncryptTypeHandler extends EncryptedStringTypeHandler {
    // 行为完全继承自 EncryptedStringTypeHandler,无需额外实现
}
