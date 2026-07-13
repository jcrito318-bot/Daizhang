package com.company.daizhang.module.dashboard.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.dashboard.vo.DashboardVO;
import com.company.daizhang.module.dashboard.vo.TodoItemVO;

/**
 * 代账公司运营看板服务
 */
public interface DashboardService {

    /**
     * 获取运营看板总览（含统计、客户摘要、待办看板）
     *
     * @param accountSetId 可选账套ID；为空时返回当前用户可访问的全部账套汇总，
     *                     非空时在可访问范围内进一步限定到该账套(无权访问时返回空结果)
     */
    DashboardVO getDashboard(Long accountSetId);

    /**
     * 分页查询待办事项（合并服务任务、凭证审核、税务申报，按创建时间倒序）
     *
     * @param page 页码（从1开始）
     * @param size 每页条数
     */
    PageResult<TodoItemVO> pageTodoItems(int page, int size);
}
