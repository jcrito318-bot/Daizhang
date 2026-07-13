package com.company.daizhang.common.utils;

import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel导入工具类
 */
public class ExcelImportUtil {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    /**
     * 支持的日期格式
     */
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    };

    private ExcelImportUtil() {
    }

    /**
     * 解析Excel文件，返回List<Map<String,String>>，每个Map是一行，key是列名
     *
     * @param file           Excel文件
     * @param headerRowIndex 表头所在行索引（从0开始）
     */
    public static List<Map<String, String>> parseExcel(MultipartFile file, int headerRowIndex) throws IOException {
        // 文件扩展名校验:仅允许.xlsx和.xls,拒绝其他格式(如.csv/.html等)
        String filename = file.getOriginalFilename();
        if (filename == null
                || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "仅支持Excel文件(.xlsx/.xls)，当前文件：" + filename);
        }

        // 文件大小校验:WorkbookFactory全量加载到内存,超大文件可能触发OOM
        // 限制20MB,覆盖正常Excel模板(万行级约2-5MB),拒绝恶意大文件
        long maxFileSize = 20 * 1024 * 1024L;
        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "文件过大(" + (file.getSize() / 1024 / 1024) + "MB)，单次最多20MB，请分批导入或精简文件");
        }

        List<Map<String, String>> result = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return result;
            }

            // 行数限制: WorkbookFactory全量加载到内存,万行级Excel可能触发OOM
            int physicalNumberOfRows = sheet.getPhysicalNumberOfRows();
            if (physicalNumberOfRows > 10000) {
                throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                        "Excel行数过多(" + physicalNumberOfRows + ")，单次最多10000行，请分批导入");
            }

            // 读取表头行
            Row headerRow = sheet.getRow(headerRowIndex);
            if (headerRow == null) {
                throw new BusinessException("Excel表头行不存在，请检查模板格式");
            }

            // 读取表头列名
            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                String headerValue = getCellValueAsString(cell).trim();
                headers.add(headerValue);
            }

            // 读取数据行
            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                Map<String, String> rowData = new LinkedHashMap<>();
                boolean hasData = false;

                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    if (header == null || header.isEmpty()) {
                        continue;
                    }
                    Cell cell = row.getCell(j);
                    String value = getCellValueAsString(cell).trim();
                    rowData.put(header, value);
                    if (!value.isEmpty()) {
                        hasData = true;
                    }
                }

                // 跳过空行
                if (hasData) {
                    result.add(rowData);
                }
            }
        }

        return result;
    }

    /**
     * 获取单元格字符串值
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // 直接取日期值,绕开 Locale 相关的格式化字符串,避免非 zh-CN/en-US 环境下
                    // 输出 08.07.26 等无法被 toLocalDate 解析的格式
                    try {
                        LocalDateTime ldt = cell.getLocalDateTimeCellValue();
                        return ldt != null ? ldt.toLocalDate().toString() : "";
                    } catch (Exception e) {
                        // 降级:用 Date
                        try {
                            Date date = cell.getDateCellValue();
                            return date != null ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString() : "";
                        } catch (Exception e2) {
                            return "";
                        }
                    }
                }
                double numVal = cell.getNumericCellValue();
                if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                    return String.valueOf((long) numVal);
                }
                return BigDecimal.valueOf(numVal).stripTrailingZeros().toPlainString();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return DATA_FORMATTER.formatCellValue(cell);
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * 验证必填字段
     *
     * @param row    行数据
     * @param fields 必填字段名
     * @throws BusinessException 当字段为空时抛出
     */
    public static void validateRequired(Map<String, String> row, String... fields) {
        for (String field : fields) {
            String value = row.get(field);
            if (value == null || value.trim().isEmpty()) {
                throw new BusinessException("字段[" + field + "]不能为空");
            }
        }
    }

    /**
     * 转换为BigDecimal
     */
    public static BigDecimal toBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim().replaceAll(",", ""));
        } catch (NumberFormatException e) {
            throw new BusinessException("数值格式不正确: " + value);
        }
    }

    /**
     * 转换为Integer
     */
    public static Integer toInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("整数格式不正确: " + value);
        }
    }

    /**
     * 转换为LocalDate，支持多种日期格式
     */
    public static LocalDate toLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String dateStr = value.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception ignored) {
            }
        }
        throw new BusinessException("日期格式不正确: " + value + "，支持格式: yyyy-MM-dd、yyyy/MM/dd、yyyy.MM.dd、yyyy年MM月dd日");
    }
}
