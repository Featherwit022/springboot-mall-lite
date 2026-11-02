package com.example.mall.auth;

import com.example.mall.security.JwtService;
import com.example.mall.security.LoginUser;
import com.example.mall.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 鉴权回归：只验证「拦截器这一层」的行为，不需要真实用户数据。
 * 因为 401/403 都在进入 Controller 之前就返回，不会碰数据库。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthGuardTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("不带 token 访问 /api/orders 返回 4010")
    void 无凭证应返回未登录() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(jsonPath("$.code").value(4010));
    }

    @Test
    @DisplayName("篡改签名的 token 返回 4010")
    void 伪造令牌应返回未登录() throws Exception {
        String token = jwtService.issue(new LoginUser(1L, "tom", Role.USER));
        String forged = token.substring(0, token.length() - 2) + "xx";

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + forged)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(jsonPath("$.code").value(4010));
    }

    @Test
    @DisplayName("普通用户调管理员接口返回 4030（不是 4010）")
    void 角色不足应返回无权限() throws Exception {
        String token = jwtService.issue(new LoginUser(1L, "tom", Role.USER));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"categoryId\":1,\"price\":1,\"stock\":1}"))
                .andExpect(jsonPath("$.code").value(4030));
    }

    @Test
    @DisplayName("登录接口免登录：能走到业务层，返回的是凭据错误而不是未登录")
    void 登录接口应放行() throws Exception {
        // 这个用例专门守 @Public 的 @Retention(RUNTIME)：
        // 一旦注解在运行期读不到，拦截器就会在进入 Controller 前抛出「未登录，请先登录」，
        // 与这里断言的「用户名或密码错误」不是同一句话，改动会立刻失败。
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no-such-user\",\"password\":\"whatever123\"}"))
                .andExpect(jsonPath("$.code").value(4010))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("公开接口无需 token")
    void 公开接口应放行() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/ping"))
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
