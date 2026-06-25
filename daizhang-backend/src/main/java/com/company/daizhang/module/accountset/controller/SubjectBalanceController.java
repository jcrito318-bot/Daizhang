package com.company.daizhang.module.accountset.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.common.vo.ImportResultVO;
import com.company.daizhang.module.accountset.dto.SubjectBalanceRequest;
import com.company.daizhang.module.accountset.service.SubjectBalanceImportService;
import com.company.daizhang.module.accountset.service.SubjectBalanceService;
import com.company.daizhang.module.accountset.vo.SubjectBalanceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 期初余额管理控制器
 */
@Slf4j
@Tag(name = "期初余额管理")
@RestController
@RequestMapping("/subject-balance")
@RequiredArgsConstructor
public class SubjectBalanceController {

    private final SubjectBalanceService subjectBalanceService;
    private final SubjectBalanceImportService subjectBalanceImportService;

    @Operation(summary = "查询期初余额列表")
    @GetMapping("/list")
    public Result<List<SubjectBalanceVO>> list(@RequestParam Long accountSetId,
                                                @RequestParam Integer year) {
        List<SubjectBalanceVO> list = subjectBalanceService.listByAccountSetAndYear(accountSetId, year);
        return Result.success(list);
    }

    @Operation(summary = "批量保存期初余额")
    @PostMapping("/batch")
    public Result<Void> batchSave(@RequestParam Long accountSetId,
                                  @RequestParam Integer year,
                                  @RequestBody List<SubjectBalanceRequest> requests) {
        subjectBalanceService.saveBatch(accountSetId, year, requests);
        return Result.success();
    }

    @Operation(summary = "试算平衡")
    @GetMapping("/trial-balance")
    public Result<Map<String, Object>> trialBalance(@RequestParam Long accountSetId,
                                                     @RequestParam Integer year) {
        Map<String, Object> result = subjectBalanceService.trialBalance(accountSetId, year);
        return Result.success(result);
    }

    @Operation(summary = "批量导入期初余额")
    @PostMapping("/import")
    public Result<ImportResultVO> importBalances(@RequestParam Long accountSetId,
                                                @RequestParam Integer year,
                                                @RequestParam("file") MultipartFile file) {
        ImportResultVO result = subjectBalanceImportService.importBalances(accountSetId, year, file);
        return Result.success(result);
    }

    @Operation(summary = "下载期初余额导入模板")
    @GetMapping("/import/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        byte[] data = subjectBalanceImportService.downloadTemplate();
        writeExcelResponse(response, data, "期初余额导入模板.xlsx");
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
}
