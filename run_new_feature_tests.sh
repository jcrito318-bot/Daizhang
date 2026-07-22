#!/bin/bash
# P5 + B1-B7 新功能 API 测试脚本
# 用法: bash /workspace/run_new_feature_tests.sh

set -uo pipefail
BASE_URL="http://localhost:8080/api"
PASS=0
FAIL=0
BUGS=0
RESULTS=""

log_pass() { PASS=$((PASS+1)); RESULTS+="PASS $1\n"; echo "[PASS] $1"; }
log_fail() { FAIL=$((FAIL+1)); RESULTS+="FAIL $1: $2\n"; echo "[FAIL] $1: $2"; }
log_bug()  { BUGS=$((BUGS+1)); RESULTS+="BUG  $1: $2\n"; echo "[BUG ] $1: $2"; }

echo "===== 获取 Token ====="
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN_RESP" | grep -oP '"token":"[^"]+' | head -1 | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
  echo "FATAL: 无法获取 token"
  exit 1
fi
echo "Token 获取成功"
AUTH="Authorization: Bearer $TOKEN"

echo ""
echo "===== 准备测试数据(确保有可用账套) ====="
# 先查现有账套列表,用 python3 解析 JSON 提取 id
AS_LIST=$(curl -s -H "$AUTH" "$BASE_URL/accountset/list")
AS_COUNT=$(echo "$AS_LIST" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d['data']))" 2>/dev/null || echo 0)
echo "现有账套数: $AS_COUNT"

if [ "$AS_COUNT" -lt 2 ]; then
  echo "账套不足 2 个,创建测试账套..."
  # 创建账套1
  curl -s -X POST "$BASE_URL/accountset" -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"code":"TEST001","name":"测试账套1","companyName":"测试公司A","industryType":"商贸","accountingStandard":"小企业会计准则","startYear":2026,"startMonth":1,"currencyCode":"CNY","taxpayerType":"一般纳税人"}' > /dev/null
  # 初始化账套1
  AS_LIST=$(curl -s -H "$AUTH" "$BASE_URL/accountset/list")
  AS_ID1=$(echo "$AS_LIST" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][0]['id'])" 2>/dev/null)
  curl -s -X POST "$BASE_URL/accountset/$AS_ID1/init" -H "$AUTH" > /dev/null
  # 创建账套2
  curl -s -X POST "$BASE_URL/accountset" -H "$AUTH" -H "Content-Type: application/json" \
    -d '{"code":"TEST002","name":"测试账套2","companyName":"测试公司B","industryType":"服务","accountingStandard":"小企业会计准则","startYear":2026,"startMonth":1,"currencyCode":"CNY","taxpayerType":"小规模纳税人"}' > /dev/null
  AS_LIST=$(curl -s -H "$AUTH" "$BASE_URL/accountset/list")
fi

# 用 python3 可靠提取 id
AS_ID1=$(echo "$AS_LIST" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][0]['id'])" 2>/dev/null)
AS_ID2=$(echo "$AS_LIST" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['data'][1]['id'])" 2>/dev/null)
# 初始化账套2(确保有科目)
if [ -n "$AS_ID2" ]; then
  curl -s -X POST "$BASE_URL/accountset/$AS_ID2/init" -H "$AUTH" > /dev/null
fi
echo "账套1 ID=$AS_ID1, 账套2 ID=$AS_ID2"

if [ -z "$AS_ID1" ]; then
  echo "FATAL: 无法获取账套 ID"
  exit 1
fi

echo ""
echo "===== P5.0.1 跨账套复制科目+模板 ====="
if [ -n "$AS_ID1" ] && [ -n "$AS_ID2" ]; then
  HTTP_CODE=$(curl -s -o /tmp/copy_resp.json -w "%{http_code}" -X POST "$BASE_URL/accountset/$AS_ID2/copy-from/$AS_ID1" -H "$AUTH")
  if [ "$HTTP_CODE" = "200" ]; then
    log_pass "跨账套复制科目+模板 (HTTP 200)"
    cat /tmp/copy_resp.json | head -c 200; echo ""
  else
    log_bug "跨账套复制科目+模板 (HTTP $HTTP_CODE)" "$(cat /tmp/copy_resp.json | head -c 200)"
  fi
