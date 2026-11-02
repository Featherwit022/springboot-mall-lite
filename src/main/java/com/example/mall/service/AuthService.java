package com.example.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mall.common.BusinessException;
import com.example.mall.common.ResultCode;
import com.example.mall.dto.LoginDTO;
import com.example.mall.entity.SysUser;
import com.example.mall.mapper.SysUserMapper;
import com.example.mall.security.JwtService;
import com.example.mall.security.LoginUser;
import com.example.mall.security.Role;
import com.example.mall.vo.LoginVO;
import com.example.mall.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务。业务简单，不拆 interface，避免为演示而生造一层。
 * 这里不读 ThreadLocal：Service 只接受显式参数，保持可单测。
 */
@Service
public class  AuthService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(SysUserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    public LoginVO login(LoginDTO dto){
        SysUser user=userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername,dto.getUsername()));
        if(user==null||!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())){
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }
        LoginUser principal =new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getRole() == null ? Role.USER : user.getRole());
        LoginVO vo = new LoginVO();
        vo.setToken(jwtService.issue(principal));
        vo.setTokenType("Bearer");
        vo.setExpiresIn(jwtService.getTtlSeconds());
        vo.setUser(toVO(user));
        return vo;
    }
    public UserVO currentUser(Long userId){
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在或已禁用，请重新登录");
        }
        return toVO(user);
    }
    private UserVO toVO(SysUser user){
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole() == null ? Role.USER.name() : user.getRole().name());
        return vo;
    }
}
