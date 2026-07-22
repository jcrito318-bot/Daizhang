package com.company.daizhang.module.system.totp.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * TOTP 工具类 (P4.2)
 * <p>
 * 因离线环境无法下载 dev.samstevens:totp 依赖,此处基于 JDK 自实现:
 * <ul>
 *     <li>Base32 编解码(RFC 4648)</li>
 *     <li>HMAC-SHA1 + RFC 6238 TOTP 算法(6 位,30 秒步长,兼容 Google Authenticator / 微信 / 火狐等)</li>
 * </ul>
 * 算法说明:
 * <pre>
 *   T = floor((currentTime - T0) / step)   T0=0, step=30s
 *   HOTP(K, T) = truncate(HMAC-SHA1(K, T)) mod 10^6
 *   TOTP = HOTP(K, current time)
 * </pre>
 */
public final class TotpUtil {

    /** TOTP 步长(秒),RFC 6238 默认 30 秒 */
    private static final int TIME_STEP = 30;
    /** 起始时间戳(Unix epoch) */
    private static final long T0 = 0L;
    /** 验证码位数 */
    private static final int CODE_DIGITS = 6;
    /** 允许的时间窗口偏差(前后各 1 个步长,即 ±30 秒),兼容设备时钟漂移 */
    private static final int WINDOW = 1;
    /** Base32 字符表(RFC 4648) */
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    /** HMAC 算法名 */
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private TotpUtil() {
    }

    /**
     * 生成随机 TOTP 密钥(Base32 编码,20 字节 = 160 位,RFC 4226 推荐长度)。
     *
     * @return Base32 编码的密钥字符串
     */
    public static String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * 生成 otpauth URL(二维码内容),格式:
     * {@code otpauth://totp/{issuer}:{account}?secret={secret}&issuer={issuer}&algorithm=SHA1&digits=6&period=30}
     *
     * @param account  账户标识(通常为用户名)
     * @param secret   Base32 密钥
     * @param issuer   发行方名称(应用名)
     * @return otpauth URL
     */
    public static String generateOtpAuthUrl(String account, String secret, String issuer) {
        String label = urlEncode(issuer) + ":" + urlEncode(account);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1"
                + "&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP;
    }

    /**
     * 校验用户输入的 6 位验证码是否正确。
     * 允许前后各 1 个时间窗口(±30 秒)的偏差。
     *
     * @param secret Base32 密钥
     * @param code   用户输入的 6 位验证码
     * @return true=校验通过
     */
    public static boolean verify(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        long currentBucket = (System.currentTimeMillis() / 1000L - T0) / TIME_STEP;
        // 当前窗口及前后各 WINDOW 个窗口,任一匹配即通过(容忍时钟漂移)
        for (int i = -WINDOW; i <= WINDOW; i++) {
            long bucket = currentBucket + i;
            String expected = generateCode(secret, bucket);
            if (constantTimeEquals(expected, code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成当前时间的 6 位 TOTP 验证码(主要用于测试与调试)。
     *
     * @param secret Base32 密钥
     * @return 6 位验证码字符串
     */
    public static String generateCurrentCode(String secret) {
        long bucket = (System.currentTimeMillis() / 1000L - T0) / TIME_STEP;
        return generateCode(secret, bucket);
    }

    /**
     * 基于 HMAC-SHA1 的 HOTP 算法核心实现(RFC 4226),再按时间窗口计算 TOTP(RFC 6238)。
     *
     * @param secret Base32 密钥
     * @param bucket 时间窗口计数
     * @return 6 位验证码字符串(前导补零)
     */
    private static String generateCode(String secret, long bucket) {
        byte[] key = base32Decode(secret);
        // 时间窗口转为 8 字节大端序
        byte[] timeBytes = new byte[8];
        long t = bucket;
        for (int i = 7; i >= 0; i--) {
            timeBytes[i] = (byte) (t & 0xFF);
            t >>= 8;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(timeBytes);
            // 动态截取:取 hash 最后一字节的低 4 位作为偏移量
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            // 前导补零到 6 位
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("生成 TOTP 验证码失败: " + e.getMessage(), e);
        }
    }

    /**
     * 常量时间字符串比较,防止计时攻击。
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    // ==================== Base32 编解码 (RFC 4648) ====================

    /**
     * Base32 编码(无填充符)。
     */
    static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                sb.append(BASE32_CHARS.charAt(index));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(BASE32_CHARS.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Base32 解码(容忍小写与填充符 = )。
     */
    static byte[] base32Decode(String data) {
        String upper = data.toUpperCase().replace("=", "").replaceAll("\\s", "");
        ByteArrayOutputStreamCompact out = new ByteArrayOutputStreamCompact();
        int buffer = 0;
        int bitsLeft = 0;
        for (int i = 0; i < upper.length(); i++) {
            int ch = upper.charAt(i);
            int val = BASE32_CHARS.indexOf(ch);
            if (val < 0) {
                throw new IllegalArgumentException("非法 Base32 字符: " + (char) ch);
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }

    /**
     * URL 编码(UTF-8)。
     */
    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    /**
     * 轻量字节数组输出缓冲(避免依赖 java.io.ByteArrayOutputStream 的逐字节写开销)。
     */
    private static final class ByteArrayOutputStreamCompact {
        private byte[] buf = new byte[32];
        private int count = 0;

        void write(int b) {
            if (count == buf.length) {
                byte[] newBuf = new byte[buf.length * 2];
                System.arraycopy(buf, 0, newBuf, 0, count);
                buf = newBuf;
            }
            buf[count++] = (byte) b;
        }

        byte[] toByteArray() {
            byte[] result = new byte[count];
            System.arraycopy(buf, 0, result, 0, count);
            return result;
        }
    }

    /**
     * 将原始字节生成 data:image/png;base64 形式的 Data URL。
     * (供调用方在无 QR 库场景下预留,当前 QR 由前端生成)
     */
    @SuppressWarnings("unused")
    private static String toDataUrl(byte[] pngBytes) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
    }
}
