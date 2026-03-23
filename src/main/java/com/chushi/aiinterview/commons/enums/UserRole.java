package com.chushi.aiinterview.commons.enums;

public enum UserRole {
    USER(1L),
    ADMIN(1L << 1),
    SUPER_ADMIN(1L << 2);

    private final long bit;

    UserRole(long bit) {
        this.bit = bit;
    }

    public long getBit() {
        return bit;
    }
}
