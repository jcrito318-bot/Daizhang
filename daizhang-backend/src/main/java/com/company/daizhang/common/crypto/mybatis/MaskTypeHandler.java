package com.company.daizhang.common.crypto.mybatis;

import com.company.daizhang.common.crypto.enums.MaskType;
import com.company.daizhang.common.crypto.util.AesGcmEncryptor;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 仅脱敏不加密的 TypeHandler (P4.1)
 * <p>
 * 用于只读场景:数据库中存储的是明文(未加密),但读取返回时需要脱敏展示。
 * 与 {@link EncryptTypeHandler} 的区别:
 * <ul>
 *     <li>{@link EncryptTypeHandler}:写库加密,读库解密为明文(业务层再决定是否脱敏)</li>
 *     <li>{@link MaskTypeHandler}:写库原样写入,读库直接返回脱敏值(不保留明文)</li>
 * </ul>
 * <p>
 * <b>注意:</b>本 TypeHandler 默认按 {@link MaskType#PHONE} 脱敏。如需指定其他脱敏类型,
 * 应在 Service 层调用 {@link AesGcmEncryptor#mask(String, MaskType)} 显式处理,
 * TypeHandler 层无法感知字段上的 {@link com.company.daizhang.common.annotation.FieldEncrypt} 注解。
 * <p>
 * 重要:本 TypeHandler 不使用 {@code @Component} / {@code @MappedTypes(String.class)},
 * 原因详见 {@link EncryptedStringTypeHandler}。仅通过
 * {@code @TableField(typeHandler = MaskTypeHandler.class)} 显式启用。
 * 静态 encryptor 由 {@link com.company.daizhang.common.config.MyBatisPlusConfig} 在启动时显式注入。
 */
public class MaskTypeHandler extends BaseTypeHandler<String> {

    /**
     * 默认脱敏类型。TypeHandler 无法读取字段注解,统一按手机号脱敏;
     * 其他脱敏类型请由 Service 层显式调用 mask()。
     */
    private static final MaskType DEFAULT_MASK_TYPE = MaskType.PHONE;

    private static AesGcmEncryptor encryptor;

    /**
     * 由配置类显式调用,注入静态 encryptor。
     * <p>
     * 注意:不要给此方法加 {@code @Autowired} 或 {@code @Component},否则会让本类成为 Spring Bean,
     * 触发 MyBatis-Spring 的 TypeHandler 注册逻辑,被注册为 String 默认 TypeHandler。
     */
    public static void setEncryptorStatic(AesGcmEncryptor encryptor) {
        MaskTypeHandler.encryptor = encryptor;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        // 写库时不加密,原样写入(本 TypeHandler 仅用于只读脱敏场景)
        ps.setString(i, parameter);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return maskValue(value);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return maskValue(value);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return maskValue(value);
    }

    /**
     * 对读取的值执行脱敏。若加密器未初始化(单元测试场景)则原样返回。
     */
    private String maskValue(String value) {
        if (value == null || encryptor == null) {
            return value;
        }
        return DEFAULT_MASK_TYPE.mask(value);
    }
}
