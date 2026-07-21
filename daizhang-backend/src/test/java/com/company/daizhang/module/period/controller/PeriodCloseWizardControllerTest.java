package com.company.daizhang.module.period.controller;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.module.period.dto.PeriodCloseWizardRequest;
import com.company.daizhang.module.period.service.PeriodCloseWizardService;
import com.company.daizhang.module.period.vo.PeriodCloseWizardVO;
import com.company.daizhang.module.period.vo.WizardStepResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * PeriodCloseWizardController 单元测试
 * <p>
 * 回归保护:验证结账向导控制器:
 * 1. closeWizard 方法标注 @RequireAccountSetAccess,且级别为 OWNER
 *    (结账为高危状态变更,仅账套所有者可执行)
 * 2. 控制器正确委托给 PeriodCloseWizardService 并封装 Result.success
 */
@ExtendWith(MockitoExtension.class)
class PeriodCloseWizardControllerTest {

    @Mock
    private PeriodCloseWizardService periodCloseWizardService;

    @InjectMocks
    private PeriodCloseWizardController controller;

    /**
     * 回归保护:closeWizard 方法必须标注 @RequireAccountSetAccess 且级别为 OWNER。
     * 结账向导涉及期间关闭与结转损益,属高危操作,必须限定 OWNER。
     */
    @Test
    void closeWizard_shouldHaveOwnerLevelRequireAccountSetAccess() throws NoSuchMethodException {
        Method method = PeriodCloseWizardController.class.getMethod(
                "closeWizard", Long.class, int.class, int.class, PeriodCloseWizardRequest.class);

        RequireAccountSetAccess annotation = method.getAnnotation(RequireAccountSetAccess.class);
        assertNotNull(annotation,
                "closeWizard 必须标注 @RequireAccountSetAccess,防止 IDOR 越权结账");
        assertEquals(RequireAccountSetAccess.AccessLevel.OWNER, annotation.value(),
                "closeWizard 应限定 OWNER 权限,实际: " + annotation.value());
        assertTrue(annotation.required(),
                "closeWizard 的 @RequireAccountSetAccess required 应为 true(fail-closed)");
    }

    /**
     * 验证 closeWizard 在 service 返回结果时正确封装为 Result.success。
     */
    @Test
    void closeWizard_shouldReturnSuccessResultWhenServiceSucceeds() {
        PeriodCloseWizardVO mockVo = new PeriodCloseWizardVO();
        ReflectionTestUtils.setField(mockVo, "steps", Collections.<WizardStepResult>emptyList());
        ReflectionTestUtils.setField(mockVo, "overallStatus", "success");
        ReflectionTestUtils.setField(mockVo, "nextPeriodOpened", true);
        ReflectionTestUtils.setField(mockVo, "successCount", 7);
        ReflectionTestUtils.setField(mockVo, "failedCount", 0);
        ReflectionTestUtils.setField(mockVo, "skippedCount", 0);

        PeriodCloseWizardRequest req = new PeriodCloseWizardRequest();
        when(periodCloseWizardService.executeCloseWizard(1L, 2026, 7, req)).thenReturn(mockVo);

        var result = controller.closeWizard(1L, 2026, 7, req);

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertSame(mockVo, result.getData());
        assertEquals("success", result.getData().getOverallStatus());
        assertTrue(result.getData().isNextPeriodOpened());
    }
}
