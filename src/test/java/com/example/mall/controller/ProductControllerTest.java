package com.example.mall.controller;

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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc            // 提供 MockMvc，不真的启动 Tomcat
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @Test
    @DisplayName("page=0 时返回 code 4000，不是 500")
    void 分页参数非法应返回参数错误码() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    @Test
    @DisplayName("创建商品缺 name 时返回 4000 并带字段名")
    void 创建商品缺字段应返回参数错误() throws Exception {
        // 参数校验发生在 Controller 内部，所以必须先通过鉴权，用 ADMIN token
        String adminToken = jwtService.issue(new LoginUser(1L, "admin", Role.ADMIN));

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":1,\"price\":9.9,\"stock\":10}"))
                .andExpect(jsonPath("$.code").value(4000))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("name")));
    }
}