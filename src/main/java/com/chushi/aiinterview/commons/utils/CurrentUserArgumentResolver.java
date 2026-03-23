package com.chushi.aiinterview.commons.utils;

import com.chushi.aiinterview.annotations.CurrentUser;
import jakarta.annotation.Nonnull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, @Nonnull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (parameter.hasParameterAnnotation(CurrentUser.class)) {
            return UserContext.getUser();
        }
        return null;
    }
}