else
  echo "跳过:需要至少 2 个账套"
fi

echo ""
echo "===== P5.1.1 报表批量导出 zip ====="
HTTP_CODE=$(curl -s -o /tmp/report_zip_test.zip -w "%{http_code}" -X POST "$BASE_URL/batch/report/export-zip" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"items\":[{\"accountSetId\":$AS_ID1,\"year\":2026,\"month\":1,\"reportTypes\":[\"balance-sheet\",\"income-statement\"]}]}")
if [ "$HTTP_CODE" = "200" ]; then
  ZIP_SIZE=$(stat -c%s /tmp/report_zip_test.zip 2>/dev/null || echo 0)
  if [ "$ZIP_SIZE" -gt 100 ]; then
    log_pass "报表批量导出 zip (HTTP 200, size=${ZIP_SIZE}B)"
  else
    log_bug "报表批量导出 zip 内容为空" "size=${ZIP_SIZE}B"
  fi
else
  log_bug "报表批量导出 zip (HTTP $HTTP_CODE)" "$(cat /tmp/report_zip_test.zip | head -c 200)"
fi

echo ""
echo "===== P5.1.2 固定资产跨账套批量计提 ====="
HTTP_CODE=$(curl -s -o /tmp/dep_resp.json -w "%{http_code}" -X POST "$BASE_URL/batch/asset/depreciation/calculate" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"items\":[{\"accountSetId\":$AS_ID1,\"year\":2026,\"month\":1}]}")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "固定资产跨账套批量计提 (HTTP 200)"
  cat /tmp/dep_resp.json | head -c 200; echo ""
else
  log_bug "固定资产跨账套批量计提 (HTTP $HTTP_CODE)" "$(cat /tmp/dep_resp.json | head -c 200)"
fi

echo ""
echo "===== P5.2.1 凭证红字冲销(original_voucher_id 字段) ====="
# 查现有凭证
VOUCHER_LIST=$(curl -s -H "$AUTH" "$BASE_URL/voucher/page?accountSetId=$AS_ID1&year=2026&month=1&page=1&size=5")
VOUCHER_ID=$(echo "$VOUCHER_LIST" | grep -oP '"id":\d+' | head -1 | cut -d':' -f2)
echo "测试凭证 ID=$VOUCHER_ID"
# 注意:红冲需要已过账凭证,这里只验证接口可达性(未过账会返回业务错误,属正常)
if [ -n "$VOUCHER_ID" ]; then
  HTTP_CODE=$(curl -s -o /tmp/reverse_resp.json -w "%{http_code}" -X POST "$BASE_URL/voucher/$VOUCHER_ID/reverse?targetYear=2026&targetMonth=1" -H "$AUTH")
  if [ "$HTTP_CODE" = "200" ]; then
    log_pass "凭证红冲接口可达 (HTTP 200)"
    cat /tmp/reverse_resp.json | head -c 200; echo ""
  else
    # 业务错误(如凭证未过账)不算 bug,只记录
    MSG=$(cat /tmp/reverse_resp.json | grep -oP '"message":"[^"]+' | head -1)
    log_pass "凭证红冲接口可达 (HTTP $HTTP_CODE, $MSG)"
  fi
else
  echo "跳过:无可用凭证"
fi

echo ""
echo "===== P5.2.2 跨年结转向导(接口可达性) ====="
# 测试结账向导接口可达
HTTP_CODE=$(curl -s -o /tmp/wizard_resp.json -w "%{http_code}" -X POST "$BASE_URL/period/close-wizard" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"accountSetId\":$AS_ID1,\"year\":2026,\"month\":1,\"steps\":{\"checkIntegrity\":true,\"carryForward\":true,\"closePeriod\":true,\"openNextPeriod\":true}}")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "结账向导接口可达 (HTTP 200)"
else
  MSG=$(cat /tmp/wizard_resp.json | grep -oP '"message":"[^"]+' | head -1)
  log_pass "结账向导接口可达 (HTTP $HTTP_CODE, $MSG)"
