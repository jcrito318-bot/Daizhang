package com.company.daizhang.module.customer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.customer.dto.PortalAccountRequest;
import com.company.daizhang.module.customer.entity.PortalAccount;
import com.company.daizhang.module.customer.mapper.PortalAccountMapper;
import com.company.daizhang.module.customer.service.PortalAccountService;
import com.company.daizhang.module.customer.vo.PortalAccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 客户看账门户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalAccountServiceImpl extends ServiceImpl<PortalAccountMapper, PortalAccount> implements PortalAccountService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public List<PortalAccountVO> listPortals(Long customerId) {
        LambdaQueryWrapper<PortalAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(customerId != null, PortalAccount::getCustomerId, customerId)
               .orderByDesc(PortalAccount::getCreateTime);
        List<PortalAccount> list = this.list(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPortal(PortalAccountRequest request) {
        // 业务校验：密码不能为空
        if (StrUtil.isBlank(request.getPortalPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "门户密码不能为空");
        }

        // 业务校验：密码长度不能少于6位
        if (request.getPortalPassword().length() < 6) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_TOO_SHORT);
        }

        // 检查用户名是否已存在
        LambdaQueryWrapper<PortalAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalAccount::getPortalUsername, request.getPortalUsername());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "门户用户名已存在");
        }

        PortalAccount portalAccount = new PortalAccount();
        BeanUtil.copyProperties(request, portalAccount);
        // 密码BCrypt加密
        portalAccount.setPortalPassword(passwordEncoder.encode(request.getPortalPassword()));
        if (portalAccount.getStatus() == null) {
            portalAccount.setStatus(1);
        }
        this.save(portalAccount);

        log.info("创建客户看账门户成功，门户ID: {}, 用户名: {}", portalAccount.getId(), portalAccount.getPortalUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePortal(Long id, PortalAccountRequest request) {
        PortalAccount portalAccount = this.getById(id);
        if (portalAccount == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门户账户不存在");
        }

        // 检查用户名是否与其他记录冲突
        LambdaQueryWrapper<PortalAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalAccount::getPortalUsername, request.getPortalUsername())
               .ne(PortalAccount::getId, id);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "门户用户名已存在");
        }

        // 保留原密码，不通过更新接口修改密码
        String originalPassword = portalAccount.getPortalPassword();
        BeanUtil.copyProperties(request, portalAccount);
        portalAccount.setId(id);
        portalAccount.setPortalPassword(originalPassword);
        this.updateById(portalAccount);

        log.info("更新客户看账门户成功，门户ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePortal(Long id) {
        PortalAccount portalAccount = this.getById(id);
        if (portalAccount == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门户账户不存在");
        }

        this.removeById(id);

        log.info("删除客户看账门户成功，门户ID: {}, 用户名: {}", id, portalAccount.getPortalUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        // 业务校验：新密码不能为空
        if (StrUtil.isBlank(newPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能为空");
        }

        // 业务校验：密码长度不能少于6位
        if (newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_TOO_SHORT);
        }

        PortalAccount portalAccount = this.getById(id);
        if (portalAccount == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门户账户不存在");
        }

        portalAccount.setPortalPassword(passwordEncoder.encode(newPassword));
        this.updateById(portalAccount);

        log.info("重置门户密码成功，门户ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        // 业务校验：状态值不能为空
        if (status == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值不能为空");
        }

        // 业务校验：状态值必须是0或1
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.USER_STATUS_INVALID);
        }

        PortalAccount portalAccount = this.getById(id);
        if (portalAccount == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "门户账户不存在");
        }

        portalAccount.setStatus(status);
        this.updateById(portalAccount);

        log.info("更新门户状态成功，门户ID: {}, 状态: {}", id, status);
    }

    /**
     * 实体转VO(不返回密码)
     */
    private PortalAccountVO convertToVO(PortalAccount portalAccount) {
        PortalAccountVO vo = new PortalAccountVO();
        BeanUtil.copyProperties(portalAccount, vo);
        return vo;
    }
}
