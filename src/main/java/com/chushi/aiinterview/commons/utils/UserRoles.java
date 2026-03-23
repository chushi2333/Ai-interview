package com.chushi.aiinterview.commons.utils;

import com.chushi.aiinterview.commons.enums.UserRole;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * 用户角色集合工具类。
 */
public class UserRoles {
    /**
     * 内部存储用户拥有的角色集合。
     */
    private final EnumSet<UserRole> roles;

    /**
     * 默认构造函数，创建一个不包含任何角色的 UserRoles 实例。
     */
    public UserRoles() {
        this.roles = EnumSet.noneOf(UserRole.class);
    }

    /**
     * 通过位标志（bit flags）构造 UserRoles 实例。
     * <p>
     * 每个 UserRole 枚举值应定义其对应的位掩码。
     *
     * @param bitFlags 表示角色集合的位标志
     */
    public UserRoles(long bitFlags) {
        this.roles = EnumSet.noneOf(UserRole.class);
        for (UserRole role : UserRole.values()) {
            if ((bitFlags & role.getBit()) != 0) {
                this.roles.add(role);
            }
        }
    }

    /**
     * 判断两个 UserRoles 实例是否相等。
     * 基于内部 roles 集合的 equals 比较。
     *
     * @param o 待比较的对象
     * @return 若 roles 集合内容相同则返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserRoles that)) {
            return false;
        }
        return this.roles.equals(that.roles);
    }

    /**
     * 判断当前用户是否拥有所有指定的角色。
     *
     * @param roles 待检查的角色列表
     * @return 若全部包含则返回 true，否则返回 false
     */
    public boolean hasAll(@NotNull UserRole... roles) {
        return Arrays.stream(roles).allMatch(this.roles::contains);
    }

    /**
     * 判断当前用户是否拥有任意一个指定的角色。
     *
     * @param roles 待检查的角色列表
     * @return 若包含至少一个则返回 true，否则返回 false
     */
    public boolean hasAny(@NotNull UserRole... roles) {
        return Arrays.stream(roles).anyMatch(this.roles::contains);
    }

    /**
     * 生成当前对象的哈希码。
     * 基于内部 roles 集合的 hashCode。
     *
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return roles.hashCode();
    }

    /**
     * 返回当前角色集合的字符串表示。
     * 格式由 EnumSet 的 toString() 决定，例如：[ADMIN, USER]
     *
     * @return 角色集合的字符串表示
     */
    @Override
    public String toString() {
        return roles.toString();
    }
}
