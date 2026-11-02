package com.example.mall.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 一次性工具：打印 BCrypt 哈希，复制到 sql/auth.sql 里初始化账号。
 * 每次运行的哈希都不同（盐随机），属正常现象；只要用的是「最后一次运行」的输出即可。
 * 运行：mvn -q -Dtest=PasswordHashPrinterTest test
 */
public class PasswordHashPrinterTest {

    @Test
    void 测试BCrypt哈希() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String h1 = encoder.encode("admin123");
        String h2 = encoder.encode("tom123");

        // 自检：同一明文两次编码结果必须不同（盐生效），但都必须能校验通过
        System.out.println("盐生效: " + (!h1.equals(h2))
                + ", 校验通过: " + (encoder.matches("admin123", h1) && encoder.matches("tom123", h2)));
        System.out.println("admin / admin123 -> " + h1);
        System.out.println("tom   / tom123   -> " + h2);
    }
}
