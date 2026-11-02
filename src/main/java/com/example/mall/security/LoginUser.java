package com.example.mall.security;

public class LoginUser {
    private final Long userId;
    private final String username;
    private final Role role;
    public LoginUser(Long userId, String username, Role role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }
    public boolean hasAnyRole(Role... required) {
        if (required == null || required.length == 0) {
            return true;
        }
        for (Role r : required) {
            if (role.covers(r)) {
                return true;
            }
        }
        return false;
    }
}
