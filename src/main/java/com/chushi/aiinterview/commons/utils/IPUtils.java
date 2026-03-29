package com.chushi.aiinterview.commons.utils;

import jakarta.servlet.http.HttpServletRequest;

public class IPUtils {
    public static String getIpAddress(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
