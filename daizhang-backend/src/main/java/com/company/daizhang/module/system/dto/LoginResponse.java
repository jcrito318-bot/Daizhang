package com.company.daizhang.module.system.dto;

import com.company.daizhang.module.system.vo.UserVO;
import lombok.Data;

/**
 * 登录响应
 */
@Data
public class LoginResponse {

    private String token;
    private UserVO userInfo;
}
