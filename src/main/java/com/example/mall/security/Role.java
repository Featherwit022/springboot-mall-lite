package com.example.mall.security;

public enum Role {
    USER,
    ADMIN;
    //根据业务需求选择是否留下this == ADMIN
    public boolean covers(Role required) {
        return this == ADMIN || this == required;
    }
}
