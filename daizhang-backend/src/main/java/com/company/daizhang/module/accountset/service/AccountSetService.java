package com.company.daizhang.module.accountset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.accountset.dto.AccountSetCreateRequest;
import com.company.daizhang.module.accountset.dto.AccountSetQueryRequest;
import com.company.daizhang.module.accountset.dto.AccountSetUpdateRequest;
import com.company.daizhang.module.accountset.entity.AccountSet;
import com.company.daizhang.module.accountset.vo.AccountSetVO;

import java.util.List;

/**
 * 账套服务接口
 */
public interface AccountSetService extends IService<AccountSet> {
    
    /**
     * 分页查询账套
     */
    PageResult<AccountSetVO> pageAccountSets(AccountSetQueryRequest request);
    
    /**
     * 查询所有账套
     */
    List<AccountSetVO> listAllAccountSets();
    
    /**
     * 根据ID查询账套
     */
    AccountSetVO getAccountSetById(Long id);
    
    /**
     * 创建账套
     */
    void createAccountSet(AccountSetCreateRequest request);
    
    /**
     * 更新账套
     */
    void updateAccountSet(Long id, AccountSetUpdateRequest request);

    /**
     * 启用账套
     */
    void enableAccountSet(Long id);

    /**
     * 停用账套
     */
    void disableAccountSet(Long id);

    /**
     * 删除账套
     */
    void deleteAccountSet(Long id);
    
    /**
     * 初始化账套（创建会计期间和默认科目）
     */
    void initAccountSet(Long id);
}
