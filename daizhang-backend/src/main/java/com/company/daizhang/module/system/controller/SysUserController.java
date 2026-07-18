package com.company.daizhang.module.system.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.system.dto.UserCreateRequest;
import com.company.daizhang.module.system.dto.UserQueryRequest;
import com.company.daizhang.module.system.dto.UserUpdateRequest;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.service.SysUserService;
import com.company.daizhang.module.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 用户管理控制器
 */
@Slf4j
@Tag(name = "用户管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysUserService userService;
    
    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<UserVO>> page(UserQueryRequest request) {
        PageResult<UserVO> page = userService.pageUsers(request);
        return Result.success(page);
    }

    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> getById(@PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        return Result.success(user);
    }
    
    @Operation(summary = "创建用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> create(@Valid @RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return Result.success();
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        userService.updateUser(id, request);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return Result.success();
    }
    
    @Operation(summary = "更新状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "导出用户列表Excel")
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void export(UserQueryRequest request, HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(request.getUsername()), SysUser::getUsername, request.getUsername())
               .like(StrUtil.isNotBlank(request.getRealName()), SysUser::getRealName, request.getRealName())
               .like(StrUtil.isNotBlank(request.getPhone()), SysUser::getPhone, request.getPhone())
               .eq(request.getStatus() != null, SysUser::getStatus, request.getStatus())
               .orderByDesc(SysUser::getCreateTime);
        List<SysUser> users = userService.list(wrapper);

        byte[] data;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户列表");

            CellStyle headerStyle = createHeaderStyle(workbook);

            // 表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"用户名", "真实姓名", "手机号", "邮箱", "状态", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 数据行
            int rowNum = 1;
            for (SysUser user : users) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getUsername() != null ? user.getUsername() : "");
                row.createCell(1).setCellValue(user.getRealName() != null ? user.getRealName() : "");
                row.createCell(2).setCellValue(user.getPhone() != null ? user.getPhone() : "");
                row.createCell(3).setCellValue(user.getEmail() != null ? user.getEmail() : "");
                row.createCell(4).setCellValue(user.getStatus() != null && user.getStatus() == 1 ? "启用" : "禁用");
                row.createCell(5).setCellValue(formatTime(user.getCreateTime()));
            }

            // 列宽
            sheet.setColumnWidth(0, 15 * 256);
            sheet.setColumnWidth(1, 15 * 256);
            sheet.setColumnWidth(2, 15 * 256);
            sheet.setColumnWidth(3, 25 * 256);
            sheet.setColumnWidth(4, 10 * 256);
            sheet.setColumnWidth(5, 20 * 256);

            workbook.write(out);
            data = out.toByteArray();
        } catch (IOException e) {
            log.error("导出用户列表失败", e);
            throw new RuntimeException("导出用户列表失败", e);
        }

        writeExcelResponse(response, data, "用户列表.xlsx");
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
     * 格式化时间
     */
    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DT_FMT) : "";
    }
}
