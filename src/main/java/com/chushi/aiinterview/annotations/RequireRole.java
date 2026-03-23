package com.chushi.aiinterview.annotations;

import com.chushi.aiinterview.commons.enums.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /// 需要的角色名称
    UserRole[] value();

    /// 谓词
    Predicate predicate() default Predicate.OR;

    /// 谓词类型
    enum Predicate {
        AND,
        OR,
    }
}