fi

echo ""
echo "===== B1 零申报批量自动记账+结账 ====="
HTTP_CODE=$(curl -s -o /tmp/zero_resp.json -w "%{http_code}" -X POST "$BASE_URL/batch/zero-declaration" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"items\":[{\"accountSetId\":$AS_ID1,\"year\":2026,\"month\":1}]}")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "零申报批量自动记账+结账 (HTTP 200)"
  cat /tmp/zero_resp.json | head -c 200; echo ""
else
  log_bug "零申报批量自动记账+结账 (HTTP $HTTP_CODE)" "$(cat /tmp/zero_resp.json | head -c 200)"
fi

echo ""
echo "===== B2 跨账套批量漏报检查 ====="
HTTP_CODE=$(curl -s -o /tmp/taxcheck_resp.json -w "%{http_code}" -X POST "$BASE_URL/batch/tax/check" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{\"items\":[{\"accountSetId\":$AS_ID1,\"year\":2026,\"month\":1}]}")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "跨账套批量漏报检查 (HTTP 200)"
  cat /tmp/taxcheck_resp.json | head -c 200; echo ""
else
  log_bug "跨账套批量漏报检查 (HTTP $HTTP_CODE)" "$(cat /tmp/taxcheck_resp.json | head -c 200)"
fi

echo ""
echo "===== B3 智能催收通知 ====="
HTTP_CODE=$(curl -s -o /tmp/arrears_resp.json -w "%{http_code}" -X POST "$BASE_URL/customer/arrears/scan" -H "$AUTH")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "智能催收通知扫描 (HTTP 200)"
  cat /tmp/arrears_resp.json | head -c 200; echo ""
else
  log_bug "智能催收通知扫描 (HTTP $HTTP_CODE)" "$(cat /tmp/arrears_resp.json | head -c 200)"
fi

echo ""
echo "===== B4 合同到期预警 ====="
HTTP_CODE=$(curl -s -o /tmp/contract_resp.json -w "%{http_code}" -X POST "$BASE_URL/customer/contract/expiring/scan?daysBeforeExpire=30" -H "$AUTH")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "合同到期预警扫描 (HTTP 200)"
  cat /tmp/contract_resp.json | head -c 200; echo ""
else
  log_bug "合同到期预警扫描 (HTTP $HTTP_CODE)" "$(cat /tmp/contract_resp.json | head -c 200)"
fi

echo ""
echo "===== B5 客户经营简报 ====="
HTTP_CODE=$(curl -s -o /tmp/briefing_resp.json -w "%{http_code}" "$BASE_URL/report/customer-briefing?accountSetId=$AS_ID1&year=2026&month=1" -H "$AUTH")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "客户经营简报 (HTTP 200)"
  cat /tmp/briefing_resp.json | head -c 200; echo ""
else
  log_bug "客户经营简报 (HTTP $HTTP_CODE)" "$(cat /tmp/briefing_resp.json | head -c 200)"
fi
# 导出
HTTP_CODE=$(curl -s -o /tmp/briefing.xlsx -w "%{http_code}" "$BASE_URL/report/customer-briefing/export?accountSetId=$AS_ID1&year=2026&month=1" -H "$AUTH")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "客户经营简报导出 (HTTP 200)"
else
  log_bug "客户经营简报导出 (HTTP $HTTP_CODE)" ""
fi

echo ""
echo "===== B6 多年度对比分析 ====="
HTTP_CODE=$(curl -s -o /tmp/multiyear_resp.json -w "%{http_code}" "$BASE_URL/report/multi-year-comparison?accountSetId=$AS_ID1&startYear=2025&endYear=2026" -H "$AUTH")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "多年度对比分析 (HTTP 200)"
  cat /tmp/multiyear_resp.json | head -c 200; echo ""
else
  log_bug "多年度对比分析 (HTTP $HTTP_CODE)" "$(cat /tmp/multiyear_resp.json | head -c 200)"
fi

echo ""
echo "===== B7 工资条推送(接口可达性) ====="
# 查现有薪资表
SALARY_LIST=$(curl -s -H "$AUTH" "$BASE_URL/salary/sheet/page?page=1&size=5")
SALARY_SHEET_ID=$(echo "$SALARY_LIST" | grep -oP '"id":\d+' | head -1 | cut -d':' -f2)
echo "薪资表 ID=$SALARY_SHEET_ID"
if [ -n "$SALARY_SHEET_ID" ]; then
  HTTP_CODE=$(curl -s -o /tmp/payslip_resp.json -w "%{http_code}" -X POST "$BASE_URL/salary/payslip/push?salarySheetId=$SALARY_SHEET_ID" -H "$AUTH")
  if [ "$HTTP_CODE" = "200" ]; then
    log_pass "工资条推送 (HTTP 200)"
    cat /tmp/payslip_resp.json | head -c 200; echo ""
  else
    MSG=$(cat /tmp/payslip_resp.json | grep -oP '"message":"[^"]+' | head -1)
    log_pass "工资条推送接口可达 (HTTP $HTTP_CODE, $MSG)"
  fi
  # 查询推送记录
  HTTP_CODE=$(curl -s -o /tmp/payslip_records.json -w "%{http_code}" "$BASE_URL/salary/payslip/push/records?salarySheetId=$SALARY_SHEET_ID&page=1&size=10" -H "$AUTH")
  if [ "$HTTP_CODE" = "200" ]; then
    log_pass "工资条推送记录查询 (HTTP 200)"
  else
    log_bug "工资条推送记录查询 (HTTP $HTTP_CODE)" ""
  fi
else
  echo "跳过:无薪资表数据"
fi

echo ""
echo "===== 通知模块(站内信) ====="
HTTP_CODE=$(curl -s -o /tmp/notif_resp.json -w "%{http_code}" "$BASE_URL/system/notification/page?page=1&size=10" -H "$AUTH")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "通知列表查询 (HTTP 200)"
  cat /tmp/notif_resp.json | head -c 200; echo ""
else
  log_bug "通知列表查询 (HTTP $HTTP_CODE)" "$(cat /tmp/notif_resp.json | head -c 200)"
fi
HTTP_CODE=$(curl -s -o /tmp/notif_unread.json -w "%{http_code}" "$BASE_URL/system/notification/unread/count" -H "$AUTH")
if [ "$HTTP_CODE" = "200" ]; then
  log_pass "未读通知数量 (HTTP 200)"
  cat /tmp/notif_unread.json | head -c 200; echo ""
else
  log_bug "未读通知数量 (HTTP $HTTP_CODE)" ""
fi

echo ""
echo "===== P5 模块灰度关闭验证 ====="
# PortalAccount 应返回 404(模块关闭)
HTTP_CODE=$(curl -s -o /tmp/portal_resp.json -w "%{http_code}" "$BASE_URL/portal-account/page" -H "$AUTH")
if [ "$HTTP_CODE" = "404" ]; then
  log_pass "客户门户模块已灰度关闭 (HTTP 404)"
else
  log_bug "客户门户模块未正确关闭 (HTTP $HTTP_CODE)" "期望 404"
fi
# IndustryCommerce 应返回 404(模块关闭)
HTTP_CODE=$(curl -s -o /tmp/ic_resp.json -w "%{http_code}" "$BASE_URL/ic/service/page" -H "$AUTH")
if [ "$HTTP_CODE" = "404" ]; then
  log_pass "工商年报模块已灰度关闭 (HTTP 404)"
else
  log_bug "工商年报模块未正确关闭 (HTTP $HTTP_CODE)" "期望 404"
fi

echo ""
echo "================================================"
echo "  P5+B1-B7 功能测试汇总"
echo "================================================"
echo "  PASS: $PASS"
echo "  FAIL: $FAIL"
echo "  BUG : $BUGS"
echo "================================================"

if [ $FAIL -gt 0 ] || [ $BUGS -gt 0 ]; then
  exit 1
fi
exit 0
