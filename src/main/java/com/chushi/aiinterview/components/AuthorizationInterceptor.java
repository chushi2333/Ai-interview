package com.chushi.aiinterview.components;

import com.chushi.aiinterview.annotations.NoAuth;
import com.chushi.aiinterview.annotations.RequireRole;
import com.chushi.aiinterview.commons.utils.UserRoles;
import com.chushi.aiinterview.commons.vo.Response;
import com.chushi.aiinterview.entities.User;
import com.chushi.aiinterview.services.impl.UserService;
import com.chushi.aiinterview.commons.utils.JwtUtil;
import com.chushi.aiinterview.commons.utils.RedisJwtUtil;
import com.chushi.aiinterview.commons.utils.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

/**
 * 权限拦截器
 * 处理 @RoleRequired 和 @NoAuth 注解
 */
@Component
@Slf4j
public class AuthorizationInterceptor implements HandlerInterceptor {
    @Resource
    private UserService userService;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private RedisJwtUtil redisJwtUtil;

    /// 处理错误
    public static void handleException(HttpServletResponse response, Exception e) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        var errorResponse = Response.builder().code(4).message(e.getMessage()).build();

        var mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(errorResponse));
    }

    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 检查方法级别的 @NoAuth 注解
        var methodNoAuth = handlerMethod.getMethodAnnotation(NoAuth.class);
        // 检查类级别的 @NoAuth 注解
        var classNoAuth = handlerMethod.getBeanType().getAnnotation(NoAuth.class);

        // 如果标记 @NoAuth 跳过权限检查
        if (methodNoAuth != null || classNoAuth != null) {
            return true;
        }

        // 进行JWT认证
        var user = authenticateJWT(request, response);
        if (user.isEmpty()) {
            return false;
        }

        // 检查方法级别的 @RoleRequired 注解
        var methodRoleRequired = handlerMethod.getMethodAnnotation(RequireRole.class);
        // 检查类级别的 @RoleRequired 注解
        var classRoleRequired = handlerMethod.getBeanType().getAnnotation(RequireRole.class);

        // 先检查方法是否有 @RoleRequired 注解 否则使用类层面的
        var roleRequired = methodRoleRequired != null ? methodRoleRequired : classRoleRequired;

        if (roleRequired != null) {
            return checkRolePermission(response, user.get(), roleRequired);
        }

        return true;

    }

    @Override
    public void afterCompletion(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler, Exception ex) {
        // 请求处理完毕，清理ThreadLocal
        UserContext.clear();
    }

    // 检查角色权限
    private boolean checkRolePermission(HttpServletResponse response, User user, RequireRole requireRole) throws IOException {
        try {
            // 获取用户角色
            var userRoles = new UserRoles(user.getRoles());
            var requiredRoles = requireRole.value();

            // 检查权限
            // 检查权限
            var hasPermission = false;
            if (requireRole.predicate() == RequireRole.Predicate.AND) {
                // AND 逻辑：用户必须拥有所有要求的角色
                hasPermission = userRoles.hasAll(requiredRoles);
            } else {
                // OR 逻辑：用户只需拥有其中一个角色
                hasPermission = userRoles.hasAny(requiredRoles);
            }
            if (!hasPermission) {
                handleException(response, new Exception("Permission denied"));
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("PermissionCheckException: {}", e.getMessage(), e);
            handleException(response, new Exception("Permission check failed"));

            return false;
        }
    }


    // 校验 JWT
    private Optional<User> authenticateJWT(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            var authHeader = request.getHeader("Authorization");

            // 检查 请求头
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new JwtException("Authorization header missing");
            }

            var token = authHeader.substring(7);

            // 验证token有效性和是否过期
            if (!jwtUtil.validateToken(token) || jwtUtil.isTokenExpired(token)) {
                throw new JwtException("Invalid token");
            }

            var userId = jwtUtil.getUserIdFromToken(token);
            var redisToken = redisJwtUtil.getUserToken(userId);

            // 对比 Redis 中的 token
            if (redisToken.isEmpty() || !redisToken.get().equals(token)) {
                throw new JwtException("Invalid token");
            }

            // 从数据库中获取 用户信息
            var user = userService.getUserById(userId).orElseThrow(() -> new JwtException("User not found"));

            // 设置用户上下文
            UserContext.setUser(user);

            // 返回 User
            return Optional.of(user);
        } catch (JwtException e) {
            handleException(response, e);
        } catch (Exception e) {
            log.error("JwtFilterException: {}", e.getMessage(), e);
            handleException(response, new JwtException("Authentication Failed"));
        }

        return Optional.empty();
    }

}
