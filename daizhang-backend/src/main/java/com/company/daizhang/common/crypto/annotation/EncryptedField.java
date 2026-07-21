package com.company.daizhang.common.crypto.annotation;

import com.company.daizhang.common.crypto.mybatis.EncryptedStringTypeHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段加密注解 (P4.1)
 * <p>
 * 标注在实体类的敏感字段上,表示该字段需要加密存储。
 * <p>
 * <b>注意:</b>本注解仅作为元数据(标记哪些字段属于敏感字段),实际加解密由
 * {@link EncryptedStringTypeHandler} 完成。配合 MyBatis-Plus 的
 * {@code @TableField(typeHandler = EncryptedStringTypeHandler.class)} 使用,
 * 写库时自动加密,读库时自动解密。
 * <p>
 * 示例:
 * <pre>
 * {@literal @}TableField(typeHandler = EncryptedStringTypeHandler.class)
 * {@literal @}EncryptedField
 * private String idCard;
 * </pre>
 *
 * @see EncryptedStringTypeHandler
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptedField {

    /**
     * 加密字段用途说明(可选,便于审计与代码维护)。
     */
    String value() default "";
}
