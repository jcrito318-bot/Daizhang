package com.company.daizhang.module.accountset.service;

import java.util.Set;

/**
 * 账套数据级授权服务(IDOR越权治理核心原语)
 * <p>
 * 所有以 accountSetId 为参数的 Controller/Service 方法,在执行业务前必须调用 {@link #checkAccess}
 * 校验当前登录用户对该账套的访问权。无权访问时抛出 FORBIDDEN(403)。
 */
public interface AccountSetAccessService {

    /**
     * 校验当前用户对指定账套的访问权(OWNER/ACCOUNTANT/VIEWER 均通过)。
     * 无权访问时抛 BusinessException(FORBIDDEN)。
     *
     * @param accountSetId 账套ID
     */
    void checkAccess(Long accountSetId);

    /**
     * 校验当前用户对指定账套的所有者权限(仅 OWNER 通过)。
     * 用于删除账套、初始化等高危操作。
     *
     * @param accountSetId 账套ID
     */
    void checkOwner(Long accountSetId);

    /**
     * 返回当前用户可访问的全部账套ID集合。
     * 用于列表/分页查询自动注入"可见账套"过滤。
     *
     * @return 可访问账套ID集合,空集合表示无任何权限
     */
    Set<Long> listAccessibleAccountSetIds();

    /**
     * 为指定用户绑定账套的所有者关系(创建账套时调用)。
     *
     * @param accountSetId 账套ID
     * @param userId       用户ID
     */
    void bindOwner(Long accountSetId, Long userId);
}
