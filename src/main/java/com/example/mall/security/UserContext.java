package com.example.mall.security;

import com.example.mall.common.BusinessException;
import com.example.mall.common.ResultCode;

public final class UserContext {
    private static final ThreadLocal<LoginUser> HOLDER=new ThreadLocal<>();
    private UserContext() {
    }
    public static void set(LoginUser user){
        HOLDER.set(user);
    }
    public static LoginUser get(){
        return HOLDER.get();
    }
    public static LoginUser require(){
        LoginUser user=HOLDER.get();
        if(user==null){
            throw new BusinessException(ResultCode.UNAUTHORIZED,"未登录，请先登录");
        }
        return user;
    }
    public static void clear(){
        HOLDER.remove();
    }
}
