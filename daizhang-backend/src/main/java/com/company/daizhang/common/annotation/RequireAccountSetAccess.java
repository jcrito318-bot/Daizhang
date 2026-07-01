package com.company.daizhang.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 账套访问授权注解(IDOR越权治理)
 * <p>
 * 标注在 Controller 方法上,由 {@code AccountSetAccessAspect} 切面拦截,
 * 自动从方法参数中解析名为 accountSetId 的参数并调用 {@code AccountSetAccessService.checkAccess} 校验。
 * <p>
 * 用法:
 * <pre>
 * &#64;PostMapping("/close")
 * &#64;RequireAccountSetAccess
 * public Result&lt;?&gt; close(@RequestParam Long accountSetId, ...) { ... }
 * </pre>
 *
 * @see com.company.daizhang.common.aspect.AccountSetAccessAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAccountSetAccess {

    /**
     * 授权级别,默认 ACCESS(OWNER/ACCOUNTANT/VIEWER 均通过),
     * OWNER 仅所有者通过(用于删除/初始化等高危操作)
     */
    AccessLevel value() default AccessLevel.ACCESS;

    /**
     * 授权级别枚举
     */
    enum AccessLevel {
        /** 访问级:OWNER/ACCOUNTANT/VIEWER 均通过 */
        ACCESS,
        /** 所有者级:仅 OWNER 通过 */
        OWNER
    }
}
