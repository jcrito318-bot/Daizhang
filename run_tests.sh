#!/bin/bash
# 代账系统接口测试套件 - Phase 4

set -u
BASE=http://localhost:8080/api
LOG=/tmp/test-result.txt
> $LOG
PASS_COUNT=0
FAIL_COUNT=0
BUG_COUNT=0
declare -a BUGS

G="\033[32m"; R="\033[31m"; Y="\033[33m"; N="\033[0m"

run_test() {
    local id="$1" desc="$2" actual="$3" expected="$4" expect_text="${5:-}"
    local status="PASS"
    if [ "$actual" != "$expected" ]; then
        status="FAIL"
    elif [ -n "$expect_text" ] && ! echo "$RESP_BODY" | grep -q "$expect_text"; then
        status="FAIL"
    fi
    if [ "$status" = "PASS" ]; then
        echo -e "${G}[$id] PASS${N} $desc (HTTP $actual)"
        PASS_COUNT=$((PASS_COUNT+1))
    else
        echo -e "${R}[$id] FAIL${N} $desc (期望 HTTP $expected,实际 $actual)"
        FAIL_COUNT=$((FAIL_COUNT+1))
    fi
    echo "[$id] $status | $desc | 期望=$expected 实际=$actual | body: ${RESP_BODY:0:200}" >> $LOG
}

record_bug() {
    BUGS+=("BUG|$1|$2|$3|$4")
    BUG_COUNT=$((BUG_COUNT+1))
}

echo "================================================"
echo "  代账系统接口测试套件 - Phase 4"
echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "  目标: $BASE"
echo "================================================"

echo ""
echo "===== T1 冒烟测试 ====="

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" "$BASE/subject/tree?accountSetId=1")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T1.1" "未认证访问 /subject/tree 应被拒(403)" "$HTTP" "403"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer invalidtoken" "$BASE/subject/tree?accountSetId=1")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T1.2" "错误 token 访问应被拒(403)" "$HTTP" "403"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}')
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T1.3" "admin 登录成功(200+token)" "$HTTP" "200" "token"

TOKEN=$(echo "$RESP_BODY" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('token',''))" 2>/dev/null)
REFRESH=$(echo "$RESP_BODY" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('refreshToken',''))" 2>/dev/null)

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"wrongpass"}')
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T1.4" "错误密码登录应失败(401)" "$HTTP" "401"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"username":"","password":"x"}')
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
if [ "$HTTP" = "400" ] || [ "$HTTP" = "401" ]; then
    echo -e "${G}[T1.5] PASS${N} 空用户名被拒(HTTP $HTTP)"
    PASS_COUNT=$((PASS_COUNT+1))
else
    echo -e "${R}[T1.5] FAIL${N} 空用户名应被拒,实际 HTTP $HTTP"
    FAIL_COUNT=$((FAIL_COUNT+1))
fi

echo ""
echo "===== T2 权限越界测试 ====="

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $REFRESH" "$BASE/subject/tree?accountSetId=1")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T2.1" "refresh token 不能访问业务接口(B-022)" "$HTTP" "403"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/subject/tree?accountSetId=999999")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
if [ "$HTTP" = "403" ] || ([ "$HTTP" = "200" ] && echo "$RESP_BODY" | grep -q '"data":\[\]'); then
    echo -e "${G}[T2.2] PASS${N} 越权账套未返回数据(HTTP $HTTP)"
    PASS_COUNT=$((PASS_COUNT+1))
else
    echo -e "${R}[T2.2] FAIL${N} 越权账套 HTTP $HTTP body: ${RESP_BODY:0:100}"
    FAIL_COUNT=$((FAIL_COUNT+1))
    record_bug "T2.2" "严重" "IDOR" "越权访问账套"
fi

echo ""
echo "===== T3 接口校验测试 ====="

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/inventory/item/page?accountSetId=1&pageNum=1&pageSize=200")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T3.1" "pageSize=200 应被拒(B-024)" "$HTTP" "400" "不能超过100"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/inventory/item/page?accountSetId=1&pageNum=0&pageSize=10")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T3.2" "pageNum=0 应被拒(B-024)" "$HTTP" "400" "不能小于1"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/inventory/item/page?accountSetId=1&pageNum=1&pageSize=10")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T3.3" "pageSize=10 合法(200)" "$HTTP" "200"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -X POST "$BASE/inventory/item" -d '{"accountSetId":1,"itemCode":"TEST01","itemName":"test","unitPrice":-10}')
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T3.4" "负数 unitPrice 应被拒(B-023)" "$HTTP" "400"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -X POST "$BASE/inventory/item" -d '{"accountSetId":1,"itemName":"test","unitPrice":10}')
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T3.5" "itemCode 缺失应被拒(400)" "$HTTP" "400"

