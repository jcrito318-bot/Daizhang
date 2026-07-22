package com.company.daizhang.common.crypto.mybatis;

import com.company.daizhang.common.crypto.util.AesGcmEncryptor;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis 字符串字段加解密 TypeHandler (P4.1)
 * <p>
 * 与 MyBatis-Plus 集成:实体字段标注
 * {@code @TableField(typeHandler = EncryptedStringTypeHandler.class)} 后:
 * <ul>
 *     <li>写库({@link #setNonNullParameter}):自动调用 {@link AesGcmEncryptor#encrypt(String)} 加密</li>
 *     <li>读库({@link #getNullableResult}):自动调用 {@link AesGcmEncryptor#decrypt(String)} 解密</li>
 * </ul>
 * <p>
 * 兼容性:对未加密的旧数据(明文),{@link AesGcmEncryptor#decrypt(String)} 会检测失败并原样返回,
 * 不会因解密异常导致查询失败,便于平滑迁移。
 * <p>
 * 重要:本 TypeHandler <b>不</b>使用 {@code @Component} / {@code @MappedTypes(String.class)} 注册为 Spring Bean。
 * 原因:MyBatis-Spring 3.x 的 {@code TypeHandlerRegistry.register(TypeHandler)} 会通过反射
 * 解析 {@code BaseTypeHandler<String>} 的泛型参数,将其注册为 {@code String.class} 的默认 TypeHandler,
 * 导致所有 String 字段(含 username / password / ip 等)被自动加解密,引发登录失败与字段超长。
 * <p>
 * 正确做法:本 TypeHandler 仅通过 {@code @TableField(typeHandler = EncryptedStringTypeHandler.class)} 显式启用,
 * 静态 encryptor 由 {@link com.company.daizhang.common.config.MyBatisPlusConfig} 在 Spring 启动时显式注入。
 */
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    /**
     * 静态持有 AesGcmEncryptor 实例,MyBatis 通过反射 newInstance 创建 TypeHandler 时,
     * 无法走 Spring 依赖注入。此处用静态字段 + 由 {@link com.company.daizhang.common.config.MyBatisPlusConfig}
     * 在启动时显式调用 {@link #setEncryptorStatic(AesGcmEncryptor)} 注入。
     */
    private static AesGcmEncryptor encryptor;

    /**
     * 由配置类显式调用,注入静态 encryptor。
     * <p>
     * 注意:不要给此方法加 {@code @Autowired} 或 {@code @Component},否则会让本类成为 Spring Bean,
     * 触发 MyBatis-Spring 的 TypeHandler 注册逻辑,被注册为 String 默认 TypeHandler。
     */
    public static void setEncryptorStatic(AesGcmEncryptor encryptor) {
        EncryptedStringTypeHandler.encryptor = encryptor;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        // 写库时加密;encryptor 未初始化(单元测试场景)时透传
        ps.setString(i, encryptor == null ? parameter : encryptor.encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return encryptor == null ? value : encryptor.decrypt(value);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return encryptor == null ? value : encryptor.decrypt(value);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return encryptor == null ? value : encryptor.decrypt(value);
    }
}
