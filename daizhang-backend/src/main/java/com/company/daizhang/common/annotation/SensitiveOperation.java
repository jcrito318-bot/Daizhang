package com.company.daizhang.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感操作注解 (P3.4)
 * <p>
 * 标注在控制器方法上,表示该操作为敏感/危险操作,需要二次确认。
 * <p>
 * 二次确认机制:
 * <ul>
 *   <li>前端:调用方在发起请求前通过 ElMessageBox.confirm 弹窗确认,确认后请求自动携带
 *       {@code X-Confirm: true} 请求头(见前端 utils/request.ts 的 sensitive 标记机制)。</li>
 *   <li>后端:由 {@link com.company.daizhang.common.aspect.SensitiveOperationAspect} 切面拦截,
 *       检查请求头 {@code X-Confirm} 是否为 "true";不是则抛出 BusinessException 直接拒绝。</li>
 * </ul>
 * <p>
 * 该注解不破坏现有 API 契约(只是增加可选的二次确认),前端未升级时调用方需显式带 X-Confirm 头。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveOperation {

    /**
     * 敏感操作描述,用于审计日志与前端确认弹窗提示。
     * 默认空字符串,由切面回退使用方法名。
     */
    String value() default "";
}
