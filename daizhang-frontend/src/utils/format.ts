/**
 * 金额格式化工具(全项目统一实现)
 *
 * 统一规则:
 * - null/undefined → '0.00'
 * - 0 → '0.00'(与 DashboardView/PeriodManage 一致)
 * - 正数 → '#,##0.00'(zh-CN 千分位)
 * - 负数 → '-#,##0.00'
 * - 非数字 → '0.00'
 */

/**
 * 格式化金额为带千分位的字符串
 * @param amount 金额数值(number | string | null | undefined)
 * @returns 格式化后的字符串,如 '1,234.56'
 */
export function formatAmount(amount: number | string | null | undefined): string {
  if (amount === null || amount === undefined || amount === '') {
    return '0.00'
  }
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) {
    return '0.00'
  }
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/**
 * 格式化金额并带人民币符号
 * @param amount 金额数值
 * @returns 如 '¥1,234.56'
 */
export function formatCurrency(amount: number | string | null | undefined): string {
  return '¥' + formatAmount(amount)
}
