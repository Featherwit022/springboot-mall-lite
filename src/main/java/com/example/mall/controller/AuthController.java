package com.example.mall.controller;


import com.example.mall.annotation.Public;
import com.example.mall.common.Result;
import com.example.mall.dto.LoginDTO;
import com.example.mall.security.UserContext;
import com.example.mall.service.AuthService;
import com.example.mall.vo.LoginVO;
import com.example.mall.vo.UserVO;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @Public
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.currentUser(UserContext.require().getUserId()));
    }
    /**
     * 登出。无状态 JWT 没有服务端会话可销毁，
     * 真正的「失效」由客户端删除 token 完成，服务端只能做「短暂宽限期」。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
