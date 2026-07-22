package com.company.daizhang.common.crypto.runner;

import com.company.daizhang.common.crypto.config.CryptoProperties;
import com.company.daizhang.common.crypto.util.AesGcmEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 敏感字段加密数据迁移 Runner (P4.1)
 * <p>
 * 启动时扫描 Employee 表的 id_card / phone / bank_account 字段,若内容为明文(非 base64 密文格式),
 * 则批量加密更新。确保历史明文数据在启用加密功能后被自动转换为密文。
 * <p>
 * 执行条件:
 * <ul>
 *     <li>{@code app.crypto.enabled=true}(加密功能启用)</li>
 * </ul>
 * <p>
 * 容错策略:迁移失败不阻塞应用启动,仅记录 WARN 日志(生产环境应人工排查)。
 * <p>
 * 简化实现:仅处理 Employee(sal_employee)表的敏感字段;其他表的迁移可按相同模式扩展。
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class FieldEncryptionMigrationRunner implements CommandLineRunner {

    private final CryptoProperties cryptoProperties;
    private final AesGcmEncryptor encryptor;
    private final JdbcTemplate jdbcTemplate;

    /** Employee 表敏感字段:列名 → 是否可空(NOT NULL 字段需特殊处理) */
    private static final String[][] EMPLOYEE_SENSITIVE_COLUMNS = {
            {"id_card", "id_card"},
            {"phone", "phone"},
            {"bank_account", "bank_account"}
    };

    @Override
    public void run(String... args) {
        if (!cryptoProperties.isEnabled()) {
            log.info("加密功能未启用,跳过敏感字段数据迁移");
            return;
        }
        migrateEmployeeSensitiveFields();
    }

    /**
     * 迁移 sal_employee 表的敏感字段。
     * 使用原生 JDBC 读写,绕过 TypeHandler,直接操作密文/明文。
     */
    private void migrateEmployeeSensitiveFields() {
        try {
            // 读取所有未删除的员工记录的原始字段值(绕过 TypeHandler 解密)
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, id_card, phone, bank_account FROM sal_employee WHERE deleted = 0");

            if (rows.isEmpty()) {
                log.info("sal_employee 表无数据,敏感字段迁移跳过");
                return;
            }

            int migratedCount = 0;
            for (Map<String, Object> row : rows) {
                Long id = ((Number) row.get("id")).longValue();
                boolean changed = false;

                String idCard = getColumnAsString(row, "id_card");
                String phone = getColumnAsString(row, "phone");
                String bankAccount = getColumnAsString(row, "bank_account");

                // 对明文字段加密(已加密的跳过)
                if (shouldMigrate(idCard)) {
                    idCard = encryptor.encrypt(idCard);
                    changed = true;
                }
                if (shouldMigrate(phone)) {
                    phone = encryptor.encrypt(phone);
                    changed = true;
                }
                if (shouldMigrate(bankAccount)) {
                    bankAccount = encryptor.encrypt(bankAccount);
                    changed = true;
                }

                if (changed) {
                    jdbcTemplate.update(
                            "UPDATE sal_employee SET id_card = ?, phone = ?, bank_account = ? WHERE id = ?",
                            idCard, phone, bankAccount, id);
                    migratedCount++;
                }
            }

            if (migratedCount > 0) {
                log.info("敏感字段加密迁移完成:共迁移 {} 条 Employee 记录", migratedCount);
            } else {
                log.info("敏感字段加密迁移完成:无需迁移(Employee 数据已全部为密文)");
            }
        } catch (Exception e) {
            // 迁移失败不阻塞应用启动,仅记录 WARN
            log.warn("敏感字段加密迁移失败,不阻塞应用启动,请人工排查: {}", e.getMessage(), e);
        }
    }

    /**
     * 判断字段值是否需要迁移(非空且非加密格式)。
     */
    private boolean shouldMigrate(String value) {
        return value != null && !value.isEmpty() && !encryptor.looksEncrypted(value);
    }

    /**
     * 安全地从查询结果行中读取字符串列值。
     */
    private String getColumnAsString(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        return value == null ? null : value.toString();
    }
}
