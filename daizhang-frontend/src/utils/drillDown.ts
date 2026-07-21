import type { DrillDownDirection } from '@/types/ledger'

/**
 * 报表钻取工具集
 * - 根据科目编码推断借贷方向(用于资产负债表、利润表)
 * - 现金流量表项目编码 → 主科目编码 + 方向 的映射
 * - 防抖工具(避免双击误触)
 */

/**
 * 根据科目编码首位数字推断借贷方向
 * <p>
 * 会计科目编码首位约定:
 * 1xxx 资产类 → 借方
 * 2xxx 负债类 → 贷方
 * 3xxx 共同类 → 默认借方(视具体科目而定,此处保守取借方)
 * 4xxx 所有者权益类 → 贷方
 * 5xxx 损益类(收入) → 贷方
 * 6xxx 损益类(成本费用) → 借方
 * 7xxx 损益类(其他业务支出/营业外支出) → 借方
 *
 * @param subjectCode 科目编码
 * @returns 推断出的借贷方向;无法识别时返回 'debit' 作为保守默认值
 */
export function inferDirectionFromSubjectCode(subjectCode: string): DrillDownDirection {
  if (!subjectCode || subjectCode.length === 0) {
    return 'debit'
  }
  const firstChar = subjectCode.charAt(0)
  switch (firstChar) {
    case '1':
    case '3':
    case '6':
    case '7':
      return 'debit'
    case '2':
    case '4':
    case '5':
      return 'credit'
    default:
      return 'debit'
  }
}

/**
 * 现金流量表项目编码 → 钻取参数映射
 * <p>
 * 现金流量表项目本身并不直接对应单一科目,而是反映"现金及现金等价物"的变动。
 * 钻取时按"主现金科目(1001/1002/1012)+ 借贷方向"反查凭证:
 * - 流入项(inflow):现金增加 → 借方
 * - 流出项(outflow):现金减少 → 贷方
 *
 * 注:由于钻取 API 单次仅支持一个 subjectCode,这里使用 "1001" 作为主现金科目编码,
 * 后端 LIKE '1001%' 会命中 1001(库存现金)及其下级。若需同时覆盖 1002/1012,
 * 用户可在弹窗中手动调整或使用模糊匹配。
 */
export interface CashFlowDrillConfig {
  /** 主现金科目编码 */
  subjectCode: string
  /** 钻取方向 */
  direction: DrillDownDirection
}

const CASH_DRILL_CONFIG: CashFlowDrillConfig = {
  subjectCode: '1001',
  direction: 'debit'
}

/**
 * 现金流量表 itemCode → 钻取配置
 * 流入项使用借方(现金增加),流出项使用贷方(现金减少)
 */
const CASH_FLOW_ITEM_DRILL_MAP: Record<string, CashFlowDrillConfig> = {
  // 经营活动流入
  SALES_RECEIPTS: { subjectCode: '1001', direction: 'debit' },
  TAX_REFUNDS: { subjectCode: '1001', direction: 'debit' },
  OTHER_OPERATING_RECEIPTS: { subjectCode: '1001', direction: 'debit' },
  // 经营活动流出
  PURCHASE_PAYMENTS: { subjectCode: '1001', direction: 'credit' },
  EMPLOYEE_PAYMENTS: { subjectCode: '1001', direction: 'credit' },
  TAX_PAYMENTS: { subjectCode: '1001', direction: 'credit' },
  OTHER_OPERATING_PAYMENTS: { subjectCode: '1001', direction: 'credit' },
  // 投资活动流入
  INVESTMENT_RECEIPTS: { subjectCode: '1001', direction: 'debit' },
  INVESTMENT_INCOME: { subjectCode: '1001', direction: 'debit' },
  ASSET_DISPOSAL: { subjectCode: '1001', direction: 'debit' },
  OTHER_INVESTING_RECEIPTS: { subjectCode: '1001', direction: 'debit' },
  // 投资活动流出
  ASSET_PURCHASE: { subjectCode: '1001', direction: 'credit' },
  INVESTMENT_PAYMENTS: { subjectCode: '1001', direction: 'credit' },
  OTHER_INVESTING_PAYMENTS: { subjectCode: '1001', direction: 'credit' },
  // 筹资活动流入
  FINANCING_RECEIPTS: { subjectCode: '1001', direction: 'debit' },
  LOAN_RECEIPTS: { subjectCode: '1001', direction: 'debit' },
  OTHER_FINANCING_RECEIPTS: { subjectCode: '1001', direction: 'debit' },
  // 筹资活动流出
  DEBT_REPAYMENT: { subjectCode: '1001', direction: 'credit' },
  DISTRIBUTION_PAYMENTS: { subjectCode: '1001', direction: 'credit' },
  OTHER_FINANCING_PAYMENTS: { subjectCode: '1001', direction: 'credit' },
  // 汇率变动
  EXCHANGE_EFFECT: { subjectCode: '1001', direction: 'debit' }
}

/**
 * 根据现金流量表 itemCode 获取钻取配置
 * @param itemCode 项目编码
 * @returns 钻取配置;未匹配时返回默认现金科目 + 借方
 */
export function getCashFlowDrillConfig(itemCode: string): CashFlowDrillConfig {
  return CASH_FLOW_ITEM_DRILL_MAP[itemCode] || CASH_DRILL_CONFIG
}

/**
 * 创建防抖函数(用于双击事件避免误触)
 * <p>
 * 在 wait 毫秒内多次触发只执行最后一次;同时如果在 wait 毫秒内重复触发,
 * 会重置计时器,避免连续双击打开多个弹窗。
 *
 * @param fn 待执行函数
 * @param wait 防抖时长(ms),默认 300
 */
export function createDebounce<T extends (...args: never[]) => void>(
  fn: T,
  wait = 300
): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout> | null = null
  return (...args: Parameters<T>) => {
    if (timer !== null) {
      clearTimeout(timer)
    }
    timer = setTimeout(() => {
      timer = null
      fn(...args)
    }, wait)
  }
}
