package com.example.mall.controller;

import com.example.mall.annotation.Public;
import com.example.mall.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ping")
public class PingController {
    @Public
    @GetMapping()
    public Map<String, Object> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("app", "mall");
        return body;   // 容器会自动把它转成 JSON
    }
    @GetMapping("/slow")
    public Result<String> slow() throws InterruptedException {
        Thread.sleep(8000);
        return Result.ok("done");
    }
}