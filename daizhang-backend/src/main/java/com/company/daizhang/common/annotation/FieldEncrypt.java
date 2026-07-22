package com.company.daizhang.common.annotation;

import com.company.daizhang.common.crypto.enums.MaskType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级加密注解 (P4.1)
 * <p>
 * 标注在实体类的敏感字段上,声明该字段的加密与脱敏策略。需配合 MyBatis-Plus 的
 * {@code @TableField(typeHandler = EncryptTypeHandler.class)} 使用:
 * <ul>
 *     <li>写库:TypeHandler 自动调用 AES-GCM 加密</li>
 *     <li>读库:TypeHandler 自动解密为明文(供业务层使用)</li>
 *     <li>对外展示:Service 层根据 {@link #maskType()} 调用脱敏方法后赋值给 VO</li>
 * </ul>
 * <p>
 * 示例:
 * <pre>
 * {@literal @}TableField(typeHandler = EncryptTypeHandler.class)
 * {@literal @}FieldEncrypt(maskType = MaskType.ID_CARD)
 * private String idCard;
 * </pre>
 *
 * @see MaskType
 * @see com.company.daizhang.common.crypto.mybatis.EncryptTypeHandler
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldEncrypt {

    /**
     * 脱敏类型,用于查询时返回脱敏值(VO 层)。
     * <p>
     * 默认 {@link MaskType#ID_CARD},实际使用时应按字段语义指定。
     *
     * @return 脱敏类型
     */
    MaskType maskType() default MaskType.ID_CARD;

    /**
     * 是否支持密文搜索(默认 false)。
     * <p>
     * 若为 true,则加密时额外用 HMAC-SHA256 生成搜索摘要并存储,支持按明文等值匹配密文。
     * 当前为预留能力,未启用独立的摘要列存储,默认 false 即可。
     *
     * @return 是否支持密文搜索
     */
    boolean searchable() default false;
}
