package com.company.daizhang.common.crypto.enums;

/**
 * 敏感字段脱敏类型枚举 (P4.1)
 * <p>
 * 用于 {@code @FieldEncrypt(maskType = ...)} 标注字段在对外展示时的脱敏规则。
 * 脱敏只作用于返回前端的 VO,数据库存储的是加密后的密文,内部读取时为明文。
 * <p>
 * 脱敏规则:
 * <ul>
 *     <li>{@link #ID_CARD}:保留前 4 后 4,中间用 * 填充。例:110101198001011234 → 1101**********1234</li>
 *     <li>{@link #BANK_ACCOUNT}:保留前 4 后 4,中间用 * 填充。例:6225880123456789 → 6225********6789</li>
 *     <li>{@link #PHONE}:保留前 3 后 4,中间用 * 填充。例:13800138000 → 138****8000</li>
 *     <li>{@link #EMAIL}:本地名首字符保留,其余用 * 替换,域名不变。例:abc@example.com → a***@example.com</li>
 * </ul>
 */
public enum MaskType {

    /**
     * 身份证号:保留前 4 后 4
     */
    ID_CARD {
        @Override
        public String mask(String plainText) {
            if (plainText == null || plainText.isEmpty()) {
                return plainText;
            }
            int len = plainText.length();
            // 长度不足 8 位时全部脱敏,避免泄露
            if (len <= 8) {
                return repeat('*', len);
            }
            return plainText.substring(0, 4) + repeat('*', len - 8) + plainText.substring(len - 4);
        }
    },

    /**
     * 银行账号:保留前 4 后 4
     */
    BANK_ACCOUNT {
        @Override
        public String mask(String plainText) {
            if (plainText == null || plainText.isEmpty()) {
                return plainText;
            }
            int len = plainText.length();
            if (len <= 8) {
                return repeat('*', len);
            }
            return plainText.substring(0, 4) + repeat('*', len - 8) + plainText.substring(len - 4);
        }
    },

    /**
     * 手机号:保留前 3 后 4
     */
    PHONE {
        @Override
        public String mask(String plainText) {
            if (plainText == null || plainText.isEmpty()) {
                return plainText;
            }
            int len = plainText.length();
            if (len <= 7) {
                return repeat('*', len);
            }
            return plainText.substring(0, 3) + repeat('*', len - 7) + plainText.substring(len - 4);
        }
    },

    /**
     * 邮箱:本地名保留首字符,域名不变。例:abc@example.com → a***@example.com
     */
    EMAIL {
        @Override
        public String mask(String plainText) {
            if (plainText == null || plainText.isEmpty()) {
                return plainText;
            }
            int atIdx = plainText.indexOf('@');
            if (atIdx <= 0) {
                // 非邮箱格式,整体按手机号规则脱敏兜底
                return plainText.length() <= 1 ? plainText : plainText.charAt(0) + repeat('*', plainText.length() - 1);
            }
            String local = plainText.substring(0, atIdx);
            String domain = plainText.substring(atIdx);
            String maskedLocal = local.length() <= 1 ? local : local.charAt(0) + repeat('*', local.length() - 1);
            return maskedLocal + domain;
        }
    };

    /**
     * 对明文执行脱敏。
     *
     * @param plainText 明文(null/空串原样返回)
     * @return 脱敏后的字符串
     */
    public abstract String mask(String plainText);

    /**
     * 生成指定长度的 * 字符串。
     */
    protected static String repeat(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        char[] arr = new char[count];
        java.util.Arrays.fill(arr, ch);
        return new String(arr);
    }
}
