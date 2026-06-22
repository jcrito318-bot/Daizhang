package com.company.daizhang.module.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.daizhang.common.constant.CommonConstant;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.dto.UserCreateRequest;
import com.company.daizhang.module.system.dto.UserQueryRequest;
import com.company.daizhang.module.system.dto.UserUpdateRequest;
import com.company.daizhang.module.system.entity.SysRole;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.entity.SysUserRole;
import com.company.daizhang.module.system.mapper.SysRoleMapper;
import com.company.daizhang.module.system.mapper.SysUserMapper;
import com.company.daizhang.module.system.mapper.SysUserRoleMapper;
import com.company.daizhang.module.system.service.SysUserService;
import com.company.daizhang.module.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    
    // 手机号正则
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    // 邮箱正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$");
    
    @Override
    public PageResult<UserVO> pageUsers(UserQueryRequest request) {
        Page<SysUser> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(request.getUsername()), SysUser::getUsername, request.getUsername())
               .like(StrUtil.isNotBlank(request.getRealName()), SysUser::getRealName, request.getRealName())
               .like(StrUtil.isNotBlank(request.getPhone()), SysUser::getPhone, request.getPhone())
               .eq(request.getStatus() != null, SysUser::getStatus, request.getStatus())
               .orderByDesc(SysUser::getCreateTime);
        
        Page<SysUser> result = this.page(page, wrapper);
        
        List<UserVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }
    
    @Override
    public UserVO getUserById(Long id) {
        // 业务校验：用户ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserCreateRequest request) {
        // 业务校验：用户名不能为空
        if (StrUtil.isBlank(request.getUsername())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        
        // 业务校验：密码不能为空
        if (StrUtil.isBlank(request.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        
        // 业务校验：密码长度不能少于6位
        if (request.getPassword().length() < 6) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_TOO_SHORT);
        }
        
        // 业务校验：手机号格式校验
        if (StrUtil.isNotBlank(request.getPhone())) {
            if (!PHONE_PATTERN.matcher(request.getPhone()).matches()) {
                throw new BusinessException(ErrorCode.USER_PHONE_INVALID);
            }
        }
        
        // 业务校验：邮箱格式校验
        if (StrUtil.isNotBlank(request.getEmail())) {
            if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
                throw new BusinessException(ErrorCode.USER_EMAIL_INVALID);
            }
        }
        
        // 业务校验：用户状态值必须是0或1
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_STATUS_INVALID);
        }
        
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        
        SysUser user = new SysUser();
        BeanUtil.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        this.save(user);
        
        // 保存用户角色关联
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            // 业务校验：角色必须存在
            for (Long roleId : request.getRoleIds()) {
                SysRole role = roleMapper.selectById(roleId);
                if (role == null) {
                    throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "角色ID " + roleId + " 不存在");
                }
            }
            saveUserRoles(user.getId(), request.getRoleIds());
        }
        
        log.info("创建用户成功，用户名: {}", user.getUsername());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UserUpdateRequest request) {
        // 业务校验：用户ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 业务校验：手机号格式校验
        if (StrUtil.isNotBlank(request.getPhone())) {
            if (!PHONE_PATTERN.matcher(request.getPhone()).matches()) {
                throw new BusinessException(ErrorCode.USER_PHONE_INVALID);
            }
        }
        
        // 业务校验：邮箱格式校验
        if (StrUtil.isNotBlank(request.getEmail())) {
            if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
                throw new BusinessException(ErrorCode.USER_EMAIL_INVALID);
            }
        }
        
        // 业务校验：用户状态值必须是0或1
        if (request.getStatus() != null && request.getStatus() != 0 && request.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_STATUS_INVALID);
        }
        
        BeanUtil.copyProperties(request, user);
        this.updateById(user);
        
        // 更新用户角色关联
        if (request.getRoleIds() != null) {
            // 业务校验：角色必须存在
            for (Long roleId : request.getRoleIds()) {
                SysRole role = roleMapper.selectById(roleId);
                if (role == null) {
                    throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "角色ID " + roleId + " 不存在");
                }
            }
            
            // 删除原有角色
            LambdaQueryWrapper<SysUserRole> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SysUserRole::getUserId, id);
            userRoleMapper.delete(deleteWrapper);
            
            // 保存新角色
            if (!request.getRoleIds().isEmpty()) {
                saveUserRoles(id, request.getRoleIds());
            }
        }
        
        log.info("更新用户成功，用户ID: {}", id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        // 业务校验：用户ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 不能删除管理员
        if (CommonConstant.SUPER_ADMIN.equals(user.getUsername())) {
            throw new BusinessException(ErrorCode.USER_CANNOT_DELETE_ADMIN);
        }
        
        this.removeById(id);
        
        // 删除用户角色关联
        LambdaQueryWrapper<SysUserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserRole::getUserId, id);
        userRoleMapper.delete(wrapper);
        
        log.info("删除用户成功，用户ID: {}, 用户名: {}", id, user.getUsername());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        // 业务校验：用户ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        
        // 业务校验：新密码不能为空
        if (StrUtil.isBlank(newPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能为空");
        }
        
        // 业务校验：密码长度不能少于6位
        if (newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_TOO_SHORT);
        }
        
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
        
        log.info("重置密码成功，用户ID: {}", id);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        // 业务校验：用户ID不能为空
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        
        // 业务校验：状态值不能为空
        if (status == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值不能为空");
        }
        
        // 业务校验：状态值必须是0或1
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.USER_STATUS_INVALID);
        }
        
        SysUser user = this.getById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        user.setStatus(status);
        this.updateById(user);
        
        log.info("更新用户状态成功，用户ID: {}, 状态: {}", id, status);
    }
    
    private void saveUserRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }
    
    private UserVO convertToVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtil.copyProperties(user, vo);
        
        // 查询用户角色
        LambdaQueryWrapper<SysUserRole> urWrapper = new LambdaQueryWrapper<>();
        urWrapper.eq(SysUserRole::getUserId, user.getId());
        List<SysUserRole> userRoles = userRoleMapper.selectList(urWrapper);
        
        if (!userRoles.isEmpty()) {
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            
            List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
            vo.setRoles(roles.stream()
                    .map(SysRole::getRoleCode)
                    .collect(Collectors.toList()));
        }
        
        return vo;
    }
}
