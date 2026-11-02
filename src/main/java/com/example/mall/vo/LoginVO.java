package com.example.mall.vo;

public class LoginVO {
    private String token;
    /** 固定 "Bearer"，前端拼 Authorization 头时不用硬编码 */
    private String tokenType;
    /** 剩余有效秒数，前端可据此提前静默续期 */
    private long expiresIn;
    private UserVO user;


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }
}
