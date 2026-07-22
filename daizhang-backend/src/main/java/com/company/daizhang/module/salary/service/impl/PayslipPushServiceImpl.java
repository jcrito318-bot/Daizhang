package com.company.daizhang.module.salary.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.salary.entity.Employee;
import com.company.daizhang.module.salary.entity.PayslipPushRecord;
import com.company.daizhang.module.salary.entity.SalarySheet;
import com.company.daizhang.module.salary.mapper.EmployeeMapper;
import com.company.daizhang.module.salary.mapper.PayslipPushRecordMapper;
import com.company.daizhang.module.salary.mapper.SalarySheetMapper;
import com.company.daizhang.module.salary.service.PayslipPushService;
import com.company.daizhang.module.salary.vo.PayslipPushRecordVO;
import com.company.daizhang.module.salary.vo.PayslipPushResultVO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工资条推送服务实现
 * <p>
 * 批量生成工资条PDF并记录推送状态。PDF渲染复用 OpenHTMLtopdf(与财务报表导出一致),
 * 生成文件持久化到临时目录下的 payslips 子目录。同时尝试向 sys_notification 表写入
 * 通知记录(若该表存在,由并行任务创建),失败时仅告警不阻断推送主流程。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipPushServiceImpl implements PayslipPushService {

    private final SalarySheetMapper salarySheetMapper;
    private final EmployeeMapper employeeMapper;
    private final PayslipPushRecordMapper payslipPushRecordMapper;
    private final AccountSetAccessService accountSetAccessService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * PDF文件输出目录(基于系统临时目录)
     */
    private static final String PAYSLIP_DIR = System.getProperty("java.io.tmpdir")
            + File.separator + "payslips";

    /**
     * 中文字体候选路径(与 ReportServiceImpl.useChineseFont 保持一致)
     */
    private static final String[] FONT_PATHS = {
            "/usr/share/fonts/truetype/arphic/uming.ttc",
            "/usr/share/fonts/truetype/arphic/ukai.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/truetype/simsun.ttc"
    };

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayslipPushResultVO batchPushPayslip(Long salarySheetId) {
        // 1. 查询薪资表,定位薪资期间
        SalarySheet salarySheet = salarySheetMapper.selectById(salarySheetId);
        if (salarySheet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资表不存在");
        }
        // IDOR治理:校验当前用户对该账套的所有者权限
        accountSetAccessService.checkOwner(salarySheet.getAccountSetId());

        Long accountSetId = salarySheet.getAccountSetId();
        Integer year = salarySheet.getYear();
        Integer month = salarySheet.getMonth();

        // 2. 查询该期间所有员工的薪资记录
        LambdaQueryWrapper<SalarySheet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalarySheet::getAccountSetId, accountSetId)
               .eq(SalarySheet::getYear, year)
               .eq(SalarySheet::getMonth, month)
               .orderByAsc(SalarySheet::getEmployeeId);
        List<SalarySheet> sheets = salarySheetMapper.selectList(wrapper);

        PayslipPushResultVO result = new PayslipPushResultVO();
        result.setTotalCount(sheets.size());
        int successCount = 0;
        int failCount = 0;

        // 3. 逐个员工生成工资条PDF并记录推送
        for (SalarySheet sheet : sheets) {
            try {
                String filePath = generatePayslipPdf(sheet);
                savePushRecord(sheet, "PDF", 1, filePath, null);
                tryWriteNotification(sheet);
                successCount++;
            } catch (Exception e) {
                log.error("生成工资条PDF失败: employeeId={}, sheetId={}", sheet.getEmployeeId(), sheet.getId(), e);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.length() > 490) {
                    errorMsg = errorMsg.substring(0, 490);
                }
                savePushRecord(sheet, "PDF", 2, null, errorMsg);
                failCount++;
            }
        }

        result.setSuccessCount(successCount);
        result.setFailCount(failCount);
        result.setMessage(String.format("工资条推送完成: 共%d人, 成功%d人, 失败%d人",
                sheets.size(), successCount, failCount));
        return result;
    }

    @Override
    public PageResult<PayslipPushRecordVO> pagePushRecords(Long salarySheetId, int page, int size) {
        // 查询薪资表,定位薪资期间
        SalarySheet salarySheet = salarySheetMapper.selectById(salarySheetId);
        if (salarySheet == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND.getCode(), "薪资表不存在");
        }
        // IDOR治理:校验当前用户对该账套的访问权
        accountSetAccessService.checkAccess(salarySheet.getAccountSetId());

        // 收集该期间所有薪资表ID
        LambdaQueryWrapper<SalarySheet> sheetWrapper = new LambdaQueryWrapper<>();
        sheetWrapper.eq(SalarySheet::getAccountSetId, salarySheet.getAccountSetId())
                    .eq(SalarySheet::getYear, salarySheet.getYear())
                    .eq(SalarySheet::getMonth, salarySheet.getMonth())
                    .select(SalarySheet::getId);
        List<SalarySheet> periodSheets = salarySheetMapper.selectList(sheetWrapper);
        List<Long> sheetIds = periodSheets.stream()
                .map(SalarySheet::getId)
                .collect(Collectors.toList());

        if (sheetIds.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), 0L, page, size);
        }

        // 分页查询推送记录
        Page<PayslipPushRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PayslipPushRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PayslipPushRecord::getSalarySheetId, sheetIds)
               .orderByDesc(PayslipPushRecord::getPushTime);
        Page<PayslipPushRecord> result = payslipPushRecordMapper.selectPage(pageParam, wrapper);

        List<PayslipPushRecordVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), page, size);
    }

    // ==================== 私有方法 ====================

    /**
     * 为单个员工生成工资条PDF
     * <p>构建HTML工资条模板,通过 OpenHTMLtopdf 渲染为PDF并保存到文件。</p>
     *
     * @param sheet 薪资表记录
     * @return PDF文件绝对路径
     */
    private String generatePayslipPdf(SalarySheet sheet) {
        Employee employee = employeeMapper.selectById(sheet.getEmployeeId());
        String html = buildPayslipHtml(sheet, employee);

        // 确保输出目录存在
        try {
            Path dir = Paths.get(PAYSLIP_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (Exception e) {
            throw new BusinessException("创建工资条输出目录失败: " + e.getMessage());
        }

        String fileName = String.format("payslip_%d_%d%02d_%d.pdf",
                sheet.getEmployeeId(), sheet.getYear(), sheet.getMonth(), System.currentTimeMillis());
        String filePath = PAYSLIP_DIR + File.separator + fileName;

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            applyChineseFont(builder);
            builder.toStream(os);
            builder.run();

            byte[] pdfBytes = os.toByteArray();
            Files.write(Paths.get(filePath), pdfBytes);
            return filePath;
        } catch (Exception e) {
            log.error("渲染工资条PDF失败: employeeId={}", sheet.getEmployeeId(), e);
            throw new BusinessException("生成工资条PDF失败: " + e.getMessage());
        }
    }

    /**
     * 构建工资条HTML模板
     */
    private String buildPayslipHtml(SalarySheet sheet, Employee employee) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body { font-family: 'SimSun','宋体',serif; font-size: 14px; }");
        sb.append(".title { text-align: center; font-size: 22px; font-weight: bold; margin: 20px 0; }");
        sb.append(".period { text-align: center; font-size: 14px; margin-bottom: 20px; }");
        sb.append("table { width: 100%; border-collapse: collapse; }");
        sb.append("th, td { border: 1px solid #333; padding: 8px 12px; text-align: left; }");
        sb.append("th { background-color: #f0f0f0; font-weight: bold; }");
        sb.append(".amount { text-align: right; }");
        sb.append(".total { font-weight: bold; background-color: #fafafa; }");
        sb.append("</style>");
        sb.append("</head><body>");

        sb.append("<div class='title'>工资条</div>");
        sb.append("<div class='period'>").append(sheet.getYear()).append("年")
          .append(sheet.getMonth()).append("月</div>");

        sb.append("<table>");
        // 员工信息
        sb.append("<tr><th>姓名</th><td>").append(esc(sheet.getEmployeeName())).append("</td>");
        sb.append("<th>员工编号</th><td>").append(employee != null ? esc(employee.getEmployeeCode()) : "").append("</td></tr>");
        sb.append("<tr><th>部门</th><td>").append(employee != null && employee.getDepartment() != null ? esc(employee.getDepartment()) : "").append("</td>");
        sb.append("<th>职位</th><td>").append(employee != null && employee.getPosition() != null ? esc(employee.getPosition()) : "").append("</td></tr>");

        // 薪资明细
        sb.append("<tr><th>项目</th><th colspan='3'>金额(元)</th></tr>");
        sb.append("<tr><td>基本工资</td><td class='amount' colspan='3'>").append(fmt(sheet.getBaseSalary())).append("</td></tr>");
        sb.append("<tr><td>津贴补贴</td><td class='amount' colspan='3'>").append(fmt(sheet.getAllowance())).append("</td></tr>");
        sb.append("<tr><td>奖金</td><td class='amount' colspan='3'>").append(fmt(sheet.getBonus())).append("</td></tr>");
        sb.append("<tr><td>扣款</td><td class='amount' colspan='3'>").append(fmt(sheet.getDeduction())).append("</td></tr>");
        sb.append("<tr><td>社保(个人)</td><td class='amount' colspan='3'>").append(fmt(sheet.getSocialSecurity())).append("</td></tr>");
        sb.append("<tr><td>公积金(个人)</td><td class='amount' colspan='3'>").append(fmt(sheet.getHousingFund())).append("</td></tr>");
        sb.append("<tr><td>应纳税所得额</td><td class='amount' colspan='3'>").append(fmt(sheet.getTaxableIncome())).append("</td></tr>");
        sb.append("<tr><td>个人所得税</td><td class='amount' colspan='3'>").append(fmt(sheet.getIncomeTax())).append("</td></tr>");
        sb.append("<tr class='total'><td>实发工资</td><td class='amount' colspan='3'>").append(fmt(sheet.getNetSalary())).append("</td></tr>");

        if (sheet.getRemark() != null && !sheet.getRemark().isEmpty()) {
            sb.append("<tr><th>备注</th><td colspan='3'>").append(esc(sheet.getRemark())).append("</td></tr>");
        }

        sb.append("</table>");
        sb.append("<p style='text-align:right; margin-top:20px; font-size:12px; color:#999;'>生成时间: ")
          .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
          .append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * 配置中文字体(PDF渲染),与 ReportServiceImpl.useChineseFont 保持一致
     */
    private void applyChineseFont(PdfRendererBuilder builder) {
        for (String path : FONT_PATHS) {
            File f = new File(path);
            if (f.exists()) {
                builder.useFont(f, "SimSun");
                builder.useFont(f, "宋体");
                return;
            }
        }
        log.warn("PDF导出中文字体未找到,中文可能无法正常渲染");
    }

    /**
     * 保存推送记录
     */
    private void savePushRecord(SalarySheet sheet, String pushMethod, int pushStatus,
                                String filePath, String errorMessage) {
        PayslipPushRecord record = new PayslipPushRecord();
        record.setSalarySheetId(sheet.getId());
        record.setEmployeeId(sheet.getEmployeeId());
        record.setEmployeeName(sheet.getEmployeeName());
        record.setAccountSetId(sheet.getAccountSetId());
        record.setYear(sheet.getYear());
        record.setMonth(sheet.getMonth());
        record.setPushMethod(pushMethod);
        record.setPushStatus(pushStatus);
        record.setPushTime(LocalDateTime.now());
        record.setFilePath(filePath);
        record.setErrorMessage(errorMessage);
        payslipPushRecordMapper.insert(record);
    }

    /**
     * 尝试向 sys_notification 表写入通知
     * <p>该表由并行任务(V14迁移)创建,可能尚不存在。写入失败时仅告警不阻断主流程。</p>
     */
    private void tryWriteNotification(SalarySheet sheet) {
        try {
            // 检查表是否存在
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_NAME) = 'SYS_NOTIFICATION'",
                    Integer.class);
            if (count == null || count == 0) {
                return;
            }
            Long currentUserId = SecurityUtils.getCurrentUserId();
            String title = sheet.getMonth() + "月工资条";
            String content = "您的" + sheet.getYear() + "年" + sheet.getMonth() + "月工资条已生成,请查看薪资表";
            // 使用常见列名插入;若列不匹配会抛异常被外层捕获
            jdbcTemplate.update(
                    "INSERT INTO sys_notification (user_id, type, title, content, is_read, create_time) " +
                    "VALUES (?, 'PAYSHEET', ?, ?, 0, NOW())",
                    currentUserId, title, content);
        } catch (Exception e) {
            log.warn("写入sys_notification通知失败(不影响推送主流程): {}", e.getMessage());
        }
    }

    /**
     * 实体转VO
     */
    private PayslipPushRecordVO convertToVO(PayslipPushRecord record) {
        PayslipPushRecordVO vo = new PayslipPushRecordVO();
        BeanUtil.copyProperties(record, vo);
        return vo;
    }

    /**
     * HTML转义
     */
    private String esc(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    /**
     * 金额格式化
     */
    private String fmt(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
