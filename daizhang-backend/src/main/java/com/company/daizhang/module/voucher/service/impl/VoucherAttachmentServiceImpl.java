package com.company.daizhang.module.voucher.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.utils.FileValidationUtil;
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
        FileValidationUtil.validate(file);

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
}
