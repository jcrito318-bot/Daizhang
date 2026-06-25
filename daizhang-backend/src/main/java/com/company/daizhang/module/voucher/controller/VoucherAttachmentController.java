package com.company.daizhang.module.voucher.controller;

import com.company.daizhang.common.result.Result;
import com.company.daizhang.module.voucher.service.VoucherAttachmentService;
import com.company.daizhang.module.voucher.vo.VoucherAttachmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 凭证附件管理控制器
 */
@Slf4j
@Tag(name = "凭证附件管理")
@RestController
@RequestMapping("/voucher/attachment")
@RequiredArgsConstructor
public class VoucherAttachmentController {

    private final VoucherAttachmentService voucherAttachmentService;

    @Operation(summary = "根据凭证ID查询附件列表")
    @GetMapping("/list")
    public Result<List<VoucherAttachmentVO>> list(@RequestParam Long voucherId) {
        List<VoucherAttachmentVO> list = voucherAttachmentService.listByVoucherId(voucherId);
        return Result.success(list);
    }

    @Operation(summary = "上传凭证附件")
    @PostMapping("/upload")
    public Result<VoucherAttachmentVO> upload(@RequestParam Long voucherId,
                                              @RequestParam("file") MultipartFile file) {
        VoucherAttachmentVO vo = voucherAttachmentService.uploadAttachment(voucherId, file);
        return Result.success(vo);
    }

    @Operation(summary = "删除凭证附件")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        voucherAttachmentService.deleteAttachment(id);
        return Result.success();
    }
}
