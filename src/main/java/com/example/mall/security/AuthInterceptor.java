package com.example.mall.security;

import com.example.mall.annotation.Public;
import com.example.mall.annotation.RequireRole;
import com.example.mall.common.BusinessException;
import com.example.mall.common.ResultCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 认证 + 授权拦截器。
 * <p>
 * 为什么用 Interceptor 而不是 Filter：
 * Filter 在 DispatcherServlet 之前，拿不到 HandlerMethod，也就无法读取 @RequireRole；
 * 而 Interceptor 能拿到「即将执行的方法」，授权判断才自然。
 * 需要更早介入的横切（traceId、编码）才用 Filter——见步骤 7。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 非 Controller 方法（静态资源、/error 转发）不参与鉴权
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        // CORS 预检请求不带 Authorization 头，直接放行，否则浏览器永远拿不到跨域许可
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        if (isPublic(handlerMethod)) {
            return true;
        }

        LoginUser user = authenticate(request);
        checkRole(handlerMethod, user);

        // 关键顺序：认证、授权都通过之后才写入上下文。
        // 若先 set 再抛异常，本次 preHandle 失败，afterCompletion 不会被调用，上下文就泄漏了。
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 无论 Controller 成功还是抛异常都会走到这里，这是清理 ThreadLocal 的唯一可靠时机
        UserContext.clear();
    }

    private boolean isPublic(HandlerMethod handlerMethod) {
        return handlerMethod.getMethodAnnotation(Public.class) != null
                || handlerMethod.getBeanType().isAnnotationPresent(Public.class);
    }

    private LoginUser authenticate(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录，请先登录");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录，请先登录");
        }
        try {
            return jwtService.parse(token);
        } catch (ExpiredJwtException e) {
            // 过期单独提示，前端可据此触发「刷新 token / 重新登录」流程
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            // 签名错、issuer 不符、结构损坏、role 值非法都落这里
            log.warn("token 校验失败 uri={} reason={}", request.getRequestURI(), e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态无效，请重新登录");
        }
    }

    private void checkRole(HandlerMethod handlerMethod, LoginUser user) {
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            // 方法上没有就看类上，支持「整个 Controller 都要管理员」的写法
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return;   // 没有声明，只要求登录，不限制角色
        }
        if (!user.hasAnyRole(requireRole.value())) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "无权限访问该接口，需要角色: " + java.util.Arrays.toString(requireRole.value()));
        }
    }
}
