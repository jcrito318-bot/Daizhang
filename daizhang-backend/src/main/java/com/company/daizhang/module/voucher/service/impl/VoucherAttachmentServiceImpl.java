package com.company.daizhang.module.voucher.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import com.company.daizhang.module.voucher.entity.Voucher;
import com.company.daizhang.module.voucher.entity.VoucherAttachment;
import com.company.daizhang.module.voucher.mapper.VoucherAttachmentMapper;
import com.company.daizhang.module.voucher.mapper.VoucherMapper;
import com.company.daizhang.module.voucher.service.VoucherAttachmentService;
import com.company.daizhang.module.voucher.vo.VoucherAttachmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 凭证附件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherAttachmentServiceImpl implements VoucherAttachmentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "gif", "xls", "xlsx", "doc", "docx", "txt", "zip"
    );

    private final VoucherAttachmentMapper voucherAttachmentMapper;
    private final VoucherMapper voucherMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Value("${file.upload-path:./data/uploads/}")
    private String uploadPath;

    @Override
    public List<VoucherAttachmentVO> listByVoucherId(Long voucherId) {
        // IDOR治理:校验当前用户对该凭证所属账套的访问权
        Long accountSetId = getVoucherAccountSetId(voucherId);
        accountSetAccessService.checkAccess(accountSetId);

        LambdaQueryWrapper<VoucherAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoucherAttachment::getVoucherId, voucherId)
               .orderByDesc(VoucherAttachment::getCreateTime);
        List<VoucherAttachment> list = voucherAttachmentMapper.selectList(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VoucherAttachmentVO uploadAttachment(Long voucherId, MultipartFile file) {
        // 先做文件安全校验:文件类型校验无需查库,任何无效文件都应最早被拒,避免无效请求消耗数据库资源
        validateFile(file);

        // IDOR治理:校验当前用户对该凭证所属账套的所有者权限(写操作)
        Long accountSetId = getVoucherAccountSetId(voucherId);
        accountSetAccessService.checkOwner(accountSetId);

        // 构建上传目录: {uploadPath}/voucher/{voucherId}/
        String dirPath = uploadPath + "voucher/" + voucherId + "/";
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("创建上传目录失败：" + dirPath);
        }

        // 使用UUID重命名文件避免冲突，保留原扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString().replace("-", "") + extension;
        File destFile = new File(dir, newFileName);

        try {
            file.transferTo(destFile.getAbsoluteFile());
        } catch (IOException e) {
            log.error("上传文件失败", e);
            throw new BusinessException("上传文件失败：" + e.getMessage());
        }

        // 保存附件记录到数据库
        VoucherAttachment attachment = new VoucherAttachment();
        attachment.setVoucherId(voucherId);
        attachment.setFileName(originalFilename);
        attachment.setFilePath(destFile.getAbsolutePath());
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        voucherAttachmentMapper.insert(attachment);

        log.info("凭证附件上传成功，voucherId={}，fileName={}", voucherId, originalFilename);
        return convertToVO(attachment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long id) {
        VoucherAttachment attachment = voucherAttachmentMapper.selectById(id);
        if (attachment == null) {
            throw new BusinessException("附件不存在");
        }
        // IDOR治理:校验当前用户对该附件所属凭证的账套所有者权限
        Long accountSetId = getVoucherAccountSetId(attachment.getVoucherId());
        accountSetAccessService.checkOwner(accountSetId);

        // 删除物理文件
        File file = new File(attachment.getFilePath());
        if (file.exists() && !file.delete()) {
            log.warn("删除附件文件失败：{}", attachment.getFilePath());
        }

        // 逻辑删除数据库记录
        voucherAttachmentMapper.deleteById(id);
        log.info("凭证附件删除成功，id={}", id);
    }

    /**
     * 根据凭证ID获取账套ID,用于权限校验
     */
    private Long getVoucherAccountSetId(Long voucherId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException("凭证不存在");
        }
        return voucher.getAccountSetId();
    }

    /**
     * 实体转VO
     */
    private VoucherAttachmentVO convertToVO(VoucherAttachment attachment) {
        VoucherAttachmentVO vo = new VoucherAttachmentVO();
        BeanUtil.copyProperties(attachment, vo);
        return vo;
    }

    /**
     * 文件安全校验:大小、扩展名白名单、Magic Number
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件大小不能超过10MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }
        // 防路径穿越:仅取文件名部分
        String filename = originalFilename;
        if (filename.contains("/") || filename.contains("\\")) {
            filename = filename.substring(Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1);
        }
        if (filename.contains("..")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名非法");
        }
        String extension = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的文件类型,允许: " + String.join(",", ALLOWED_EXTENSIONS));
        }

        // Magic Number 校验(txt 无固定 magic number,跳过)
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = is.read(header);
            if (read > 0) {
                validateMagicNumber(header, read, extension);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件读取失败");
        }
    }

    /**
     * 校验文件头Magic Number与扩展名是否匹配
     */
    private void validateMagicNumber(byte[] header, int length, String extension) {
        // PDF: %PDF-
        boolean pdfHeader = length >= 5
                && header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44
                && header[3] == 0x46 && header[4] == 0x2D;
        // PNG: 89 50 4E 47
        boolean pngHeader = length >= 8
                && header[0] == (byte) 0x89 && header[1] == 0x50
                && header[2] == 0x4E && header[3] == 0x47;
        // JPG/JPEG: FF D8 FF
        boolean jpgHeader = length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
        // GIF: 47 49 46 38
        boolean gifHeader = length >= 4
                && header[0] == 0x47 && header[1] == 0x49
                && header[2] == 0x46 && header[3] == 0x38;
        // ZIP-based (xls/xlsx/doc/docx/zip): 50 4B 03 04
        boolean zipHeader = length >= 4
                && header[0] == 0x50 && header[1] == 0x4B
                && header[2] == 0x03 && header[3] == 0x04;

        switch (extension) {
            case "pdf":
                if (!pdfHeader) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配(pdf)");
                }
                break;
            case "png":
                if (!pngHeader) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配(png)");
                }
                break;
            case "jpg":
            case "jpeg":
                if (!jpgHeader) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配(jpg)");
                }
                break;
            case "gif":
                if (!gifHeader) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配(gif)");
                }
                break;
            case "xls":
            case "xlsx":
            case "doc":
            case "docx":
            case "zip":
                if (!zipHeader) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不匹配(office/zip)");
                }
                break;
            case "txt":
                // 无固定 magic number,跳过
                break;
            default:
                break;
        }
    }
}