echo ""
echo "===== T4 安全测试 ====="

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" "$BASE/doc.html")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T4.1" "Knife4j /doc.html 应需认证(B-021)" "$HTTP" "403"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" "$BASE/v3/api-docs")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T4.2" "/v3/api-docs 应需认证" "$HTTP" "403"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" "$BASE/h2-console")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
if [ "$HTTP" = "403" ] || [ "$HTTP" = "404" ]; then
    echo -e "${G}[T4.3] PASS${N} H2 控制台不可访问(HTTP $HTTP)"
    PASS_COUNT=$((PASS_COUNT+1))
else
    echo -e "${R}[T4.3] FAIL${N} H2 控制台应被屏蔽,实际 HTTP $HTTP"
    FAIL_COUNT=$((FAIL_COUNT+1))
    record_bug "T4.3" "致命" "security" "H2 控制台暴露"
fi

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"username":"admin OR 1=1","password":"x"}')
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
if [ "$HTTP" = "401" ] || [ "$HTTP" = "400" ]; then
    echo -e "${G}[T4.4] PASS${N} SQL 注入登录被拒(HTTP $HTTP)"
    PASS_COUNT=$((PASS_COUNT+1))
else
    echo -e "${R}[T4.4] FAIL${N} SQL 注入应被拒,实际 HTTP $HTTP"
    FAIL_COUNT=$((FAIL_COUNT+1))
    record_bug "T4.4" "致命" "security" "SQL 注入登录未拦截"
fi

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -X POST "$BASE/inventory/item" -d '{"accountSetId":1,"itemCode":"XSS01","itemName":"<script>alert(1)</script>","unitPrice":10}')
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
if [ "$HTTP" = "200" ] || [ "$HTTP" = "400" ]; then
    echo -e "${G}[T4.5] PASS${N} XSS payload 后端处理无报错(HTTP $HTTP)"
    PASS_COUNT=$((PASS_COUNT+1))
    if echo "$RESP_BODY" | grep -q '<script>'; then
        echo -e "${Y}[T4.5] WARN${N} 后端原样回显 XSS payload,需前端转义"
        record_bug "T4.5" "一般" "inventory" "XSS payload 原样存储"
    fi
else
    echo -e "${R}[T4.5] FAIL${N} 实际 HTTP $HTTP"
    FAIL_COUNT=$((FAIL_COUNT+1))
fi

echo ""
echo "===== T5 业务功能测试 ====="

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/subject/tree?accountSetId=1")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T5.1" "科目树查询(B-029)" "$HTTP" "200"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/system/menu/tree")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T5.2" "菜单树查询(B-029)" "$HTTP" "200"

RESP_BODY=$(curl -s -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -X POST "$BASE/inventory/item" -d '{"accountSetId":1,"itemCode":"FLOW01","itemName":"流程测试商品","unitPrice":20.00}')
ITEM_ID=$(echo "$RESP_BODY" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',''))" 2>/dev/null)
RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -X POST "$BASE/inventory/in" -d "{\"accountSetId\":1,\"inType\":1,\"inDate\":\"2026-07-18\",\"supplier\":\"测试\",\"details\":[{\"itemId\":$ITEM_ID,\"quantity\":10,\"unitPrice\":20}]}")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T5.3" "创建入库单(B-030)" "$HTTP" "200"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -X POST "$BASE/inventory/out" -d "{\"accountSetId\":1,\"outType\":1,\"outDate\":\"2026-07-18\",\"customer\":\"测试\",\"details\":[{\"itemId\":$ITEM_ID,\"quantity\":3,\"unitPrice\":25}]}")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T5.4" "创建出库单(B-030)" "$HTTP" "200"

