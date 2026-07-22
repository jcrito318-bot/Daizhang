package com.company.daizhang.module.batch.service;

import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.batch.dto.BatchDepreciationRequest;
import com.company.daizhang.module.batch.dto.BatchOperationResponse;
import com.company.daizhang.module.batch.dto.BatchPeriodCloseRequest;
import com.company.daizhang.module.batch.dto.BatchReportExportRequest;
import com.company.daizhang.module.batch.dto.BatchReportGenerateRequest;
import com.company.daizhang.module.batch.dto.BatchVoucherAuditRequest;
import com.company.daizhang.module.batch.dto.BatchHistoryQueryRequest;
import com.company.daizhang.module.system.entity.SysOperationLog;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 跨账套批量操作服务
 * <p>
 * 面向代账公司的核心场景:一名会计代多家账,需批量审核凭证、批量结账、批量生成报表。
 * 设计要点:
 * <ul>
 *   <li>单个账套失败不影响其他账套(try-catch per item)</li>
 *   <li>每个账套独立事务(per item 事务,非整体事务)</li>
 *   <li>每个账套均校验当前用户的访问权限</li>
 *   <li>返回每个账套的详细成功/失败结果</li>
 * </ul>
 */
public interface BatchOperationService {

    /**
     * 批量审核凭证(跨多个账套)
     * <p>
     * 每个 item 可指定 voucherIds;为空时审核该期间所有未审核凭证。
     *
     * @param request 批量审核请求
     * @return 批量操作响应(含每个账套的结果)
     */
    BatchOperationResponse batchAuditVoucher(BatchVoucherAuditRequest request);

    /**
     * 批量结账(跨多个账套)
     *
     * @param request 批量结账请求
     * @return 批量操作响应(含每个账套的结果)
     */
    BatchOperationResponse batchClosePeriod(BatchPeriodCloseRequest request);

    /**
     * 批量生成报表(跨多个账套)
     *
     * @param request 批量生成报表请求
     * @return 批量操作响应(含每个账套的结果)
     */
    BatchOperationResponse batchGenerateReport(BatchReportGenerateRequest request);

    /**
     * 查询批量操作历史(分页)
     *
     * @param request 历史查询请求
     * @return 操作日志分页结果
     */
    PageResult<SysOperationLog> queryHistory(BatchHistoryQueryRequest request);

    /**
     * 批量导出报表(zip 打包,跨多个账套)
     * <p>
     * 遍历每个账套及报表类型,生成 Excel 并打包为 zip 流式写入响应。
     * 单个账套或单个报表类型失败时跳过并告警,不中断整个 zip。
     *
     * @param request  批量导出报表请求
     * @param response HTTP 响应(写入 zip 流)
     */
    void batchExportReportZip(BatchReportExportRequest request, HttpServletResponse response);

    /**
     * 跨账套批量计提固定资产折旧
     * <p>
     * 每个账套独立事务(per item 事务),单个账套失败不影响其他账套。
     *
     * @param request 批量计提折旧请求
     * @return 批量操作响应(含每个账套的结果)
     */
    BatchOperationResponse batchCalculateDepreciation(BatchDepreciationRequest request);
}
