/**
 * 金额位格解析工具
 *
 * 用于记账凭证的金额拆分显示:将输入文本解析为 11 位数字数组,
 * 对应位格:亿、千万、百万、十万、万、千、百、十、元、角、分
 *
 * 解析规则:
 * - 无小数点:整数部分从"元"(索引 8)开始向前对应,"角""分"留空
 *   例: "5000" → ['', '', '', '', '', '5', '0', '0', '0', '', '']
 * - 有小数点:整数部分同上,小数部分对应"角"(索引 9)、"分"(索引 10)
 *   例: "5000.56" → [..., '5', '0', '0', '0', '5', '6']
 *   例: "5000.5"  → [..., '5', '0', '0', '0', '5', '']
 */

/** 金额位格标签:亿、千万、百万、十万、万、千、百、十、元、角、分 */
export const AMOUNT_LABELS = ['亿', '千', '百', '十', '万', '千', '百', '十', '元', '角', '分']

/** "元" 在位数组中的索引 */
export const YUAN_INDEX = 8

/** "角" 在位数组中的索引 */
export const JIAO_INDEX = 9

/** "分" 在位数组中的索引 */
export const FEN_INDEX = 10

/** 金额位格总数 */
export const DIGIT_COUNT = 11

/**
 * 将金额输入文本解析为 11 位数字数组
 *
 * @param input 金额输入文本(如 "5000" 或 "5000.56")
 * @returns 长度为 11 的字符串数组,空字符串表示该位无数字
 */
export function parseAmountToDigits(input: string | undefined | null): string[] {
  const digits = Array(DIGIT_COUNT).fill('')
  if (!input || input.trim() === '') return digits

  const str = input.trim()
  const hasDecimal = str.includes('.')

  let intPart: string
  let decPart: string = ''

  if (hasDecimal) {
    const dotIdx = str.indexOf('.')
    intPart = str.substring(0, dotIdx)
    decPart = str.substring(dotIdx + 1)
  } else {
    intPart = str
  }

  // 仅保留数字字符
  intPart = intPart.replace(/[^0-9]/g, '')
  decPart = decPart.replace(/[^0-9]/g, '')

  // 整数部分:从"元"(索引 8)开始向前填充
  let idx = YUAN_INDEX
  for (let i = intPart.length - 1; i >= 0 && idx >= 0; i--) {
    digits[idx] = intPart[i]
    idx--
  }

  // 小数部分:仅当存在小数点时才填充"角""分"
  if (hasDecimal) {
    if (decPart.length >= 1) digits[JIAO_INDEX] = decPart[0]
    if (decPart.length >= 2) digits[FEN_INDEX] = decPart[1]
  }

  return digits
}

/**
 * 将数字转换为输入文本(用于编辑模式加载已有凭证)
 * - 整数:返回不带小数点的字符串,如 5000 → "5000"
 * - 有小数:返回带小数点的字符串,如 5000.5 → "5000.5",5000.56 → "5000.56"
 *
 * @param num 金额数值
 * @returns 输入文本,0 或空返回空字符串
 */
export function numberToInput(num: number | undefined | null): string {
  if (!num || num === 0) return ''
  const rounded = Math.round(num * 100) / 100
  if (Number.isInteger(rounded)) {
    return String(rounded)
  }
  return rounded.toFixed(2).replace(/0+$/, '').replace(/\.$/, '')
}

/**
 * 将数字直接转换为 11 位数字数组(用于合计行)
 *
 * @param num 金额数值
 * @returns 长度为 11 的字符串数组
 */
export function numberToDigits(num: number | undefined | null): string[] {
  if (!num || num === 0) return Array(DIGIT_COUNT).fill('')
  return parseAmountToDigits(numberToInput(num))
}

/**
 * 清理金额输入:仅保留数字和一个小数点,小数部分最多 2 位
 *
 * @param val 原始输入
 * @returns 清理后的字符串
 */
export function sanitizeAmountInput(val: string | undefined | null): string {
  if (!val) return ''
  let result = val.replace(/[^\d.]/g, '')
  const firstDot = result.indexOf('.')
  if (firstDot !== -1) {
    const intPart = result.substring(0, firstDot)
    const decPart = result.substring(firstDot + 1).replace(/\./g, '').substring(0, 2)
    result = intPart + '.' + decPart
  }
  return result
}
