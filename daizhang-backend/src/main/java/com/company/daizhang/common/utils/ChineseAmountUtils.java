package com.company.daizhang.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
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
     * 转换整数部分（元以上的部分），支持到 long 范围上限（约9.2亿亿元）
     * 采用递归按4位一段处理（个、万、亿、万亿、...）
     */
    private static String convertIntegerPart(long yuan) {
        if (yuan == 0) {
            return "";
        }
        // 按4位一段分组（低位在前），段单位依次为：""、"万"、"亿"、"万亿"...
        // 递归处理高位段
        return convertSegment(yuan, 0);
    }

    /**
     * 递归处理整数段的中文转换
     * @param num 当前剩余数值
     * @param segmentLevel 段级别：0=个段,1=万段,2=亿段,3=万亿段...
     * @return 中文大写
     */
    private static String convertSegment(long num, int segmentLevel) {
        if (num == 0) {
            return "";
        }
        // 当前段的4位
        long currentSegment = num % 10000L;
        // 高位剩余
        long higher = num / 10000L;
        String higherStr = convertSegment(higher, segmentLevel + 1);
        if (currentSegment == 0) {
            // 当前段为0，仅返回高位（万/亿单位由高位拼接时已含）
            return higherStr;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(higherStr);
        // 高位存在且当前段不足4位时，需要在单位后补零
        if (!higherStr.isEmpty() && currentSegment < 1000) {
            sb.append(CN_UPPER_NUMBER[0]);
        }
        sb.append(convertFourDigits(currentSegment));
        sb.append(segmentUnit(segmentLevel));
        return sb.toString();
    }

    /**
     * 段单位
     */
    private static String segmentUnit(int segmentLevel) {
        switch (segmentLevel) {
            case 0: return "";
            case 1: return CN_UNIT_WAN;
            case 2: return CN_UNIT_YI;
            case 3: return CN_UNIT_YI + CN_UNIT_WAN; // 万亿
            default: return CN_UNIT_YI + CN_UNIT_WAN; // 万亿以上仍用"万亿"（实际不会触发）
        }
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
