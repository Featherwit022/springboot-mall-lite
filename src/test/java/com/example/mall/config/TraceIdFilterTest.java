package com.example.mall.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * traceId 回归：验证过滤器确实挂在链上、MDC 里能取到值、响应头与响应体一致。
 * 只用一个 4010 响应做验证，不碰数据库写操作。
 */
@SpringBootTest
@AutoConfigureMockMvc
class TraceIdFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("未带 traceId 时自动生成，并同时写回响应头与 Result")
    void 应自动生成traceId() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.code").value(4010))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("上游带了 traceId 就复用，保证跨服务可串联")
    void 应复用上游traceId() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("X-Trace-Id", "trace-from-gateway"))
                .andExpect(header().string("X-Trace-Id", "trace-from-gateway"))
                .andExpect(jsonPath("$.traceId").value("trace-from-gateway"));
    }
}
