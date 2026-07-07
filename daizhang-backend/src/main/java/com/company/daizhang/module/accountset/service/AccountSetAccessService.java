package com.company.daizhang.module.accountset.service;

import com.company.daizhang.module.accountset.vo.UserAccountSetVO;

import java.util.List;
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

    /**
     * 为用户分配账套访问权限(仅账套OWNER或管理员可操作)。
     * 已存在关系则更新角色,不存在则新增。
     *
     * @param userId       被分配用户ID
     * @param accountSetId 账套ID
     * @param roleType     角色类型 OWNER/ACCOUNTANT/VIEWER
     */
    void assignAccountSet(Long userId, Long accountSetId, String roleType);

    /**
     * 移除用户的账套访问权限(仅账套OWNER或管理员可操作)。
     * 不能移除OWNER关系,防止账套无主。
     *
     * @param userId       被移除用户ID
     * @param accountSetId 账套ID
     */
    void revokeAccountSet(Long userId, Long accountSetId);

    /**
     * 查询账套下的所有用户及角色(需对该账套有访问权)。
     *
     * @param accountSetId 账套ID
     * @return 用户账套关系列表(含用户名/真实姓名)
     */
    List<UserAccountSetVO> listAccountSetUsers(Long accountSetId);

    /**
     * 查询用户可访问的所有账套及角色。
     * 管理员可查任意用户,普通用户只能查自己。
     *
     * @param userId 用户ID
     * @return 用户账套关系列表(含账套名称)
     */
    List<UserAccountSetVO> listUserAccountSets(Long userId);
}
