package com.company.daizhang.common.crypto.mybatis;

import com.company.daizhang.common.crypto.util.AesGcmEncryptor;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
 * TypeHandler 通过 Spring 注入 {@link AesGcmEncryptor},需 MyBatis 配置注册为 Bean 后
 * 由 SqlSessionFactory 关联(参见 {@code MyBatisPlusConfig})。
 * <p>
 * 兼容性:对未加密的旧数据(明文),{@link AesGcmEncryptor#decrypt(String)} 会检测失败并原样返回,
 * 不会因解密异常导致查询失败,便于平滑迁移。
 */
@Component
@MappedTypes(String.class)
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    /**
     * 静态持有 AesGcmEncryptor 实例,MyBatis 通过反射 newInstance 创建 TypeHandler 时,
     * 无法走 Spring 依赖注入。此处用静态字段 + setter 由 Spring 配置类初始化注入。
     */
    private static AesGcmEncryptor encryptor;

    @Autowired
    public void setEncryptor(AesGcmEncryptor encryptor) {
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
