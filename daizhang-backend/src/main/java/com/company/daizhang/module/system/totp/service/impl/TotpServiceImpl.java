package com.company.daizhang.module.system.totp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.system.totp.entity.UserTotp;
import com.company.daizhang.module.system.totp.mapper.UserTotpMapper;
import com.company.daizhang.module.system.totp.service.TotpService;
import com.company.daizhang.module.system.totp.util.TotpUtil;
import com.company.daizhang.module.system.totp.vo.TotpEnableResponse;
import com.company.daizhang.module.system.totp.vo.TotpSetupVO;
import com.company.daizhang.module.system.totp.vo.TotpStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TOTP 双因素认证服务实现 (P4.2)
 * <p>
 * 备用恢复码采用简化方案:UUID 生成 10 个,以明文 JSON 数组存储,使用时明文比对并移除(一次性)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TotpServiceImpl implements TotpService {

    /** 二维码发行方名称(展示在 Authenticator App 中) */
    private static final String ISSUER = "Daizhang";
    /** 备用码生成数量 */
    private static final int BACKUP_CODE_COUNT = 10;

    private final UserTotpMapper userTotpMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public TotpSetupVO generateSecret(Long userId) {
        requireUserId(userId);
        UserTotp totp = getOrCreate(userId);
        // 若已启用,不允许重新生成密钥(需先禁用)
        if (totp.getEnabled() != null && totp.getEnabled() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "2FA 已启用,请先禁用后再重新设置");
        }

        TotpSetupVO vo = new TotpSetupVO();
        vo.setSecret(totp.getSecret());
        // 账户标识用用户名,便于用户在 Authenticator 中识别
        String account = resolveUsername(userId);
        vo.setOtpauthUrl(TotpUtil.generateOtpAuthUrl(account, totp.getSecret(), ISSUER));
        // QR 码由前端根据 otpauthUrl 生成(后端无 QR 库依赖),此字段预留 null
        vo.setQrCodeBase64(null);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TotpEnableResponse enableTotp(Long userId, String code) {
        requireUserId(userId);
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码不能为空");
        }
        UserTotp totp = userTotpMapper.selectOne(
                new LambdaQueryWrapper<UserTotp>().eq(UserTotp::getUserId, userId));
        if (totp == null || StrUtil.isBlank(totp.getSecret())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先调用 setup 生成密钥");
        }
        if (totp.getEnabled() != null && totp.getEnabled() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "2FA 已启用,无需重复启用");
        }

        // 校验验证码
        if (!TotpUtil.verify(totp.getSecret(), code.trim())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误,请重试");
        }

        // 生成 10 个备用码
        List<String> backupCodes = generateBackupCodes();

        totp.setEnabled(1);
        totp.setBackupCodes(JSONUtil.toJsonStr(backupCodes));
        totp.setEnabledAt(LocalDateTime.now());
        totp.setUpdateTime(LocalDateTime.now());
        userTotpMapper.updateById(totp);

        TotpEnableResponse resp = new TotpEnableResponse();
        resp.setEnabled(true);
        resp.setBackupCodes(backupCodes);
        log.info("用户 {} 启用 2FA 成功", userId);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableTotp(Long userId, String code) {
        requireUserId(userId);
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码不能为空");
        }
        UserTotp totp = userTotpMapper.selectOne(
                new LambdaQueryWrapper<UserTotp>().eq(UserTotp::getUserId, userId));
        if (totp == null || totp.getEnabled() == null || totp.getEnabled() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "2FA 未启用");
        }

        // 校验验证码(允许使用当前 TOTP 码或备用码禁用,避免用户丢失设备后无法关闭)
        if (!TotpUtil.verify(totp.getSecret(), code.trim()) && !consumeBackupCode(totp, code.trim())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码错误,无法禁用 2FA");
        }

        totp.setEnabled(0);
        totp.setBackupCodes(null);
        totp.setEnabledAt(null);
        totp.setUpdateTime(LocalDateTime.now());
        userTotpMapper.updateById(totp);
        log.info("用户 {} 禁用 2FA", userId);
    }

    @Override
    public boolean verifyCode(Long userId, String code) {
        if (userId == null || StrUtil.isBlank(code)) {
            return false;
        }
        UserTotp totp = userTotpMapper.selectOne(
                new LambdaQueryWrapper<UserTotp>().eq(UserTotp::getUserId, userId));
        if (totp == null || totp.getEnabled() == null || totp.getEnabled() != 1) {
            return false;
        }
        return TotpUtil.verify(totp.getSecret(), code.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyBackupCode(Long userId, String code) {
        if (userId == null || StrUtil.isBlank(code)) {
            return false;
        }
        UserTotp totp = userTotpMapper.selectOne(
                new LambdaQueryWrapper<UserTotp>().eq(UserTotp::getUserId, userId));
        if (totp == null || totp.getEnabled() == null || totp.getEnabled() != 1) {
            return false;
        }
        return consumeBackupCode(totp, code.trim());
    }

    @Override
    public TotpStatusVO getStatus(Long userId) {
        requireUserId(userId);
        UserTotp totp = userTotpMapper.selectOne(
                new LambdaQueryWrapper<UserTotp>().eq(UserTotp::getUserId, userId));
        TotpStatusVO vo = new TotpStatusVO();
        if (totp == null) {
            vo.setEnabled(false);
            vo.setSecretGenerated(false);
        } else {
            vo.setEnabled(totp.getEnabled() != null && totp.getEnabled() == 1);
            vo.setSecretGenerated(StrUtil.isNotBlank(totp.getSecret()));
        }
        return vo;
    }

    @Override
    public boolean isTwoFactorEnabled(Long userId) {
        if (userId == null) {
            return false;
        }
        UserTotp totp = userTotpMapper.selectOne(
                new LambdaQueryWrapper<UserTotp>().eq(UserTotp::getUserId, userId));
        return totp != null && totp.getEnabled() != null && totp.getEnabled() == 1;
    }

    // ==================== 内部方法 ====================

    /**
     * 获取或创建用户的 TOTP 记录。若不存在则生成新密钥并插入。
     */
    private UserTotp getOrCreate(Long userId) {
        UserTotp totp = userTotpMapper.selectOne(
                new LambdaQueryWrapper<UserTotp>().eq(UserTotp::getUserId, userId));
        if (totp == null) {
            totp = new UserTotp();
            totp.setUserId(userId);
            totp.setSecret(TotpUtil.generateSecret());
            totp.setEnabled(0);
            totp.setCreateTime(LocalDateTime.now());
            totp.setUpdateTime(LocalDateTime.now());
            userTotpMapper.insert(totp);
        }
        return totp;
    }

    /**
     * 生成 10 个备用恢复码(UUID 去掉连字符,8-4-4-4-12 → 32 位十六进制)。
     */
    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>(BACKUP_CODE_COUNT);
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            codes.add(UUID.randomUUID().toString().replace("-", ""));
        }
        return codes;
    }

    /**
     * 消耗一个备用码(匹配则从列表移除并持久化,返回 true)。
     */
    private boolean consumeBackupCode(UserTotp totp, String code) {
        if (StrUtil.isBlank(totp.getBackupCodes())) {
            return false;
        }
        List<String> codes = JSONUtil.toList(totp.getBackupCodes(), String.class);
        boolean matched = codes.remove(code);
        if (matched) {
            totp.setBackupCodes(codes.isEmpty() ? null : JSONUtil.toJsonStr(codes));
            totp.setUpdateTime(LocalDateTime.now());
            userTotpMapper.updateById(totp);
        }
        return matched;
    }

    private String resolveUsername(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user != null && StrUtil.isNotBlank(user.getUsername()) ? user.getUsername() : ("user-" + userId);
    }

    private void requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "未登录");
        }
    }
}
