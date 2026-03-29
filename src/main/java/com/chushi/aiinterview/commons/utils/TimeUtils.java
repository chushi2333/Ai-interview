package com.chushi.aiinterview.commons.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class TimeUtils {
    public static Long toUnixTimestampSeconds(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.toEpochSecond(ZoneOffset.UTC);
    }

    public static LocalDateTime fromUnixTimestampSeconds(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC);
    }

    public static Long currentUnixTimestampSeconds() {
        return LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    }

    public static LocalDateTime currentLocalDateTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
