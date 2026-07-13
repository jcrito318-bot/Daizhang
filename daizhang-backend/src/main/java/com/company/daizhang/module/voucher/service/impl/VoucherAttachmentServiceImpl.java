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

    private final VoucherAttachmentMapper voucherAttachmentMapper;
    private final VoucherMapper voucherMapper;
    private final AccountSetAccessService accountSetAccessService;

    @Value("${file.upload-path:./data/uploads/}")
    private String uploadPath;

    /**
     * 允许上传的文件扩展名白名单(仅图片和常用文档格式,拒绝可执行/脚本类文件)
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp",
            ".pdf", ".xls", ".xlsx", ".doc", ".docx", ".zip"
    );

    /**
     * 服务层文件大小上限(10MB),不依赖全局配置作为唯一防线
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Override
    public List<VoucherAttachmentVO> listByVoucherId(Long voucherId) {
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        accountSetAccessService.checkAccess(voucher.getAccountSetId());
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
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        accountSetAccessService.checkOwner(voucher.getAccountSetId());
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 服务层文件大小校验:不依赖全局multipart配置作为唯一防线
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小超过限制(最大10MB)，当前大小：" + file.getSize() / 1024 / 1024 + "MB");
        }

        // 文件类型白名单校验:防止上传.html/.svg/.js/.exe等可执行或脚本类文件
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的文件类型：" + extension
                    + "，允许的格式：jpg/jpeg/png/gif/bmp/pdf/xls/xlsx/doc/docx/zip");
        }

        // 构建上传目录: {uploadPath}/voucher/{voucherId}/
        String dirPath = uploadPath + "voucher/" + voucherId + "/";
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("创建上传目录失败：" + dirPath);
        }

        // 使用UUID重命名文件避免冲突，保留原扩展名(extension已在上方白名单校验中提取)
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
        Voucher voucher = voucherMapper.selectById(attachment.getVoucherId());
        if (voucher == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        accountSetAccessService.checkOwner(voucher.getAccountSetId());

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
     * 实体转VO
     */
    private VoucherAttachmentVO convertToVO(VoucherAttachment attachment) {
        VoucherAttachmentVO vo = new VoucherAttachmentVO();
        BeanUtil.copyProperties(attachment, vo);
        return vo;
    }
}
