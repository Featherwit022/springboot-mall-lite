package com.example.mall.config;

import com.example.mall.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册鉴权拦截器。
 * 只覆盖 /api/**（actuator 在 /actuator 下，天然不受影响）；
 * 需要公开的接口不在这里配路径，而是在方法上打 @Public——
 * 这样「为什么公开」写在代码旁边，评审时一眼能看到。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .order(0);   // 排在日志/其他拦截器之前
    }
}
