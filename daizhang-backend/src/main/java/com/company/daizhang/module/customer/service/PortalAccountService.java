package com.company.daizhang.module.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.module.customer.dto.PortalAccountRequest;
import com.company.daizhang.module.customer.entity.PortalAccount;
import com.company.daizhang.module.customer.vo.PortalAccountVO;

import java.util.List;

/**
 * 客户看账门户服务接口
 */
public interface PortalAccountService extends IService<PortalAccount> {

    /**
     * 查询客户门户列表
     */
    List<PortalAccountVO> listPortals(Long customerId);

    /**
     * 创建门户账户(密码BCrypt加密)
     */
    void createPortal(PortalAccountRequest request);

    /**
     * 更新门户账户
     */
    void updatePortal(Long id, PortalAccountRequest request);

    /**
     * 删除门户账户
     */
    void deletePortal(Long id);

    /**
     * 重置密码
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 更新状态
     */
    void updateStatus(Long id, Integer status);
}
