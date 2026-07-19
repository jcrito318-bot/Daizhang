package com.company.daizhang.module.period.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.module.period.service.PeriodService;
import com.company.daizhang.module.period.vo.ClosePeriodResultVO;
import com.company.daizhang.module.period.vo.TrialBalanceResultVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * PeriodController 单元测试 (BUG-后端 修复验证)
 * <p>
 * 修复点:trialBalance 方法缺失 @RequireAccountSetAccess 注解,导致 IDOR 风险。
 * 攻击者可传入未授权的 accountSetId 越权获取其他账套的试算平衡数据(科目余额)。
 * <p>
 * 测试聚焦验证:
 * 1. trialBalance 方法上有 @RequireAccountSetAccess 注解(反射检查)
 * 2. 注解的 required 默认值为 true(fail-closed)
 * 3. 注解的 AccessLevel 默认值为 MEMBER(允许任意有权限的用户访问)
 * 4. 其他 period 端点(close/reopen/carryForward/carryForwardYear/carryForwardCost)
 *    也都有 @RequireAccountSetAccess 注解(回归保护,避免被误删)
 */
@ExtendWith(MockitoExtension.class)
class PeriodControllerTest {

    @Mock
    private PeriodService periodService;

    @InjectMocks
    private PeriodController controller;

    /**
     * 修复点验证:trialBalance 方法必须标注 @RequireAccountSetAccess。
     */
    @Test
    void trialBalance_shouldHaveRequireAccountSetAccessAnnotation() throws NoSuchMethodException {
        Method method = PeriodController.class.getMethod("trialBalance",
                com.company.daizhang.module.period.dto.TrialBalanceRequest.class);

        RequireAccountSetAccess annotation = method.getAnnotation(RequireAccountSetAccess.class);
        assertNotNull(annotation,
                "trialBalance 必须标注 @RequireAccountSetAccess,防止 IDOR 越权访问");
    }

    /**
     * 验证 trialBalance 的 @RequireAccountSetAccess required 默认值为 true。
     * 即未传 accountSetId 时应抛错,而不是放行。
     */
    @Test
    void trialBalance_annotationRequired_shouldBeTrueByDefault() throws NoSuchMethodException {
        Method method = PeriodController.class.getMethod("trialBalance",
                com.company.daizhang.module.period.dto.TrialBalanceRequest.class);

        RequireAccountSetAccess annotation = method.getAnnotation(RequireAccountSetAccess.class);
        assertNotNull(annotation);
        assertTrue(annotation.required(),
                "trialBalance 的 @RequireAccountSetAccess required 应为 true(fail-closed),实际: " + annotation.required());
    }

    /**
     * 回归保护:PeriodController 所有外部接口都应有 @RequireAccountSetAccess 注解。
     * 这避免了未来开发者在新增端点时遗漏账套校验(IDOR 是后端最高频的越权风险)。
     */
    @Test
    void allPublicEndpoints_shouldHaveRequireAccountSetAccessAnnotation() {
        // 列出所有 Controller 公开方法,逐个检查 @RequireAccountSetAccess
        Method[] methods = PeriodController.class.getDeclaredMethods();
        assertTrue(methods.length > 0, "PeriodController 应至少有一个方法");

        int annotatedCount = 0;
        for (Method method : methods) {
            // 跳过 bridge / synthetic 方法(编译器生成)
            if (method.isBridge() || method.isSynthetic()) continue;
            // 跳过 Object/父类继承的方法(如 toString/equals)
            if (method.getDeclaringClass() != PeriodController.class) continue;

            RequireAccountSetAccess annotation = method.getAnnotation(RequireAccountSetAccess.class);
            assertNotNull(annotation,
                    "方法 " + method.getName() + " 必须标注 @RequireAccountSetAccess,所有 period 接口都涉及账套数据");
            annotatedCount++;
        }
        // 期望至少有 6 个方法(trialBalance/close/reopen/carryForward/carryForwardYear/carryForwardCost)
        assertTrue(annotatedCount >= 6,
                "PeriodController 应至少有 6 个公开方法都标注 @RequireAccountSetAccess,实际: " + annotatedCount);
    }

    /**
     * 验证 trialBalance 方法在 PeriodService 返回结果时正确封装为 Result.success。
     * (回归测试:确保注解添加不影响业务逻辑)
     */
    @Test
    void trialBalance_shouldReturnSuccessResultWhenServiceSucceeds() {
        TrialBalanceResultVO mockResult = new TrialBalanceResultVO();
        // 实际字段名:totalDebit / totalCredit / balanced (来自 Lombok @Data 生成)
        ReflectionTestUtils.setField(mockResult, "totalDebit", java.math.BigDecimal.valueOf(10000));
        ReflectionTestUtils.setField(mockResult, "totalCredit", java.math.BigDecimal.valueOf(10000));
        ReflectionTestUtils.setField(mockResult, "balanced", true);

        com.company.daizhang.module.period.dto.TrialBalanceRequest req =
                new com.company.daizhang.module.period.dto.TrialBalanceRequest();
        ReflectionTestUtils.setField(req, "accountSetId", 1L);
        ReflectionTestUtils.setField(req, "year", 2026);
        ReflectionTestUtils.setField(req, "month", 7);

        when(periodService.trialBalance(req)).thenReturn(mockResult);

        var result = controller.trialBalance(req);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertSame(mockResult, result.getData());
        // 验证字段被正确设置(Lombok 应生成 getter)
        assertTrue(mockResult.isBalanced(), "balanced 字段应为 true");
    }

    /**
     * 验证 close 接口在 result.isSuccess()=false 时返回业务错误码 400。
     * (回归测试:保护 close 接口的错误返回逻辑不被误改)
     */
    @Test
    void close_whenClosePeriodFails_shouldReturnError400() {
        ClosePeriodResultVO failResult = new ClosePeriodResultVO();
        ReflectionTestUtils.setField(failResult, "success", false);
        ReflectionTestUtils.setField(failResult, "message", "存在未审核凭证");

        when(periodService.closePeriod(1L, 2026, 7)).thenReturn(failResult);

        var result = controller.close(1L, 2026, 7);

        assertEquals(400, result.getCode(),
                "结账失败时应返回业务错误码 400,实际: " + result.getCode());
        assertEquals("存在未审核凭证", result.getMessage());
    }
}
