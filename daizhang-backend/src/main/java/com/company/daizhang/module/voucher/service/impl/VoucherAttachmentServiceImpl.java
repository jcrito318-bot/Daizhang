package com.company.daizhang.module.voucher.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.module.voucher.entity.VoucherAttachment;
import com.company.daizhang.module.voucher.mapper.VoucherAttachmentMapper;
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

    @Value("${file.upload-path:./data/uploads/}")
    private String uploadPath;

    @Override
    public List<VoucherAttachmentVO> listByVoucherId(Long voucherId) {
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
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

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
