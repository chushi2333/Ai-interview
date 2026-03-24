package com.chushi.aiinterview.configurations;

import com.chushi.aiinterview.components.AuthorizationInterceptor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
@Slf4j
public class AuthInterceptorConfiguration implements WebMvcConfigurer {
    @Resource
    private AuthorizationInterceptor authorizationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册认证拦截器
        registry.addInterceptor(authorizationInterceptor)
                // 添加所有路径
                .addPathPatterns("/**")
                // 排除 openapi 和 swagger 路径
                .excludePathPatterns("/swagger-ui/**", "/api-docs/**");
    }
}