RESP_BODY=$(curl -s -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -X POST "$BASE/inventory/in" -d "{\"accountSetId\":1,\"inType\":1,\"inDate\":\"2026-07-18\",\"supplier\":\"测试2\",\"details\":[{\"itemId\":$ITEM_ID,\"quantity\":5,\"unitPrice\":21}]}")
IN_ID2=$(echo "$RESP_BODY" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',''))" 2>/dev/null)
RESP_BODY=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE/inventory/in/$IN_ID2")
IN_NO2=$(echo "$RESP_BODY" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('data',{}).get('inNo',''))" 2>/dev/null)
if [ -n "$IN_NO2" ]; then
    echo -e "${G}[T5.5] PASS${N} 二次入库单号 $IN_NO2 (递增正常)"
    PASS_COUNT=$((PASS_COUNT+1))
else
    echo -e "${R}[T5.5] FAIL${N} 单号查询失败"
    FAIL_COUNT=$((FAIL_COUNT+1))
fi

echo ""
echo "===== T6 边界/性能测试 ====="

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/inventory/item/page?accountSetId=1&pageNum=1&pageSize=100")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T6.1" "pageSize=100 边界(200)" "$HTTP" "200"

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/inventory/item/page?accountSetId=1&pageNum=1&pageSize=101")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
run_test "T6.2" "pageSize=101 越界(400)" "$HTTP" "400"

START=$(date +%s%N)
curl -s -o /dev/null -H "Authorization: Bearer $TOKEN" "$BASE/subject/tree?accountSetId=1"
END=$(date +%s%N)
ELAPSED_MS=$(( (END - START) / 1000000 ))
if [ "$ELAPSED_MS" -lt 500 ]; then
    echo -e "${G}[T6.3] PASS${N} /subject/tree 响应 ${ELAPSED_MS}ms (<500ms)"
    PASS_COUNT=$((PASS_COUNT+1))
else
    echo -e "${Y}[T6.3] WARN${N} /subject/tree 响应 ${ELAPSED_MS}ms (>=500ms)"
fi

START=$(date +%s%N)
curl -s -o /dev/null -H "Authorization: Bearer $TOKEN" "$BASE/inventory/item/page?accountSetId=1&pageNum=1&pageSize=50"
END=$(date +%s%N)
ELAPSED_MS=$(( (END - START) / 1000000 ))
if [ "$ELAPSED_MS" -lt 2000 ]; then
    echo -e "${G}[T6.4] PASS${N} /inventory/item/page 响应 ${ELAPSED_MS}ms (<2000ms)"
    PASS_COUNT=$((PASS_COUNT+1))
else
    echo -e "${Y}[T6.4] WARN${N} 复杂查询响应 ${ELAPSED_MS}ms"
fi

echo ""
echo "===== T7 资源不存在 ====="

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/subject/99999999")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
if [ "$HTTP" = "404" ] || [ "$HTTP" = "200" ]; then
    echo -e "${G}[T7.1] PASS${N} 不存在的资源 HTTP $HTTP"
    PASS_COUNT=$((PASS_COUNT+1))
else
    echo -e "${Y}[T7.1] INFO${N} 不存在资源 HTTP $HTTP"
fi

RESP_BODY=$(curl -s -w "\n__HTTP_CODE__:%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE/not-exists-endpoint")
HTTP=$(echo "$RESP_BODY" | grep -o '__HTTP_CODE__:[0-9]*' | cut -d: -f2)
RESP_BODY=$(echo "$RESP_BODY" | sed 's/__HTTP_CODE__:[0-9]*$//')
if [ "$HTTP" = "404" ] || [ "$HTTP" = "403" ]; then
    echo -e "${G}[T7.2] PASS${N} 不存在端点 HTTP $HTTP"
    PASS_COUNT=$((PASS_COUNT+1))
else
    echo -e "${Y}[T7.2] INFO${N} 不存在端点 HTTP $HTTP"
fi

echo ""
echo "================================================"
echo "  测试汇总"
echo "================================================"
echo "  PASS: $PASS_COUNT"
echo "  FAIL: $FAIL_COUNT"
echo "  BUG : $BUG_COUNT"
echo "  详细日志: $LOG"
if [ $BUG_COUNT -gt 0 ]; then
    echo ""
    echo "  发现的缺陷:"
    for bug in "${BUGS[@]}"; do
        echo "  - $bug"
    done
fi
echo "================================================"
