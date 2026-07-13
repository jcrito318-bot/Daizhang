package com.company.daizhang.module.subject.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.common.vo.ImportResultVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.dto.SubjectCreateRequest;
import com.company.daizhang.module.subject.dto.SubjectUpdateRequest;
import com.company.daizhang.module.subject.service.SubjectImportService;
import com.company.daizhang.module.subject.service.SubjectService;
import com.company.daizhang.module.subject.vo.SubjectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 科目管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/subject")
@RequiredArgsConstructor
@Tag(name = "科目管理", description = "会计科目管理接口")
public class SubjectController {

    private final SubjectService subjectService;
    private final SubjectImportService subjectImportService;

    /**
     * 查询科目树
     */
    @GetMapping("/tree")
    @Operation(summary = "查询科目树", description = "根据账套ID查询科目树形结构")
    @RequireAccountSetAccess
    public Result<List<SubjectVO>> getTree(@RequestParam Long accountSetId) {
        List<SubjectVO> subjects = subjectService.listSubjectsByAccountSetId(accountSetId);
        
        // 构建树形结构
        List<SubjectVO> tree = buildTree(subjects, 0L);
        
        return Result.success(tree);
    }

    /**
     * 根据ID查询科目
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询科目详情", description = "根据ID查询科目详情")
    public Result<SubjectVO> getById(@PathVariable Long id) {
        SubjectVO subject = subjectService.getSubjectById(id);
        return Result.success(subject);
    }

    /**
     * 分页查询科目
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询科目", description = "根据账套ID分页查询科目，支持按编码/名称/类别筛选")
    @RequireAccountSetAccess
    public Result<PageResult<Subject>> page(@RequestParam Long accountSetId,
                                             @RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String subjectCode,
                                             @RequestParam(required = false) String subjectName,
                                             @RequestParam(required = false) String category) {
        Page<Subject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Subject> wrapper = new LambdaQueryWrapper<Subject>()
                .eq(Subject::getAccountSetId, accountSetId)
                .like(StrUtil.isNotBlank(subjectCode), Subject::getCode, subjectCode)
                .like(StrUtil.isNotBlank(subjectName), Subject::getName, subjectName)
                .eq(StrUtil.isNotBlank(category), Subject::getCategory, category)
                .orderByAsc(Subject::getCode);
        Page<Subject> result = subjectService.page(page, wrapper);
        return Result.success(new PageResult<>(result.getRecords(), result.getTotal(), pageNum, pageSize));
    }

    /**
     * 创建科目
     */
    @PostMapping
    @Operation(summary = "创建科目", description = "创建新的会计科目")
    @RequireAccountSetAccess
    public Result<SubjectVO> create(@Valid @RequestBody SubjectCreateRequest request) {
        subjectService.createSubject(request);
        return Result.success(null);
    }

    /**
     * 更新科目
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新科目", description = "更新会计科目信息")
    public Result<SubjectVO> update(@PathVariable Long id, @Valid @RequestBody SubjectUpdateRequest request) {
        subjectService.updateSubject(id, request);
        return Result.success(null);
    }

    /**
     * 删除科目
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除科目", description = "删除会计科目")
    public Result<Void> delete(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return Result.success(null);
    }

    /**
     * 初始化默认科目
     */
    @PostMapping("/init")
    @Operation(summary = "初始化默认科目", description = "为账套初始化默认科目模板")
    @RequireAccountSetAccess
    public Result<Void> initDefaultSubjects(@RequestParam Long accountSetId, 
                                            @RequestParam(defaultValue = "小企业会计准则") String accountingStandard) {
        subjectService.initDefaultSubjects(accountSetId, accountingStandard);
        return Result.success(null);
    }

    /**
     * 批量导入科目
     */
    @PostMapping("/import")
    @Operation(summary = "批量导入科目", description = "从Excel文件批量导入科目")
    @RequireAccountSetAccess
    public Result<ImportResultVO> importSubjects(@RequestParam Long accountSetId,
                                                  @RequestParam("file") MultipartFile file) {
        ImportResultVO result = subjectImportService.importSubjects(accountSetId, file);
        return Result.success(result);
    }

    /**
     * 下载科目导入模板
     */
    @GetMapping("/import/template")
    @Operation(summary = "下载科目导入模板", description = "下载科目Excel导入模板")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        byte[] data = subjectImportService.downloadTemplate();
        writeExcelResponse(response, data, "科目导入模板.xlsx");
    }

    /**
     * 导出科目列表Excel
     */
    @GetMapping("/export")
    @Operation(summary = "导出科目列表Excel", description = "根据账套ID导出科目列表为Excel")
    @RequireAccountSetAccess
    public void export(@RequestParam Long accountSetId,
                       @RequestParam(required = false) Integer year,
                       HttpServletResponse response) throws IOException {
        List<SubjectVO> subjects = subjectService.listSubjectsByAccountSetId(accountSetId);

        byte[] data;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("科目列表");

            CellStyle headerStyle = createHeaderStyle(workbook);

            // 表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"科目编码", "科目名称", "科目类别", "余额方向", "是否辅助核算", "状态"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 1;
            for (SubjectVO vo : subjects) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(vo.getSubjectCode() != null ? vo.getSubjectCode() : "");
                row.createCell(1).setCellValue(vo.getSubjectName() != null ? vo.getSubjectName() : "");
                row.createCell(2).setCellValue(vo.getCategory() != null ? vo.getCategory() : "");
                row.createCell(3).setCellValue(balanceDirectionText(vo.getBalanceDirection()));
                row.createCell(4).setCellValue(vo.getAuxiliaryAccounting() != null && vo.getAuxiliaryAccounting() == 1 ? "是" : "否");
                row.createCell(5).setCellValue(vo.getStatus() != null && vo.getStatus() == 1 ? "启用" : "禁用");
            }

            // 列宽
            sheet.setColumnWidth(0, 15 * 256);
            sheet.setColumnWidth(1, 25 * 256);
            sheet.setColumnWidth(2, 14 * 256);
            sheet.setColumnWidth(3, 12 * 256);
            sheet.setColumnWidth(4, 15 * 256);
            sheet.setColumnWidth(5, 10 * 256);

            workbook.write(out);
            data = out.toByteArray();
        } catch (IOException e) {
            log.error("导出科目列表失败", e);
            throw new BusinessException("导出科目列表失败");
        }

        writeExcelResponse(response, data, "科目列表_" + accountSetId + ".xlsx");
    }

    /**
     * 构建树形结构
     */
    private List<SubjectVO> buildTree(List<SubjectVO> subjects, Long parentId) {
        return subjects.stream()
                .filter(subject -> parentId.equals(subject.getParentId()))
                .peek(subject -> {
                    List<SubjectVO> children = buildTree(subjects, subject.getId());
                    subject.setChildren(children);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 输出Excel文件到响应
     */
    private void writeExcelResponse(HttpServletResponse response, byte[] data, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }

    /**
     * 表头样式：加粗居中
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        return headerStyle;
    }

    /**
     * 余额方向转换：1-借/2-贷
     */
    private String balanceDirectionText(Integer direction) {
        if (direction == null) {
            return "";
        }
        switch (direction) {
            case 1:
                return "借";
            case 2:
                return "贷";
            default:
                return "";
        }
    }
}
