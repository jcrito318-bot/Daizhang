package com.company.daizhang.module.subject.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.utils.ExcelImportUtil;
import com.company.daizhang.common.vo.ImportResultVO;
import com.company.daizhang.module.subject.entity.Subject;
import com.company.daizhang.module.subject.mapper.SubjectMapper;
import com.company.daizhang.module.subject.service.SubjectImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 科目Excel导入服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectImportServiceImpl implements SubjectImportService {

    private final SubjectMapper subjectMapper;

    /**
     * 科目编码正则：4位数字开头，可以有下级编码（每级2位）
     */
    private static final Pattern SUBJECT_CODE_PATTERN = Pattern.compile("^\\d{4}(\\d{2})*$");

    private static final String COL_CODE = "科目编码";
    private static final String COL_NAME = "科目名称";
    private static final String COL_CATEGORY = "科目类别";
    private static final String COL_DIRECTION = "余额方向";
    private static final String COL_PARENT_CODE = "上级科目编码";
    private static final String COL_AUXILIARY = "是否辅助核算";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importSubjects(Long accountSetId, MultipartFile file) {
        List<Map<String, String>> rows;
        try {
            rows = ExcelImportUtil.parseExcel(file, 0);
        } catch (IOException e) {
            log.error("解析Excel失败", e);
            return ImportResultVO.of(0, 0, 0, Collections.singletonList("Excel文件解析失败: " + e.getMessage()));
        }

        if (rows.isEmpty()) {
            return ImportResultVO.of(0, 0, 0, Collections.singletonList("Excel文件中没有数据行"));
        }

        int successCount = 0;
        int failCount = 0;
        List<String> errorMessages = new ArrayList<>();

        // 记录本批次已导入的科目编码，用于检测文件内重复
        Set<String> importedCodes = new HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            // Excel行号：表头在第1行，数据从第2行开始
            int rowNum = i + 2;
            try {
                successCount += importOneSubject(accountSetId, row, importedCodes);
            } catch (Exception e) {
                failCount++;
                errorMessages.add("第" + rowNum + "行: " + e.getMessage());
            }
        }

        log.info("科目导入完成，账套ID: {}, 总数: {}, 成功: {}, 失败: {}",
                accountSetId, rows.size(), successCount, failCount);

        return ImportResultVO.of(rows.size(), successCount, failCount, errorMessages);
    }

    /**
     * 导入单条科目
     *
     * @return 成功返回1，失败抛出异常
     */
    private int importOneSubject(Long accountSetId, Map<String, String> row, Set<String> importedCodes) {
        // 校验必填字段
        ExcelImportUtil.validateRequired(row, COL_CODE, COL_NAME, COL_CATEGORY, COL_DIRECTION);

        String code = row.get(COL_CODE).trim();
        String name = row.get(COL_NAME).trim();
        String category = row.get(COL_CATEGORY).trim();
        String directionStr = row.get(COL_DIRECTION).trim();
        String parentCode = StrUtil.trim(row.get(COL_PARENT_CODE));
        String auxiliaryStr = StrUtil.trim(row.get(COL_AUXILIARY));

        // 校验科目编码格式
        if (!SUBJECT_CODE_PATTERN.matcher(code).matches()) {
            throw new BusinessException("科目编码格式不正确（需为4位数字开头，每级2位）");
        }

        // 校验科目编码长度（最多4级，4+2+2+2=10）
        if (code.length() > 10) {
            throw new BusinessException("科目编码长度超过限制（最多4级）");
        }

        // 校验本批次内重复
        if (importedCodes.contains(code)) {
            throw new BusinessException("科目编码在导入文件中重复");
        }

        // 校验数据库内重复
        Long dbCount = subjectMapper.selectCount(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getAccountSetId, accountSetId)
                .eq(Subject::getCode, code));
        if (dbCount != null && dbCount > 0) {
            throw new BusinessException("科目编码已存在");
        }

        // 转换余额方向
        Integer balanceDirection = parseBalanceDirection(directionStr);

        // 转换是否辅助核算
        Integer isAuxiliary = parseYesNo(auxiliaryStr, 0);

        // 计算科目层级
        int level = (code.length() - 4) / 2 + 1;

        // 解析上级科目
        Long parentId = 0L;
        if (StrUtil.isNotBlank(parentCode)) {
            Subject parent = subjectMapper.selectOne(new LambdaQueryWrapper<Subject>()
                    .eq(Subject::getAccountSetId, accountSetId)
                    .eq(Subject::getCode, parentCode));
            if (parent == null) {
                throw new BusinessException("上级科目编码不存在: " + parentCode);
            }
            // 校验科目类别一致
            if (!parent.getCategory().equals(category)) {
                throw new BusinessException("科目类别与上级科目不一致");
            }
            // 校验科目编码需以上级科目编码开头
            if (!code.startsWith(parent.getCode())) {
                throw new BusinessException("科目编码需以上级科目编码开头");
            }
            parentId = parent.getId();
        }

        // 创建并保存科目
        Subject subject = new Subject();
        subject.setAccountSetId(accountSetId);
        subject.setCode(code);
        subject.setName(name);
        subject.setCategory(category);
        subject.setParentId(parentId);
        subject.setLevel(level);
        subject.setBalanceDirection(balanceDirection);
        subject.setIsAuxiliary(isAuxiliary);
        subject.setIsCash(0);
        subject.setIsBank(0);
        subject.setIsCurrent(0);
        subject.setStatus(1);
        subjectMapper.insert(subject);

        importedCodes.add(code);
        return 1;
    }

    @Override
    public byte[] downloadTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("科目导入模板");

            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {COL_CODE, COL_NAME, COL_CATEGORY, COL_DIRECTION, COL_PARENT_CODE, COL_AUXILIARY};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 示例数据
            Row sample1 = sheet.createRow(1);
            sample1.createCell(0).setCellValue("1001");
            sample1.createCell(1).setCellValue("库存现金");
            sample1.createCell(2).setCellValue("资产");
            sample1.createCell(3).setCellValue("借");
            sample1.createCell(4).setCellValue("");
            sample1.createCell(5).setCellValue("否");

            Row sample2 = sheet.createRow(2);
            sample2.createCell(0).setCellValue("100101");
            sample2.createCell(1).setCellValue("人民币");
            sample2.createCell(2).setCellValue("资产");
            sample2.createCell(3).setCellValue("借");
            sample2.createCell(4).setCellValue("1001");
            sample2.createCell(5).setCellValue("否");

            // 列宽
            sheet.setColumnWidth(0, 15 * 256);
            sheet.setColumnWidth(1, 20 * 256);
            sheet.setColumnWidth(2, 12 * 256);
            sheet.setColumnWidth(3, 12 * 256);
            sheet.setColumnWidth(4, 15 * 256);
            sheet.setColumnWidth(5, 15 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("生成科目导入模板失败", e);
            throw new BusinessException("生成模板失败");
        }
    }

    /**
     * 解析余额方向：借/1->1，贷/2->2
     */
    private Integer parseBalanceDirection(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException("余额方向不能为空");
        }
        String v = value.trim();
        if ("借".equals(v) || "1".equals(v)) {
            return 1;
        }
        if ("贷".equals(v) || "2".equals(v)) {
            return 2;
        }
        throw new BusinessException("余额方向不正确（应为'借'或'贷'）");
    }

    /**
     * 解析是/否：是/1->1，否/0->0
     */
    private Integer parseYesNo(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        String v = value.trim();
        if ("是".equals(v) || "1".equals(v) || "true".equalsIgnoreCase(v) || "Y".equalsIgnoreCase(v)) {
            return 1;
        }
        if ("否".equals(v) || "0".equals(v) || "false".equalsIgnoreCase(v) || "N".equalsIgnoreCase(v)) {
            return 0;
        }
        return defaultValue;
    }
}
