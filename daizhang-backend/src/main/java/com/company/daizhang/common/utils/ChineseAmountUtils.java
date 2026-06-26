package com.company.daizhang.common.utils;

import java.math.BigDecimal;

/**
 * 中文大写金额转换工具类
 * 用于会计凭证、单据打印的标准大写金额格式（符合《会计基础工作规范》）
 * 示例：1234.56 -> 壹仟贰佰叁拾肆元伍角陆分
 */
public final class ChineseAmountUtils {

    private static final String[] CN_UPPER_NUMBER = {
            "零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"
    };
    private static final String[] CN_UPPER_UNIT = {
            "", "拾", "佰", "仟"
    };
    private static final String CN_UNIT_WAN = "万";
    private static final String CN_UNIT_YI = "亿";
    private static final String CN_UNIT_YUAN = "元";
    private static final String CN_UNIT_JIAO = "角";
    private static final String CN_UNIT_FEN = "分";
    private static final String CN_NEGATIVE = "负";
    private static final String CN_FULL = "整";

    private ChineseAmountUtils() {
    }

    /**
     * 将BigDecimal金额转换为中文大写
     *
     * @param amount 金额（元）
     * @return 中文大写金额，如"壹仟元整"
     */
    public static String toChinese(BigDecimal amount) {
        if (amount == null) {
            return CN_UPPER_NUMBER[0] + CN_UNIT_YUAN + CN_FULL;
        }
        // 保留两位小数，四舍五入
        BigDecimal scaled = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
        boolean negative = scaled.compareTo(BigDecimal.ZERO) < 0;
        if (negative) {
            scaled = scaled.negate();
        }
        long longValue = scaled.movePointRight(2).longValueExact();
        // 分离整数部分和分角
        long fen = longValue % 100;
        long yuan = longValue / 100;

        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append(CN_NEGATIVE);
        }
        // 整数部分
        if (yuan == 0) {
            sb.append(CN_UPPER_NUMBER[0]);
        } else {
            sb.append(convertIntegerPart(yuan));
        }
        sb.append(CN_UNIT_YUAN);
        // 小数部分（角分）
        int jiao = (int) (fen / 10);
        int fenPart = (int) (fen % 10);
        if (jiao == 0 && fenPart == 0) {
            sb.append(CN_FULL);
        } else {
            if (jiao != 0) {
                sb.append(CN_UPPER_NUMBER[jiao]).append(CN_UNIT_JIAO);
            } else if (yuan != 0 && fenPart != 0) {
                // 元后有分时，角位补零，如 壹元零伍分
                sb.append(CN_UPPER_NUMBER[0]);
            }
            if (fenPart != 0) {
                sb.append(CN_UPPER_NUMBER[fenPart]).append(CN_UNIT_FEN);
            }
        }
        return sb.toString();
    }

    /**
     * 转换整数部分（元以上的部分），支持到千亿位
     */
    private static String convertIntegerPart(long yuan) {
        if (yuan == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        // 分段处理：每4位一段（个、万、亿）
        long yi = yuan / 100000000L;
        long wan = (yuan % 100000000L) / 10000L;
        long ge = yuan % 10000L;

        if (yi > 0) {
            sb.append(convertFourDigits(yi)).append(CN_UNIT_YI);
            if (wan == 0 && ge > 0) {
                // 亿后无万，补零
                sb.append(CN_UPPER_NUMBER[0]);
            }
        }
        if (wan > 0) {
            sb.append(convertFourDigits(wan)).append(CN_UNIT_WAN);
            if (ge == 0) {
                // 正好整万
            } else if (ge < 1000) {
                // 万后不足4位，补零
                sb.append(CN_UPPER_NUMBER[0]);
            }
        }
        if (ge > 0) {
            sb.append(convertFourDigits(ge));
        }
        return sb.toString();
    }

    /**
     * 转换4位数（0-9999）
     */
    private static String convertFourDigits(long num) {
        if (num == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int thousands = (int) (num / 1000);
        int hundreds = (int) (num % 1000 / 100);
        int tens = (int) (num % 100 / 10);
        int ones = (int) (num % 10);

        if (thousands > 0) {
            sb.append(CN_UPPER_NUMBER[thousands]).append(CN_UPPER_UNIT[3]);
            if (hundreds == 0 && (tens > 0 || ones > 0)) {
                sb.append(CN_UPPER_NUMBER[0]);
            }
        }
        if (hundreds > 0) {
            sb.append(CN_UPPER_NUMBER[hundreds]).append(CN_UPPER_UNIT[2]);
            if (tens == 0 && ones > 0) {
                sb.append(CN_UPPER_NUMBER[0]);
            }
        }
        if (tens > 0) {
            sb.append(CN_UPPER_NUMBER[tens]).append(CN_UPPER_UNIT[1]);
        }
        if (ones > 0) {
            sb.append(CN_UPPER_NUMBER[ones]);
        }
        return sb.toString();
    }
}
