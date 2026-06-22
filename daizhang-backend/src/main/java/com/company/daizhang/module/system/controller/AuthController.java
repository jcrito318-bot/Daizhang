package com.company.daizhang.module.system.controller;

import com.company.daizhang.common.config.SecurityUserDetails;
import com.company.daizhang.common.result.Result;
import com.company.daizhang.common.utils.JwtUtils;
import com.company.daizhang.module.system.dto.LoginRequest;
import com.company.daizhang.module.system.dto.LoginResponse;
import com.company.daizhang.module.system.vo.MenuVO;
import com.company.daizhang.module.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "用户登录、登出、获取用户信息")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();
            String token = jwtUtils.generateToken(userDetails.getUserId(), userDetails.getUsername());

            LoginResponse response = new LoginResponse();
            response.setToken(token);

            UserVO userVO = new UserVO();
            userVO.setId(userDetails.getUserId());
            userVO.setUsername(userDetails.getUsername());
            userVO.setPermissions(new ArrayList<>(userDetails.getPermissions()));
            response.setUserInfo(userVO);

            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return Result.error(401, "用户名或密码错误");
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout() {
        SecurityContextHolder.clearContext();
        return Result.success("登出成功", null);
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserVO> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Result.error(401, "未登录");
        }

        SecurityUserDetails userDetails = (SecurityUserDetails) authentication.getPrincipal();

        UserVO userVO = new UserVO();
        userVO.setId(userDetails.getUserId());
        userVO.setUsername(userDetails.getUsername());
        userVO.setPermissions(new ArrayList<>(userDetails.getPermissions()));

        // 临时返回空菜单列表，实际项目中应该从数据库查询
        userVO.setMenus(new ArrayList<>());
        userVO.setRoles(new ArrayList<>());

        return Result.success(userVO);
    }
}
