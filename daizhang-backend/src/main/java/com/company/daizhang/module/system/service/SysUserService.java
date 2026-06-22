package com.company.daizhang.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.dto.UserCreateRequest;
import com.company.daizhang.module.system.dto.UserQueryRequest;
import com.company.daizhang.module.system.dto.UserUpdateRequest;
import com.company.daizhang.module.system.entity.SysUser;
import com.company.daizhang.module.system.vo.UserVO;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {
    
    /**
     * 分页查询用户
     */
    PageResult<UserVO> pageUsers(UserQueryRequest request);
    
    /**
     * 根据ID查询用户
     */
    UserVO getUserById(Long id);
    
    /**
     * 创建用户
     */
    void createUser(UserCreateRequest request);
    
    /**
     * 更新用户
     */
    void updateUser(Long id, UserUpdateRequest request);
    
    /**
     * 删除用户
     */
    void deleteUser(Long id);
    
    /**
     * 重置密码
     */
    void resetPassword(Long id, String newPassword);
    
    /**
     * 更新状态
     */
    void updateStatus(Long id, Integer status);
}
