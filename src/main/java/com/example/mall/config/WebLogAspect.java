package com.example.mall.config;


import com.example.mall.annotation.CostTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;


@Aspect
@Component
@Order(1)
public class WebLogAspect {
    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);
    // controller 包及子包下的所有方法，任意返回值、任意参数
    @Pointcut("execution(* com.example.mall.controller..*.*(..))")
    public void controllerMethods() {}
    @Around("controllerMethods()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        String uri = currentUri();
        String sig = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();

        log.info("→ {} {} params={}", uri, sig, safeArgs(pjp));
        try {
            Object result = pjp.proceed();          // 真正执行目标方法
            log.info("← {} {} {}ms", uri, sig, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            log.warn("✗ {} {} {}ms err={}", uri, sig,
                    System.currentTimeMillis() - start, e.getMessage());
            throw e;                           // ← 关键：必须抛出去
        }
    }
    // 加在 WebLogAspect 里：只切"打了 @CostTime 的方法"
    @Around("execution(* com.example.mall.service..*.*(..))&&@annotation(costTime)")
    public Object costAround(ProceedingJoinPoint pjp, CostTime costTime) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            log.info("[{}] {} 耗时 {}ms", costTime.value(), pjp.getSignature().getName(), cost);
        }
    }
    private String currentUri() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? "-" : attrs.getRequest().getRequestURI();
    }
    private String safeArgs(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) {
            return "[]";
        }
        String[] parts = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof com.example.mall.dto.LoginDTO) {
                com.example.mall.dto.LoginDTO dto = (com.example.mall.dto.LoginDTO) arg;
                parts[i] = "LoginDTO(username=" + dto.getUsername() + ", password=***)";
            } else {
                parts[i] = String.valueOf(arg);
            }
        }
        return Arrays.toString(parts);
    }
}
